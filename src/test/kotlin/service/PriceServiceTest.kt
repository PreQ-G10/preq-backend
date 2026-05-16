package service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import preq.model.Location
import preq.model.LocationProductPrice
import preq.model.Product
import preq.repository.LocationProductPriceRepository
import preq.repository.LocationRepository
import preq.repository.ProductRepository
import preq.service.PriceService
import preq.web.dto.projection.PriceStats
import preq.web.dto.projection.TopLocationResult
import preq.web.dto.request.ReportProductPriceRequest
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.Test


class PriceServiceTest {

    private val priceRepo: LocationProductPriceRepository = mock()
    private val productRepo: ProductRepository = mock()
    private val locationRepo: LocationRepository = mock()
    private val service = PriceService(priceRepo, productRepo, locationRepo)

    private fun mockProduct(id: Long) = Product().apply { this.id = id }
    private fun mockLocation(id: Long) = Location().apply { this.id = id }

    private fun mockStats(avg: Double? = 10.0, max: Double? = 15.0, min: Double? = 5.0): PriceStats {
        val m = mock<PriceStats>()
        whenever(m.getAvgPrice()).thenReturn(avg)
        whenever(m.getMaxPrice()).thenReturn(max)
        whenever(m.getMinPrice()).thenReturn(min)
        return m
    }

    private fun mockTopLocation(name: String = "Supermercado Norte", address: String = "Av. Corrientes 1234", avg: Double = 9.5, count: Int = 3): TopLocationResult {
        val m = mock<TopLocationResult>()
        whenever(m.getName()).thenReturn(name)
        whenever(m.getAddress()).thenReturn(address)
        whenever(m.getAvgPrice()).thenReturn(avg)
        whenever(m.getReportCount()).thenReturn(count)
        return m
    }

    private fun priceEntry(price: BigDecimal, daysAgo: Long = 0) = LocationProductPrice().apply {
        this.price = price
        this.reportedAt = LocalDateTime.now().minusDays(daysAgo)
    }

    private fun stubSummaryDeps(productId: Long, stats: PriceStats, prices: List<LocationProductPrice>, topLocations: List<TopLocationResult> = emptyList()) {
        whenever(productRepo.findById(productId)).thenReturn(Optional.of(mockProduct(productId)))
        whenever(priceRepo.getPriceStats(productId)).thenReturn(stats)
        whenever(priceRepo.getTopLocations(productId)).thenReturn(topLocations)
        whenever(priceRepo.findByProductIdOrderByReportedAtDesc(productId)).thenReturn(prices)
    }

    // ─── reportPrice ─────────────────────────────────────────────────────────────

    @Test
    fun `reportPrice saves and returns entry with correct fields`() {
        val product = mockProduct(1L)
        val location = mockLocation(2L)
        whenever(productRepo.findById(1L)).thenReturn(Optional.of(product))
        whenever(locationRepo.findById(2L)).thenReturn(Optional.of(location))
        whenever(priceRepo.save(any())).thenAnswer { it.arguments[0] as LocationProductPrice }

        val result = service.reportPrice(ReportProductPriceRequest(1L, 2L, BigDecimal("9.99")))

        assertEquals(product, result.product)
        assertEquals(location, result.location)
        assertEquals(BigDecimal("9.99"), result.price)
        verify(priceRepo).save(argThat { price == BigDecimal("9.99") && this.product == product && this.location == location })
    }

    @Test
    fun `reportPrice sets reportedAt to now`() {
        whenever(productRepo.findById(1L)).thenReturn(Optional.of(mockProduct(1L)))
        whenever(locationRepo.findById(2L)).thenReturn(Optional.of(mockLocation(2L)))
        whenever(priceRepo.save(any())).thenAnswer { it.arguments[0] as LocationProductPrice }

        val before = LocalDateTime.now()
        val result = service.reportPrice(ReportProductPriceRequest(1L, 2L, BigDecimal("5.00")))
        val after = LocalDateTime.now()

        assertFalse(result.reportedAt.isBefore(before))
        assertFalse(result.reportedAt.isAfter(after))
    }

    @Test
    fun `reportPrice throws and never saves when product not found`() {
        whenever(productRepo.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> {
            service.reportPrice(ReportProductPriceRequest(99L, 1L, BigDecimal("5.00")))
        }

        verify(priceRepo, never()).save(any())
    }

    @Test
    fun `reportPrice throws and never saves when location not found`() {
        whenever(productRepo.findById(1L)).thenReturn(Optional.of(mockProduct(1L)))
        whenever(locationRepo.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> {
            service.reportPrice(ReportProductPriceRequest(1L, 99L, BigDecimal("5.00")))
        }

        verify(priceRepo, never()).save(any())
    }

    // ─── getPriceSummary ──────────────────────────────────────────────────────────

    @Test
    fun `getPriceSummary throws and never queries prices when product not found`() {
        whenever(productRepo.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.getPriceSummary(99L) }

        verify(priceRepo, never()).getPriceStats(any())
    }

    @Test
    fun `getPriceSummary maps all top location fields correctly`() {
        val stats = mockStats()
        val topLocation = mockTopLocation(name = "Disco", address = "Av. Santa Fe 800", avg = 7.25, count = 5)
        stubSummaryDeps(1L, stats, listOf(priceEntry(BigDecimal("10.00"))), listOf(topLocation))

        val result = service.getPriceSummary(1L)

        val mapped = result.topLocations[0]
        assertEquals("Disco", mapped.name)
        assertEquals("Av. Santa Fe 800", mapped.address)
        assertEquals(7.25, mapped.avgPrice)
        assertEquals(5, mapped.reportCount)
    }

    @Test
    fun `getPriceSummary returns all top locations preserving order`() {
        val stats = mockStats()
        val first = mockTopLocation(name = "Carrefour", address = "Av. Cabildo 200", avg = 6.0, count = 10)
        val second = mockTopLocation(name = "Coto", address = "Av. Rivadavia 500", avg = 8.0, count = 4)
        stubSummaryDeps(1L, stats, listOf(priceEntry(BigDecimal("10.00"))), listOf(first, second))

        val result = service.getPriceSummary(1L)

        assertEquals(2, result.topLocations.size)
        assertEquals("Carrefour", result.topLocations[0].name)
        assertEquals("Coto", result.topLocations[1].name)
    }

    @Test
    fun `getPriceSummary returns correct stats`() {
        val stats = mockStats()
        stubSummaryDeps(1L, stats, listOf(priceEntry(BigDecimal("10.00"))))

        val result = service.getPriceSummary(1L)

        assertEquals(10.0, result.avgPrice)
        assertEquals(15.0, result.maxPrice)
        assertEquals(5.0, result.minPrice)
        assertNotNull(result.weightedPrice)
    }

    @Test
    fun `getPriceSummary returns null weightedPrice and empty topLocations when no prices`() {
        val stats = mockStats(null, null, null)
        stubSummaryDeps(1L, stats, emptyList())

        val result = service.getPriceSummary(1L)

        assertNull(result.weightedPrice)
        assertTrue(result.topLocations.isEmpty())
    }

    // ─── computeWeightedPrice ─────────────────────────────────────────────────────

    @Test
    fun `weightedPrice favors recent reports over old ones`() {
        val stats = mockStats()
        stubSummaryDeps(1L, stats, listOf(
            priceEntry(BigDecimal("100.00"), daysAgo = 0),
            priceEntry(BigDecimal("10.00"), daysAgo = 300),
        ))

        val result = service.getPriceSummary(1L)

        // Simple avg = 55.0, weighted should be significantly higher due to recency
        assertTrue(result.weightedPrice!! > 55.0)
    }

    @Test
    fun `weightedPrice with single entry equals that price`() {
        val stats = mockStats()
        stubSummaryDeps(1L, stats, listOf(priceEntry(BigDecimal("42.00"))))

        val result = service.getPriceSummary(1L)

        assertEquals(42.0, result.weightedPrice!!, 0.001)
    }

    @Test
    fun `weightedPrice with same-age entries equals arithmetic mean`() {
        val stats = mockStats()
        stubSummaryDeps(1L, stats, listOf(
            priceEntry(BigDecimal("20.00"), daysAgo = 5),
            priceEntry(BigDecimal("40.00"), daysAgo = 5),
        ))

        val result = service.getPriceSummary(1L)

        assertEquals(30.0, result.weightedPrice!!, 0.001)
    }
}