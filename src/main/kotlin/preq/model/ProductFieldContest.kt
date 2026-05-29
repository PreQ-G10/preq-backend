package preq.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import preq.enum.FieldType

@Entity
@Table(name = "product_field_contest")
class ProductFieldContest : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false)
    lateinit var fieldType: FieldType

    @Column(nullable = false)
    var fieldValue: String = ""
}
