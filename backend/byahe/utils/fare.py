from geopy.distance import geodesic
from prisma.enums import VehicleType

from ..schemas.request import Location


def calculate_fare(taxi_type: VehicleType, location_from: Location, location_to: Location) -> float:
    base_fares = {
        VehicleType.EMC: 40.0,
        VehicleType.ECART: 60.0,
        VehicleType.MOTORCYCLE: 45.0,
        VehicleType.TRICYCLE: 6.05
    }

    increments = {
        VehicleType.EMC: 10.0,
        VehicleType.ECART: 10.0,
        VehicleType.MOTORCYCLE: 15,
        VehicleType.TRICYCLE: 15
    }

    distance_km = geodesic((location_from.lat, location_from.lng), (location_to.lat, location_to.lng)).kilometers
    distance = round(distance_km)

    if distance_km <= 0:
        raise ValueError('Distance must be greater than 0.')

    if distance_km <= 1:
        return base_fares[taxi_type]

    if distance_km < 10:
        return round(base_fares[taxi_type] + int(distance - 1) * increments[taxi_type], 2)

    return round(base_fares[taxi_type] + (distance_km - 1) * increments[taxi_type], 2)
