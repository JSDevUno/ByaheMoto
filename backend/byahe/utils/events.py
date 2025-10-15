import asyncio
from collections import defaultdict

from .queue import LocationUpdate


class Emitter:
    __instance = None

    def __init__(self):
        self.__drivers = defaultdict(list)

    def emit(self, data: LocationUpdate):
        """
        Emit a notification to a client
        :param data:  Notification
        """
        self.__drivers[data.driver_id].append(data)

    async def retrieve(self, driver_id: int):
        """
        Generator to retrieve notifications for a client

        :param driver_id: int
        :return: generator
        """
        while True:
            if driver_id in self.__drivers and self.__drivers[driver_id]:
                data: LocationUpdate = self.__drivers[driver_id].pop()
                yield f'data: {data.model_dump_json()}\n\n'

            await asyncio.sleep(2)

    def __new__(cls, *args, **kwargs):
        if cls.__instance is None:
            cls.__instance = super(Emitter, cls).__new__(cls, *args, **kwargs)
        return cls.__instance
