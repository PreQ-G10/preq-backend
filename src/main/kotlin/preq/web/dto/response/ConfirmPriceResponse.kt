package preq.web.dto.response

import java.time.LocalDateTime

data class ConfirmPriceResponse(
    val confirmedAt: LocalDateTime = LocalDateTime.now(),
)