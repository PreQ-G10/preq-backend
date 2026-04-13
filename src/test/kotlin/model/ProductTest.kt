package model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import preq.enum.ProductImageStatus
import preq.model.Product
import preq.model.ProductImage

class ProductTest {

    @Test
    fun `approvedImages returns only approved images`() {
        val product = Product().apply {
            images.add(ProductImage().apply { status = ProductImageStatus.APPROVED })
            images.add(ProductImage().apply { status = ProductImageStatus.PENDING_REVIEW })
            images.add(ProductImage().apply { status = ProductImageStatus.REJECTED })
        }

        val approved = product.approvedImages()

        assertEquals(1, approved.size)
        assertTrue(approved.all { it.status == ProductImageStatus.APPROVED })
    }

    @Test
    fun `hasBarcode returns false when barcode is null`() {
        val product = Product()
        assertFalse(product.hasBarcode())
    }

    @Test
    fun `hasBarcode returns true when barcode is set`() {
        val product = Product().apply { barcode = "7790001234567" }
        assertTrue(product.hasBarcode())
    }
}