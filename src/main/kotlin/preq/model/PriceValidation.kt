package preq.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import preq.enum.PriceValidationType

@Entity
@Table(name = "price_validations")
class PriceValidation : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    var report: LocationProductPrice? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: PriceValidationType = PriceValidationType.CONFIRM
}
