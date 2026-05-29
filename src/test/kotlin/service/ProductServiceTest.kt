package service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import preq.enum.BarcodeDetectionStatus
import preq.enum.ProductImageStatus
import preq.exceptions.ExternalApiException
import preq.model.Product
import preq.model.ProductImage
import preq.model.User
import preq.repository.ProductFieldContestRepository
import preq.repository.ProductImageRepository
import preq.repository.ProductRepository
import preq.service.CloudinaryService
import preq.service.ImageEmbeddingService
import preq.service.OpenFoodFactsService
import preq.service.ProductService
import preq.service.UserService
import preq.web.dto.projection.SimilarProductResult
import preq.web.dto.request.CreateProductRequest
import preq.web.dto.response.OpenFoodFactsProductResponse
import preq.web.dto.response.OpenFoodFactsResponse
import java.math.BigDecimal
import java.util.Optional
import kotlin.math.roundToInt

class ProductServiceTest {
    private val productRepository: ProductRepository = mock()
    private val productImageRepository: ProductImageRepository = mock()
    private val imageEmbeddingService: ImageEmbeddingService = mock()
    private val cloudinaryService: CloudinaryService = mock()
    private val openFoodFactsService: OpenFoodFactsService = mock()
    private val userService: UserService = mock()
    private val contestRepository: ProductFieldContestRepository = mock()
    private val service =
        ProductService(
            productRepository,
            productImageRepository,
            imageEmbeddingService,
            cloudinaryService,
            openFoodFactsService,
            userService,
            contestRepository,
            minimumTrustScore = 0.8,
        )

    private val mockFile = MockMultipartFile("file", "img.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

    private val mockUser = mock<User>()

    private fun mockProduct(id: Long = 1L) =
        Product().apply {
            this.id = id
            name = "Leche"
            brand = "La Serenísima"
            barcode = "123"
        }

    private fun mockApiProduct(
        name: String = "Leche",
        brand: String = "La Serenísima",
        quantity: BigDecimal = BigDecimal.valueOf(1L),
        unit: String = "L",
    ) = OpenFoodFactsProductResponse(
        product_name = name,
        brands = brand,
        product_quantity = quantity,
        product_quantity_unit = unit,
        image_front_url = null,
    )

    private fun mockApiResponse(
        status: Int = 1,
        product: OpenFoodFactsProductResponse? = mockApiProduct(),
    ) = OpenFoodFactsResponse(status = status, product = product)

    // ─── getOrCreateByBarcode ─────────────────────────────────────────────────────

    @Test
    fun `getOrCreateByBarcode returns FOUND when product already in DB`() {
        whenever(productRepository.findByBarcode("123")).thenReturn(mockProduct())

        val result = service.getOrCreateByBarcode("123", mockUser)

        assertEquals(BarcodeDetectionStatus.FOUND, result.status)
        assertNotNull(result.product)
        verify(openFoodFactsService, never()).getProduct(any())
    }

    @Test
    fun `getOrCreateByBarcode returns NOT_FOUND when external API returns null`() {
        whenever(productRepository.findByBarcode("123")).thenReturn(null)
        whenever(openFoodFactsService.getProduct("123")).thenReturn(null)

        val result = service.getOrCreateByBarcode("123", mockUser)

        assertEquals(BarcodeDetectionStatus.NOT_FOUND, result.status)
    }

    @Test
    fun `getOrCreateByBarcode returns NOT_FOUND when API status is not 1`() {
        whenever(productRepository.findByBarcode("123")).thenReturn(null)
        whenever(openFoodFactsService.getProduct("123")).thenReturn(mockApiResponse(status = 0))

        val result = service.getOrCreateByBarcode("123", mockUser)

        assertEquals(BarcodeDetectionStatus.NOT_FOUND, result.status)
    }

    @Test
    fun `getOrCreateByBarcode returns INCOMPLETE_DATA when product name is blank`() {
        whenever(productRepository.findByBarcode("123")).thenReturn(null)
        whenever(openFoodFactsService.getProduct("123")).thenReturn(mockApiResponse(product = mockApiProduct(name = "")))

        val result = service.getOrCreateByBarcode("123", mockUser)

        assertEquals(BarcodeDetectionStatus.INCOMPLETE_DATA, result.status)
    }

    @Test
    fun `getOrCreateByBarcode returns INCOMPLETE_DATA when brand is blank`() {
        whenever(productRepository.findByBarcode("123")).thenReturn(null)
        whenever(openFoodFactsService.getProduct("123")).thenReturn(mockApiResponse(product = mockApiProduct(brand = "")))

        val result = service.getOrCreateByBarcode("123", mockUser)

        assertEquals(BarcodeDetectionStatus.INCOMPLETE_DATA, result.status)
    }

    @Test
    fun `getOrCreateByBarcode returns COLLISION when matching product exists`() {
        val existing = mockProduct(id = 5L)
        whenever(productRepository.findByBarcode("123")).thenReturn(null)
        whenever(openFoodFactsService.getProduct("123")).thenReturn(mockApiResponse())
        whenever(productRepository.findPotentialCollisions(any(), any(), any(), any())).thenReturn(listOf(existing))

        val result = service.getOrCreateByBarcode("123", mockUser)

        assertEquals(BarcodeDetectionStatus.COLLISION, result.status)
        assertEquals(5L, result.apiProduct?.id)
        assertNotNull(result.existingProduct)
        verify(productRepository, never()).save(any())
    }

    @Test
    fun `getOrCreateByBarcode returns CREATED and saves product when no collision`() {
        val saved = mockProduct()
        whenever(productRepository.findByBarcode("123")).thenReturn(null)
        whenever(openFoodFactsService.getProduct("123")).thenReturn(mockApiResponse())
        whenever(productRepository.findPotentialCollisions(any(), any(), any(), any())).thenReturn(emptyList())
        whenever(productRepository.save(any())).thenReturn(saved)

        val result = service.getOrCreateByBarcode("123", mockUser)

        assertEquals(BarcodeDetectionStatus.CREATED, result.status)
        assertNotNull(result.product)
        verify(productRepository).save(any())
    }

    // ─── resolveBarcodeCollision ──────────────────────────────────────────────────

    @Test
    fun `resolveBarcodeCollision confirm=true assigns barcode to existing product`() {
        val product = mockProduct()
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(productRepository.save(any())).thenAnswer { it.arguments[0] as Product }

        val result = service.resolveBarcodeCollision(1L, "999", confirm = true, mockUser)

        assertEquals("999", result.barcode)
        verify(productRepository).save(product)
        verify(openFoodFactsService, never()).getProduct(any())
    }

    @Test
    fun `resolveBarcodeCollision confirm=true throws when product not found`() {
        whenever(productRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> {
            service.resolveBarcodeCollision(99L, "999", confirm = true, mockUser)
        }
        verify(productRepository, never()).save(any())
    }

    @Test
    fun `resolveBarcodeCollision confirm=false creates new product from API`() {
        val saved = mockProduct()
        whenever(openFoodFactsService.getProduct("123")).thenReturn(mockApiResponse())
        whenever(productRepository.save(any())).thenReturn(saved)

        val result = service.resolveBarcodeCollision(1L, "123", confirm = false, mockUser)

        assertNotNull(result)
        verify(productRepository).save(any())
        verify(productRepository, never()).findById(any())
    }

    @Test
    fun `resolveBarcodeCollision confirm=false throws when API is unavailable`() {
        whenever(openFoodFactsService.getProduct("123")).thenReturn(null)

        assertThrows<ExternalApiException> {
            service.resolveBarcodeCollision(1L, "123", confirm = false, mockUser)
        }
        verify(productRepository, never()).save(any())
    }

    @Test
    fun `resolveBarcodeCollision confirm=false throws when API response status is not 1`() {
        whenever(openFoodFactsService.getProduct("123")).thenReturn(mockApiResponse(status = 0))

        assertThrows<NoSuchElementException> {
            service.resolveBarcodeCollision(1L, "123", confirm = false, mockUser)
        }
        verify(productRepository, never()).save(any())
    }

    // ─── detect ───────────────────────────────────────────────────────────────────

    @Test
    fun `detect returns empty list when no similar products found`() {
        whenever(imageEmbeddingService.generateEmbedding(mockFile)).thenReturn(floatArrayOf(0.1f, 0.2f))
        whenever(productImageRepository.findSimilarProducts(any(), any())).thenReturn(emptyList())
        whenever(productRepository.findAllById(any())).thenReturn(emptyList())

        val result = service.detect(mockFile)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `detect returns mapped results above threshold`() {
        val product = mockProduct()
        val similarResult =
            mock<SimilarProductResult>().also {
                whenever(it.getProductId()).thenReturn(1L)
                whenever(it.getSimilarity()).thenReturn(0.95)
            }
        whenever(imageEmbeddingService.generateEmbedding(mockFile)).thenReturn(floatArrayOf(0.1f, 0.2f))
        whenever(productImageRepository.findSimilarProducts(any(), any())).thenReturn(listOf(similarResult))
        whenever(productRepository.findAllById(listOf(1L))).thenReturn(listOf(product))

        val result = service.detect(mockFile)

        assertEquals(1, result.size)
        assertEquals(1L, result[0].productId)
    }

    // ─── addImage ─────────────────────────────────────────────────────────────────

    @Test
    fun `addImage throws when product not found`() {
        whenever(productRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.addImage(99L, mockFile, mockUser) }

        verify(cloudinaryService, never()).upload(any())
        verify(productRepository, never()).save(any())
    }

    @Test
    fun `addImage uploads image and saves product with APPROVED status`() {
        val product = mockProduct()
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(cloudinaryService.upload(mockFile)).thenReturn("http://img.url")
        whenever(imageEmbeddingService.generateEmbedding(mockFile)).thenReturn(floatArrayOf(0.1f))
        whenever(productRepository.save(any())).thenAnswer { it.arguments[0] as Product }

        val result = service.addImage(1L, mockFile, mockUser)

        val addedImage = result.images.last()
        assertEquals("http://img.url", addedImage.imageUrl)
        assertEquals(1.0, addedImage.confidenceScore)
        assertEquals(ProductImageStatus.APPROVED, addedImage.status)
    }

    // ─── confirmMatch ─────────────────────────────────────────────────────────────

    @Test
    fun `confirmMatch sets APPROVED when similarity is above threshold`() {
        val product = mockProduct()
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(imageEmbeddingService.generateEmbedding(mockFile)).thenReturn(floatArrayOf(0.1f))
        whenever(cloudinaryService.upload(mockFile)).thenReturn("http://img.url")
        whenever(productRepository.save(any())).thenAnswer { it.arguments[0] as Product }

        val result = service.confirmMatch(1L, mockFile, similarity = 0.95, mockUser)

        assertEquals(ProductImageStatus.APPROVED, result.images.last().status)
    }

    @Test
    fun `confirmMatch sets PENDING_REVIEW when similarity is below threshold`() {
        val product = mockProduct()
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(imageEmbeddingService.generateEmbedding(mockFile)).thenReturn(floatArrayOf(0.1f))
        whenever(cloudinaryService.upload(mockFile)).thenReturn("http://img.url")
        whenever(productRepository.save(any())).thenAnswer { it.arguments[0] as Product }

        val result = service.confirmMatch(1L, mockFile, similarity = 0.50, mockUser)

        assertEquals(ProductImageStatus.PENDING_REVIEW, result.images.last().status)
    }

    @Test
    fun `confirmMatch stores correct similarity score on image`() {
        val product = mockProduct()
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(imageEmbeddingService.generateEmbedding(mockFile)).thenReturn(floatArrayOf(0.1f))
        whenever(cloudinaryService.upload(mockFile)).thenReturn("http://img.url")
        whenever(productRepository.save(any())).thenAnswer { it.arguments[0] as Product }

        val result = service.confirmMatch(1L, mockFile, similarity = 0.82, mockUser)

        assertEquals(0.82, result.images.last().confidenceScore)
    }

    // ─── create ───────────────────────────────────────────────────────────────────

    @Test
    fun `create saves product with all fields from request`() {
        val request =
            CreateProductRequest(name = "Yogur", brand = "Danone", quantity = BigDecimal.valueOf(200L), quantityType = "g", barcode = "456")
        whenever(productRepository.save(any())).thenAnswer { it.arguments[0] as Product }

        val result = service.create(request)

        assertEquals("Yogur", result.name)
        assertEquals("Danone", result.brand)
        assertEquals(BigDecimal.valueOf(200L), result.quantity)
        assertEquals("g", result.quantityType)
        assertEquals("456", result.barcode)
    }

    // ─── getById ─────────────────────────────────────────────────────────────────

    @Test
    fun `getById returns product when found`() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct()))

        val result = service.getById(1L)

        assertEquals(1L, result.id)
    }

    @Test
    fun `getById throws when product not found`() {
        whenever(productRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.getById(99L) }
    }

    // ─── searchByName ─────────────────────────────────────────────────────────────

    @Test
    fun `searchByName returns matching products`() {
        whenever(productRepository.searchByName("Leche")).thenReturn(listOf(mockProduct()))

        val result = service.searchByName("Leche")

        assertEquals(1, result.size)
        assertEquals("Leche", result[0].name)
    }

    @Test
    fun `searchByName returns empty list when no matches`() {
        whenever(productRepository.searchByName("xyz")).thenReturn(emptyList())

        val result = service.searchByName("xyz")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `confirmMatch should increase user scores, add images to consensus, and approve when consensus is reached via resolveConsensus`() {
        val productId = 1L
        val product = Product().apply { id = productId }
        val file: MultipartFile = mock()
        val similarity = 0.5
        val currentUser =
            User().apply {
                id = 10L
                trustScore = 0.1
            }

        var embedding = floatArrayOf(0.1f, 0.2f, 0.3f)
        val imageUrl = "http://cloudinary.com/image.jpg"

        val user1 =
            User().apply {
                id = 20L
                trustScore = 0.2
            }
        val user2 =
            User().apply {
                id = 30L
                trustScore = 0.3
            }
        val user3 =
            User().apply {
                id = 40L
                trustScore = 0.4
            }

        val image1 =
            ProductImage().apply {
                id = 100L
                user = user1
                embedding = floatArrayOf(0.11f, 0.21f, 0.31f)
            }
        val image2 =
            ProductImage().apply {
                id = 101L
                user = user2
                embedding = floatArrayOf(0.12f, 0.22f, 0.32f)
            }
        val image3 =
            ProductImage().apply {
                id = 102L
                user = user3
                embedding = floatArrayOf(0.13f, 0.23f, 0.33f)
            }

        val consensusImages = listOf(image1, image2, image3)

        // 2. Mock Dependencies
        `when`(productRepository.findById(productId)).thenReturn(Optional.of(product))
        `when`(imageEmbeddingService.generateEmbedding(file)).thenReturn(embedding)
        `when`(cloudinaryService.upload(file)).thenReturn(imageUrl)

        `when`(
            productImageRepository.findToproductConsensusForEmbedding(
                eq(product.id),
                anyString(),
                eq(currentUser.id),
            ),
        ).thenReturn(consensusImages)

        // Mock userService.addScore to simulate score updates
        `when`(userService.addScore(any(), anyDouble())).thenAnswer { invocation ->
            val userArg = invocation.getArgument<User>(0)
            val scoreArg = invocation.getArgument<Double>(1)
            userArg.trustScore =
                ((userArg.trustScore + scoreArg) * 100)
                    .roundToInt() / 100.0
            null
        }

        `when`(productRepository.save(any())).thenAnswer { invocation ->
            invocation.getArgument<Product>(0)
        }

        val updatedProduct = service.confirmMatch(productId, file, similarity, currentUser)

        verify(productRepository).findById(productId)
        verify(imageEmbeddingService).generateEmbedding(file)
        verify(cloudinaryService).upload(file)

        verify(userService).addScore(currentUser, 0.01)
        assertEquals(0.11, currentUser.trustScore, "Current user's score should be increased")

        verify(userService).addScore(user1, 0.01)
        assertEquals(0.21, user1.trustScore, "Consensus user 1's score should be increased")
        verify(userService).addScore(user2, 0.01)
        assertEquals(0.31, user2.trustScore, "Consensus user 2's score should be increased")
        verify(userService).addScore(user3, 0.01)
        assertEquals(0.41, user3.trustScore, "Consensus user 3's score should be increased")

        assertEquals(3, updatedProduct.consensusImages.size, "Product should have 3 consensus images")
        assertTrue(updatedProduct.consensusImages.containsAll(consensusImages), "Consensus images should be added to product")

        assertEquals(1, updatedProduct.images.size, "One new image should be added to product images")
        val newProductImage = updatedProduct.images.first()
        assertEquals(ProductImageStatus.APPROVED, newProductImage.status, "New image status should be APPROVED")
        assertEquals(imageUrl, newProductImage.imageUrl, "New image URL should match")
        assertEquals(embedding, newProductImage.embedding, "New image embedding should match")
        assertEquals(similarity, newProductImage.confidenceScore, "New image confidence score should match")
        assertEquals(currentUser, newProductImage.user, "New image user should match")
        assertTrue(newProductImage.validForConsensus, "New image validForConsensus should be false as consensus was reached")

        verify(productRepository).save(updatedProduct)
    }
}
