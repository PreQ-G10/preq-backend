package preq.web.dto.response

import preq.model.LocationProductPrice
import java.math.BigDecimal
import java.time.LocalDateTime

data class PendingValidationResponse(
    val id: Long,
    val price: BigDecimal,
    val reportedAt: LocalDateTime,
    val locationId: Long,
    val locationName: String,
    val locationAddress: String,
    val product: ProductResponse,
) {
    companion object {
        fun from(report: LocationProductPrice) =
            PendingValidationResponse(
                id = report.id,
                price = report.price,
                reportedAt = report.reportedAt,
                locationId = report.location!!.id,
                locationName = report.location!!.name,
                locationAddress = report.location!!.address,
                product = ProductResponse.from(report.product!!),
            )
    }
}
