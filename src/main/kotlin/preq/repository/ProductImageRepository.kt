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
            AND pi.created_at >= NOW() - INTERVAL '1 year' -- Added filter for images created within the last year
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
            SELECT pi.* -- Select all columns for ProductImage entity
            FROM product_image pi
            WHERE pi.product_id = :productId
            AND pi.embedding IS NOT NULL
            AND pi.status = 'PENDING_REVIEW' -- Changed from 'PENDING' to 'PENDING_REVIEW' based on ProductImageStatus enum
            AND pi.user_id IS NOT NULL
            AND pi.user_id != :current_user
            AND pi.created_at >= NOW() - INTERVAL '1 year' -- Added filter for images created within the last year
            AND (1 - (pi.embedding <=> CAST(:newEmbedding AS vector))) >= 0.8
            ORDER BY (1 - (pi.embedding <=> CAST(:newEmbedding AS vector))) DESC -- Order by similarity
            LIMIT 3
        """,
        nativeQuery = true,
    )
    fun findToproductConsensusForEmbedding(
        @Param("productId") productId: Long,
        @Param("newEmbedding") newEmbedding: String,
        @Param("current_user") currentUser: Long,
    ): List<ProductImage>
}
