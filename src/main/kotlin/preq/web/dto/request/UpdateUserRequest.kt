package preq.web.dto.request

data class UpdateUserRequest(
    val name: String,
    val lastName: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)