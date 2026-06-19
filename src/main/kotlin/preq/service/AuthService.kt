package preq.service

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import preq.model.User
import preq.repository.UserRepository
import preq.web.dto.request.LoginRequest
import preq.web.dto.request.RefreshTokenRequest
import preq.web.dto.request.RegisterRequest
import preq.web.dto.response.AuthResponse
import jakarta.persistence.EntityNotFoundException
import preq.enum.UserRole

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager,
) {
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already registered")
        }

        val user = User().apply {
            this.name = request.name
            this.lastName = request.lastName
            this.address = request.address
            this.addressLocation = (
                    if (request.latitude != null && request.longitude != null) {
                        GeometryFactory(PrecisionModel(), 4326)
                            .createPoint(Coordinate(request.longitude, request.latitude))
                    } else {
                        null
                    }
                    )
            this.email = request.email
            this.role = UserRole.valueOf(request.role)
            this.password = passwordEncoder.encode(request.password)
        }

        userRepository.save(user)

        return AuthResponse(
            accessToken = jwtService.generateAccessToken(user),
            refreshToken = jwtService.generateRefreshToken(user.email),
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password),
        )

        val user = userRepository.findByEmail(request.email)
            .orElseThrow { EntityNotFoundException("User not found") }

        return AuthResponse(
            accessToken = jwtService.generateAccessToken(user),
            refreshToken = jwtService.generateRefreshToken(user.email),
        )
    }

    fun refresh(request: RefreshTokenRequest): AuthResponse {
        val email = jwtService.extractEmail(request.refreshToken)

        if (!jwtService.isTokenValid(request.refreshToken)) {
            throw IllegalArgumentException("Invalid or expired refresh token")
        }

        val user = userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("User not found") }

        return AuthResponse(
            accessToken = jwtService.generateAccessToken(user),
            refreshToken = jwtService.generateRefreshToken(user.email),
        )
    }
}
