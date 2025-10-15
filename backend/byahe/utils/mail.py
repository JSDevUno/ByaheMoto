from fastapi_mail import ConnectionConfig, FastMail, MessageSchema, MessageType

from ..settings import settings


class Mailer:
    """
    Mailer class to handle sending of emails
    """

    def __init__(self):
        conf = ConnectionConfig(
            MAIL_USERNAME=settings.mail_username,
            MAIL_PASSWORD=settings.mail_password,
            MAIL_FROM=settings.mail_username,
            MAIL_SERVER='smtp.gmail.com',
            MAIL_PORT=587,
            MAIL_STARTTLS=True,
            MAIL_SSL_TLS=False
        )

        self.mail = FastMail(conf)

    async def send(self, to: str, subject: str, body: str):
        """
        Send an email
        :param to: str
        :param subject: str
        :param body: str
        :return:
        """
        message = MessageSchema(
            subject=subject,
            recipients=[to],
            body=body,
            subtype=MessageType.html
        )

        await self.mail.send_message(message)


mailer = Mailer()
