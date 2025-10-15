import pathlib

from fastapi import APIRouter, Depends
from fastapi_utils.cbv import cbv
from fastapi.responses import FileResponse

from ..repositories import User, Verification
from ..utils import jwt
from ..utils.response import ResponseBuilder

router = APIRouter(
    prefix='/users',
    tags=['Users'],
    dependencies=[Depends(jwt.is_admin)]
)

repository = User()
verification_repository = Verification()


@cbv(router)
class Users:
    @router.get('/')
    async def all(self):
        users = await repository.all(order={'created_at': 'desc'})

        return ResponseBuilder.build_success('Users retrieved', data=[user.model_dump() for user in users])

    @router.get('/{user_id}')
    async def get(self, user_id: int):
        user = await repository.get(user_id)

        if not user:
            raise ResponseBuilder.build_not_found('User not found')

        return ResponseBuilder.build_success('User retrieved', data=user.model_dump())

    @router.delete('/{user_id}')
    async def delete(self, user_id: int):
        user = await repository.get(user_id)

        if not user:
            raise ResponseBuilder.build_not_found('User not found')

        await repository.delete(user_id)

        return ResponseBuilder.build_success('User deleted')

    @router.get('/{user_id}/identity')
    async def get_identity(self, user_id: int):
        user = await repository.get(user_id, include={'identity': True})

        if not user:
            raise ResponseBuilder.build_not_found('User not found')

        if not user.identity:
            raise ResponseBuilder.build_not_found('User identity not found')

        path = pathlib.Path(user.identity.file_path)

        if not path.exists():
            raise ResponseBuilder.build_not_found('User identity not found')

        return FileResponse(user.identity.file_path)
