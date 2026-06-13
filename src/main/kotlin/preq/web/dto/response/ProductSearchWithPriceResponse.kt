package preq.web.dto.response

data class ProductSearchWithPriceResponse(
    val product: ProductResponse,
    val maxPrice: Double?,
    val minPrice: Double?,
)
