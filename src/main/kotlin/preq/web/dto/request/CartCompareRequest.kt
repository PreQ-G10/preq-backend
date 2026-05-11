package preq.web.dto.request

data class CartCompareRequest(
    val items: List<CartItemRequest>,
    val userLatitude: Double?,
    val userLongitude: Double?,
)