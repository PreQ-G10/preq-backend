package preq.web.dto.request

import java.math.BigDecimal

data class ShoppingListItemRequest(
    val productId: Long,
    val cartQuantity: Int,
    val unitPrice: BigDecimal
)