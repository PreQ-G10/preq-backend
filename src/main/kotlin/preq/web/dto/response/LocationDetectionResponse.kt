package preq.web.dto.response

import preq.enum.LocationDetectionStatus

data class LocationDetectionResponse(
    val location: LocationResponse?,
    val distanceMeters: Double?,
    val status: LocationDetectionStatus,
) {
    companion object {
        fun found(
            locationResponse: LocationResponse,
            distanceMeters: Double,
        ): LocationDetectionResponse = LocationDetectionResponse(locationResponse, distanceMeters, LocationDetectionStatus.FOUND)

        fun notFound(): LocationDetectionResponse = LocationDetectionResponse(null, null, LocationDetectionStatus.NOT_FOUND)
    }
}
