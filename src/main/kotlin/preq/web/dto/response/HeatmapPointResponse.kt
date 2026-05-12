package preq.web.dto.response

import java.math.BigDecimal

data class HeatmapPointResponse(
    val locationId: Long,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val avgPrice: BigDecimal,
)
