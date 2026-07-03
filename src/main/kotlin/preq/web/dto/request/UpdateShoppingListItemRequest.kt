package preq.web.dto.request

data class UpdateShoppingListItemRequest(
    val itemId: Long,
    val checkedQuantity: Int
)