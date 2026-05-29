package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import preq.enum.FieldType
import preq.model.ProductFieldContest

interface ProductFieldContestRepository : JpaRepository<ProductFieldContest, Long> {
    fun existsByProductIdAndUserIdAndFieldType(
        productId: Long,
        userId: Long,
        fieldType: preq.enum.FieldType,
    ): Boolean

    @Query(
        """
            SELECT pfc.fieldValue
            FROM ProductFieldContest pfc
            WHERE pfc.product.id = :productId
              AND pfc.fieldType = :fieldType
            GROUP BY pfc.fieldValue
            ORDER BY COUNT(pfc) DESC
        """,
    )
    fun getMostVotedValue(
        productId: Long,
        fieldType: FieldType,
    ): String
}
