package service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import preq.enum.PriceSource
import preq.model.Product
import preq.repository.LocationProductPriceRepository
import preq.repository.ProductRepository
import preq.service.CartService
import preq.web.dto.projection.LocationPriceResult
import preq.web.dto.request.CartCompareRequest
import preq.web.dto.request.CartItemRequest
import java.util.Optional

class CartServiceTest {

    private val locationProductPriceRepository: LocationProductPriceRepository = mock()
    private val productRepository: ProductRepository = mock()
    private val service = CartService(locationProductPriceRepository, productRepository, nearbyRadiusMeters = 10000.0)

    private fun mockProduct(id: Long, name: String = "Producto $id") =
        Product().apply { this.id = id; this.name = name }

    private fun mockLocationPrice(locationId: Long, avgPrice: Double, name: String = "Supermercado $locationId", address: String = "Calle $locationId", lat: Double = -34.6, lon: Double = -58.3): LocationPriceResult {
        val m = mock<LocationPriceResult>()
        whenever(m.getLocationId()).thenReturn(locationId)
        whenever(m.getAvgPrice()).thenReturn(avgPrice)
        whenever(m.getName()).thenReturn(name)
        whenever(m.getAddress()).thenReturn(address)
        whenever(m.getLatitude()).thenReturn(lat)
        whenever(m.getLongitude()).thenReturn(lon)
        return m
    }

    private fun cartRequest(
        vararg items: Pair<Long, Int>,
        userLat: Double? = null,
        userLon: Double? = null,
    ) = CartCompareRequest(
        items = items.map { (productId, qty) -> CartItemRequest(productId = productId, quantity = qty) },
        userLatitude = userLat,
        userLongitude = userLon,
    )

    private fun stubProduct(id: Long, name: String = "Producto $id") {
        whenever(productRepository.findById(id)).thenReturn(Optional.of(mockProduct(id, name)))
    }

    private fun stubPrices(productId: Long, globalAvg: Double?, vararg locationPrices: LocationPriceResult) {
        whenever(locationProductPriceRepository.getGlobalAvgPrice(productId)).thenReturn(globalAvg)
        whenever(locationProductPriceRepository.getLocationPricesForProduct(productId)).thenReturn(locationPrices.toList())
    }

    // ─── empty / no data ─────────────────────────────────────────────────────────

    @Test
    fun `compare returns empty locations and skips product when no price data`() {
        stubProduct(1L, "Leche")
        stubPrices(1L, null)

        val result = service.compare(cartRequest(1L to 2))

        assertTrue(result.locations.isEmpty())
        assertEquals(listOf("Leche"), result.skippedProducts)
    }

    @Test
    fun `compare skips product and reports its name when it has no price data anywhere`() {
        stubProduct(1L, "Leche")
        stubPrices(1L, null)
        stubProduct(2L, "Yogur")
        val locationPrice = mockLocationPrice(10L, avgPrice = 5.0)
        stubPrices(2L, null, locationPrice)

        val result = service.compare(cartRequest(1L to 1, 2L to 1))

        assertTrue(result.skippedProducts.contains("Leche"))
        assertFalse(result.skippedProducts.contains("Yogur"))
    }

    // ─── price source: REPORTED ───────────────────────────────────────────────────

    @Test
    fun `compare uses reported price when location has a direct price report`() {
        stubProduct(1L, "Arroz")
        val locationPrice = mockLocationPrice(10L, avgPrice = 3.50)
        stubPrices(1L, 5.0, locationPrice)

        val result = service.compare(cartRequest(1L to 2))

        val product = result.locations.first().products.first()
        assertEquals(PriceSource.REPORTED, product.priceSource)
        assertEquals(3.50, product.unitPrice)
        assertEquals(7.0, product.totalPrice)
    }

    @Test
    fun `compare multiplies unit price by quantity correctly`() {
        stubProduct(1L, "Fideos")
        val locationPrice = mockLocationPrice(10L, avgPrice = 2.0)
        stubPrices(1L, null, locationPrice)

        val result = service.compare(cartRequest(1L to 4))

        val product = result.locations.first().products.first()
        assertEquals(8.0, product.totalPrice)
    }

    // ─── price source: NEARBY_FALLBACK ────────────────────────────────────────────

    @Test
    fun `compare uses nearby fallback when location has no report but others do`() {
        stubProduct(1L, "Azucar")
        // Location 10 has a price, location 20 does not and has no global avg
        val locationPrice10 = mockLocationPrice(10L, avgPrice = 5.0)
        stubPrices(1L, null, locationPrice10)

        stubProduct(2L, "Sal")
        val locationPrice20 = mockLocationPrice(20L, avgPrice = 1.0)
        stubPrices(2L, null, locationPrice20)

        val result = service.compare(cartRequest(1L to 1, 2L to 1))

        val location20 = result.locations.first { it.locationId == 20L }
        val azucar = location20.products.first { it.productId == 1L }
        assertEquals(PriceSource.NEARBY_FALLBACK, azucar.priceSource)
    }

    // ─── sorting and top 5 ────────────────────────────────────────────────────────

    @Test
    fun `compare returns locations sorted by total estimated price ascending`() {
        stubProduct(1L, "Pan")
        val cheapLocation = mockLocationPrice(10L, avgPrice = 1.0, name = "Barato")
        val expensiveLocation = mockLocationPrice(20L, avgPrice = 9.0, name = "Caro")
        stubPrices(1L, null, cheapLocation, expensiveLocation)

        val result = service.compare(cartRequest(1L to 1))

        assertEquals("Barato", result.locations.first().name)
        assertEquals("Caro", result.locations.last().name)
    }

    @Test
    fun `compare returns at most 5 locations`() {
        stubProduct(1L, "Pan")
        val locationPrices = (10L..16L).map { mockLocationPrice(it, avgPrice = it.toDouble()) }
        stubPrices(1L, null, *locationPrices.toTypedArray())

        val result = service.compare(cartRequest(1L to 1))

        assertTrue(result.locations.size <= 5)
    }

    // ─── distance ─────────────────────────────────────────────────────────────────

    @Test
    fun `compare includes distance when user coordinates are provided`() {
        stubProduct(1L, "Leche")
        val locationPrice = mockLocationPrice(10L, avgPrice = 2.0, lat = -34.603722, lon = -58.381592)
        stubPrices(1L, null, locationPrice)

        val result = service.compare(cartRequest(1L to 1, userLat = -34.600000, userLon = -58.370000))

        assertNotNull(result.locations.first().distanceMeters)
        assertTrue(result.locations.first().distanceMeters!! > 0)
    }

    @Test
    fun `compare returns null distance when user coordinates are not provided`() {
        stubProduct(1L, "Leche")
        val locationPrice = mockLocationPrice(10L, avgPrice = 2.0)
        stubPrices(1L, null, locationPrice)

        val result = service.compare(cartRequest(1L to 1))

        assertNull(result.locations.first().distanceMeters)
    }

    // ─── total price excludes NO_DATA products ────────────────────────────────────

    @Test
    fun `compare excludes NEARBY_FALLBACK products from location total when no global avg`() {
        stubProduct(1L, "Arroz")
        val locationPrice10 = mockLocationPrice(10L, avgPrice = 4.0)
        stubPrices(1L, null, locationPrice10)

        stubProduct(2L, "Quinoa")
        val locationPrice20 = mockLocationPrice(20L, avgPrice = 99.0)
        stubPrices(2L, null, locationPrice20)

        val result = service.compare(cartRequest(1L to 1, 2L to 1))

        val location = result.locations.first { it.locationId == 10L }
        val quinoa = location.products.first { it.productId == 2L }
        assertEquals(PriceSource.NEARBY_FALLBACK, quinoa.priceSource)
    }
}