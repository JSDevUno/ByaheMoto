import re
import secrets
from datetime import datetime, timedelta, timezone
from typing import Annotated

import jwt as pyjwt
from fastapi import APIRouter, BackgroundTasks, Depends, UploadFile
from fastapi.security import OAuth2PasswordRequestForm
from fastapi_utils.cbv import cbv
from prisma.enums import Role
from pydantic import create_model

from ..repositories import User as Repository, Verification
from ..schemas import request
from ..schemas.request import RegistrationType
from ..schemas.response import LoginResponse
from ..utils import jwt
from ..utils.decorators import schema_name
from ..utils.file import write_file
from ..utils.hashing import Hashing
from ..utils.mail import Mailer
from ..utils.queue import booking_queue
from ..utils.response import ResponseBuilder

router = APIRouter(
    prefix='/auth',
    tags=['Authentication']
)

repository = Repository()
verification_repo = Verification()


@cbv(router)
class Authentication:
    @router.post(
        '/login',
        responses={
            200: {
                'description': 'Successful login',
                'model': LoginResponse
            },
            401: {
                'description': 'Unauthorized',
                'model': create_model(
                    'LoginError',
                    success=(bool, False),
                    message=(str, 'User not found. | Invalid password.')
                )
            }
        }
    )
    @schema_name('Login')
    async def login(self, form_data: Annotated[OAuth2PasswordRequestForm, Depends()]):
        user = await repository.get_by_email(form_data.username) or await repository.get_by_username(form_data.username)

        if not user:
            raise ResponseBuilder.build_unauthorized('User not found.')

        if not Hashing.verify(form_data.password, user.password):
            raise ResponseBuilder.build_unauthorized('Invalid password.')

        user_data = user.model_dump(
            exclude={
                'password',
                'created_at',
                'updated_at',
            }
        )

        token_data = {key: value for key, value in user_data.items() if key not in {'email', 'id'}}

        access_token = jwt.encode({
            'sub': user.email,
            'jti': user.id,
            **token_data,
        })
        refresh_token = jwt.encode({
            'sub': user.email,
            'jti': user.id,
            **token_data,
        }, expires_in=timedelta(days=30))

        return LoginResponse(
            user=user_data,
            access_token=access_token,
            refresh_token=refresh_token,
            token_type='bearer',
        )

    @router.post(
        '/register',
        status_code=201,
        responses={
            201: {
                'description': 'Successful registration',
                'model': create_model(
                    'RegisterSuccess',
                    success=(bool, True),
                    message=(str, 'User successfully registered.'),
                    data=(dict, None)
                )
            },
            400: {
                'description': 'Bad request',
                'model': create_model(
                    'RegisterError',
                    success=(bool, False),
                    message=(
                            str,
                            'Email already exists. | Username already exists. | Passwords do not match. | '
                            'Password must be at least 8 characters long and contain at least one uppercase '
                            'letter, one lowercase letter, and one number.'
                    )
                )
            }
        }
    )
    @schema_name('Register')
    async def register(
        self,
        data: request.Register,
        background_tasks: BackgroundTasks,
        mailer: Mailer = Depends(Mailer)
    ):
        user = await repository.get_by_email(data.email)

        if user:
            raise ResponseBuilder.build_bad_request('Email already exists.')

        user = await repository.get_by_username(data.username)

        if user:
            raise ResponseBuilder.build_bad_request('Username already exists.')

        if re.match(
                r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$%^&*!()_+\-=\[\]{}|;:,.<>?/]).{8,}$',
                data.password
        ) is None:
            raise ResponseBuilder.build_bad_request(
                'Password must be at least 8 characters long and contain at least one uppercase letter, '
                'one lowercase letter, and one number.'
            )

        if data.password != data.confirm_password:
            raise ResponseBuilder.build_bad_request('Passwords do not match.')

        data.password = Hashing.hash(data.password)
        body = data.model_dump()

        registration_type = body.pop('registration_type')

        if registration_type == RegistrationType.DRIVER:
            if not body.get('vehicle_type'):
                raise ResponseBuilder.build_bad_request('Vehicle type is required.')

            body['role'] = Role.DRIVER

        if registration_type == RegistrationType.REGULAR and body.get('vehicle_type'):
            raise ResponseBuilder.build_bad_request('User does not have a vehicle type.')

        body.pop('confirm_password')

        user = await repository.create(body)

        if user.role == Role.DRIVER:
            await booking_queue.create_driver_queue(user)

        token = secrets.token_urlsafe(32)

        token = await repository.create_token(user.id, token, 'verify_email')

        background_tasks.add_task(
            mailer.send,
            user.email,
            'Verify Email',
            f'Verify your email {token.token}.'  # TODO: Implement this 'myapp://verify?token${token}'
        )

        return ResponseBuilder.build_success(
            'User successfully registered.',
            user.model_dump(
                exclude={
                    'password',
                    'created_at',
                    'updated_at',
                }
            )
        )

    @router.post(
        '/refresh-token',
        responses={
            200: {
                'description': 'Successful token refresh',
                'model': create_model(
                    'RefreshTokenSuccess',
                    success=(bool, True),
                    message=(str, 'Token successfully refreshed.'),
                    data=(
                            dict,
                            create_model('Token', access_token=(str, '<access-token>'), token_type=(str, 'bearer'))()
                    )
                )
            },
            401: {
                'description': 'Unauthorized',
                'model': create_model(
                    'RefreshTokenError',
                    success=(bool, False),
                    message=(str, 'Token has expired. | Invalid token. | User not found.')
                )
            }
        }
    )
    async def refresh_token(self, data: request.RefreshAccessToken):
        try:
            payload = jwt.decode(data.refresh_token)
        except pyjwt.ExpiredSignatureError as _:
            raise ResponseBuilder.build_unauthorized('Token has expired.')
        except pyjwt.InvalidTokenError as _:
            raise ResponseBuilder.build_unauthorized('Invalid token.')

        if not payload:
            raise ResponseBuilder.build_unauthorized('Invalid token.')

        user = await repository.get_by_email(payload['sub'])

        if not user:
            raise ResponseBuilder.build_unauthorized('User not found.')

        user_data = user.model_dump(
            exclude={
                'password',
                'created_at',
                'updated_at',
            }
        )

        token_data = {key: value for key, value in user_data.items() if key not in {'email', 'id'}}

        access_token = jwt.encode({
            'sub': user.email,
            'jti': user.id,
            **token_data,
        })

        return ResponseBuilder.build_success(
            'Token successfully refreshed.',
            {
                'access_token': access_token,
                'token_type': 'bearer',
            }
        )

    @router.post(
        '/forgot-password',
        responses={
            200: {
                'description': 'Successful password reset',
                'model': create_model(
                    'ForgotPasswordSuccess',
                    success=(bool, True),
                    message=(str, 'Password reset link sent to email.')
                )
            },
            404: {
                'description': 'Not found',
                'model': create_model(
                    'EmailNotFound',
                    success=(bool, False),
                    message=(str, 'Email not found.')
                )
            }
        }
    )
    async def forgot_password(
        self,
        data: request.ForgotPassword,
        background_tasks: BackgroundTasks,
        mailer = Depends(Mailer)
    ):
        user = await repository.get_by_email(data.email)

        if not user:
            raise ResponseBuilder.build_not_found('Email not found.')

        token = secrets.token_urlsafe(32)

        await repository.create_token(user.id, token)

        background_tasks.add_task(
            mailer.send,
            user.email, 'Forgot Password',
            f'byahe://auth/reset-password?token={token}'
        )

        return ResponseBuilder.build_success('Password reset link sent to email.')

    @router.post(
        '/reset-password',
        responses={
            200: {
                'description': 'Successful password reset',
                'model': create_model(
                    'ResetPasswordSuccess',
                    success=(bool, True),
                    message=(str, 'Password reset success.')
                )
            },
            400: {
                'description': 'Bad request',
                'model': create_model(
                    'ResetPasswordError',
                    success=(bool, False),
                    message=(str, 'Passwords do not match. | Token not found. | Invalid token.')
                )
            }
        }
    )
    async def reset_password(self, data: request.ResetPassword):
        if re.match(
                r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$%^&*!()_+\-=\[\]{}|;:,.<>?/]).{8,}$',
                data.password
        ) is None:
            raise ResponseBuilder.build_bad_request(
                'Password must be at least 8 characters long and contain at least one uppercase letter, '
                'one lowercase letter, and one number.'
            )

        if data.password != data.confirm_password:
            raise ResponseBuilder.build_bad_request('Passwords do not match.')

        token = await repository.get_token(data.token)

        if not token:
            raise ResponseBuilder.build_bad_request('Token not found.')

        if token.type != 'reset_password':
            raise ResponseBuilder.build_bad_request('Invalid token.')

        await repository.update(token.user_id, {'password': Hashing.hash(data.password)})

        return ResponseBuilder.build_success('Password reset success.')

    @router.post(
        '/verify-email',
        responses={
            200: {
                'description': 'Successful email verification',
                'model': create_model(
                    'VerifyEmailSuccess',
                    success=(bool, True),
                    message=(str, 'Email successfully verified.')
                )
            },
            400: {
                'description': 'Bad request',
                'model': create_model(
                    'InvalidToken',
                    success=(bool, False),
                    message=(str, 'Token not found. | Invalid token. | Token has expired. | Already verified.')
                )
            }
        }
    )
    async def verify_email(self, data: request.VerifyEmail):
        token = await repository.get_token(data.token)

        if not token:
            raise ResponseBuilder.build_bad_request('Token not found.')

        if token.type != 'verify_email':
            raise ResponseBuilder.build_bad_request('Invalid token.')

        if token.expires_at < datetime.now(timezone.utc):
            raise ResponseBuilder.build_bad_request('Token has expired.')

        if not token.user:
            raise ResponseBuilder.build_bad_request('User not found.')

        if token.user.is_verified:
            raise ResponseBuilder.build_bad_request('Already verified')

        await repository.update(token.user_id, {'is_verified': True})

        return ResponseBuilder.build_success('Email successfully verified.')

    @router.post(
        '/verification',
        responses={
            200: {
                'description': 'Successful verification',
                'model': create_model(
                    'VerificationSuccess',
                    success=(bool, True),
                    message=(str, 'Verification request successfully submitted.')
                )
            },
            404: {
                'description': 'Not found',
                'model': create_model(
                    'UserNotFound',
                    success=(bool, False),
                    message=(str, 'User not found.')
                )
            }
        }
    )
    @schema_name('IdentityVerification')
    async def identity_verification(
        self,
        file: UploadFile,
        data: request.IdentityVerification = Depends(request.IdentityVerification.form),  # type: ignore
    ):
        body = data.model_dump()

        user = await repository.get(body['user_id'], include={'identity': True})

        if not user:
            raise ResponseBuilder.build_not_found('User not found.')

        if user.identity:
            await repository.delete_identity(user.id)

        path = write_file(file, f'identity-{user.id}', 'identity')

        identity = await repository.identify({
            'user_id': user.id,
            'url': f'/users/{user.id}/identity',
            'file_path': path.as_posix()
        })
        await verification_repo.create({'identity_id': identity.id})

        return ResponseBuilder.build_success('Identity verification request successfully submitted.')
