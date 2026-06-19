package preq.web.dto.response

import preq.model.LocationProductPrice
import java.math.BigDecimal
import java.time.LocalDateTime

data class CatalogueItemResponse(
    val productId: Long,
    val name: String,
    val brand: String,
    val quantity: BigDecimal,
    val quantityType: String,
    val barcode: String?,
    val price: BigDecimal,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(lpp: LocationProductPrice) =
            CatalogueItemResponse(
                productId = lpp.product!!.id,
                name = lpp.product!!.name,
                brand = lpp.product!!.brand,
                quantity = lpp.product!!.quantity,
                quantityType = lpp.product!!.quantityType,
                barcode = lpp.product!!.barcode,
                price = lpp.price,
                updatedAt = lpp.reportedAt,
            )
    }
}
