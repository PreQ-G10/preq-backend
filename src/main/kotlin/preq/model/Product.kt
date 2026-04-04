package preq.model

import com.pgvector.PGvector
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank

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

    @NotBlank
    @Column
    var description: String? = null

    @Column
    var quantity: Int? = null

    @Column(columnDefinition = "vector(512)")
    var embedding: PGvector? = null

    @ElementCollection
    @CollectionTable(name = "product_tags", joinColumns = [JoinColumn(name = "product_id")])
    @Column(name = "tag")
    var tags: MutableList<String> = mutableListOf()
}