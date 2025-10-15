from typing import Any, Dict, List, Optional, Union

from pydantic import BaseModel

from .base import Base


class Response(Base):
    """Base response schema."""

    success: bool = True
    message: str
    data: Optional[Union[Dict[str, Any], List[Dict[str, Any]]]] = None


class LoginResponse(BaseModel):
    user: dict
    access_token: str
    refresh_token: str
    token_type: str
