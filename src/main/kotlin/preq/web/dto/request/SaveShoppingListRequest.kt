package preq.web.dto.request

data class SaveShoppingListRequest(
    val locationId: Long,
    val items: List<ShoppingListItemRequest>
)