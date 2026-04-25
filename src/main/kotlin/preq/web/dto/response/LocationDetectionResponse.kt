package preq.web.dto.response

data class LocationDetectionResponse(
    val location: LocationResponse,
    val distanceMeters: Double,
) {
    companion object {
        fun from(
            locationResponse: LocationResponse,
            distanceMeters: Double,
        ): LocationDetectionResponse = LocationDetectionResponse(locationResponse, distanceMeters)
    }
}
