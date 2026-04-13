package preq.web.dto

data class PriceSummaryResponse(
    val avgPrice: Double,
    val maxPrice: Double,
    val minPrice: Double,
    val weightedPrice: Double,
    val topLocations: List<TopLocationResponse>
) {
    companion object {
        fun from(
            stats: Array<Any>,
            topLocations: List<Array<Any>>,
            weightedPrice: Double
        ) = PriceSummaryResponse(
            avgPrice = (stats[0] as Number).toDouble(),
            maxPrice = (stats[1] as Number).toDouble(),
            minPrice = (stats[2] as Number).toDouble(),
            weightedPrice = weightedPrice,
            topLocations = topLocations.map { TopLocationResponse.from(it) }
        )
    }
}