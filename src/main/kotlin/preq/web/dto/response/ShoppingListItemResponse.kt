package preq.web.dto.response

import preq.model.ShoppingListItem
import java.math.BigDecimal

data class ShoppingListItemResponse(
    val id: Long,
    val productId: Long,
    val name: String,
    val brand: String,
    val quantity: String,
    val quantityType: String,
    val cartQuantity: Int,
    val checkedQuantity: Int,
    val unitPrice: BigDecimal
) {
    companion object {
        fun from(item: ShoppingListItem) = ShoppingListItemResponse(
            id = item.id,
            productId = item.product.id,
            name = item.product.name,
            brand = item.product.brand,
            quantity = item.product.quantity.toPlainString(),
            quantityType = item.product.quantityType,
            cartQuantity = item.cartQuantity,
            checkedQuantity = item.checkedQuantity,
            unitPrice = item.unitPrice
        )
    }
}