package preq.web.dto.response

data class OpenFoodFactsResponse(
    val status: Int,
    val product: OpenFoodFactsProductResponse?
)