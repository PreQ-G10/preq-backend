package preq.web.dto.request

import java.math.BigDecimal

data class ReportProductPriceRequest(
    val productId: Long,
    val locationId: Long,
    val price: BigDecimal,
)
