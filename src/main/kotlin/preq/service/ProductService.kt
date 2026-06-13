package preq.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import preq.enum.BarcodeDetectionStatus
import preq.enum.FieldContestStatus
import preq.enum.FieldType
import preq.enum.ProductImageStatus
import preq.exceptions.ExternalApiException
import preq.model.Product
import preq.model.ProductFieldContest
import preq.model.ProductImage
import preq.model.User
import preq.repository.LocationProductPriceRepository
import preq.repository.ProductFieldContestRepository
import preq.repository.ProductImageRepository
import preq.repository.ProductRepository
import preq.util.mapper.ProductMapper
import preq.web.dto.request.ContestProductFieldRequest
import preq.web.dto.request.CreateProductRequest
import preq.web.dto.response.BarcodeDetectionResponse
import preq.web.dto.response.NearbyOfferResponse
import preq.web.dto.response.NearbyOffersResponse
import preq.web.dto.response.ProductDetectionResponse
import preq.web.dto.response.ProductResponse
import preq.web.dto.response.ProductSearchWithPriceResponse
import java.math.BigDecimal

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val locationProductPriceRepository: LocationProductPriceRepository,
    private val productImageRepository: ProductImageRepository,
    private val imageEmbeddingService: ImageEmbeddingService,
    private val cloudinaryService: CloudinaryService,
    private val openFoodFactsService: OpenFoodFactsService,
    private val userService: UserService,
    private val contestRepository: ProductFieldContestRepository,
    private val confidenceThreshold: Double = 0.78,
    @Value("\${preq.trust.minimum-score}") private val minimumTrustScore: Double,
    @Value("3") private val minimumFieldContestVotesRequired: Double,
    @Value("\${offers.nearby.radius-meters:5000.0}") private val radiusMeters: Double = 5000.0,
    @Value("\${offers.nearby.discount-threshold:0.05}") private val discountThreshold: BigDecimal = BigDecimal("0.05"),
) {
    val productMapper = ProductMapper

    fun getOrCreateByBarcode(
        barcode: String,
        user: User,
    ): BarcodeDetectionResponse {
        // Already in DB
        val existing = productRepository.findByBarcode(barcode)
        if (existing != null) {
            return BarcodeDetectionResponse(
                status = BarcodeDetectionStatus.FOUND,
                product = ProductResponse.from(existing),
            )
        }

        // Query external API
        val response =
            openFoodFactsService.getProduct(barcode)
                ?: return BarcodeDetectionResponse(status = BarcodeDetectionStatus.NOT_FOUND)
        if (response.status != 1 || response.product == null) {
            return BarcodeDetectionResponse(status = BarcodeDetectionStatus.NOT_FOUND)
        }

        val apiResponse = response.product
        val name =
            apiResponse.product_name?.takeIf { it.isNotBlank() }
                ?: return BarcodeDetectionResponse(status = BarcodeDetectionStatus.INCOMPLETE_DATA)
        val brand =
            apiResponse.brands?.takeIf { it.isNotBlank() }
                ?: return BarcodeDetectionResponse(status = BarcodeDetectionStatus.INCOMPLETE_DATA)
        val quantity =
            apiResponse.product_quantity
                ?: return BarcodeDetectionResponse(status = BarcodeDetectionStatus.INCOMPLETE_DATA)
        val quantityType =
            apiResponse.product_quantity_unit?.takeIf { it.isNotBlank() }
                ?: return BarcodeDetectionResponse(status = BarcodeDetectionStatus.INCOMPLETE_DATA)

        // Check for manual product collision
        val collisions =
            productRepository.findPotentialCollisions(
                name,
                brand,
                quantity,
                quantityType,
            )
        val collision = collisions.firstOrNull()

        if (collision != null) {
            val apiProductMapped = productMapper.fromOpenFoodFactsApiResponse(barcode, apiResponse, user)
            return BarcodeDetectionResponse(
                status = BarcodeDetectionStatus.COLLISION,
                apiProduct =
                    ProductResponse(
                        id = collision.id,
                        name = apiProductMapped.name,
                        brand = apiProductMapped.brand,
                        quantity = apiProductMapped.quantity,
                        quantityType = apiProductMapped.quantityType,
                        barcode = barcode,
                        images = apiProductMapped.images.map { it.imageUrl },
                    ),
                existingProduct = ProductResponse.from(collision),
            )
        }

        // No collision, create new
        val product = productRepository.save(productMapper.fromOpenFoodFactsApiResponse(barcode, response.product, user))
        return BarcodeDetectionResponse(
            status = BarcodeDetectionStatus.CREATED,
            product = ProductResponse.from(product),
        )
    }

    fun resolveBarcodeCollision(
        existingId: Long,
        barcode: String,
        confirm: Boolean,
        user: User,
    ): Product {
        if (confirm) {
            // User confirmed — assign barcode to existing product
            val product = productRepository.findById(existingId).orElseThrow()
            product.barcode = barcode
            return productRepository.save(product)
        } else {
            // User denied — create new product from API
            val response =
                openFoodFactsService.getProduct(barcode)
                    ?: throw ExternalApiException("OpenFoodFacts API error")
            if (response.status != 1 || response.product == null) throw NoSuchElementException()
            return productRepository.save(productMapper.fromOpenFoodFactsApiResponse(barcode, response.product, user))
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

    fun getById(id: Long): Product = productRepository.findById(id).orElseThrow { NoSuchElementException("Product not found") }

    fun addImage(
        productId: Long,
        file: MultipartFile,
        user: User,
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
                this.user = user
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
        user: User,
    ): Product {
        val product = productRepository.findById(productId).orElseThrow()
        val embedding = imageEmbeddingService.generateEmbedding(file)
        val imageUrl = cloudinaryService.upload(file)

        val status =
            if (similarity >= confidenceThreshold) {
                ProductImageStatus.APPROVED
            } else {
                resolveConsensus(product, embedding, user)
            }

        product.images.add(
            ProductImage().apply {
                this.product = product
                this.embedding = embedding
                this.imageUrl = imageUrl
                this.confidenceScore = similarity
                this.status = status
                this.user = user
            },
        )

        return productRepository.save(product)
    }

    private fun resolveConsensus(
        product: Product,
        embedding: FloatArray,
        user: User,
    ): ProductImageStatus {
        val eligibleForConsensus = user.trustScore < minimumTrustScore

        if (eligibleForConsensus && product.compareInConsensus(embedding)) {
            return ProductImageStatus.APPROVED
        } else if (!eligibleForConsensus) {
            return ProductImageStatus.REJECTED
        }

        val consensus =
            productImageRepository.findToproductConsensusForEmbedding(
                product.id,
                embedding.joinToString(",", "[", "]"),
                user.id,
            )

        if (consensus.size == 3) {
            userService.addScore(user, 0.01)
            consensus.forEach { pi ->
                userService.addScore(pi.user!!, 0.01)
                product.consensusImages.add(pi)
            }
            return ProductImageStatus.APPROVED
        } else {
            return ProductImageStatus.PENDING_REVIEW
        }
    }

    fun contestField(
        productId: Long,
        field: ContestProductFieldRequest,
        user: User,
    ): FieldContestStatus {
        val contestAlreadyExists =
            contestRepository.existsRecentByProductIdAndUserIdAndFieldType(
                productId,
                user.id,
                field.fieldType,
            )
        if (contestAlreadyExists) {
            return FieldContestStatus.ALREADY_SUBMITTED
        }

        val product =
            productRepository
                .findById(productId)
                .orElseThrow { EntityNotFoundException("Product $productId not found") }

        val field =
            contestRepository.save(
                ProductFieldContest().apply
                    {
                        this.product = product
                        this.user = user
                        this.fieldType = field.fieldType
                        this.fieldValue = field.fieldValue
                    },
            )

        contestCurrentField(product, field)

        return FieldContestStatus.FIRST_SUBMIT
    }

    private fun contestCurrentField(
        product: Product,
        field: ProductFieldContest,
    ) {
        val voteCount =
            contestRepository.countByProductIdAndFieldTypeAndFieldValue(
                product.id,
                field.fieldType,
                field.fieldValue,
            )
        if (voteCount < minimumFieldContestVotesRequired) return

        val normalizedValue = normalizeValue(field.fieldType, field.fieldValue)
        val bestContest = contestRepository.getMostVotedValue(product.id, field.fieldType)

        if (normalizedValue != bestContest) return

        when (field.fieldType) {
            FieldType.BRAND -> product.brand = field.fieldValue
            FieldType.NAME -> product.name = field.fieldValue
            FieldType.QUANTITY -> product.quantity = field.fieldValue.toBigDecimal()
            FieldType.QUANTITY_TYPE -> product.quantityType = field.fieldValue
            FieldType.BARCODE -> product.barcode = field.fieldValue
        }

        productRepository.save(product)
    }

    private fun normalizeValue(
        field: FieldType,
        value: String,
    ): String =
        when (field) {
            FieldType.BRAND -> value.trim()
            FieldType.NAME -> value.trim()
            FieldType.QUANTITY -> value.toDouble().toString()
            FieldType.QUANTITY_TYPE -> value.trim()
            FieldType.BARCODE -> value.toInt().toString()
        }

    fun searchByName(name: String): List<ProductSearchWithPriceResponse> {
        val results =
            productRepository
                .searchByNameWithPrice(name)
                .map { row ->
                    ProductSearchWithPriceResponse(
                        product = ProductResponse.from(row[0] as Product),
                        maxPrice = row[1].toString().toDouble(),
                        minPrice = row[2].toString().toDouble(),
                    )
                }
        return results
    }

    fun getNearbyOffers(user: User): NearbyOffersResponse {
        requireNotNull(user.addressLocation) { "User has no location set" }

        val offers =
            locationProductPriceRepository
                .findNearbyOffers(
                    userLat = user.addressLocation!!.y,
                    userLng = user.addressLocation!!.x,
                    radiusMeters = radiusMeters,
                    thresholdFraction = discountThreshold,
                ).map { NearbyOfferResponse.from(it) }

        return NearbyOffersResponse(offers)
    }
}
