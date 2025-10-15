import bcrypt

from ..settings import settings


class Hashing:
    @staticmethod
    def hash(password: str) -> str:
        password = password + settings.secret_key

        return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')

    @staticmethod
    def verify(password: str, hashed_password: str) -> bool:
        password = password + settings.secret_key

        return bcrypt.checkpw(password.encode('utf-8'), hashed_password.encode('utf-8'))
