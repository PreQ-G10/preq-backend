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
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import preq.enum.PriceValidity
import preq.model.Location
import preq.model.LocationProductPrice
import preq.model.Product
import preq.model.User
import preq.repository.LocationProductPriceRepository
import preq.repository.LocationRepository
import preq.repository.ProductRepository
import preq.repository.UserRepository
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
    private val userRepo: UserRepository = mock()
    private val service =
        PriceService(
            priceRepo,
            productRepo,
            locationRepo,
            userRepo,
            thresholdPercentage = 0.40,
            proximityMeters = 200.0,
            coldStartMinimumReports = 3,
            minimumTrustScore = 0.25,
        )

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private fun mockProduct(id: Long) = Product().apply { this.id = id }

    private fun mockLocation(
        id: Long,
        latitude: Double? = -34.7144208,
        longitude: Double? = -58.2979084,
    ) = Location().apply {
        this.id = id
        this.latitude = latitude
        this.longitude = longitude
    }

    private fun mockUser(
        trustScore: Double = 0.5,
        recoveryMultiplier: Double = 1.0,
    ) = User().apply {
        this.trustScore = trustScore
        this.recoveryMultiplier = recoveryMultiplier
    }

    private fun mockStats(
        avg: Double? = 10.0,
        max: Double? = 15.0,
        min: Double? = 5.0,
    ): PriceStats {
        val m = mock<PriceStats>()
        whenever(m.getAvgPrice()).thenReturn(avg)
        whenever(m.getMaxPrice()).thenReturn(max)
        whenever(m.getMinPrice()).thenReturn(min)
        return m
    }

    private fun mockTopLocation(
        name: String = "Supermercado Norte",
        address: String = "Av. Corrientes 1234",
        avg: Double = 9.5,
        count: Int = 3,
    ): TopLocationResult {
        val m = mock<TopLocationResult>()
        whenever(m.getName()).thenReturn(name)
        whenever(m.getAddress()).thenReturn(address)
        whenever(m.getAvgPrice()).thenReturn(avg)
        whenever(m.getReportCount()).thenReturn(count)
        return m
    }

    private fun priceEntry(
        price: BigDecimal,
        daysAgo: Long = 0,
    ) = LocationProductPrice().apply {
        this.price = price
        this.reportedAt = LocalDateTime.now().minusDays(daysAgo)
    }

    private fun stubReportDeps(
        productId: Long = 1L,
        locationId: Long = 2L,
        user: User = mockUser(),
        location: Location = mockLocation(locationId),
        existingPrices: List<LocationProductPrice> = emptyList(),
        totalReports: Long = 0L,
    ) {
        whenever(productRepo.findById(productId)).thenReturn(Optional.of(mockProduct(productId)))
        whenever(locationRepo.findById(locationId)).thenReturn(Optional.of(location))
        whenever(userRepo.save(any())).thenAnswer { it.arguments[0] as User }
        whenever(priceRepo.findValidByProductIdOrderByReportedAtDesc(eq(productId), any())).thenReturn(existingPrices)
        whenever(priceRepo.countValidByProductId(eq(productId), any())).thenReturn(totalReports)
        whenever(priceRepo.save(any())).thenAnswer { it.arguments[0] as LocationProductPrice }
    }

    private fun stubSummaryDeps(
        productId: Long,
        stats: PriceStats,
        prices: List<LocationProductPrice>,
        topLocations: List<TopLocationResult> = emptyList(),
    ) {
        whenever(productRepo.findById(productId)).thenReturn(Optional.of(mockProduct(productId)))
        whenever(priceRepo.getPriceStats(productId, 0.25)).thenReturn(stats)
        whenever(priceRepo.getTopLocations(productId, 0.25)).thenReturn(topLocations)
        whenever(priceRepo.findValidByProductIdOrderByReportedAtDesc(eq(productId), any())).thenReturn(prices)
    }

    private fun makeRequest(
        productId: Long = 1L,
        locationId: Long = 2L,
        price: BigDecimal = BigDecimal("10.00"),
        userLatitude: Double? = null,
        userLongitude: Double? = null,
    ) = ReportProductPriceRequest(productId, locationId, price, userLatitude, userLongitude)

    private fun existingPricesNear(
        basePrice: Double,
        count: Int = 5,
    ): List<LocationProductPrice> =
        (1..count).map {
            priceEntry(BigDecimal(basePrice + it * 0.01))
        }

    // ─────────────────────────────────────────────────────────
    // reportPrice — basic
    // ─────────────────────────────────────────────────────────

    @Test
    fun `reportPrice saves and returns entry with correct fields`() {
        val user = mockUser()
        stubReportDeps(user = user)

        val result = service.reportPrice(makeRequest(price = BigDecimal("9.99")), user)

        assertEquals(BigDecimal("9.99"), result.price)
        verify(priceRepo).save(argThat { price == BigDecimal("9.99") })
    }

    @Test
    fun `reportPrice sets reportedAt to now`() {
        val user = mockUser()
        stubReportDeps(user = user)

        val before = LocalDateTime.now()
        val result = service.reportPrice(makeRequest(), user)
        val after = LocalDateTime.now()

        assertFalse(result.reportedAt.isBefore(before))
        assertFalse(result.reportedAt.isAfter(after))
    }

    @Test
    fun `reportPrice throws and never saves when product not found`() {
        whenever(productRepo.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> {
            service.reportPrice(makeRequest(productId = 99L), mockUser())
        }

        verify(priceRepo, never()).save(any())
    }

    @Test
    fun `reportPrice throws and never saves when location not found`() {
        whenever(productRepo.findById(1L)).thenReturn(Optional.of(mockProduct(1L)))
        whenever(locationRepo.findById(99L)).thenReturn(Optional.empty())
        whenever(priceRepo.findValidByProductIdOrderByReportedAtDesc(eq(1L), any())).thenReturn(emptyList())
        whenever(priceRepo.countValidByProductId(eq(1L), any())).thenReturn(0L)

        assertThrows<NoSuchElementException> {
            service.reportPrice(makeRequest(locationId = 99L), mockUser())
        }

        verify(priceRepo, never()).save(any())
    }

    // ─────────────────────────────────────────────────────────
    // R4 — cold start
    // ─────────────────────────────────────────────────────────

    @Test
    fun `R4 - accepts any price when total reports below cold start threshold`() {
        val user = mockUser()
        // price deviates 90% — would normally be rejected by R1
        val existingPrices = existingPricesNear(10.0, count = 2)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 2L)

        val result = service.reportPrice(makeRequest(price = BigDecimal("19.00")), user)

        assertEquals(PriceValidity.VALID, result.priceValidity)
    }

    @Test
    fun `R4 - applies R1 when total reports meet cold start threshold`() {
        val user = mockUser()
        val existingPrices = existingPricesNear(10.0, count = 5)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)

        val result = service.reportPrice(makeRequest(price = BigDecimal("19.00")), user)

        assertEquals(PriceValidity.INVALID, result.priceValidity)
    }

    // ─────────────────────────────────────────────────────────
    // R1 — threshold check
    // ─────────────────────────────────────────────────────────

    @Test
    fun `R1 - accepts price within threshold`() {
        val user = mockUser()
        val existingPrices = existingPricesNear(10.0)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)

        val result = service.reportPrice(makeRequest(price = BigDecimal("11.00")), user)

        assertEquals(PriceValidity.VALID, result.priceValidity)
    }

    @Test
    fun `R1 - rejects price above threshold`() {
        val user = mockUser()
        val existingPrices = existingPricesNear(10.0)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)

        val result = service.reportPrice(makeRequest(price = BigDecimal("20.00")), user)

        assertEquals(PriceValidity.INVALID, result.priceValidity)
    }

    @Test
    fun `R1 - rejects price below threshold`() {
        val user = mockUser()
        val existingPrices = existingPricesNear(10.0)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)

        val result = service.reportPrice(makeRequest(price = BigDecimal("4.00")), user)

        assertEquals(PriceValidity.INVALID, result.priceValidity)
    }

    @Test
    fun `R1 - accepted report still saves to DB`() {
        val user = mockUser()
        val existingPrices = existingPricesNear(10.0)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)

        service.reportPrice(makeRequest(price = BigDecimal("20.00")), user)

        verify(priceRepo).save(any())
    }

    // ─────────────────────────────────────────────────────────
    // R10 — penalty on rejection
    // ─────────────────────────────────────────────────────────

    @Test
    fun `R10 - trust score decreases on rejected report`() {
        val user = mockUser(trustScore = 0.5)
        val existingPrices = existingPricesNear(10.0)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)

        service.reportPrice(makeRequest(price = BigDecimal("20.00")), user)

        assertTrue(user.trustScore < 0.5)
    }

    @Test
    fun `R10 - larger deviation causes larger penalty`() {
        val userSmall = mockUser(trustScore = 0.5)
        val userLarge = mockUser(trustScore = 0.5)
        val existingPrices = existingPricesNear(10.0)

        stubReportDeps(user = userSmall, existingPrices = existingPrices, totalReports = 5L)
        service.reportPrice(makeRequest(price = BigDecimal("15.00")), userSmall) // 50% deviation

        stubReportDeps(user = userLarge, existingPrices = existingPrices, totalReports = 5L)
        service.reportPrice(makeRequest(price = BigDecimal("30.00")), userLarge) // 200% deviation

        assertTrue(userLarge.trustScore < userSmall.trustScore)
    }

    @Test
    fun `R10 - trust score never goes below 0`() {
        val user = mockUser(trustScore = 0.01)
        val existingPrices = existingPricesNear(10.0)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)

        service.reportPrice(makeRequest(price = BigDecimal("100.00")), user)

        assertTrue(user.trustScore >= 0.0)
    }

    // ─────────────────────────────────────────────────────────
    // R11 — trust recovery
    // ─────────────────────────────────────────────────────────

    @Test
    fun `R11 - trust score increases on valid report`() {
        val user = mockUser(trustScore = 0.5)
        stubReportDeps(user = user, totalReports = 0L)

        service.reportPrice(makeRequest(), user)

        assertTrue(user.trustScore > 0.5)
    }

    @Test
    fun `R11 - trust score increase is scaled by recoveryMultiplier`() {
        val userFull = mockUser(trustScore = 0.5, recoveryMultiplier = 1.0)
        val userHalf = mockUser(trustScore = 0.5, recoveryMultiplier = 0.5)

        stubReportDeps(user = userFull, totalReports = 0L)
        service.reportPrice(makeRequest(), userFull)

        stubReportDeps(user = userHalf, totalReports = 0L)
        service.reportPrice(makeRequest(), userHalf)

        assertTrue(userFull.trustScore > userHalf.trustScore)
    }

    @Test
    fun `R11 - trust score never exceeds 1`() {
        val user = mockUser(trustScore = 0.999)
        stubReportDeps(user = user, totalReports = 0L)

        service.reportPrice(makeRequest(), user)

        assertTrue(user.trustScore <= 1.0)
    }

    @Test
    fun `R11 - trust score does not increase on rejected report`() {
        val user = mockUser(trustScore = 0.5)
        val existingPrices = existingPricesNear(10.0)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)

        service.reportPrice(makeRequest(price = BigDecimal("20.00")), user)

        assertTrue(user.trustScore < 0.5)
    }

    // ─────────────────────────────────────────────────────────
    // R13 — recoveryMultiplier halved on threshold crossing
    // ─────────────────────────────────────────────────────────

    @Test
    fun `R13 - recoveryMultiplier halved when trust score crosses below minimum`() {
        val user = mockUser(trustScore = 0.26, recoveryMultiplier = 1.0)
        val existingPrices = existingPricesNear(10.0)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)

        // Large deviation to push score below 0.25
        service.reportPrice(makeRequest(price = BigDecimal("20.00")), user)

        if (user.trustScore < 0.25) {
            assertEquals(0.5, user.recoveryMultiplier, 0.001)
        }
    }

    @Test
    fun `R13 - recoveryMultiplier not halved when already below minimum`() {
        val user = mockUser(trustScore = 0.10, recoveryMultiplier = 0.5)
        val existingPrices = existingPricesNear(10.0)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)

        service.reportPrice(makeRequest(price = BigDecimal("20.00")), user)

        // Was already below — multiplier should not halve again
        assertEquals(0.5, user.recoveryMultiplier, 0.001)
    }

    // ─────────────────────────────────────────────────────────
    // R3 — location confidence
    // ─────────────────────────────────────────────────────────

    @Test
    fun `R3 - locationConfidence is 1 when user coords are null`() {
        val user = mockUser()
        stubReportDeps(user = user, totalReports = 0L)

        val result = service.reportPrice(makeRequest(userLatitude = null, userLongitude = null), user)

        assertEquals(1.0, result.locationConfidence, 0.001)
    }

    @Test
    fun `R3 - locationConfidence is reduced when user is far from location`() {
        val user = mockUser()
        val location = mockLocation(2L, latitude = -34.7144208, longitude = -58.2979084)
        stubReportDeps(user = user, location = location, totalReports = 0L)
        // Simulate far distance — 5km away
        whenever(priceRepo.getDistanceMeters(any(), any(), any())).thenReturn(5000.0)

        val result =
            service.reportPrice(
                makeRequest(userLatitude = -34.7600, userLongitude = -58.3500),
                user,
            )

        assertTrue(result.locationConfidence < 1.0)
    }

    @Test
    fun `R3 - locationConfidence stays 1 when user is within proximity`() {
        val user = mockUser()
        val location = mockLocation(2L, latitude = -34.7144208, longitude = -58.2979084)
        stubReportDeps(user = user, location = location, totalReports = 0L)
        // Simulate close distance — 50m away
        whenever(priceRepo.getDistanceMeters(any(), any(), any())).thenReturn(50.0)

        val result =
            service.reportPrice(
                makeRequest(userLatitude = -34.7148, userLongitude = -58.2983),
                user,
            )

        assertEquals(1.0, result.locationConfidence, 0.001)
    }

    // ─────────────────────────────────────────────────────────
    // getPriceSummary
    // ─────────────────────────────────────────────────────────

    @Test
    fun `getPriceSummary throws when product not found`() {
        whenever(productRepo.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.getPriceSummary(99L) }

        verify(priceRepo, never()).getPriceStats(any(), any())
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

    // ─────────────────────────────────────────────────────────
    // computeWeightedPrice
    // ─────────────────────────────────────────────────────────

    @Test
    fun `weightedPrice favors recent reports over old ones`() {
        val stats = mockStats()
        stubSummaryDeps(
            1L,
            stats,
            listOf(
                priceEntry(BigDecimal("100.00"), daysAgo = 0),
                priceEntry(BigDecimal("10.00"), daysAgo = 300),
            ),
        )

        val result = service.getPriceSummary(1L)

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
        stubSummaryDeps(
            1L,
            stats,
            listOf(
                priceEntry(BigDecimal("20.00"), daysAgo = 5),
                priceEntry(BigDecimal("40.00"), daysAgo = 5),
            ),
        )

        val result = service.getPriceSummary(1L)

        assertEquals(30.0, result.weightedPrice!!, 0.001)
    }
}
