package preq.web.dto.response

import preq.model.ProductImage

data class ProductImageResponse(
    val id: Long,
    val imageUrl: String,
    val disputeCount: Long,
) {
    companion object {
        fun from(
            image: ProductImage,
            disputeCount: Long,
        ) = ProductImageResponse(
            id = image.id,
            imageUrl = image.imageUrl,
            disputeCount = disputeCount,
        )
    }
}
