package preq.web.dto.response

import java.time.LocalDateTime

data class ShoppingListSummaryResponse(
    val id: Long,
    val locationId: Long,
    val locationName: String,
    val locationAddress: String,
    val completed: Boolean,
    val createdAt: LocalDateTime,
    val itemCount: Int
)