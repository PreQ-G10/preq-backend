package preq.web.dto

import com.fasterxml.jackson.annotation.JsonProperty
import preq.model.Product

data class ProductDetectionResponse(
    val productId: Long,
    val name: String,
    val brand: String,
    val quantity: Int,
    val quantityType: String,
    val imageUrl: String?,
    val similarity: Double,
    val confident: Boolean
) {
    companion object {
        fun from(product: Product, similarity: Double, confidenceThreshold: Double) = ProductDetectionResponse(
            productId = product.id,
            name = product.name,
            brand = product.brand,
            quantity = product.quantity,
            quantityType = product.quantityType,
            imageUrl = product.approvedImages().firstOrNull()?.imageUrl,
            similarity = similarity,
            confident = similarity >= confidenceThreshold
        )
    }
}