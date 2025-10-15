import pathlib
import re
from typing import Annotated

from fastapi import APIRouter, Depends, status
from fastapi.exceptions import HTTPException
from fastapi.responses import FileResponse
from fastapi_utils.cbv import cbv
from httpx import Response

from ..repositories import User, Booking
from ..schemas import models, request
from ..utils import jwt, Hashing
from ..utils.decorators import schema_name
from ..utils.external import PayPal
from ..utils.file import write_file
from ..utils.response import ResponseBuilder

router = APIRouter(
    prefix='/profile',
    tags=['Profile'],
    dependencies=[Depends(jwt.is_authenticated)]
)

repository = User()
booking_repository = Booking()


@cbv(router)
class Profile:
    @router.get('/')
    async def get(self, user: Annotated[models.User, Depends(jwt.is_authenticated)]):
        user = await repository.get(
            user.id,
            include={
                'identity': True,
                'wallet': True
            }
        )

        return ResponseBuilder.build_success('Profile retrieved', data=user.model_dump())

    @router.put('/')
    async def update(self, data: request.ProfileUpdate, user: Annotated[models.User, Depends(jwt.is_authenticated)]):
        if data.email:
            existing = await repository.get_by_email(data.email)

            if existing:
                raise ResponseBuilder.build_bad_request('Email already exists')

        if data.username:
            existing = await repository.get_by_username(data.username)

            if existing:
                raise ResponseBuilder.build_bad_request('Username already exists')

        user = await repository.update(user.id, data.model_dump())

        return ResponseBuilder.build_success('Profile updated', data=user.model_dump())

    @router.get('/picture')
    async def get_profile_picture(self, user: Annotated[models.User, Depends(jwt.is_authenticated)]):
        user = await repository.get(user.id)

        if not user.profile_picture:
            raise ResponseBuilder.build_not_found('Profile picture not found')

        return FileResponse(user.profile_picture)

    @router.put('/picture')
    @schema_name('ProfilePicture')
    async def update_profile_picture(
            self,
            body: Annotated[request.ProfilePicture, Depends(request.ProfilePicture.form)],  # noqa
            user: Annotated[models.User, Depends(jwt.is_authenticated)]
    ):
        file = body.profile_picture

        if not file.content_type.startswith('image'):
            raise ResponseBuilder.build_bad_request('Invalid file type')

        # check if user has existing profile picture
        if user.profile_picture:
            pathlib.Path(user.profile_picture).unlink()

        path = write_file(file, f'profile-{user.id}', 'profile-pictures')

        await repository.update(user.id, {'profile_picture': path.as_posix()})

        return ResponseBuilder.build_success('Profile picture updated')

    @router.put('/change-password')
    async def change_password(
            self,
            body: request.ChangePassword,
            user: Annotated[models.User, Depends(jwt.is_authenticated)]
    ):
        user = await repository.get(user.id)

        if not Hashing.verify(body.current_password, user.password):
            raise ResponseBuilder.build_unauthorized('Invalid password')

        if body.confirm_password != body.new_password:
            raise ResponseBuilder.build_bad_request('Passwords do not match')

        if re.match(
                r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$%^&*!()_+\-=\[\]{}|;:,.<>?/]).{8,}$',
                body.new_password
        ) is None:
            raise ResponseBuilder.build_bad_request(
                'Password must be at least 8 characters long and contain at least one uppercase letter, '
                'one lowercase letter, and one number.'
            )

        await repository.update(user.id, {'password': Hashing.hash(body.new_password)})

        return ResponseBuilder.build_success('Password changed')

    @router.get('/history/transaction')
    async def get_transaction_history(self, user: Annotated[models.User, Depends(jwt.is_authenticated)]):
        user = await repository.get(user.id, include={'transactions': True})

        transactions = user.transactions

        return ResponseBuilder.build_success(
            'Transaction history retrieved',
            data=[transaction.model_dump() for transaction in transactions]
        )

    @router.get('/history/ride')
    async def get_ride_history(self, user: Annotated[models.User, Depends(jwt.is_authenticated)]):
        bookings = await booking_repository.all(filters={'user_id': user.id})

        return ResponseBuilder.build_success(
            'Ride history retrieved',
            data=[booking.model_dump() for booking in bookings]
        )

    @router.post('/top-up')
    async def top_up(self, body: request.TopUp):
        paypal = await PayPal.create()
        response = await paypal.create_order(body.amount)

        json = await self.__parse_json(response)

        if not response.is_success:
            raise ResponseBuilder.exception(HTTPException(
                status_code=response.status_code,
                detail=json['detail'] if json else response.reason_phrase
            ))

        return ResponseBuilder.build_success('Order created', data=json)

    @router.post('/top-up/{order_id}/capture')
    async def capture_top_up(self, order_id: str, user: Annotated[models.User, Depends(jwt.is_authenticated)]):
        paypal = await PayPal.create()

        # Check if order is already completed
        response = await paypal.get_order(order_id)
        json = await self.__parse_json(response)

        if not response.is_success:
            detail = json.get('details', [{}])[0].get('description', None)
            raise ResponseBuilder.build_error(
                status_code=response.status_code,
                message=detail or response.reason_phrase
            )

        if json['status'] == 'COMPLETED':
            raise ResponseBuilder.build_bad_request('Order already completed')

        response = await paypal.capture_order(order_id)
        json = await self.__parse_json(response)

        if response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY:
            raise ResponseBuilder.build_bad_request('Order not approved by user')

        if not response.is_success:
            raise ResponseBuilder.build_error(
                status_code=response.status_code,
                message=json.get('detail', None) or response.reason_phrase
            )

        try:
            await repository.add_balance(user.id, float(json['purchase_units'][0]['amount']['value']))
        except Exception as e:  # noqa
            raise ResponseBuilder.build_internal_server_error(str(e))

        user = await repository.get(user.id, include={'wallet': True})

        if not user.wallet:
            raise ResponseBuilder.build_internal_server_error('User wallet not found')

        return ResponseBuilder.build_success('Balance updated.', data=user.wallet.model_dump())

    @staticmethod
    async def __parse_json(response: Response):
        try:
            return response.json()
        except Exception as e:  # noqa
            return {}
