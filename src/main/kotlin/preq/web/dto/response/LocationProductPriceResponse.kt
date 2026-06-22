package preq.web.dto.response

import preq.enum.ReportScore
import preq.enum.ReportSource
import preq.model.LocationProductPrice
import java.math.BigDecimal
import java.time.LocalDateTime

data class LocationProductPriceResponse(
    val id: Long,
    val productId: Long,
    val locationId: Long,
    val price: BigDecimal,
    val reportedAt: LocalDateTime,
    val score: Double,
    val reportScore: ReportScore,
    val isBusinessReported: Boolean,
) {
    companion object {
        fun from(price: LocationProductPrice) =
            LocationProductPriceResponse(
                id = price.id,
                productId = price.product!!.id,
                locationId = price.location!!.id,
                price = price.price,
                reportedAt = price.reportedAt,
                score = price.score,
                reportScore = price.reportScore,
                isBusinessReported = price.source == ReportSource.BUSINESS_CATALOGUE,
            )
    }
}
