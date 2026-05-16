package service

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.assertThrows
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import preq.model.User
import preq.repository.UserRepository
import preq.service.UserService
import preq.web.dto.request.UpdateUserRequest
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals

class UserServiceTest {

    private val userRepository: UserRepository = mock()
    private val service = UserService(userRepository)

    private fun mockUser(
        name: String = "Juan",
        lastName: String = "Pérez",
        email: String = "juan@email.com",
        address: String? = "Av. Corrientes 1234",
        latitude: Double? = -34.603722,
        longitude: Double? = -58.381592,
    ) = User().apply {
        this.name = name
        this.lastName = lastName
        this.email = email
        this.address = address
        this.addressLocation = if (latitude != null && longitude != null)
            GeometryFactory(PrecisionModel(), 4326).createPoint(Coordinate(longitude, latitude))
        else null
    }

    private fun updateRequest(
        name: String = "Carlos",
        lastName: String = "García",
        address: String? = "Av. Santa Fe 800",
        latitude: Double? = -34.603722,
        longitude: Double? = -58.381592,
    ) = UpdateUserRequest(
        name = name,
        lastName = lastName,
        address = address,
        latitude = latitude,
        longitude = longitude
    )

    // ─── getProfile ───────────────────────────────────────────────────────────────

    @Test
    fun `getProfile returns correct profile for existing user`() {
        whenever(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(mockUser()))

        val result = service.getProfile("juan@email.com")

        assertEquals("Juan", result.name)
        assertEquals("Pérez", result.lastName)
        assertEquals("juan@email.com", result.email)
        assertEquals("Av. Corrientes 1234", result.address)
        assertEquals(-34.603722, result.latitude)
        assertEquals(-58.381592, result.longitude)
    }

    @Test
    fun `getProfile returns null lat and lon when user has no address location`() {
        whenever(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(mockUser(latitude = null, longitude = null)))

        val result = service.getProfile("juan@email.com")

        assertNull(result.latitude)
        assertNull(result.longitude)
    }

    @Test
    fun `getProfile throws when user not found`() {
        whenever(userRepository.findByEmail("missing@email.com")).thenReturn(Optional.empty())

        assertThrows<IllegalArgumentException> { service.getProfile("missing@email.com") }
    }

    // ─── updateProfile ────────────────────────────────────────────────────────────

    @Test
    fun `updateProfile updates name, lastName and address`() {
        val user = mockUser()
        whenever(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(user))
        whenever(userRepository.save(any())).thenReturn(user)

        val result = service.updateProfile("juan@email.com", updateRequest(name = "Carlos", lastName = "García", address = "Av. Santa Fe 800"))

        assertEquals("Carlos", result.name)
        assertEquals("García", result.lastName)
        assertEquals("Av. Santa Fe 800", result.address)
    }

    @Test
    fun `updateProfile sets addressLocation as JTS Point when lat and lon are provided`() {
        val user = mockUser()
        whenever(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(user))
        whenever(userRepository.save(any())).thenReturn(user)

        val result = service.updateProfile("juan@email.com", updateRequest(latitude = -34.603722, longitude = -58.381592))

        assertEquals(-58.381592, result.longitude)
        assertEquals(-34.603722, result.latitude)
    }

    @Test
    fun `updateProfile sets addressLocation to null when lat or lon are missing`() {
        val user = mockUser()
        whenever(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(user))
        whenever(userRepository.save(any())).thenReturn(user)

        val result = service.updateProfile("juan@email.com", updateRequest(latitude = null, longitude = null))

        assertNull(result.latitude)
        assertNull(result.longitude)
    }

    @Test
    fun `updateProfile throws when user not found`() {
        whenever(userRepository.findByEmail("missing@email.com")).thenReturn(Optional.empty())

        assertThrows<IllegalArgumentException> {
            service.updateProfile("missing@email.com", updateRequest())
        }

        verify(userRepository, never()).save(any())
    }

    @Test
    fun `updateProfile saves user exactly once`() {
        val user = mockUser()
        whenever(userRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(user))
        whenever(userRepository.save(any())).thenReturn(user)

        service.updateProfile("juan@email.com", updateRequest())

        verify(userRepository, times(1)).save(user)
    }
}