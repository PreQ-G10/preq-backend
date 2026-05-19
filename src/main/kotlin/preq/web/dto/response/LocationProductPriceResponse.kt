package preq.web.dto.response

import preq.enum.PriceValidity
import preq.model.LocationProductPrice
import java.math.BigDecimal
import java.time.LocalDateTime

data class LocationProductPriceResponse(
    val id: Long,
    val productId: Long,
    val locationId: Long,
    val price: BigDecimal,
    val reportedAt: LocalDateTime,
    val status: PriceValidity,
) {
    companion object {
        fun from(price: LocationProductPrice) =
            LocationProductPriceResponse(
                id = price.id,
                productId = price.product!!.id,
                locationId = price.location!!.id,
                price = price.price,
                reportedAt = price.reportedAt,
                status = price.priceValidity,
            )
    }
}
