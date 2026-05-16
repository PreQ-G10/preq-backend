package model

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import preq.enum.LocationType
import preq.model.Location
import preq.model.LocationProductPrice

class LocationTest {
    private lateinit var validator: Validator

    @BeforeEach
    fun setup() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    private fun validLocation() =
        Location().apply {
            name = "Supermercado Día"
            address = "Av. Corrientes 1234, Buenos Aires"
            type = LocationType.SUPERMARKET
            latitude = -34.6037
            longitude = -58.3816
        }

    // ── valid ─────────────────────────────────────────────────────────────

    @Test
    fun `valid location passes validation`() {
        val violations = validator.validate(validLocation())
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `valid location with no coordinates passes validation`() {
        val location =
            validLocation().apply {
                latitude = null
                longitude = null
            }
        val violations = validator.validate(location)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `type defaults to OTHER`() {
        val location =
            Location().apply {
                name = "Supermercado Día"
                address = "Av. Corrientes 1234, Buenos Aires"
            }
        assertEquals(LocationType.OTHER, location.type)
    }

    // ── name ──────────────────────────────────────────────────────────────

    @Test
    fun `blank name fails validation`() {
        val location = validLocation().apply { name = "   " }
        val violations = validator.validate(location)
        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `empty name fails validation`() {
        val location = validLocation().apply { name = "" }
        val violations = validator.validate(location)
        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }

    // ── address ───────────────────────────────────────────────────────────

    @Test
    fun `blank address fails validation`() {
        val location = validLocation().apply { address = "   " }
        val violations = validator.validate(location)
        assertTrue(violations.any { it.propertyPath.toString() == "address" })
    }

    @Test
    fun `empty address fails validation`() {
        val location = validLocation().apply { address = "" }
        val violations = validator.validate(location)
        assertTrue(violations.any { it.propertyPath.toString() == "address" })
    }

    // ── latitude ──────────────────────────────────────────────────────────

    @Test
    fun `latitude below minus 90 fails validation`() {
        val location = validLocation().apply { latitude = -90.1 }
        val violations = validator.validate(location)
        assertTrue(violations.any { it.propertyPath.toString() == "latitude" })
    }

    @Test
    fun `latitude above 90 fails validation`() {
        val location = validLocation().apply { latitude = 90.1 }
        val violations = validator.validate(location)
        assertTrue(violations.any { it.propertyPath.toString() == "latitude" })
    }

    @Test
    fun `latitude at boundary minus 90 passes validation`() {
        val location = validLocation().apply { latitude = -90.0 }
        val violations = validator.validate(location)
        assertTrue(violations.none { it.propertyPath.toString() == "latitude" })
    }

    @Test
    fun `latitude at boundary 90 passes validation`() {
        val location = validLocation().apply { latitude = 90.0 }
        val violations = validator.validate(location)
        assertTrue(violations.none { it.propertyPath.toString() == "latitude" })
    }

    // ── longitude ─────────────────────────────────────────────────────────

    @Test
    fun `longitude below minus 180 fails validation`() {
        val location = validLocation().apply { longitude = -180.1 }
        val violations = validator.validate(location)
        assertTrue(violations.any { it.propertyPath.toString() == "longitude" })
    }

    @Test
    fun `longitude above 180 fails validation`() {
        val location = validLocation().apply { longitude = 180.1 }
        val violations = validator.validate(location)
        assertTrue(violations.any { it.propertyPath.toString() == "longitude" })
    }

    @Test
    fun `longitude at boundary minus 180 passes validation`() {
        val location = validLocation().apply { longitude = -180.0 }
        val violations = validator.validate(location)
        assertTrue(violations.none { it.propertyPath.toString() == "longitude" })
    }

    @Test
    fun `longitude at boundary 180 passes validation`() {
        val location = validLocation().apply { longitude = 180.0 }
        val violations = validator.validate(location)
        assertTrue(violations.none { it.propertyPath.toString() == "longitude" })
    }

    // ── hasCoordinates ────────────────────────────────────────────────────

    @Test
    fun `hasCoordinates returns true when both lat and lon are set`() {
        assertTrue(validLocation().hasCoordinates())
    }

    @Test
    fun `hasCoordinates returns false when latitude is null`() {
        val location = validLocation().apply { latitude = null }
        assertFalse(location.hasCoordinates())
    }

    @Test
    fun `hasCoordinates returns false when longitude is null`() {
        val location = validLocation().apply { longitude = null }
        assertFalse(location.hasCoordinates())
    }

    @Test
    fun `hasCoordinates returns false when both are null`() {
        val location =
            validLocation().apply {
                latitude = null
                longitude = null
            }
        assertFalse(location.hasCoordinates())
    }

    // ── prices ────────────────────────────────────────────────────────────

    @Test
    fun `prices list is empty by default`() {
        assertTrue(validLocation().prices.isEmpty())
    }

    @Test
    fun `prices list accepts multiple entries`() {
        val location = validLocation()
        repeat(3) { location.prices.add(LocationProductPrice()) }
        assertEquals(3, location.prices.size)
    }
}
