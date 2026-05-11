package preq.web.dto.response

import preq.enum.PriceSource

data class CartProductResponse(
    val productId: Long,
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val priceSource: PriceSource,
)
