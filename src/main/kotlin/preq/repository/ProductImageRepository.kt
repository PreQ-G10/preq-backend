package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import preq.model.ProductImage

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
        nativeQuery = true
    )
    fun findSimilarProducts(
        @Param("embedding") embedding: String,
        @Param("limit") limit: Int = 10
    ): List<Array<Any>>
}