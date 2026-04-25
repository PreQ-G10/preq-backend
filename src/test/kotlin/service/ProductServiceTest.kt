package service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import preq.enum.ProductImageStatus
import preq.model.Product
import preq.repository.ProductImageRepository
import preq.repository.ProductRepository
import preq.service.BarcodeService
import preq.service.CloudinaryService
import preq.service.ImageEmbeddingService
import preq.service.ProductService
import preq.service.mapper.ProductMapper
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ProductServiceTest {
    @Mock lateinit var productRepository: ProductRepository

    @Mock lateinit var productImageRepository: ProductImageRepository

    @Mock lateinit var imageEmbeddingService: ImageEmbeddingService

    @Mock lateinit var cloudinaryService: CloudinaryService

    @Mock lateinit var barcodeService: BarcodeService

    @Mock lateinit var apiMapper: ProductMapper

    lateinit var productService: ProductService

    @BeforeEach
    fun setup() {
        productService =
            ProductService(
                productRepository,
                productImageRepository,
                imageEmbeddingService,
                cloudinaryService,
                confidenceThreshold = 0.78,
                barcodeService = barcodeService,
                apiMapper = apiMapper,
            )
    }

    @Test
    fun `confirmMatch sets APPROVED status when similarity is above threshold`() {
        val product = Product().apply { name = "Maní King" }
        val file = MockMultipartFile("file", "test.jpg", "image/jpeg", ByteArray(1))

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(imageEmbeddingService.generateEmbedding(file)).thenReturn(FloatArray(512))
        whenever(cloudinaryService.upload(file)).thenReturn("https://cloudinary.com/image.jpg")
        whenever(productRepository.save(any())).thenReturn(product)

        productService.confirmMatch(1L, file, 0.92)

        assertEquals(ProductImageStatus.APPROVED, product.images.first().status)
    }

    @Test
    fun `confirmMatch sets PENDING_REVIEW when similarity is below threshold`() {
        val product = Product().apply { name = "Maní King" }
        val file = MockMultipartFile("file", "test.jpg", "image/jpeg", ByteArray(1))

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(imageEmbeddingService.generateEmbedding(file)).thenReturn(FloatArray(512))
        whenever(cloudinaryService.upload(file)).thenReturn("https://cloudinary.com/image.jpg")
        whenever(productRepository.save(any())).thenReturn(product)

        productService.confirmMatch(1L, file, 0.70)

        assertEquals(ProductImageStatus.PENDING_REVIEW, product.images.first().status)
    }

    @Test
    fun `confirmMatch throws when product not found`() {
        val file = MockMultipartFile("file", "test.jpg", "image/jpeg", ByteArray(1))
        whenever(productRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> {
            productService.confirmMatch(99L, file, 0.90)
        }
    }

    @Test
    fun `detect returns empty list when no similar products found`() {
        val file = MockMultipartFile("file", "test.jpg", "image/jpeg", ByteArray(1))
        whenever(imageEmbeddingService.generateEmbedding(file)).thenReturn(FloatArray(512))
        whenever(productImageRepository.findSimilarProducts(any(), any()))
            .thenReturn(emptyList())

        val result = productService.detect(file)

        assertTrue(result.isEmpty())
    }
}
