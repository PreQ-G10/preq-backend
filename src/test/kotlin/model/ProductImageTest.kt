package model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import preq.enum.ProductImageStatus
import preq.model.ProductImage

class ProductImageTest {

    @Test
    fun `isApproved returns true only for approved status`() {
        val approved = ProductImage().apply { status = ProductImageStatus.APPROVED }
        val pending = ProductImage().apply { status = ProductImageStatus.PENDING_REVIEW }

        assertTrue(approved.isApproved())
        assertFalse(pending.isApproved())
    }

    @Test
    fun `embeddingAsString throws when embedding is null`() {
        val image = ProductImage()
        assertThrows<IllegalStateException> { image.embeddingAsString() }
    }

    @Test
    fun `embeddingAsString returns correct pgvector format`() {
        val image = ProductImage().apply {
            embedding = floatArrayOf(0.1f, 0.2f, 0.3f)
        }
        assertEquals("[0.1,0.2,0.3]", image.embeddingAsString())
    }
}