package preq.service

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import preq.enum.ProductImageStatus
import preq.model.Product
import preq.model.ProductImage
import preq.repository.ProductImageRepository
import preq.repository.ProductRepository
import preq.service.mapper.ProductMapper
import preq.web.dto.request.CreateProductRequest
import preq.web.dto.response.ProductDetectionResponse

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productImageRepository: ProductImageRepository,
    private val imageEmbeddingService: ImageEmbeddingService,
    private val cloudinaryService: CloudinaryService,
    private val barcodeService: BarcodeService,
    private val apiMapper: ProductMapper,
    private val confidenceThreshold: Double = 0.78,
) {
    fun getOrCreateByBarcode(barcode: String): Product {
        val existing = productRepository.findByBarcode(barcode)
        if (existing != null) return existing

        val response = barcodeService.getProduct(barcode)
            ?: throw RuntimeException("API error")

        if (response.status != 1 || response.product == null) {
            throw NoSuchElementException()
        }

        val product = apiMapper.fromApi(barcode, response.product)

        return productRepository.save(product)
    }

    fun detect(file: MultipartFile): List<ProductDetectionResponse> {
        val embedding = imageEmbeddingService.generateEmbedding(file)
        val vectorString = embedding.joinToString(",", "[", "]")

        val results = productImageRepository.findSimilarProducts(vectorString, 10)
        val productMap = productRepository.findAllById(results.map { it.getProductId() }).associateBy { it.id }

        return results.mapNotNull { result ->
            productMap[result.getProductId()]?.let { product ->
                ProductDetectionResponse.from(product, result.getSimilarity(), confidenceThreshold)
            }
        }
    }

    fun addImage(
        productId: Long,
        file: MultipartFile,
    ): Product {
        val product =
            productRepository
                .findById(productId)
                .orElseThrow { NoSuchElementException("Product not found") }
        val imageUrl = cloudinaryService.upload(file)
        val embedding = imageEmbeddingService.generateEmbedding(file)

        product.images.add(
            ProductImage().apply {
                this.product = product
                this.embedding = embedding
                this.imageUrl = imageUrl
                this.confidenceScore = 1.0
                this.status = ProductImageStatus.APPROVED
            },
        )

        return productRepository.save(product)
    }

    fun create(request: CreateProductRequest): Product =
        productRepository.save(
            Product().apply {
                name = request.name
                brand = request.brand
                quantity = request.quantity
                quantityType = request.quantityType
                barcode = request.barcode
            },
        )

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
