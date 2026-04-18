package model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import preq.model.LocationProductPrice
import java.time.LocalDateTime

class LocationProductPriceTest {
    @Test
    fun `ageInDays returns 0 for price reported today`() {
        val price =
            LocationProductPrice().apply {
                reportedAt = LocalDateTime.now()
            }
        assertEquals(0, price.ageInDays())
    }

    @Test
    fun `ageInDays returns correct days for old price`() {
        val price =
            LocationProductPrice().apply {
                reportedAt = LocalDateTime.now().minusDays(30)
            }
        assertEquals(30, price.ageInDays())
    }
}
