package preq.web.dto.response

data class UserProfileResponse(
    val name: String,
    val lastName: String,
    val email: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)