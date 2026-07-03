package preq.web.dto.response

import preq.model.ShoppingList
import java.math.BigDecimal
import java.time.LocalDateTime

data class ShoppingListResponse(
    val id: Long,
    val locationId: Long,
    val locationName: String,
    val locationAddress: String,
    val completed: Boolean,
    val totalPrice: BigDecimal,
    val createdAt: LocalDateTime,
    val items: List<ShoppingListItemResponse>
) {
    companion object {
        fun from(list: ShoppingList) = ShoppingListResponse(
            id = list.id,
            locationId = list.location.id,
            locationName = list.location.name,
            locationAddress = list.location.address,
            completed = list.completed,
            totalPrice = list.totalPrice,
            createdAt = list.createdAt,
            items = list.items.map { ShoppingListItemResponse.from(it) }
        )
    }
}