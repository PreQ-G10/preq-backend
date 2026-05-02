package preq.web.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import preq.service.AuthService
import preq.web.dto.request.LoginRequest
import preq.web.dto.request.RefreshTokenRequest
import preq.web.dto.request.RegisterRequest
import preq.web.dto.response.AuthResponse

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): AuthResponse =
        authService.register(request)

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): AuthResponse =
        authService.login(request)

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshTokenRequest): AuthResponse =
        authService.refresh(request)
}