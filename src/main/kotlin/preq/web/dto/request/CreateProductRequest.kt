package preq.web.dto.request

import java.math.BigDecimal

data class CreateProductRequest(
    val name: String,
    val brand: String,
    val quantity: BigDecimal,
    val quantityType: String,
    val barcode: String? = null,
)
