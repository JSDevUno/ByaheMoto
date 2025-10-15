from enum import Enum
from typing import Optional

from fastapi import UploadFile
from prisma import enums
from pydantic import EmailStr, model_validator

from .base import Base
from ..utils.decorators import form


class RegistrationType(Enum):
    PRIORITY = 'priority'
    REGULAR = 'regular'
    DRIVER = 'driver'


class Register(Base):
    full_name: str
    username: str
    email: EmailStr
    password: str
    confirm_password: str
    registration_type: RegistrationType = RegistrationType.REGULAR
    vehicle_type: Optional[enums.VehicleType] = None


class RefreshAccessToken(Base):
    refresh_token: str


class ForgotPassword(Base):
    email: EmailStr


class ResetPassword(Base):
    token: str
    password: str
    confirm_password: str


class VerifyEmail(Base):
    token: str


@form
class IdentityVerification(Base):
    user_id: int
    identity_type: enums.IdentificationType


class VerificationStatusUpdate(Base):
    reason: Optional[str] = None


class Location(Base):
    lat: float
    lng: float

class BookingCreate(Base):
    payment_method: enums.PaymentMode
    vehicle_type: enums.VehicleType
    location_from: Location
    location_to: Location

class TopUp(Base):
    amount: float


class ProfileUpdate(Base):
    full_name: Optional[str] = None
    username: Optional[str] = None
    email: Optional[EmailStr] = None
    phone_number: Optional[str] = None

    @model_validator(mode='after')
    def one_of(self):
        if not any(getattr(self, field) for field in self.model_fields):
            raise ValueError('At least one field is required')

        return self


class ChangePassword(Base):
    current_password: str
    new_password: str
    confirm_password: str


@form
class ProfilePicture(Base):
    profile_picture: UploadFile
