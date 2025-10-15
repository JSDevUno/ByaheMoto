from datetime import UTC, datetime, timedelta
from typing import Annotated, Optional

import jwt as pyjwt
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from jwt.exceptions import InvalidTokenError
from pydantic import BaseModel
from prisma.enums import Role

from ..repositories.user import User as Repository
from ..schemas.models import User
from ..settings import settings

oauth2_scheme = OAuth2PasswordBearer(
    tokenUrl='/auth/login',
)


class Token(BaseModel):
    access: str
    token_type: str


def decode(token: str) -> Optional[dict]:
    try:
        return pyjwt.decode(token, settings.jwt_secret, algorithms=['HS256'])
    except InvalidTokenError:
        return None


def encode(data: dict, expires_in: Optional[timedelta] = None) -> str:
    to_encode = data.copy()
    expire = datetime.now(tz=UTC) + expires_in if expires_in else datetime.now(tz=UTC) + timedelta(minutes=15)
    to_encode.update({"exp": expire})

    return pyjwt.encode(to_encode, settings.jwt_secret, algorithm='HS256')


async def is_authenticated(token: Annotated[str, Depends(oauth2_scheme)]) -> User:
    exc = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid token.",
        headers={"WWW-Authenticate": "Bearer"},
    )

    payload = decode(token)

    if payload is None:
        raise exc

    user = await Repository().get_by_email(payload.get('sub', ''))

    if user is None:
        raise exc

    return user


def is_admin(user: Annotated[User, Depends(is_authenticated)]) -> User:
    if not user.role == Role.ADMIN:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not authorized to access this resource.",
        )

    return user


def is_driver(user: Annotated[User, Depends(is_authenticated)]) -> User:
    if user.role == Role.USER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not authorized to access this resource.",
        )

    return user
