package preq.service

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.stereotype.Service
import preq.model.Location
import preq.repository.LocationRepository
import preq.web.dto.request.CreateLocationRequest
import preq.web.dto.response.LocationDetectionResponse
import preq.web.dto.response.LocationResponse

@Service
class LocationService(
    private val locationRepository: LocationRepository,
) {
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    fun search(name: String): List<Location> = locationRepository.searchByName(name)

    fun create(request: CreateLocationRequest): Location {
        val requestLatitude = request.latitude
        val requestLongitude = request.longitude
        val calculatedCoordinates = geometryFactory.createPoint(Coordinate(request.longitude, request.latitude))

        return locationRepository.save(
            Location().apply {
                name = request.name
                address = request.address
                type = request.type
                latitude = requestLatitude
                longitude = requestLongitude
                coordinates = calculatedCoordinates
            },
        )
    }

    fun findNearby(
        latitude: Double,
        longitude: Double,
    ): LocationDetectionResponse {
        val locationDetected =
            locationRepository.findWithinRange(latitude, longitude, 150.00)
                ?: throw(NoSuchElementException("No location found with latitude $latitude, longitude $longitude"))

        val locationResponse = LocationResponse.from(locationDetected)
        val distanceMeters = locationDetected.getDistanceMeters()

        return LocationDetectionResponse.from(locationResponse, distanceMeters)
    }
}
