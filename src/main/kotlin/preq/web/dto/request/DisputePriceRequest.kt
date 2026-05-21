package preq.web.dto.request

import java.math.BigDecimal

data class DisputePriceRequest(
    val alternativePrice: BigDecimal,
    val userLatitude: Double?,
    val userLongitude: Double?,
)