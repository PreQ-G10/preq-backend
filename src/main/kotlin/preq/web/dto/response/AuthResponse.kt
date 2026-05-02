package preq.web.dto.response

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String
)