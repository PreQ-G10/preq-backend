package preq.web.dto.response

import preq.web.dto.projection.PriceStats
import preq.web.dto.projection.TopLocationResult

data class PriceSummaryResponse(
    val avgPrice: Double,
    val maxPrice: Double,
    val minPrice: Double,
    val weightedPrice: Double,
    val topLocations: List<TopLocationResponse>,
) {
    companion object {
        fun from(
            stats: PriceStats,
            topLocations: List<TopLocationResult>,
            weightedPrice: Double,
        ) = PriceSummaryResponse(
            avgPrice = stats.getAvgPrice(),
            maxPrice = stats.getMaxPrice(),
            minPrice = stats.getMinPrice(),
            weightedPrice = weightedPrice,
            topLocations =
                topLocations.map {
                    TopLocationResponse(
                        name = it.getName(),
                        address = it.getAddress(),
                        avgPrice = it.getAvgPrice(),
                        reportCount = it.getReportCount(),
                    )
                },
        )
    }
}
