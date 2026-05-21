package preq.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import preq.enum.ReportScore
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @NotNull
    var user: User? = null

    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    var price: BigDecimal = BigDecimal.ZERO

    @PastOrPresent(message = "Reported date cannot be in the future")
    var reportedAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "location_confidence", nullable = false)
    var locationConfidence: Double = 1.0

    @Column(name = "score", nullable = false)
    var score: Double = 1.0

    val reportScore: ReportScore
        get() = ReportScore.fromScore(score)

    fun ageInDays() = ChronoUnit.DAYS.between(reportedAt, LocalDateTime.now())
}
