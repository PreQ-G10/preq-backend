package preq.web.dto.response

data class BusinessRegisterResponse(
    val userRegisterResponse: AuthResponse,
    val locationResponse: LocationResponse,
)