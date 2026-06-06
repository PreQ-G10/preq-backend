package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import preq.enum.FieldType
import preq.model.ProductFieldContest
import java.time.LocalDateTime
import java.time.ZoneOffset

interface ProductFieldContestRepository : JpaRepository<ProductFieldContest, Long> {
    fun countByProductIdAndFieldTypeAndFieldValue(productId: Long, fieldType: FieldType, fieldValue: String): Long

    @Query("""
    SELECT COUNT(pfc) > 0
    FROM ProductFieldContest pfc
    WHERE pfc.product.id = :productId
      AND pfc.user.id = :userId
      AND pfc.fieldType = :fieldType
      AND pfc.createdAt >= :since
""")
    fun existsRecentByProductIdAndUserIdAndFieldType(
        productId: Long,
        userId: Long,
        fieldType: FieldType,
        @Param("since") since: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC).minusDays(7),
    ): Boolean

    @Query(
        """
            SELECT pfc.fieldValue
            FROM ProductFieldContest pfc
            WHERE pfc.product.id = :productId
              AND pfc.fieldType = :fieldType
            GROUP BY pfc.fieldValue
            ORDER BY COUNT(pfc) DESC
            LIMIT 1
        """,
    )
    fun getMostVotedValue(
        productId: Long,
        fieldType: FieldType,
    ): String
}
