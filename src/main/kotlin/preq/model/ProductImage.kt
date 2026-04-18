package preq.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import preq.config.FloatArrayVectorType
import preq.enum.ProductImageStatus

@Entity
@Table(name = "product_image")
class ProductImage : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    var product: Product? = null

    var imageUrl: String = ""

    @Column(columnDefinition = "vector(1000)")
    @Type(FloatArrayVectorType::class)
    var embedding: FloatArray? = null

    @Enumerated(EnumType.STRING)
    var status: ProductImageStatus = ProductImageStatus.PENDING_REVIEW

    var confidenceScore: Double = 0.0

    fun isApproved() = status == ProductImageStatus.APPROVED

    fun hasEmbedding() = embedding != null

    fun embeddingAsString() =
        embedding?.joinToString(",", "[", "]")
            ?: throw IllegalStateException("No embedding available")
}
