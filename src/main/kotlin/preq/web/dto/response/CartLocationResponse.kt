package preq.web.dto.response

data class CartLocationResponse(
    val locationId: Long,
    val name: String,
    val address: String,
    val totalEstimatedPrice: Double,
    val distanceMeters: Double?,
    val products: List<CartProductResponse>,
)