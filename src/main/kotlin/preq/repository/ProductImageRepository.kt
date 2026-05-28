package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import preq.model.ProductImage
import preq.web.dto.projection.SimilarProductResult

@Repository
interface ProductImageRepository : JpaRepository<ProductImage, Long> {
    @Query(
        value = """
            SELECT pi.product_id, 1 - (pi.embedding <=> CAST(:embedding AS vector)) AS similarity
            FROM product_image pi
            WHERE pi.embedding IS NOT NULL
            AND pi.status = 'APPROVED'
            ORDER BY pi.embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findSimilarProducts(
        @Param("embedding") embedding: String,
        @Param("limit") limit: Int = 10,
    ): List<SimilarProductResult>

    @Query(
        value = """
            SELECT pi.product_id, pi.user_id
            FROM product_image pi
            WHERE pi.product_id = :productId
            AND pi.embedding IS NOT NULL
            AND pi.consensus = true
            AND pi.status = 'PENDING'
            AND user IS NOT NULL
            AND pi.user_id != :current_user
            AND (1 - (e.embedding <=> :newEmbedding)) >= 0.8;
            ORDER BY similarity DESC;
            LIMIT 3
        """,
    )
    fun findToproductConsensusForEmbedding(
        productId: Long,
        newEmbedding: String,
        currentUser: Long,
    ): List<ProductImage>
}
