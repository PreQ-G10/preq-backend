package preq.model

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Entity
@Table(name = "location_product_price")
class LocationProductPrice : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @NotNull
    var product: Product? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    @NotNull
    var location: Location? = null

    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    var price: BigDecimal = BigDecimal.ZERO

    @PastOrPresent(message = "Reported date cannot be in the future")
    var reportedAt: LocalDateTime = LocalDateTime.now()

    fun ageInDays() = ChronoUnit.DAYS.between(reportedAt, LocalDateTime.now())
}
