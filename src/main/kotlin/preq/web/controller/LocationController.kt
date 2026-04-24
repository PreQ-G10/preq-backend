package preq.web.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import preq.service.LocationService
import preq.web.dto.request.CreateLocationRequest
import preq.web.dto.response.LocationDetectionResponse
import preq.web.dto.response.LocationResponse

@RestController
@RequestMapping("/api/locations")
class LocationController(
    private val locationService: LocationService,
) {
    @GetMapping("/search")
    fun search(
        @RequestParam name: String,
    ): List<LocationResponse> = locationService.search(name).map { LocationResponse.from(it) }

    @PostMapping
    fun create(
        @RequestBody request: CreateLocationRequest,
    ): LocationResponse = LocationResponse.from(locationService.create(request))

    @PostMapping("/nearby")
    fun findNearby(
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
    ): LocationDetectionResponse = locationService.findNearby(latitude, longitude)
}
