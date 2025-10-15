from fastapi import APIRouter, Depends
from fastapi_utils.cbv import cbv
from prisma import enums

from ..repositories import Verification
from ..schemas import request
from ..utils import jwt
from ..utils.response import ResponseBuilder

router = APIRouter(
    prefix='/verification-requests',
    tags=['Verification Requests'],
    dependencies=[Depends(jwt.is_admin)]
)

repository = Verification()


@cbv(router)
class VerificationRequests:
    @router.get('/')
    async def verification_requests(self):
        verification_requests = await repository.all(
            order={'created_at': 'desc'},
            include={'identity': {'include': {'user': True}}}
        )

        return ResponseBuilder.build_success(
            'Verification requests retrieved',
            data=[verification_request.model_dump() for verification_request in verification_requests]
        )

    @router.get('/{verification_request_id}')
    async def verification_request(self, verification_request_id: int):
        verification_request = await repository.get(verification_request_id)

        if not verification_request:
            raise ResponseBuilder.build_not_found('Verification request not found')

        return ResponseBuilder.build_success('Verification request retrieved', data=verification_request.model_dump())

    @router.put('/{verification_request_id}/approve')
    async def approve_verification_request(self, verification_request_id: int):
        verification_request = await repository.get(verification_request_id)

        if not verification_request:
            raise ResponseBuilder.build_not_found('Verification request not found')

        if verification_request.status != enums.Status.PENDING:
            raise ResponseBuilder.build_bad_request('Verification request is not pending')

        await repository.update(verification_request_id, {'status': enums.Status.APPROVED})

        return ResponseBuilder.build_success('Verification request approved')

    @router.put('/{verification_request_id}/reject')
    async def reject_verification_request(self, verification_request_id: int, body: request.VerificationStatusUpdate):
        verification_request = await repository.get(verification_request_id)

        if not verification_request:
            raise ResponseBuilder.build_not_found('Verification request not found')

        if verification_request.status != enums.Status.PENDING:
            raise ResponseBuilder.build_bad_request('Verification request is not pending')

        await repository.update(verification_request_id, {
            'status': enums.Status.REJECTED,
            'reason': body.reason
        })

        return ResponseBuilder.build_success('Verification request rejected')
