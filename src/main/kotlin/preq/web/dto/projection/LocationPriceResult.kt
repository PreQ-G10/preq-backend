package preq.web.dto.projection

interface LocationPriceResult {
    fun getLocationId(): Long
    fun getName(): String
    fun getAddress(): String
    fun getLatitude(): Double?
    fun getLongitude(): Double?
    fun getAvgPrice(): Double
}