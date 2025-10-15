import json
from typing import Generic, List, Optional, Type, TypeVar, get_origin, Union, get_args

from prisma import Prisma, Json
from prisma.models import BaseModel

from ..utils import database

T = TypeVar('T', bound=BaseModel)  # Generic type for models
CreateInput = TypeVar('CreateInput', bound=object)
UpdateInput = TypeVar('UpdateInput', bound=object)
FilterInput = TypeVar('FilterInput', bound=object)
IncludeInput = TypeVar('IncludeInput', bound=object)
OrderInput = TypeVar('OrderInput', bound=object)


class Repository(
    Generic[
        T,
        CreateInput,
        UpdateInput,
        FilterInput,
        IncludeInput,
        OrderInput
    ]):
    database: Prisma = database
    model: Type[T]

    async def get(self, _id: int, include: Optional[IncludeInput] = None) -> Optional[T]:
        result = await getattr(self.database, self.model.__name__.lower()).find_unique(
            where={'id': _id},
            include=include
        )
        return self.model(**self._dump(result)) if result else None

    async def create(self, data: CreateInput, include: Optional[IncludeInput] = None) -> T:
        result = await getattr(self.database, self.model.__name__.lower()).create(data=data, include=include)
        return self.model(**self._dump(result))

    async def update(self, _id: int, data: UpdateInput, include: Optional[IncludeInput] = None) -> T:
        result = await getattr(self.database, self.model.__name__.lower()).update(
            where={'id': _id},
            data=data,
            include=include
        )
        return self.model(**self._dump(result))

    async def delete(self, _id: int, include: Optional[IncludeInput] = None) -> T:
        result = await getattr(self.database, self.model.__name__.lower()).delete(where={'id': _id}, include=include)
        return self.model(**self._dump(result))

    async def all(
        self,
        filters: Optional[FilterInput] = None,
        include: Optional[IncludeInput] = None,
        order: Optional[OrderInput] = None
    ) -> List[T]:
        results = await getattr(self.database, self.model.__name__.lower()).find_many(
            where=filters,
            include=include,
            order=order
        )
        return [self.model(**self._dump(result)) for result in results]

    def _dump(self, result: T):
        dump = result.model_dump(exclude_none=False)

        def handle_field(annotation, value):
            # Check if the annotation is a Json or Optional[Json]
            if annotation == Json or annotation == Optional[Json] and value is not None:
                return json.dumps(value)

            # Check if the annotation is a BaseModel or Optional[BaseModel] ps: Optional[BaseModel] is a Union
            if get_origin(annotation) is Union:
                # Extract the possible types from Union
                possible_types = list(get_args(annotation))

                # Check if there's a NoneType (Optional case) and handle accordingly
                if type(None) in possible_types:
                    possible_types.remove(type(None))

                # Attempt to instantiate the first non-None type if value is not None
                if value is not None and possible_types:
                    for typ in possible_types:
                        # Check if list
                        if get_origin(typ) is list:
                            return [self._dump(typ.__args__[0](**v)) for v in value]

                        if not issubclass(typ, BaseModel): continue
                        return self._dump(typ(**value))

            return value

        res = {
            k: handle_field(v.annotation, dump[k])
            for k, v in result.model_fields.items()
        }

        return res
