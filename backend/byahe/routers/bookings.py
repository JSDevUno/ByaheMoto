from typing import Annotated

from fastapi import APIRouter, BackgroundTasks, status
from fastapi.params import Depends
from fastapi.responses import StreamingResponse
from fastapi_utils.cbv import cbv
from prisma import enums

from ..repositories import Booking, User
from ..schemas import models, request
from ..utils import jwt
from ..utils.events import Emitter
from ..utils.fare import calculate_fare
from ..utils.queue import BookingQueue, LocationUpdate, booking_queue
from ..utils.response import ResponseBuilder

router = APIRouter(
    prefix='/bookings',
    tags=['Bookings'],
    dependencies=[Depends(jwt.is_authenticated)]
)

repository = Booking()
user_repository = User()
emitter = Emitter()


@cbv(router)
class Bookings:
    @router.get('/')
    async def all(self, user: Annotated[models.User, Depends(jwt.is_driver)]):
        bookings = await repository.all(
            filters={'driver_id': user.id} if user.role == enums.Role.DRIVER else None,
            order={'created_at': 'desc'},
            include={'user': True}
        )

        return ResponseBuilder.build_success(
            'Bookings retrieved',
            data=[booking.model_dump() for booking in bookings]
        )

    @router.get('/{booking_id}')
    async def get(self, booking_id: int):
        booking = await repository.get(booking_id, include={'user': True})

        if not booking:
            raise ResponseBuilder.build_not_found('Booking not found')

        return ResponseBuilder.build_success('Booking retrieved', data=booking.model_dump())

    @router.post('/', status_code=status.HTTP_201_CREATED)
    async def create(self, data: request.BookingCreate, user: Annotated[models.User, Depends(jwt.is_authenticated)]):
        transaction = None
        try:
            fare = calculate_fare(data.vehicle_type, data.location_from, data.location_to)
        except ValueError as e:
            raise ResponseBuilder.build_bad_request(str(e))

        if data.payment_method == enums.PaymentMode.WALLET:
            try:
                transaction = await user_repository.deduct_balance(user.id, fare)
            except ValueError as e:
                raise ResponseBuilder.build_bad_request(str(e))

            if not transaction:
                raise ResponseBuilder.build_internal_server_error('Failed to create transaction')

        booking = await repository.create({
            **data.model_dump(exclude={'location_from', 'location_to', 'payment_method'}),
            'location_from': data.location_from.model_dump_json(),
            'location_to': data.location_to.model_dump_json(),
            'user_id': user.id,
            'fare': fare,
            'mode_of_payment': data.payment_method,
            'transaction_id': transaction.id if transaction else None
        },
            include={'user': {
                'include': {
                    'identity': True
                }
            }}  # type: ignore # noqa
        )

        await booking_queue.add(booking)

        return ResponseBuilder.build_success('Booking created', data=booking.model_dump())

    @router.put('/{booking_id}/accept')
    async def accept(self, booking_id: int, driver: Annotated[models.User, Depends(jwt.is_driver)]):
        bookings = await repository.all(filters={'driver_id': driver.id, 'AND': [
            {'status': enums.BookingStatus.PICKED_UP},
            {'status': enums.BookingStatus.PICKING},
            {'status': enums.BookingStatus.ACCEPTED}
        ]})

        if bookings:
            raise ResponseBuilder.build_bad_request('You can only accept one booking at a time')

        booking = await repository.get(booking_id)

        if not booking:
            raise ResponseBuilder.build_not_found('Booking not found')

        if booking.driver_id != driver.id:
            raise ResponseBuilder.build_unauthorized('You are not assigned to this booking')

        if booking.status != enums.BookingStatus.PENDING:
            raise ResponseBuilder.build_bad_request('Booking is not pending')

        booking = await repository.update(
            booking_id,
            data={'status': enums.BookingStatus.ACCEPTED},
        )

        return ResponseBuilder.build_success('Booking accepted', data=booking.model_dump())

    @router.put('/{booking_id}/reject')
    async def reject(self, booking_id: int, driver: Annotated[models.User, Depends(jwt.is_driver)]):
        booking = await repository.get(booking_id)

        if not booking:
            raise ResponseBuilder.build_not_found('Booking not found')

        if booking.driver_id != driver.id:
            raise ResponseBuilder.build_unauthorized('You are not assigned to this booking')

        if booking.status != enums.BookingStatus.PENDING:
            raise ResponseBuilder.build_bad_request('Booking already is not pending')

        booking = await repository.update(
            booking_id,
            data={'status': enums.BookingStatus.REJECTED}
        )

        return ResponseBuilder.build_success('Booking accepted', data=booking.model_dump())

    @router.put('/{booking_id}/cancel')
    async def cancel(self, booking_id: int, user: Annotated[models.User, Depends(jwt.is_authenticated)]):
        booking = await repository.get(booking_id)

        if not booking:
            raise ResponseBuilder.build_not_found('Booking not found')

        if booking.user_id != user.id:
            raise ResponseBuilder.build_unauthorized('You are not the owner of this booking')

        if booking.status != enums.BookingStatus.PENDING:
            raise ResponseBuilder.build_bad_request('Booking is not pending')

        booking = await repository.update(
            booking_id,
            data={'status': enums.BookingStatus.CANCELLED}
        )

        return ResponseBuilder.build_success('Booking cancelled', data=booking.model_dump())

    @router.post('/location')
    async def update_location(
            self,
            data: request.Location,
            driver: Annotated[models.User, Depends(jwt.is_driver)],
            background_tasks: BackgroundTasks
    ):
        background_tasks.add_task(
            user_repository.update,
            driver.id, {'last_known_location': data.model_dump_json()}, include=None  # type: ignore
        )

        location_update = LocationUpdate(driver_id=driver.id, **data.model_dump())
        emitter.emit(location_update)
        await booking_queue.update_location(location_update)

        return ResponseBuilder.build_success('Location updated')

    @router.get(
        '/{booking_id}/location',
        description='Stream of driver location updates, used by the user to render the driver\'s location on the map',
        response_class=StreamingResponse,
        responses={
            200: {
                'description': 'Stream of location updates',
                'content': {'text/event-stream': {}}
            }
        }
    )
    async def get_location(self, booking_id: int):
        booking = await repository.get(booking_id)

        if not booking:
            raise ResponseBuilder.build_not_found('Booking not found')

        if booking.status != enums.BookingStatus.ACCEPTED:
            raise ResponseBuilder.build_bad_request('Booking is not accepted')

        return StreamingResponse(emitter.retrieve(booking.driver_id), media_type='text/event-stream')
