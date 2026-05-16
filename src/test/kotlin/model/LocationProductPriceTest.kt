package model

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import preq.model.Location
import preq.model.LocationProductPrice
import preq.model.Product
import java.math.BigDecimal
import java.time.LocalDateTime

class LocationProductPriceTest {
    private lateinit var validator: Validator

    @BeforeEach
    fun setup() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    private fun validProduct() =
        Product().apply {
            name = "Pasta de Maní"
            brand = "Maní King"
            quantity = BigDecimal("485.00")
            quantityType = "g"
        }

    private fun validLocation() =
        Location().apply {
            name = "Supermercado Día"
            address = "Av. Corrientes 1234, Buenos Aires"
            latitude = -34.6037
            longitude = -58.3816
        }

    private fun validPrice() =
        LocationProductPrice().apply {
            product = validProduct()
            location = validLocation()
            price = BigDecimal("1500.00")
            reportedAt = LocalDateTime.now()
        }

    // ── valid ─────────────────────────────────────────────────────────────

    @Test
    fun `valid price passes validation`() {
        val violations = validator.validate(validPrice())
        assertTrue(violations.isEmpty())
    }

    // ── product ───────────────────────────────────────────────────────────

    @Test
    fun `null product fails validation`() {
        val price = validPrice().apply { product = null }
        val violations = validator.validate(price)
        assertTrue(violations.any { it.propertyPath.toString() == "product" })
    }

    // ── location ──────────────────────────────────────────────────────────

    @Test
    fun `null location fails validation`() {
        val price = validPrice().apply { location = null }
        val violations = validator.validate(price)
        assertTrue(violations.any { it.propertyPath.toString() == "location" })
    }

    // ── price ─────────────────────────────────────────────────────────────

    @Test
    fun `zero price fails validation`() {
        val price = validPrice().apply { price = BigDecimal.ZERO }
        val violations = validator.validate(price)
        assertTrue(violations.any { it.propertyPath.toString() == "price" })
    }

    @Test
    fun `negative price fails validation`() {
        val price = validPrice().apply { price = BigDecimal("-1.00") }
        val violations = validator.validate(price)
        assertTrue(violations.any { it.propertyPath.toString() == "price" })
    }

    @Test
    fun `minimum valid price 0_01 passes validation`() {
        val price = validPrice().apply { price = BigDecimal("0.01") }
        val violations = validator.validate(price)
        assertTrue(violations.none { it.propertyPath.toString() == "price" })
    }

    @Test
    fun `very large price passes validation`() {
        val price = validPrice().apply { price = BigDecimal("999999999.99") }
        val violations = validator.validate(price)
        assertTrue(violations.none { it.propertyPath.toString() == "price" })
    }

    // ── reportedAt ────────────────────────────────────────────────────────

    @Test
    fun `future reportedAt fails validation`() {
        val price = validPrice().apply { reportedAt = LocalDateTime.now().plusDays(1) }
        val violations = validator.validate(price)
        assertTrue(violations.any { it.propertyPath.toString() == "reportedAt" })
    }

    @Test
    fun `past reportedAt passes validation`() {
        val price = validPrice().apply { reportedAt = LocalDateTime.now().minusDays(30) }
        val violations = validator.validate(price)
        assertTrue(violations.none { it.propertyPath.toString() == "reportedAt" })
    }

    @Test
    fun `current reportedAt passes validation`() {
        val price = validPrice().apply { reportedAt = LocalDateTime.now() }
        val violations = validator.validate(price)
        assertTrue(violations.none { it.propertyPath.toString() == "reportedAt" })
    }

    // ── ageInDays ─────────────────────────────────────────────────────────

    @Test
    fun `ageInDays returns zero for price reported today`() {
        val price = validPrice().apply { reportedAt = LocalDateTime.now() }
        assertEquals(0L, price.ageInDays())
    }

    @Test
    fun `ageInDays returns correct days for old report`() {
        val price = validPrice().apply { reportedAt = LocalDateTime.now().minusDays(10) }
        assertEquals(10L, price.ageInDays())
    }

    @Test
    fun `ageInDays returns correct days for report one year ago`() {
        val price = validPrice().apply { reportedAt = LocalDateTime.now().minusDays(365) }
        assertEquals(365L, price.ageInDays())
    }

    // ── defaults ──────────────────────────────────────────────────────────

    @Test
    fun `price defaults to zero`() {
        val price = LocationProductPrice()
        assertEquals(BigDecimal.ZERO, price.price)
    }

    @Test
    fun `reportedAt defaults to now`() {
        val before = LocalDateTime.now().minusSeconds(1)
        val price = LocationProductPrice()
        val after = LocalDateTime.now().plusSeconds(1)
        assertTrue(price.reportedAt.isAfter(before) && price.reportedAt.isBefore(after))
    }
}
