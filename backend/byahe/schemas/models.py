from prisma import models

from .base import Base


class User(Base, models.User):
    pass


class Identity(Base, models.Identity):
    pass


class VerificationRequest(Base, models.VerificationRequest):
    pass


class Token(Base, models.Token):
    pass


class Wallet(Base, models.Wallet):
    pass


class Transaction(Base, models.Transaction):
    pass


class Booking(Base, models.Booking):
    pass


class RideHistory(Base, models.RideHistory):
    pass

