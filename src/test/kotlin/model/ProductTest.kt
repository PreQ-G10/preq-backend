package model

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import preq.enum.ProductImageStatus
import preq.model.Product
import preq.model.ProductImage
import java.math.BigDecimal

class ProductTest {
    private lateinit var validator: Validator

    @BeforeEach
    fun setup() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    // ── @NotBlank validation ──────────────────────────────────────────────

    @Test
    fun `brand blank fails validation`() {
        val product =
            Product().apply {
                brand = " "
                name = "Pasta de Maní"
            }
        val violations = validator.validate(product)
        assertTrue(violations.any { it.propertyPath.toString() == "brand" })
    }

    @Test
    fun `name blank fails validation`() {
        val product =
            Product().apply {
                brand = "Maní King"
                name = " "
            }
        val violations = validator.validate(product)
        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `valid product passes validation`() {
        val product =
            Product().apply {
                brand = "Maní King"
                name = "Pasta de Maní"
                quantity = BigDecimal("485.00")
                quantityType = "g"
            }
        val violations = validator.validate(product)
        print(violations)
        println(product.toString())
        assertTrue(violations.isEmpty())
    }

    // ── quantity edge cases ───────────────────────────────────────────────

    @Test
    fun `quantity defaults to zero`() {
        val product =
            Product().apply {
                brand = "Maní King"
                name = "Pasta de Maní"
            }
        assertEquals(BigDecimal.ZERO, product.quantity)
    }

    @Test
    fun `quantity accepts maximum precision 10 scale 2`() {
        val product =
            Product().apply {
                brand = "Maní King"
                name = "Pasta de Maní"
                quantity = BigDecimal("99999999.99")
            }
        assertEquals(BigDecimal("99999999.99"), product.quantity)
    }

    @Test
    fun `quantity accepts negative value`() {
        // DB constraint is precision+scale only — negative not blocked at model level
        val product =
            Product().apply {
                brand = "Maní King"
                name = "Pasta de Maní"
                quantity = BigDecimal("-1.00")
            }
        assertEquals(BigDecimal("-1.00"), product.quantity)
    }

    @Test
    fun `quantity accepts zero`() {
        val product =
            Product().apply {
                brand = "Maní King"
                name = "Pasta de Maní"
                quantity = BigDecimal.ZERO
            }
        assertEquals(BigDecimal.ZERO, product.quantity)
    }

    // ── barcode ───────────────────────────────────────────────────────────

    @Test
    fun `barcode is null by default`() {
        val product =
            Product().apply {
                brand = "Maní King"
                name = "Pasta de Maní"
            }
        assertFalse(product.hasBarcode())
    }

    @Test
    fun `hasBarcode returns true when barcode is set`() {
        val product =
            Product().apply {
                brand = "Maní King"
                name = "Pasta de Maní"
                barcode = "7790895000153"
            }
        assertTrue(product.hasBarcode())
    }

    @Test
    fun `hasBarcode returns false when barcode is null`() {
        val product =
            Product().apply {
                brand = "Maní King"
                name = "Pasta de Maní"
                barcode = null
            }
        assertFalse(product.hasBarcode())
    }

    // ── images ────────────────────────────────────────────────────────────

    @Test
    fun `images list is empty by default`() {
        val product =
            Product().apply {
                brand = "Maní King"
                name = "Pasta de Maní"
            }
        assertTrue(product.images.isEmpty())
    }

    @Test
    fun `approvedImages returns only approved images`() {
        val product =
            Product().apply {
                brand = "Maní King"
                name = "Pasta de Maní"
            }
        val approved = ProductImage().apply { status = ProductImageStatus.APPROVED }
        val pending = ProductImage().apply { status = ProductImageStatus.PENDING_REVIEW }
        product.images.addAll(listOf(approved, pending))

        val result = product.approvedImages()
        assertEquals(1, result.size)
        assertEquals(ProductImageStatus.APPROVED, result[0].status)
    }

    @Test
    fun `approvedImages returns empty when no approved images`() {
        val product =
            Product().apply {
                brand = "Maní King"
                name = "Pasta de Maní"
            }
        val pending = ProductImage().apply { status = ProductImageStatus.PENDING_REVIEW }
        product.images.add(pending)

        assertTrue(product.approvedImages().isEmpty())
    }

    @Test
    fun `approvedImages returns all when all are approved`() {
        val product =
            Product().apply {
                brand = "Maní King"
                name = "Pasta de Maní"
            }
        repeat(3) {
            product.images.add(ProductImage().apply { status = ProductImageStatus.APPROVED })
        }
        assertEquals(3, product.approvedImages().size)
    }

    // ── quantityType ──────────────────────────────────────────────────────

    @Test
    fun `quantityType defaults to empty string`() {
        val product =
            Product().apply {
                brand = "Maní King"
                name = "Pasta de Maní"
            }
        assertEquals("", product.quantityType)
    }

    @Test
    fun `quantityType accepts long string`() {
        val product =
            Product().apply {
                brand = "Maní King"
                name = "Pasta de Maní"
                quantityType = "a".repeat(255)
            }
        assertEquals(255, product.quantityType.length)
    }
}
