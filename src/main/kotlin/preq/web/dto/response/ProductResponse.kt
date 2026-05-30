package preq.web.dto.response

import preq.model.Product
import java.math.BigDecimal

data class ProductResponse(
    val id: Long,
    val name: String,
    val brand: String,
    val quantity: BigDecimal,
    val quantityType: String,
    val minPrice: BigDecimal?,
    val maxPrice: BigDecimal?,
    val barcode: String?,
    val images: List<String>,
) {
    companion object {
        fun from(product: Product) =
            ProductResponse(
                id = product.id,
                name = product.name,
                brand = product.brand,
                quantity = product.quantity,
                quantityType = product.quantityType,
                minPrice = product.minPrice,
                maxPrice = product.maxPrice,
                barcode = product.barcode,
                images = product.approvedImages().map { it.imageUrl },
            )
    }
}
