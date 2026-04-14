package service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import preq.model.Location
import preq.model.LocationProductPrice
import preq.model.Product
import preq.repository.LocationProductPriceRepository
import preq.repository.LocationRepository
import preq.repository.ProductRepository
import preq.service.PriceService
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PriceServiceTest {
    @Mock
    lateinit var locationProductPriceRepository: LocationProductPriceRepository

    @Mock lateinit var productRepository: ProductRepository

    @Mock lateinit var locationRepository: LocationRepository

    @InjectMocks
    lateinit var priceService: PriceService

    @Test
    fun `reportPrice saves with correct product and location`() {
        val product = Product().apply { name = "Hileret" }
        val location = Location().apply { name = "Carrefour" }

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(locationRepository.findById(1L)).thenReturn(Optional.of(location))
        whenever(locationProductPriceRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = priceService.reportPrice(1L, 1L, BigDecimal("500.00"))

        assertEquals(product, result.product)
        assertEquals(location, result.location)
        assertEquals(BigDecimal("500.00"), result.price)
    }

    @Test
    fun `weighted price gives more weight to recent reports`() {
        val recentPrice =
            LocationProductPrice().apply {
                price = BigDecimal("1000.00")
                reportedAt = LocalDateTime.now()
            }
        val oldPrice =
            LocationProductPrice().apply {
                price = BigDecimal("100.00")
                reportedAt = LocalDateTime.now().minusDays(365)
            }

        whenever(locationProductPriceRepository.getPriceStats(1L))
            .thenReturn(arrayOf(550.0, 1000.0, 100.0))
        whenever(locationProductPriceRepository.getTopLocations(1L)).thenReturn(emptyList())
        whenever(locationProductPriceRepository.findByProductIdOrderByReportedAtDesc(1L))
            .thenReturn(listOf(recentPrice, oldPrice))

        val summary = priceService.getPriceSummary(1L)

        // weighted price should be closer to 1000 than to 100
        assertTrue(summary.weightedPrice > 500.0)
    }
}
