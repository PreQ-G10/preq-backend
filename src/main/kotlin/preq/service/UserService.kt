package preq.service

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.stereotype.Service
import preq.model.User
import preq.repository.UserRepository
import preq.web.dto.request.UpdateUserRequest
import preq.web.dto.response.UserProfileResponse

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun getProfile(email: String): UserProfileResponse {
        val user = userRepository.findByEmail(email).orElseThrow { IllegalArgumentException("User not found") }
        return UserProfileResponse(
            name = user.name,
            lastName = user.lastName,
            email = user.email,
            address = user.address,
            latitude = user.addressLocation?.y,
            longitude = user.addressLocation?.x,
        )
    }

    fun updateProfile(
        email: String,
        request: UpdateUserRequest,
    ): UserProfileResponse {
        val user = userRepository.findByEmail(email).orElseThrow { IllegalArgumentException("User not found") }
        user.name = request.name
        user.lastName = request.lastName
        user.address = request.address
        user.addressLocation =
            if (request.latitude != null && request.longitude != null) {
                GeometryFactory(PrecisionModel(), 4326)
                    .createPoint(Coordinate(request.longitude, request.latitude))
            } else {
                null
            }
        userRepository.save(user)
        return getProfile(email)
    }

    fun addScore(
        user: User,
        score: Double,
    ) {
        user.trustScore += score
        userRepository.save(user)
    }
}
