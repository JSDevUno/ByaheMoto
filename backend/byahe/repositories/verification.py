from prisma import types

from .base import Repository
from ..schemas.models import VerificationRequest


class Verification(
    Repository[
        VerificationRequest,
        types.VerificationRequestCreateInput,
        types.VerificationRequestUpdateInput,
        types.VerificationRequestWhereInput,
        types.VerificationRequestInclude,
        types.VerificationRequestOrderByInput
    ]
):
    model = VerificationRequest
