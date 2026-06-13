package preq.web.dto.response

import java.time.LocalDate

data class PriceHistoryPointResponse(
    val weekStart: String,
    val avgPrice: Double,
) {
    companion object {
        fun from(weekStart: LocalDate, avgPrice: Double) = PriceHistoryPointResponse(
            weekStart = weekStart.toString(),
            avgPrice = avgPrice,
        )
    }
}