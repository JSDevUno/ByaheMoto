from datetime import datetime, timedelta

from fastapi import APIRouter
from fastapi_utils.cbv import cbv

from ..repositories import Booking, User
from ..utils.response import ResponseBuilder

router = APIRouter(
    tags=['System']
)
booking_repository = Booking()
user_repository = User()


@cbv(router)
class System:
    @router.get('/')
    async def report(self):
        # Get the last 12 months
        months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
        current_month = datetime.now().month - 1
        last_12_months = months[current_month:] + months[:current_month]

        one_year_ago = datetime.now() - timedelta(days=365)
        bookings = await booking_repository.all(
            filters={
                'created_at': {'gte': one_year_ago}
            },
            order={'created_at': 'desc'}
        )
        users = await user_repository.all(
            filters={
                'created_at': {'gte': one_year_ago}
            },
            order={'created_at': 'desc'}
        )

        bookings_stats = [0] * 12
        users_stats = [0] * 12

        for booking in bookings:
            month_index = (booking.created_at.month - 1 - current_month) % 12  # Adjust month index to match last 12 months
            bookings_stats[month_index] += 1

        for user in users:
            month_index = (user.created_at.month - 1 - current_month) % 12  # Adjust month index to match last 12 months
            users_stats[month_index] += 1

        return ResponseBuilder.build_success('Statistics reports retrieved', data={
            'last_12_months': last_12_months[::-1],
            'bookings': bookings_stats[::-1],
            'users': users_stats[::-1]
        })

    @router.get('/ping')
    async def ping(self):
        return {'message': 'pong'}

    @router.get('/health')
    async def health(self):
        return {'status': 'ok'}
