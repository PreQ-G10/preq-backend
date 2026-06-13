package preq.web.dto.projection

import java.time.LocalDateTime

interface PriceHistoryResult {
    fun getWeekStart(): LocalDateTime

    fun getAvgPrice(): Double
}
