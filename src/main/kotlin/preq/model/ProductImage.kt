package preq.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import org.hibernate.annotations.Type
import preq.config.FloatArrayVectorType
import preq.enum.ProductImageStatus

@Entity
@Table(name = "product_image")
class ProductImage : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @NotNull
    var product: Product? = null

    @NotBlank
    @Pattern(regexp = "^https://res\\.cloudinary\\.com/.*", message = "Image URL must be a valid Cloudinary URL")
    var imageUrl: String = ""

    @Column(columnDefinition = "vector(1000)")
    @Type(FloatArrayVectorType::class)
    var embedding: FloatArray? = null

    @Enumerated(EnumType.STRING)
    var status: ProductImageStatus = ProductImageStatus.PENDING_REVIEW

    @DecimalMin(value = "0.0", message = "Confidence score must be >= 0.0")
    @DecimalMax(value = "1.0", message = "Confidence score must be <= 1.0")
    var confidenceScore: Double = 0.0

    fun isApproved() = status == ProductImageStatus.APPROVED

    fun hasEmbedding() = embedding != null

    fun embeddingAsString() =
        embedding?.joinToString(",", "[", "]")
            ?: throw IllegalStateException("No embedding available")
}
