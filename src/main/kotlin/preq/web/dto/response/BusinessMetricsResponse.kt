package preq.web.dto.response

import java.math.BigDecimal

data class BusinessMetricsResponse(
    val uniqueUsersLast30Days: Long,
    val averagePriceLast10Lists: BigDecimal?,
    val topProducts: List<TopProductResponse>,
)
