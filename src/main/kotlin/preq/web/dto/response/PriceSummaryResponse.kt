package preq.web.dto.response

import preq.web.dto.projection.PriceStats
import preq.web.dto.projection.TopLocationResult

data class PriceSummaryResponse(
    val avgPrice: Double?,
    val maxPrice: Double?,
    val minPrice: Double?,
    val weightedPrice: Double?,
    val weightedByConfidencePrice: Double?,
    val topLocations: List<TopLocationResponse>,
    val totalReportCount: Long,
) {
    companion object {
        fun from(
            stats: PriceStats,
            topLocations: List<TopLocationResult>,
            weightedPrice: Double?,
            weightedByConfidencePrice: Double?,
        ) = PriceSummaryResponse(
            avgPrice = stats.getAvgPrice(),
            maxPrice = stats.getMaxPrice(),
            minPrice = stats.getMinPrice(),
            weightedPrice = weightedPrice,
            weightedByConfidencePrice = weightedByConfidencePrice,
            topLocations = topLocations.map { TopLocationResponse.from(it) },
            totalReportCount = stats.getTotalReportCount(),
        )
    }
}