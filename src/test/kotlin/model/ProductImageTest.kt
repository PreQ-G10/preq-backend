package model

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import preq.enum.ProductImageStatus
import preq.model.Product
import preq.model.ProductImage
import java.math.BigDecimal

class ProductImageTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setup() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    private fun validProduct() = Product().apply {
        name = "Pasta de Maní"
        brand = "Maní King"
        quantity = BigDecimal("485.00")
        quantityType = "g"
    }

    private fun validProductImage() = ProductImage().apply {
        product = validProduct()
        imageUrl = "https://res.cloudinary.com/dkk1fjp1x/image/upload/v1/photo.jpg"
        confidenceScore = 0.92
        status = ProductImageStatus.PENDING_REVIEW
    }

    // ── valid ─────────────────────────────────────────────────────────────

    @Test
    fun `valid product image passes validation`() {
        val violations = validator.validate(validProductImage())
        assertTrue(violations.isEmpty())
    }

    // ── product ───────────────────────────────────────────────────────────

    @Test
    fun `null product fails validation`() {
        val image = validProductImage().apply { product = null }
        val violations = validator.validate(image)
        assertTrue(violations.any { it.propertyPath.toString() == "product" })
    }

    // ── imageUrl ──────────────────────────────────────────────────────────

    @Test
    fun `blank imageUrl fails validation`() {
        val image = validProductImage().apply { imageUrl = "   " }
        val violations = validator.validate(image)
        assertTrue(violations.any { it.propertyPath.toString() == "imageUrl" })
    }

    @Test
    fun `empty imageUrl fails validation`() {
        val image = validProductImage().apply { imageUrl = "" }
        val violations = validator.validate(image)
        assertTrue(violations.any { it.propertyPath.toString() == "imageUrl" })
    }

    @Test
    fun `imageUrl not starting with cloudinary domain fails validation`() {
        val image = validProductImage().apply { imageUrl = "https://imgur.com/photo.jpg" }
        val violations = validator.validate(image)
        assertTrue(violations.any { it.propertyPath.toString() == "imageUrl" })
    }

    @Test
    fun `imageUrl with http instead of https fails validation`() {
        val image = validProductImage().apply { imageUrl = "http://res.cloudinary.com/dkk1fjp1x/image/upload/v1/photo.jpg" }
        val violations = validator.validate(image)
        assertTrue(violations.any { it.propertyPath.toString() == "imageUrl" })
    }

    @Test
    fun `imageUrl with valid cloudinary url passes validation`() {
        val image = validProductImage().apply {
            imageUrl = "https://res.cloudinary.com/dkk1fjp1x/image/upload/v1776252009/photo.jpg"
        }
        val violations = validator.validate(image)
        assertTrue(violations.none { it.propertyPath.toString() == "imageUrl" })
    }

    @Test
    fun `imageUrl with arbitrary path after cloudinary domain passes validation`() {
        val image = validProductImage().apply {
            imageUrl = "https://res.cloudinary.com/anything/goes/here.png"
        }
        val violations = validator.validate(image)
        assertTrue(violations.none { it.propertyPath.toString() == "imageUrl" })
    }

    // ── confidenceScore ───────────────────────────────────────────────────

    @Test
    fun `confidenceScore below 0 fails validation`() {
        val image = validProductImage().apply { confidenceScore = -0.01 }
        val violations = validator.validate(image)
        assertTrue(violations.any { it.propertyPath.toString() == "confidenceScore" })
    }

    @Test
    fun `confidenceScore above 1 fails validation`() {
        val image = validProductImage().apply { confidenceScore = 1.01 }
        val violations = validator.validate(image)
        assertTrue(violations.any { it.propertyPath.toString() == "confidenceScore" })
    }

    @Test
    fun `confidenceScore at boundary 0_0 passes validation`() {
        val image = validProductImage().apply { confidenceScore = 0.0 }
        val violations = validator.validate(image)
        assertTrue(violations.none { it.propertyPath.toString() == "confidenceScore" })
    }

    @Test
    fun `confidenceScore at boundary 1_0 passes validation`() {
        val image = validProductImage().apply { confidenceScore = 1.0 }
        val violations = validator.validate(image)
        assertTrue(violations.none { it.propertyPath.toString() == "confidenceScore" })
    }

    @Test
    fun `confidenceScore midpoint 0_5 passes validation`() {
        val image = validProductImage().apply { confidenceScore = 0.5 }
        val violations = validator.validate(image)
        assertTrue(violations.none { it.propertyPath.toString() == "confidenceScore" })
    }

    // ── status ────────────────────────────────────────────────────────────

    @Test
    fun `status defaults to PENDING_REVIEW`() {
        val image = ProductImage()
        assertEquals(ProductImageStatus.PENDING_REVIEW, image.status)
    }

    @Test
    fun `isApproved returns false for PENDING_REVIEW`() {
        val image = validProductImage().apply { status = ProductImageStatus.PENDING_REVIEW }
        assertFalse(image.isApproved())
    }

    @Test
    fun `isApproved returns true for APPROVED`() {
        val image = validProductImage().apply { status = ProductImageStatus.APPROVED }
        assertTrue(image.isApproved())
    }

    // ── embedding ─────────────────────────────────────────────────────────

    @Test
    fun `embedding is null by default`() {
        val image = ProductImage()
        assertFalse(image.hasEmbedding())
    }

    @Test
    fun `hasEmbedding returns true when embedding is set`() {
        val image = validProductImage().apply { embedding = FloatArray(1000) { 0.1f } }
        assertTrue(image.hasEmbedding())
    }

    @Test
    fun `hasEmbedding returns false when embedding is null`() {
        val image = validProductImage().apply { embedding = null }
        assertFalse(image.hasEmbedding())
    }

    @Test
    fun `embeddingAsString throws when embedding is null`() {
        val image = validProductImage().apply { embedding = null }
        assertThrows(IllegalStateException::class.java) { image.embeddingAsString() }
    }

    @Test
    fun `embeddingAsString returns correct format`() {
        val image = validProductImage().apply { embedding = floatArrayOf(0.1f, 0.2f, 0.3f) }
        val result = image.embeddingAsString()
        assertEquals("[0.1,0.2,0.3]", result)
    }

    @Test
    fun `embeddingAsString with 1000 dimensions returns correctly formatted string`() {
        val image = validProductImage().apply { embedding = FloatArray(1000) { 0.5f } }
        val result = image.embeddingAsString()
        assertTrue(result.startsWith("["))
        assertTrue(result.endsWith("]"))
        assertEquals(1000, result.removePrefix("[").removeSuffix("]").split(",").size)
    }

    // ── defaults ──────────────────────────────────────────────────────────

    @Test
    fun `confidenceScore defaults to 0_0`() {
        val image = ProductImage()
        assertEquals(0.0, image.confidenceScore)
    }

    @Test
    fun `imageUrl defaults to empty string`() {
        val image = ProductImage()
        assertEquals("", image.imageUrl)
    }
}