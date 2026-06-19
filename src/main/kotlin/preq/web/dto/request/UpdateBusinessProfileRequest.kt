package preq.web.dto.request

data class UpdateBusinessProfileRequest(
    val ownerName: String,
    val ownerLastName: String,
    val businessPhone: String,
    val cuit: String? = null,
    val locationName: String? = null,
    val locationAddress: String? = null,
)
