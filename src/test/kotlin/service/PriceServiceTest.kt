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
import preq.enum.ReportScore
import preq.model.Location
import preq.model.LocationProductPrice
import preq.model.Product
import preq.model.User
import preq.repository.LocationProductPriceRepository
import preq.repository.LocationRepository
import preq.repository.PriceValidationRepository
import preq.repository.ProductRepository
import preq.repository.UserRepository
import preq.service.PriceService
import preq.web.dto.projection.PriceStats
import preq.web.dto.projection.TopLocationResult
import preq.web.dto.request.DisputePriceRequest
import preq.web.dto.request.ReportProductPriceRequest
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertNotEquals

class PriceServiceTest {
    private val priceRepo: LocationProductPriceRepository = mock()
    private val productRepo: ProductRepository = mock()
    private val locationRepo: LocationRepository = mock()
    private val userRepo: UserRepository = mock()
    private val validationRepo: PriceValidationRepository = mock()
    private val service =
        PriceService(
            priceRepo,
            productRepo,
            locationRepo,
            userRepo,
            validationRepo,
            thresholdPercentage = 0.40,
            proximityMeters = 200.0,
            coldStartMinimumReports = 3,
            minimumTrustScore = 0.25,
            averageMonthlyInflation = 0.03
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
        id: Long = 1L,
        trustScore: Double = 0.5,
        recoveryMultiplier: Double = 1.0,
    ) = User().apply {
        this.id = id
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
        locationConfidence: Double = 1.0,
    ) = LocationProductPrice().apply {
        this.price = price
        this.reportedAt = LocalDateTime.now().minusDays(daysAgo)
        this.locationConfidence = locationConfidence
    }

    private fun mockReport(
        id: Long = 10L,
        price: BigDecimal = BigDecimal("10.00"),
        score: Double = 0.55,
        user: User = mockUser(),
        location: Location = mockLocation(2L),
        product: Product = mockProduct(1L),
    ) = LocationProductPrice().apply {
        this.id = id
        this.price = price
        this.score = score
        this.user = user
        this.location = location
        this.product = product
        this.reportedAt = LocalDateTime.now()
        this.locationConfidence = 1.0
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
        whenever(priceRepo.getPriceStats(eq(productId), any())).thenReturn(stats)
        whenever(priceRepo.getTopLocations(eq(productId), any())).thenReturn(topLocations)
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
    ): List<LocationProductPrice> = (1..count).map { priceEntry(BigDecimal(basePrice + it * 0.01)) }

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
    fun `R4 - cold start accepts any price below threshold count`() {
        val user = mockUser()
        val existingPrices = existingPricesNear(10.0, count = 2)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 2L)

        val result = service.reportPrice(makeRequest(price = BigDecimal("19.00")), user)

        assertTrue(result.reportScore != ReportScore.INVALID)
    }

    @Test
    fun `R4 - applies R1 when total reports meet cold start threshold`() {
        val user = mockUser()
        val existingPrices = existingPricesNear(10.0, count = 5)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)

        val result = service.reportPrice(makeRequest(price = BigDecimal("19.00")), user)

        assertEquals(ReportScore.INVALID, result.reportScore)
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

        assertNotEquals(ReportScore.INVALID, result.reportScore)
    }

    @Test
    fun `R1 - rejects price above threshold`() {
        val user = mockUser()
        val existingPrices = existingPricesNear(10.0)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)

        val result = service.reportPrice(makeRequest(price = BigDecimal("20.00")), user)

        assertEquals(ReportScore.INVALID, result.reportScore)
    }

    @Test
    fun `R1 - rejects price below threshold`() {
        val user = mockUser()
        val existingPrices = existingPricesNear(10.0)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)

        val result = service.reportPrice(makeRequest(price = BigDecimal("4.00")), user)

        assertEquals(ReportScore.INVALID, result.reportScore)
    }

    @Test
    fun `R1 - rejected report still saves to DB`() {
        val user = mockUser()
        val existingPrices = existingPricesNear(10.0)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)

        service.reportPrice(makeRequest(price = BigDecimal("20.00")), user)

        verify(priceRepo).save(any())
    }

    // ─────────────────────────────────────────────────────────
    // Score — initial score formula
    // ─────────────────────────────────────────────────────────

    @Test
    fun `score - high trust user with no deviation gets high score`() {
        val user = mockUser(trustScore = 0.9)
        stubReportDeps(user = user, totalReports = 0L)

        val result = service.reportPrice(makeRequest(), user)

        assertTrue(result.score >= ReportScore.VALID_MIN)
    }

    @Test
    fun `score - low trust user gets lower score than high trust user`() {
        val highTrust = mockUser(id = 1L, trustScore = 0.9)
        val lowTrust = mockUser(id = 2L, trustScore = 0.3)

        stubReportDeps(user = highTrust, totalReports = 0L)
        val highResult = service.reportPrice(makeRequest(), highTrust)

        stubReportDeps(user = lowTrust, totalReports = 0L)
        val lowResult = service.reportPrice(makeRequest(), lowTrust)

        assertTrue(highResult.score > lowResult.score)
    }

    @Test
    fun `score - higher deviation reduces score`() {
        val user = mockUser(trustScore = 0.9)
        val existingPrices = existingPricesNear(10.0)

        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)
        val smallDeviation = service.reportPrice(makeRequest(price = BigDecimal("10.50")), user)

        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)
        val largeDeviation = service.reportPrice(makeRequest(price = BigDecimal("13.00")), user)

        assertTrue(smallDeviation.score > largeDeviation.score)
    }

    @Test
    fun `score - invalid report gets score 0`() {
        val user = mockUser()
        val existingPrices = existingPricesNear(10.0)
        stubReportDeps(user = user, existingPrices = existingPrices, totalReports = 5L)

        val result = service.reportPrice(makeRequest(price = BigDecimal("20.00")), user)

        assertEquals(0.0, result.score, 0.001)
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
        val userSmall = mockUser(id = 1L, trustScore = 0.5)
        val userLarge = mockUser(id = 2L, trustScore = 0.5)
        val existingPrices = existingPricesNear(10.0)

        stubReportDeps(user = userSmall, existingPrices = existingPrices, totalReports = 5L)
        service.reportPrice(makeRequest(price = BigDecimal("15.00")), userSmall)

        stubReportDeps(user = userLarge, existingPrices = existingPrices, totalReports = 5L)
        service.reportPrice(makeRequest(price = BigDecimal("30.00")), userLarge)

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
        val userFull = mockUser(id = 1L, trustScore = 0.5, recoveryMultiplier = 1.0)
        val userHalf = mockUser(id = 2L, trustScore = 0.5, recoveryMultiplier = 0.5)

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
        whenever(priceRepo.getDistanceMeters(any(), any(), any())).thenReturn(50.0)

        val result =
            service.reportPrice(
                makeRequest(userLatitude = -34.7148, userLongitude = -58.2983),
                user,
            )

        assertEquals(1.0, result.locationConfidence, 0.001)
    }

    // ─────────────────────────────────────────────────────────
    // confirmPrice — R9
    // ─────────────────────────────────────────────────────────

    @Test
    fun `R9 - confirm increases report score`() {
        val confirmer = mockUser(trustScore = 0.8)
        val report = mockReport(score = 0.55)
        whenever(priceRepo.findById(10L)).thenReturn(Optional.of(report))
        whenever(validationRepo.existsByReportIdAndUserId(10L, confirmer.id)).thenReturn(false)
        whenever(priceRepo.save(any())).thenAnswer { it.arguments[0] as LocationProductPrice }
        whenever(userRepo.save(any())).thenAnswer { it.arguments[0] as User }

        service.confirmPrice(10L, confirmer)

        assertTrue(report.score > 0.55)
    }

    @Test
    fun `R9 - confirm is idempotent for same user`() {
        val confirmer = mockUser(trustScore = 0.8)
        val report = mockReport(score = 0.55)
        whenever(priceRepo.findById(10L)).thenReturn(Optional.of(report))
        whenever(validationRepo.existsByReportIdAndUserId(10L, confirmer.id)).thenReturn(true)

        service.confirmPrice(10L, confirmer)

        verify(priceRepo, never()).save(any())
    }

    @Test
    fun `R9 - confirm throws when user trust score too low`() {
        val confirmer = mockUser(trustScore = 0.10)

        assertThrows<IllegalStateException> {
            service.confirmPrice(10L, confirmer)
        }
    }

    @Test
    fun `R9 - confirmer trust score increases after confirmation`() {
        val confirmer = mockUser(trustScore = 0.8)
        val report = mockReport(score = 0.55)
        whenever(priceRepo.findById(10L)).thenReturn(Optional.of(report))
        whenever(validationRepo.existsByReportIdAndUserId(10L, confirmer.id)).thenReturn(false)
        whenever(priceRepo.save(any())).thenAnswer { it.arguments[0] as LocationProductPrice }
        whenever(userRepo.save(any())).thenAnswer { it.arguments[0] as User }

        service.confirmPrice(10L, confirmer)

        assertTrue(confirmer.trustScore > 0.8)
    }

    // ─────────────────────────────────────────────────────────
    // disputePrice — R12
    // ─────────────────────────────────────────────────────────

    @Test
    fun `R12 - dispute reduces report score`() {
        val disputer = mockUser(id = 2L, trustScore = 0.8)
        val reporter = mockUser(id = 3L, trustScore = 0.6)
        val report = mockReport(score = 0.55, user = reporter)
        whenever(priceRepo.findById(10L)).thenReturn(Optional.of(report))
        whenever(validationRepo.existsByReportIdAndUserId(10L, disputer.id)).thenReturn(false)
        whenever(priceRepo.save(any())).thenAnswer { it.arguments[0] as LocationProductPrice }
        whenever(userRepo.save(any())).thenAnswer { it.arguments[0] as User }
        stubReportDeps(productId = 1L, locationId = 2L, user = disputer)

        val disputeRequest = DisputePriceRequest(BigDecimal("15.00"), null, null)
        service.disputePrice(10L, disputer, disputeRequest)

        assertTrue(report.score < 0.55)
    }

    @Test
    fun `R12 - dispute penalizes original reporter`() {
        val disputer = mockUser(id = 2L, trustScore = 0.8)
        val reporter = mockUser(id = 3L, trustScore = 0.6)
        val report = mockReport(score = 0.55, user = reporter)
        whenever(priceRepo.findById(10L)).thenReturn(Optional.of(report))
        whenever(validationRepo.existsByReportIdAndUserId(10L, disputer.id)).thenReturn(false)
        whenever(priceRepo.save(any())).thenAnswer { it.arguments[0] as LocationProductPrice }
        whenever(userRepo.save(any())).thenAnswer { it.arguments[0] as User }
        stubReportDeps(productId = 1L, locationId = 2L, user = disputer)

        service.disputePrice(10L, disputer, DisputePriceRequest(BigDecimal("15.00"), null, null))

        assertTrue(reporter.trustScore < 0.6)
    }

    @Test
    fun `R12 - dispute throws when user trust score too low`() {
        val disputer = mockUser(trustScore = 0.10)

        assertThrows<IllegalStateException> {
            service.disputePrice(10L, disputer, DisputePriceRequest(BigDecimal("15.00"), null, null))
        }
    }

    @Test
    fun `R12 - dispute throws when already disputed`() {
        val disputer = mockUser(id = 2L, trustScore = 0.8)
        val report = mockReport(score = 0.55)
        whenever(priceRepo.findById(10L)).thenReturn(Optional.of(report))
        whenever(validationRepo.existsByReportIdAndUserId(10L, disputer.id)).thenReturn(true)

        assertThrows<IllegalStateException> {
            service.disputePrice(10L, disputer, DisputePriceRequest(BigDecimal("15.00"), null, null))
        }
    }

    @Test
    fun `R12 - larger price difference causes larger score reduction`() {
        val disputer = mockUser(id = 2L, trustScore = 0.8)
        val reporter = mockUser(id = 3L, trustScore = 0.6)

        val reportSmall = mockReport(id = 10L, score = 0.55, user = reporter, price = BigDecimal("10.00"))
        val reportLarge = mockReport(id = 11L, score = 0.55, user = reporter, price = BigDecimal("10.00"))

        whenever(priceRepo.findById(10L)).thenReturn(Optional.of(reportSmall))
        whenever(priceRepo.findById(11L)).thenReturn(Optional.of(reportLarge))
        whenever(validationRepo.existsByReportIdAndUserId(any(), any())).thenReturn(false)
        whenever(priceRepo.save(any())).thenAnswer { it.arguments[0] as LocationProductPrice }
        whenever(userRepo.save(any())).thenAnswer { it.arguments[0] as User }
        stubReportDeps(productId = 1L, locationId = 2L, user = disputer)

        service.disputePrice(10L, disputer, DisputePriceRequest(BigDecimal("11.00"), null, null))
        service.disputePrice(11L, disputer, DisputePriceRequest(BigDecimal("20.00"), null, null))

        assertTrue(reportLarge.score < reportSmall.score)
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
                priceEntry(BigDecimal("20.00"), daysAgo = 0),
                priceEntry(BigDecimal("40.00"), daysAgo = 0),
            )
        )

        val result = service.getPriceSummary(1L)

        assertEquals(30.0, result.weightedPrice!!, 0.001)
    }

    @Test
    fun `weightedPrice - low locationConfidence reduces weight of report`() {
        val stats = mockStats()
        stubSummaryDeps(
            1L,
            stats,
            listOf(
                priceEntry(BigDecimal("100.00"), daysAgo = 0, locationConfidence = 1.0),
                priceEntry(BigDecimal("10.00"), daysAgo = 0, locationConfidence = 0.1),
            ),
        )

        val result = service.getPriceSummary(1L)

        // weighted result should be much closer to 100 than to 55 (simple average)
        assertTrue(result.weightedPrice!! > 55.0)
    }
}
