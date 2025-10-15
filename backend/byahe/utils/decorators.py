import inspect
from functools import wraps
from typing import Callable, Type, List

from fastapi import Form, UploadFile, File
from pydantic.fields import FieldInfo # noqa

from ..schemas.base import Base


def get_form_field(model_field: FieldInfo):
    if model_field.annotation in [UploadFile, List[UploadFile]]:
        return File(...) if model_field.is_required() else File(model_field.default)
    return Form(...) if model_field.is_required() else Form(model_field.default)

def form(cls: Type[Base]) -> Type[Base]:
    new_parameters = []

    for name, model_field in cls.model_fields.items():
        model_field: FieldInfo
        
        if not model_field.alias: continue 

        new_parameters.append(
            inspect.Parameter(
                model_field.alias,
                inspect.Parameter.POSITIONAL_ONLY,
                default=get_form_field(model_field),
                annotation=model_field.annotation,
            )
        )

    async def form_func(**data):
        return cls(**data)

    sig = inspect.signature(form_func)
    sig = sig.replace(parameters=new_parameters)
    form_func.__signature__ = sig  # type: ignore
    setattr(cls, 'form', form_func)
    return cls


def schema_name(name: str):
    """
    Decorator to update the Pydantic schema name for a FastAPI route
    that handles file uploads. Paired with a modification in the FastAPI
    lifespan context manager to update the schema name of the route's

    Args:
        name: The new name of the schema.
    """

    def decorator(func: Callable):
        @wraps(func)
        async def wrapper(*args, **kwargs):
            return await func(*args, **kwargs)

        setattr(wrapper, '__schema_name__', name)

        return wrapper

    return decorator
