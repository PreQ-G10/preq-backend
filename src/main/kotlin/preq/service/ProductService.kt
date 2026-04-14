package preq.service

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import preq.enum.ProductImageStatus
import preq.model.Product
import preq.model.ProductImage
import preq.repository.ProductImageRepository
import preq.repository.ProductRepository
import preq.web.dto.ProductDetectionResponse

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productImageRepository: ProductImageRepository,
    private val imageEmbeddingService: ImageEmbeddingService,
    private val cloudinaryService: CloudinaryService,
    private val confidenceThreshold: Double = 0.78,
) {
    fun detect(file: MultipartFile): List<ProductDetectionResponse> {
        val embedding = imageEmbeddingService.generateEmbedding(file)
        val vectorString = embedding.joinToString(",", "[", "]")

        val results = productImageRepository.findSimilarProducts(vectorString, 10)

        val productMap =
            productRepository
                .findAllById(
                    results.map { (it[0] as Number).toLong() },
                ).associateBy { it.id }

        return results.mapNotNull { row ->
            val productId = (row[0] as Number).toLong()
            val similarity = (row[1] as Number).toDouble()
            productMap[productId]?.let { ProductDetectionResponse.from(it, similarity, confidenceThreshold) }
        }
    }

    fun confirmMatch(
        productId: Long,
        file: MultipartFile,
        similarity: Double,
    ): Product {
        val product = productRepository.findById(productId).orElseThrow()
        val embedding = imageEmbeddingService.generateEmbedding(file)
        val imageUrl = cloudinaryService.upload(file)

        val status =
            if (similarity >= confidenceThreshold) {
                ProductImageStatus.APPROVED
            } else {
                ProductImageStatus.PENDING_REVIEW
            }

        product.images.add(
            ProductImage().apply {
                this.product = product
                this.embedding = embedding
                this.imageUrl = imageUrl
                this.confidenceScore = similarity
                this.status = status
            },
        )

        return productRepository.save(product)
    }

    fun searchByName(name: String): List<Product> = productRepository.searchByName(name)
}
