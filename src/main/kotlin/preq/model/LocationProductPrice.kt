package preq.model

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Entity
@Table(name = "location_product_price")
class LocationProductPrice : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    var product: Product? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    var location: Location? = null

    var price: BigDecimal = BigDecimal.ZERO

    var reportedAt: LocalDateTime = LocalDateTime.now()

    fun ageInDays() = ChronoUnit.DAYS.between(reportedAt, LocalDateTime.now())
}
