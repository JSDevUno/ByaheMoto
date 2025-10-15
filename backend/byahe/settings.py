import enum

from pydantic_settings import BaseSettings, SettingsConfigDict


class AppMode(enum.Enum):
    DEVELOPMENT = 'development'
    PRODUCTION = 'production'


class Settings(BaseSettings):
    database_url: str
    mail_username: str
    mail_password: str
    secret_key: str
    jwt_secret: str
    paypal_client_id: str
    paypal_client_secret: str
    app_mode: AppMode = AppMode.DEVELOPMENT

    model_config = SettingsConfigDict(
        env_file='.env',
        env_file_encoding='utf-8',
        env_ignore_empty=True
    )


settings = Settings()
