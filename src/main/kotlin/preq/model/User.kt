package preq.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.locationtech.jts.geom.Point
import preq.enum.UserRole
import kotlin.math.abs

@Entity
@Table(name = "users")
class User : BaseEntity() {
    @NotBlank
    @Pattern(regexp = "^[^0-9]{1,50}$", message = "Name must not contain numbers and be at most 50 characters")
    @Column(nullable = false)
    lateinit var name: String

    @NotBlank
    @Pattern(regexp = "^[^0-9]{1,50}$", message = "Last name must not contain numbers and be at most 50 characters")
    @Column(name = "last_name", nullable = false)
    lateinit var lastName: String

    @Column(nullable = true)
    var address: String? = null

    @Column(nullable = true, columnDefinition = "geometry(Point, 4326)")
    var addressLocation: Point? = null

    @NotBlank
    @Email(message = "Email must be valid")
    @Column(nullable = false, unique = true)
    lateinit var email: String

    @NotBlank
    @Size(min = 5, message = "Password must be at least 5 characters")
    @Column(nullable = false)
    lateinit var password: String

    @DecimalMin(value = "0.0", message = "Trust score must be >= 0.0")
    @DecimalMax(value = "1.0", message = "Trust score must be <= 1.0")
    @Column(name = "trust_score", nullable = false)
    var trustScore: Double = 0.5

    @Column(name = "recovery_multiplier", nullable = false)
    var recoveryMultiplier: Double = 1.0

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.USER

    fun adjustTrustScore(delta: Double, threshold: Double) {
        val wasAboveThreshold = trustScore >= threshold
        trustScore = (trustScore + delta).coerceIn(0.0, 1.0)
        if (wasAboveThreshold && trustScore < threshold) {
            recoveryMultiplier *= 0.5
        }
    }

    fun canValidatePrices(threshold: Double) = trustScore >= threshold

    fun applyPricePenaltyIfNeeded(deviation: Double, priceThreshold: Double, trustThreshold: Double): Boolean {
        if (abs(deviation) <= priceThreshold) return false
        val penalty = deviation * deviation
        adjustTrustScore(-penalty, trustThreshold)
        return true
    }
}
