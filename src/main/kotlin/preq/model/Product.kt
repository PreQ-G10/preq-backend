package preq.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
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

    @Pattern(regexp = "^[0-9]{13}$", message = "Barcode must be a valid EAN-13")
    @Column(unique = true)
    var barcode: String? = null

    @DecimalMin(value = "0.01", message = "Quantity must be greater than zero")
    @Column(precision = 10, scale = 2)
    var quantity: BigDecimal = BigDecimal.ZERO

    @NotBlank
    @Column
    var quantityType: String = ""

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val images: MutableList<ProductImage> = mutableListOf()

    fun approvedImages() = images.filter { it.status == ProductImageStatus.APPROVED }

    fun hasBarcode() = barcode != null
}
