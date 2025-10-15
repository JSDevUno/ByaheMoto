import asyncio
from datetime import datetime, timedelta
from typing import Any, Dict, Tuple, Optional

from geopy.distance import geodesic
from prisma import enums

from .. import repositories
from ..schemas import models
from ..schemas.base import Base
from ..utils.logger import logger


class LocationUpdate(Base):
    driver_id: int
    lat: float
    lng: float


class QueueItem:
    prio_map = {
        enums.IdentificationType.NONE: 0,
        enums.IdentificationType.STUDENT: 1,
        enums.IdentificationType.PWD: 2,
        enums.IdentificationType.SENIOR: 3,
    }

    def __init__(self, booking: models.Booking):
        self.booking = booking

        if not booking.user:
            raise ValueError('Booking must have a user, include it in the query')

        self.priority = self.prio_map.get(booking.user.identity.type, 0) if booking.user.identity else 0

    def __lt__(self, other: 'QueueItem') -> bool:
        return self.priority > other.priority


class BookingQueue:
    __shared_state = {}
    __instance = None

    def __init__(self):
        # TODO: Add data persistence for queues in case of server restart, maybe use Redis and Celery for this to reduce memory usage
        self.main_queue: asyncio.PriorityQueue[QueueItem] = asyncio.PriorityQueue()
        self.driver_queues: Dict[int, asyncio.PriorityQueue[QueueItem]] = {}
        self.driver_locations: Dict[int, Tuple[float, float]] = {}
        self.accepted_bookings = {}
        self.booking_repo = repositories.Booking()
        self.user_repo = repositories.User()

    async def update_location(self, location: LocationUpdate):
        self.driver_locations[location.driver_id] = (location.lat, location.lng)

    async def add(self, booking: models.Booking):
        logger.debug(f'Adding <Booking [{booking.id}]> to queue...')
        await self.main_queue.put(QueueItem(booking))

    async def dispatch(self):
        logger.info('Starting booking queue dispatcher...')
        main_queue = self.main_queue
        driver_queues = self.driver_queues

        while True:
            if not main_queue.empty():
                item = await main_queue.get()
                driver_id = await self.find_suitable_driver(item.booking.id)

                if driver_id:
                    logger.debug(f'Suitable driver found <Driver [{driver_id}]> for <Booking [{item.booking.id}]>')

                    driver_queue = driver_queues.get(driver_id)
                    await driver_queue.put(item)
                    item.booking = await self.booking_repo.update(item.booking.id, {'driver_id': driver_id})
                else:
                    # TODO: Add a fallback when no suitable driver is found, maybe a timeout or a notification to the user
                    logger.debug(f'No suitable driver found for <Booking [{item.booking.id}]>')
                    await main_queue.put(item)  # Requeue if no suitable driver found

            await asyncio.sleep(5)  # Avoid busy waiting

    async def process_all(self):
        logger.debug('Creating driver queues...')
        drivers = await self.user_repo.all(filters={'role': enums.Role.DRIVER})

        for driver in drivers:
            await self.create_driver_queue(driver)

        logger.info('Queue processor started')

    async def find_suitable_driver(self, booking_id: int) -> Optional[int]:
        user_repository = repositories.User()
        booking = await self.booking_repo.get(booking_id)

        if not booking:
            return None

        drivers = await user_repository.all(filters={
            'vehicle_type': booking.vehicle_type,
            'role': enums.Role.DRIVER
        })

        if not drivers:
            return None

        pickup = booking.location_from
        pickup_coords = (pickup['lat'], pickup['lng'])

        def distance_to_pickup(driver: models.User) -> float:  # TODO: refactor this to not be a nested function
            driver_location = driver.last_known_location

            if not driver_location:
                return float('inf')

            driver_location_coords = (driver_location['lat'], driver_location['lng'])

            return geodesic(driver_location_coords, pickup_coords).meters

        drivers = sorted(drivers, key=distance_to_pickup)

        for d in drivers:
            if d.id == booking.driver_id and booking.status == enums.Status.REJECTED:
                continue

            # check the distance between the driver and the pickup location
            if distance_to_pickup(d) <= 5000:  # 5km radius
                return d.id

        return None

    async def create_driver_queue(self, driver: Any):
        driver_queue = asyncio.PriorityQueue()
        last_known_location = driver.last_known_location
        self.driver_queues[driver.id] = driver_queue
        self.driver_locations[driver.id] = (
            last_known_location['lat'], last_known_location['lng']
        ) if last_known_location else (float('inf'), float('inf'))

        logger.debug(f'Starting driver queue processor for <Driver [{driver.id}]>...')
        asyncio.create_task(self.process_driver_bookings(driver.id))  # noqa
        logger.debug(f'Driver queue processor for <Driver [{driver.id}]> started')

    async def process_driver_bookings(self, driver_id: int):
        main_queue = self.main_queue
        driver_queues = self.driver_queues

        driver_queue = driver_queues.get(driver_id)
        if not driver_queue:
            driver_queue = asyncio.PriorityQueue()
            driver_queues[driver_id] = driver_queue

        while True:
            item = await driver_queue.get()

            try:
                await self.wait_for_booking_acceptance(item.booking.id)
                await self.process_booking(item)

            except (TimeoutError, ValueError) as e:  # noqa
                await main_queue.put(item)  # assign to another driver

            except Exception as e:  # noqa
                await driver_queue.put(item)

            await asyncio.sleep(5)

    async def wait_for_booking_acceptance(self, booking_id: int):
        repo = self.booking_repo
        timeout = timedelta(minutes=5)
        start_time = datetime.now()

        logger.debug(f'Waiting for booking acceptance <Booking [{booking_id}]>...')
        while True:
            logger.debug(f'<Booking [{booking_id}]> {datetime.now() - start_time} seconds after start')
            if datetime.now() - start_time >= timeout:
                await repo.update(booking_id, {
                    'status': enums.BookingStatus.CANCELLED
                })
                # TODO:  notify user
                raise TimeoutError('Booking acceptance timeout')

            booking = await repo.get(booking_id)

            if not booking:
                await asyncio.sleep(5)
                continue

            if booking.status == enums.BookingStatus.ACCEPTED:
                logger.info(f'<Booking [{booking_id}]> accepted')
                break

            if booking.status == enums.BookingStatus.REJECTED:
                raise ValueError('Booking rejected')

            await  asyncio.sleep(5)

    async def process_booking(self, queue_item: QueueItem):
        booking_repo = self.booking_repo
        user_repo = self.user_repo
        booking = queue_item.booking
        destination = booking.location_to
        pickup = booking.location_from

        destination_coords = (destination['lat'], destination['lng'])
        pickup_coords = (pickup['lat'], pickup['lng'])

        await booking_repo.update(booking.id, {
            'status': enums.BookingStatus.PICKING
        })

        await self.wait_until_location_reached(booking.driver_id, pickup_coords)

        await booking_repo.update(booking.id, {
            'status': enums.BookingStatus.PICKED_UP
        })

        await self.wait_until_location_reached(booking.driver_id, destination_coords)

        # Database updates
        await booking_repo.update(booking.id, {
            'status': enums.BookingStatus.DROPPED
        })

        if booking.mode_of_payment != enums.PaymentMode.WALLET:
            return

        await user_repo.add_balance(booking.driver_id, booking.fare)

    async def wait_until_location_reached(self, driver_id: int, coords: Tuple[float, float], radius: float = 50):
        while True:
            driver_location = self.driver_locations.get(driver_id, (0, 0))
            distance = geodesic(driver_location, coords).meters

            if distance <= radius:
                break

            await asyncio.sleep(5)

    def __new__(cls, *args, **kwargs):  # singleton pattern
        if not cls.__instance:
            cls.__instance = super(BookingQueue, cls).__new__(cls)

        return cls.__instance


booking_queue = BookingQueue()
