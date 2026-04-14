package preq.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import preq.enum.ProductImageStatus

@Entity
@Table(name = "products")
class Product : BaseEntity() {
    @NotBlank
    @Column(nullable = false)
    lateinit var brand: String

    @NotBlank
    @Column(nullable = false)
    lateinit var name: String

    @NotBlank
    @Column(unique = true)
    var barcode: String? = null

    @Column
    var quantity: Int = 0

    @Column
    var quantityType: String = ""

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val images: MutableList<ProductImage> = mutableListOf()

    fun approvedImages() = images.filter { it.status == ProductImageStatus.APPROVED }

    fun hasBarcode() = barcode != null
}
