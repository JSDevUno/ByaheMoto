from datetime import datetime, timedelta
from typing import Optional

from prisma import types

from .base import Repository
from ..schemas.models import (
    Identity as IdentityModel,
    Token as TokenModel,
    Transaction as TransactionModel,
    User as UserModel,
    VerificationRequest as VerificationRequestModel
)


class User(
    Repository[
        UserModel,
        types.UserCreateInput,
        types.UserUpdateInput,
        types.UserWhereInput,
        types.UserInclude,
        types.UserOrderByInput
    ]
):
    model = UserModel

    async def create(self, data: types.UserCreateInput, include: Optional[types.UserInclude] = None) -> UserModel:
        user: UserModel = await super().create(data, include)

        await self.database.wallet.create(data={'user_id': user.id})

        return UserModel(**self._dump(user))

    async def get_by_email(self, email: str) -> Optional[UserModel]:
        user = await self.database.user.find_first(where={'email': email})

        if not user:
            return None

        return UserModel(**self._dump(user))

    async def get_by_username(self, username: str) -> Optional[UserModel]:
        user = await self.database.user.find_first(where={'username': username})

        if not user:
            return None

        return UserModel(**self._dump(user))

    async def create_token(self, user_id: int, token_str: str, token_type: str = 'reset_password') -> TokenModel:
        token = await self.get_token(token_str)

        if token:
            raise ValueError('Token already exists.')

        token = await self.database.token.create(data={
            'user_id': user_id,
            'type': token_type,
            'token': token_str,
            'expires_at': datetime.utcnow() + timedelta(minutes=60)
        })

        return TokenModel(**token.model_dump())

    async def get_token(self, token: str) -> Optional[TokenModel]:
        token = await self.database.token.find_first(where={'token': token})

        if not token:
            return None

        return TokenModel(**token.model_dump())

    async def identify(self, data: types.IdentityCreateInput) -> IdentityModel:
        user = await self.get(data['user_id'])

        if not user:
            raise ValueError('User not found.')

        identity = await self.database.identity.create(data=data)

        return IdentityModel(**identity.model_dump())

    async def add_balance(self, user_id: int, amount: float) -> TransactionModel:
        user: UserModel = await self.get(user_id, include={'wallet': True})

        if not user:
            raise ValueError('User not found.')

        wallet = user.wallet

        if not wallet:
            raise ValueError('Wallet not found.')

        transaction = await self.database.transaction.create(data={
            'user_id': user.id,
            'amount': amount,
            'type': 'credit',
        })

        await self.database.wallet.update(where={'id': wallet.id}, data={'balance': wallet.balance + amount})

        return TransactionModel(**transaction.model_dump())

    async def deduct_balance(self, user_id: int, amount: float, payment_type: str = 'payment') -> TransactionModel:
        user = await self.get(user_id, include={'wallet': True})

        if not user:
            raise ValueError('User not found.')

        wallet = user.wallet

        if wallet.balance < amount:
            raise ValueError('Insufficient balance.')

        transaction = await self.database.transaction.create(data={
            'user_id': user.id,
            'amount': amount,
            'type': payment_type,
        })

        await self.database.wallet.update(where={'id': wallet.id}, data={'balance': wallet.balance - amount})

        return TransactionModel(**transaction.model_dump())

    async def delete_identity(self, user_id: int) -> IdentityModel:
        identity = await self.database.identity.find_first(where={'user_id': user_id})

        if not identity:
            raise ValueError('Identity not found.')

        await self.database.identity.delete(where={'id': identity.id})
