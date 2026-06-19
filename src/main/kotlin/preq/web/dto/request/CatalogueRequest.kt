package preq.web.dto.request

import java.math.BigDecimal

data class CatalogueRequest(
    val productId: Long,
    val price: BigDecimal,
)