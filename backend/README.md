# ByaheMoTo! (Backend)

ByaheMoTo! is a mobile application that has a similar functionality with angkas
app in the Philippines. This is the backend part of the application.

## Pre-requisites

1. Python 3.11 or higher
2. Poetry
3. PostgreSQL

## Installation

1. Clone the repository
2. Install the dependencies
    ```bash
    poetry install
    ```
3. Copy the `.env.example` file to `.env` and update the values
    ```bash
    cp .env.example .env
    ```
    - `DATABASE_URL` - The URL of the PostgreSQL database
    - `SECRET_KEY` - The secret key for the JWT token
    - `JWT_SECRET` - The secret key for the JWT token
    - `MAIL_USERNAME` - The email address of the sender
    - `MAIL_PASSWORD` - The password of the sender

   **Note**: The `MAIL_USERNAME` and `MAIL_PASSWORD` are used for sending emails.
   Visit [this link](https://support.google.com/mail/answer/185833?hl=en) to create an app password via google.
   <br/>
   <br/>
4. Run the migrations
    ```bash
    poetry run prisma migrate dev
    ```
5. Run the application
    ```bash
    poetry run fastapi dev main.py # for development.
    # or
    poetry run fastapi run main.py # for production.
    ```
