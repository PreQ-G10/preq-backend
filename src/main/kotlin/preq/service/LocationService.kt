package preq.service

import jakarta.persistence.EntityNotFoundException
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.stereotype.Service
import preq.enum.ClaimStatus
import preq.enum.LocationType
import preq.model.Location
import preq.model.User
import preq.repository.LocationRepository
import preq.repository.UserRepository
import preq.web.dto.request.BusinessRegisterRequest
import preq.web.dto.request.CreateLocationRequest
import preq.web.dto.request.RegisterRequest
import preq.web.dto.request.UpdateBusinessProfileRequest
import preq.web.dto.response.AuthResponse
import preq.web.dto.response.BusinessProfileResponse
import preq.web.dto.response.BusinessRegisterResponse
import preq.web.dto.response.LocationDetectionResponse
import preq.web.dto.response.LocationResponse
import preq.web.dto.response.LocationSearchResponse
import kotlin.jvm.optionals.getOrElse

@Service
class LocationService(
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository,
    private val authService: AuthService,
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

        return if (locationDetected != null) {
            val locationResponse = LocationResponse.from(locationDetected)
            val distanceMeters = locationDetected.getDistanceMeters()
            LocationDetectionResponse.found(locationResponse, distanceMeters)
        } else {
            LocationDetectionResponse.notFound()
        }
    }

    fun searchNearby(
        latitude: Double,
        longitude: Double,
    ): List<LocationSearchResponse> =
        locationRepository
            .findNearby(latitude, longitude)
            .map { LocationSearchResponse.from(it) }

    fun register(request: BusinessRegisterRequest): BusinessRegisterResponse {
        check(!userRepository.existsByEmail(request.email)) { "Email already in use" }

        val location =
            if (request.locationId != null) {
                findAndValidateLocation(request.locationId)
            } else {
                createLocation(request)
            }

        val registerUserResponse =
            authService.register(
                RegisterRequest(
                    name = request.ownerName,
                    lastName = request.ownerLastName,
                    email = request.email,
                    password = request.password,
                    role = "BUSINESS",
                ),
            )

        val user =
            userRepository
                .findByEmail(request.email)
                .getOrElse { throw EntityNotFoundException("We had a problem registering user ${request.email}") }

        locationRepository.save(
            location.apply {
                claimedBy = user
                businessPhone = request.businessPhone
                cuit = request.cuit
                claimStatus = if (request.cuit != null) ClaimStatus.PENDING_FORMAL else ClaimStatus.PENDING
            },
        )

        return BusinessRegisterResponse(
            userRegisterResponse = AuthResponse(registerUserResponse.accessToken, registerUserResponse.refreshToken),
            locationResponse = LocationResponse.from(location),
        )
    }

    private fun findAndValidateLocation(locationId: Long): Location =
        locationRepository
            .findById(locationId)
            .orElseThrow { EntityNotFoundException("Location $locationId not found") }
            .also {
                check(!it.isClaimed()) { "Location is already claimed" }
                check(!it.isPending()) { "Location already has a pending claim" }
            }

    private fun createLocation(request: BusinessRegisterRequest): Location {
        requireNotNull(request.locationName) { "Location name is required" }
        requireNotNull(request.locationAddress) { "Location address is required" }

        return locationRepository.save(
            Location().apply {
                name = request.locationName
                address = request.locationAddress
                type = request.locationType ?: LocationType.OTHER
                latitude = request.latitude
                longitude = request.longitude
            },
        )
    }

    fun getProfile(user: User): BusinessProfileResponse {
        val location =
            locationRepository.findByClaimedBy(user)
                ?: throw EntityNotFoundException("No location found for this business account")
        return BusinessProfileResponse.from(user, location)
    }

    fun updateProfile(
        user: User,
        request: UpdateBusinessProfileRequest,
    ): BusinessProfileResponse {
        val location =
            locationRepository.findByClaimedBy(user)
                ?: throw EntityNotFoundException("No location found for this business account")

        user.name = request.ownerName
        user.lastName = request.ownerLastName
        userRepository.save(user)

        location.businessPhone = request.businessPhone
        location.cuit = request.cuit
        request.locationName?.let { location.name = it }
        request.locationAddress?.let { location.address = it }
        locationRepository.save(location)

        return BusinessProfileResponse.from(user, location)
    }
}
