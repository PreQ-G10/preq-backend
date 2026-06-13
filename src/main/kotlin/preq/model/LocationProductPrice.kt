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
import kotlin.math.exp
import kotlin.math.pow

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

    fun applyDisputePenalty(deviation: Double) {
        score = (score - deviation * 0.3).coerceIn(0.0, 1.0)
    }

    fun applyConfirmationBoost(confirmerTrustScore: Double) {
        score = (score + (1 - score) * confirmerTrustScore * 0.3).coerceIn(0.0, 1.0)
    }

    companion object {
        fun computeInflationAdjustedPrice(
            prices: List<LocationProductPrice>,
            monthlyInflationRate: Double,
        ): Double? {
            if (prices.isEmpty()) return null
            val decayFactor = 0.01
            var weightedSum = 0.0
            var totalWeight = 0.0
            prices.forEach { report ->
                val inflatedPrice = report.price.toDouble() * (1 + monthlyInflationRate).pow(report.ageInDays() / 30.0)
                val weight = exp(-decayFactor * report.ageInDays()) * report.locationConfidence
                weightedSum += inflatedPrice * weight
                totalWeight += weight
            }
            return if (totalWeight == 0.0) null else weightedSum / totalWeight
        }

        fun computeInitialScore(
            locationConfidence: Double,
            userTrustScore: Double,
            deviationPenalty: Double,
        ): Double = (locationConfidence * (0.5 + userTrustScore * 0.5) * (1 - deviationPenalty * 0.3)).coerceIn(0.0, 1.0)

        fun computeDecayWeightedPrice(prices: List<LocationProductPrice>): Double? {
            if (prices.isEmpty()) return null
            val decayFactor = 0.01
            var weightedSum = 0.0
            var totalWeight = 0.0
            prices.forEach { report ->
                val weight = exp(-decayFactor * report.ageInDays()) * report.locationConfidence
                weightedSum += report.price.toDouble() * weight
                totalWeight += weight
            }
            return if (totalWeight == 0.0) null else weightedSum / totalWeight
        }
    }
}
