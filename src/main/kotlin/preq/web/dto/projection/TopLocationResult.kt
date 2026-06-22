package preq.web.dto.projection

interface TopLocationResult {
    fun getId(): Long
    fun getName(): String
    fun getAddress(): String
    fun getAvgPrice(): Double
    fun getReportCount(): Int
}