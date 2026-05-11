package preq.web.dto.response

data class CartCompareResponse(
    val locations: List<CartLocationResponse>,
    val skippedProducts: List<String>,
)
