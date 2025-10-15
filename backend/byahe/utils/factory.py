import asyncio
import importlib
import logging
from contextlib import asynccontextmanager
from typing import Awaitable, Union

from fastapi import FastAPI, HTTPException, Request, Response, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.routing import APIRoute
# from prisma import enums, models
from pydantic import create_model
from starlette.exceptions import HTTPException as StarletteHTTPException

from . import routing
from .database import database
from .logger import logger
from .queue import BookingQueue
# from .queue import Booking, booking_queue, user_prio
from .response import ResponseBuilder
from ..settings import AppMode, settings

logger.level = logging.DEBUG if settings.app_mode == AppMode.DEVELOPMENT else logging.INFO


class Routes:
    __routers = [
        'byahe.routers.system',
        'byahe.routers.authentication',
        'byahe.routers.bookings',
        'byahe.routers.profile',
        'byahe.routers.users',
        'byahe.routers.verification_requests',
    ]

    @staticmethod
    def load(app: FastAPI):
        logger.info('Loading routes...')

        for router in Routes.__routers:
            module = importlib.import_module(router)

            if not hasattr(module, 'router'):
                print(f'Module {router} has no attribute \'router\'. Skipping...')
                continue

            app.include_router(module.router)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info('Connecting to database...')
    await database.connect()

    route: APIRoute

    for route in app.routes:  # type: ignore
        endpoint = route.endpoint

        if endpoint and hasattr(endpoint, '__schema_name__'):
            route.body_field.type_.__name__ = endpoint.__schema_name__  # type: ignore

    # start background tasks
    asyncio.create_task(BookingQueue().dispatch())  # noqa
    asyncio.create_task(BookingQueue().process_all())  # noqa

    yield  # Lifespan context

    logger.info('Disconnecting from database...')
    await database.disconnect()


def setup_app(app: FastAPI):
    app.add_middleware(
        CORSMiddleware,  # type: ignore
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    Routes.load(app)

    @app.exception_handler(StarletteHTTPException)
    async def exception_handler(_request: Request, exc: HTTPException) -> Union[Response, Awaitable[Response]]:
        return ResponseBuilder.exception(exc)  # type: ignore

    @app.exception_handler(status.HTTP_404_NOT_FOUND)
    async def not_found(_request: Request, exc: HTTPException) -> Union[Response, Awaitable[Response]]:
        exc.detail = exc.detail or 'Resource Not found'
        return ResponseBuilder.exception(exc)  # type: ignore

    @app.exception_handler(status.HTTP_500_INTERNAL_SERVER_ERROR)
    async def internal_server_error(_request: Request, exc: RuntimeError) -> Union[Response, Awaitable[Response]]:
        e = HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(exc),
        )

        return ResponseBuilder.exception(e)  # type: ignore

    return app


def create_app() -> FastAPI:
    app = FastAPI(
        title='Byahe',
        lifespan=lifespan,
        default_response_class=routing.Response,
        responses={
            200: {
                'description': 'Successful response',
                'model': create_model(
                    'Success',
                    success=(bool, True),
                    message=(str, 'Success.'),
                    data=(dict, None)
                )
            },
            500: {
                'description': 'Internal server error',
                'model': create_model(
                    'InternalServerError',
                    success=(bool, False),
                    message=(str, 'Internal server error.')
                )
            }
        }
    )

    setup_app(app)

    return app
