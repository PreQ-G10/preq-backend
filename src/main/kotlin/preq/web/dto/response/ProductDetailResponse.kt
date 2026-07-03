package preq.web.dto.response

import preq.model.Product
import preq.model.ProductImage
import java.math.BigDecimal

data class ProductDetailResponse(
    val id: Long,
    val name: String,
    val brand: String,
    val quantity: BigDecimal,
    val quantityType: String,
    val barcode: String?,
    val images: List<ProductImageResponse>,
) {
    companion object {
        fun from(
            product: Product,
            images: List<ProductImage>,
            disputeCounts: Map<Long, Long>,
        ) = ProductDetailResponse(
            id = product.id,
            name = product.name,
            brand = product.brand,
            quantity = product.quantity,
            quantityType = product.quantityType,
            barcode = product.barcode,
            images =
                images.map { image ->
                    ProductImageResponse.from(image, disputeCounts.getOrDefault(image.id, 0L))
                },
        )
    }
}
