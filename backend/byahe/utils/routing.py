import typing

from fastapi.responses import JSONResponse
from starlette.background import BackgroundTask


class Response(JSONResponse):
    def __init__(
        self,
        content: typing.Dict,
        status_code: int = 200,
        headers: typing.Mapping[str, str] | None = None,
        media_type: str | None = None,
        background: BackgroundTask | None = None,
    ):
        data = content.copy()
        for k, v in content.items():
            if v is None:
                data.pop(k)

        super().__init__(
            data,
            status_code,
            headers,
            media_type,
            background
        )
