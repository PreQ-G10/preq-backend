package preq.service

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import preq.enum.BarcodeDetectionStatus
import preq.enum.ProductImageStatus
import preq.exceptions.ExternalApiException
import preq.model.Product
import preq.model.ProductImage
import preq.repository.ProductImageRepository
import preq.repository.ProductRepository
import preq.util.mapper.ProductMapper
import preq.web.dto.request.CreateProductRequest
import preq.web.dto.response.BarcodeDetectionResponse
import preq.web.dto.response.ProductDetectionResponse
import preq.web.dto.response.ProductResponse

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productImageRepository: ProductImageRepository,
    private val imageEmbeddingService: ImageEmbeddingService,
    private val cloudinaryService: CloudinaryService,
    private val openFoodFactsService: OpenFoodFactsService,
    private val confidenceThreshold: Double = 0.78,
) {
    val productMapper = ProductMapper

    fun getOrCreateByBarcode(barcode: String): BarcodeDetectionResponse {
        // Already in DB
        val existing = productRepository.findByBarcode(barcode)
        if (existing != null) return BarcodeDetectionResponse(
            status = BarcodeDetectionStatus.FOUND,
            product = ProductResponse.from(existing)
        )

        // Query external API
        val response = openFoodFactsService.getProduct(barcode)
            ?: return BarcodeDetectionResponse(status = BarcodeDetectionStatus.NOT_FOUND)
        if (response.status != 1 || response.product == null) {
            return BarcodeDetectionResponse(status = BarcodeDetectionStatus.NOT_FOUND)
        }

        val apiResponse = response.product
        val name = apiResponse.product_name?.takeIf { it.isNotBlank() }
            ?: return BarcodeDetectionResponse(status = BarcodeDetectionStatus.INCOMPLETE_DATA)
        val brand = apiResponse.brands?.takeIf { it.isNotBlank() }
            ?: return BarcodeDetectionResponse(status = BarcodeDetectionStatus.INCOMPLETE_DATA)
        val quantity = apiResponse.product_quantity
            ?: return BarcodeDetectionResponse(status = BarcodeDetectionStatus.INCOMPLETE_DATA)
        val quantityType = apiResponse.product_quantity_unit?.takeIf { it.isNotBlank() }
            ?: return BarcodeDetectionResponse(status = BarcodeDetectionStatus.INCOMPLETE_DATA)

        // Check for manual product collision
        val collisions = productRepository.findPotentialCollisions(
             name, brand, quantity, quantityType
        )
        val collision = collisions.firstOrNull()

        if (collision != null) {
            val apiProductMapped = productMapper.fromOpenFoodFactsApiResponse(barcode, apiResponse)
            return BarcodeDetectionResponse(
                status = BarcodeDetectionStatus.COLLISION,
                apiProduct = ProductResponse(
                    id = collision.id,
                    name = apiProductMapped.name,
                    brand = apiProductMapped.brand,
                    quantity = apiProductMapped.quantity,
                    quantityType = apiProductMapped.quantityType,
                    barcode = barcode,
                    images = apiProductMapped.images.map { it.imageUrl },
                ),
                existingProduct = ProductResponse.from(collision)
            )
        }

        // No collision, create new
        val product = productRepository.save(productMapper.fromOpenFoodFactsApiResponse(barcode, response.product))
        return BarcodeDetectionResponse(
            status = BarcodeDetectionStatus.CREATED,
            product = ProductResponse.from(product)
        )
    }

    fun resolveBarcodeCollision(existingId: Long, barcode: String, confirm: Boolean): Product {
        if (confirm) {
            // User confirmed — assign barcode to existing product
            val product = productRepository.findById(existingId).orElseThrow()
            product.barcode = barcode
            return productRepository.save(product)
        } else {
            // User denied — create new product from API
            val response = openFoodFactsService.getProduct(barcode)
                ?: throw ExternalApiException("OpenFoodFacts API error")
            if (response.status != 1 || response.product == null) throw NoSuchElementException()
            return productRepository.save(productMapper.fromOpenFoodFactsApiResponse(barcode, response.product))
        }
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
