package preq.web.dto.request

data class CartItemRequest(
    val productId: Long,
    val quantity: Int,
)