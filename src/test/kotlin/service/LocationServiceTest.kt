package service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import preq.enum.LocationDetectionStatus
import preq.enum.LocationType
import preq.model.Location
import preq.repository.LocationRepository
import preq.service.LocationService
import preq.web.dto.projection.LocationWithDistance
import preq.web.dto.request.CreateLocationRequest
import kotlin.test.assertNotEquals

class LocationServiceTest {
    private val locationRepository: LocationRepository = mock()
    private val service = LocationService(locationRepository)

    private fun mockLocationWithDistance(distanceMeters: Double = 50.0): LocationWithDistance {
        val m = mock<LocationWithDistance>()
        whenever(m.getId()).thenReturn(1L)
        whenever(m.getName()).thenReturn("Carrefour")
        whenever(m.getAddress()).thenReturn("Av. Corrientes 1234")
        whenever(m.getType()).thenReturn("SUPERMARKET")
        whenever(m.getDistanceMeters()).thenReturn(distanceMeters)
        return m
    }

    private fun createRequest(
        name: String = "Carrefour",
        address: String = "Av. Corrientes 1234",
        type: LocationType = LocationType.SUPERMARKET,
        latitude: Double = -34.603722,
        longitude: Double = -58.381592,
    ) = CreateLocationRequest(name = name, address = address, type = type, latitude = latitude, longitude = longitude)

    // ─── search ───────────────────────────────────────────────────────────────────

    @Test
    fun `search returns matching locations`() {
        val location = Location().apply { name = "Carrefour" }
        whenever(locationRepository.searchByName("Carrefour")).thenReturn(listOf(location))

        val result = service.search("Carrefour")

        assertEquals(1, result.size)
        assertEquals("Carrefour", result[0].name)
    }

    @Test
    fun `search returns empty list when no matches`() {
        whenever(locationRepository.searchByName("xyz")).thenReturn(emptyList())

        assertTrue(service.search("xyz").isEmpty())
    }

    // ─── create ───────────────────────────────────────────────────────────────────

    @Test
    fun `create saves location with all fields from request`() {
        whenever(locationRepository.save(any())).thenAnswer { it.arguments[0] as Location }

        val result = service.create(createRequest())

        assertEquals("Carrefour", result.name)
        assertEquals("Av. Corrientes 1234", result.address)
        assertEquals(LocationType.SUPERMARKET, result.type)
        assertEquals(-34.603722, result.latitude)
        assertEquals(-58.381592, result.longitude)
        verify(locationRepository, times(1)).save(any())
    }

    @Test
    fun `create sets coordinates as JTS Point with correct values`() {
        whenever(locationRepository.save(any())).thenAnswer { it.arguments[0] as Location }

        val result = service.create(createRequest(latitude = -34.603722, longitude = -58.381592))

        // JTS Point stores (longitude, latitude) as (x, y)
        assertEquals(-58.381592, result.coordinates!!.x, 0.000001)
        assertEquals(-34.603722, result.coordinates!!.y, 0.000001)
    }

    // ─── findNearby ───────────────────────────────────────────────────────────────

    @Test
    fun `findNearby returns found response with distance when location is within range`() {
        val locationWithDistance = mockLocationWithDistance(distanceMeters = 80.0)
        whenever(locationRepository.findWithinRange(-34.603722, -58.381592, 150.0)).thenReturn(locationWithDistance)

        val result = service.findNearby(-34.603722, -58.381592)

        assertTrue(result.status == LocationDetectionStatus.FOUND)
        assertNotNull(result.location)
        assertEquals(80.0, result.distanceMeters)
    }

    @Test
    fun `findNearby returns notFound when no location is within range`() {
        whenever(locationRepository.findWithinRange(any(), any(), any())).thenReturn(null)

        val result = service.findNearby(-34.603722, -58.381592)

        assertNotEquals(result.status, LocationDetectionStatus.FOUND)
        assertNull(result.location)
    }

    @Test
    fun `findNearby always uses 150 meters as search radius`() {
        whenever(locationRepository.findWithinRange(any(), any(), any())).thenReturn(null)

        service.findNearby(-34.603722, -58.381592)

        verify(locationRepository).findWithinRange(-34.603722, -58.381592, 150.0)
    }
}
