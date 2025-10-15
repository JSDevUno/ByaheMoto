from prisma import types

from .base import Repository
from ..schemas import models


class Booking(
    Repository[
        models.Booking,
        types.BookingCreateInput,
        types.BookingUpdateInput,
        types.BookingWhereInput,
        types.BookingInclude,
        types.BookingOrderByInput
    ]
):
    model = models.Booking

    async def create_ride_history(self, data: types.RideHistoryCreateInput) -> models.RideHistory:
        booking: models.Booking = await self.get(data['booking_id'])

        if not booking:
            raise ValueError('Booking not found')

        ride_history = await self.database.ridehistory.create(data=data)

        return models.RideHistory(**ride_history.model_dump())
