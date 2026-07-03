package preq.web.dto.request

data class UpdateShoppingListRequest(
    val completed: Boolean,
    val items: List<UpdateShoppingListItemRequest>,
)
