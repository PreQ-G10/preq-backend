package preq.web.dto.response

import preq.model.LocationProductPrice
import java.math.BigDecimal
import java.time.LocalDateTime

data class LocationProductPriceResponse(
    val id: Long,
    val productId: Long,
    val locationId: Long,
    val price: BigDecimal,
    val reportedAt: LocalDateTime,
) {
    companion object {
        fun from(price: LocationProductPrice) =
            LocationProductPriceResponse(
                id = price.id,
                productId = price.product!!.id,
                locationId = price.location!!.id,
                price = price.price,
                reportedAt = price.reportedAt,
            )
    }
}
