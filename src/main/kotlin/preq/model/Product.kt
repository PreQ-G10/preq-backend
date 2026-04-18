package preq.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import preq.enum.ProductImageStatus
import java.math.BigDecimal

@Entity
@Table(name = "product")
class Product : BaseEntity() {
    @NotBlank
    @Column(nullable = false)
    lateinit var brand: String

    @NotBlank
    @Column(nullable = false)
    lateinit var name: String

    @Column(unique = true)
    var barcode: String? = null

    @Column(precision = 10, scale = 2)
    var quantity: BigDecimal = BigDecimal.ZERO

    @Column
    var quantityType: String = ""

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val images: MutableList<ProductImage> = mutableListOf()

    fun approvedImages() = images.filter { it.status == ProductImageStatus.APPROVED }

    fun hasBarcode() = barcode != null
}
