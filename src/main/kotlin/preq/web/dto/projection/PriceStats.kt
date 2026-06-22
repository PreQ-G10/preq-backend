package preq.web.dto.projection

interface PriceStats {
    fun getAvgPrice(): Double?

    fun getMaxPrice(): Double?

    fun getMinPrice(): Double?

    fun getTotalReportCount(): Long
}
