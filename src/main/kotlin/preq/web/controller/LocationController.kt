package preq.web.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import preq.model.User
import preq.repository.UserRepository
import preq.service.LocationService
import preq.web.dto.request.BusinessRegisterRequest
import preq.web.dto.request.CreateLocationRequest
import preq.web.dto.request.UpdateBusinessProfileRequest
import preq.web.dto.response.BusinessProfileResponse
import preq.web.dto.response.BusinessRegisterResponse
import preq.web.dto.response.LocationDetectionResponse
import preq.web.dto.response.LocationResponse
import preq.web.dto.response.LocationSearchResponse
import java.security.Principal

@RestController
@RequestMapping("/api/locations")
class LocationController(
    private val locationService: LocationService,
    private val userRepository: UserRepository
) {
    private fun user(principal: Principal): User = userRepository.findByEmail(principal.name).orElseThrow()

    @GetMapping("/search")
    fun search(
        @RequestParam name: String,
    ): List<LocationResponse> = locationService.search(name).map { LocationResponse.from(it) }

    @PostMapping
    fun create(
        @RequestBody request: CreateLocationRequest,
    ): LocationResponse = LocationResponse.from(locationService.create(request))

    @PostMapping("/register")
    fun registerBusiness(
        @RequestBody request: BusinessRegisterRequest,
    ): BusinessRegisterResponse {
        val response = locationService.register(request)
        return response
    }

    @PostMapping("/nearby")
    fun findNearby(
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
    ): LocationDetectionResponse = locationService.findNearby(latitude, longitude)

    @GetMapping("/search-point")
    fun searchNearby(
        @RequestParam lat: Double,
        @RequestParam lng: Double,
    ): List<LocationSearchResponse> {
        val results = locationService.searchNearby(lat, lng)
        return results
    }

    @GetMapping("/profile")
    fun getProfile(principal: Principal): BusinessProfileResponse {
        val user = user(principal)
        return locationService.getProfile(user)
    }

    @PutMapping("/update")
    fun updateProfile(
        principal: Principal,
        @RequestBody request: UpdateBusinessProfileRequest,
    ): BusinessProfileResponse {
        val user = user(principal)
        return locationService.updateProfile(user, request)
    }
}
