package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import preq.model.ShoppingList
import preq.web.dto.response.ShoppingListSummaryResponse
import preq.web.dto.response.TopProductResponse
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface ShoppingListRepository : JpaRepository<ShoppingList, Long> {

    fun findByIdAndUserId(id: Long, userId: Long): ShoppingList?

    @Query("""
        SELECT
            sl.id, sl.location.id, sl.location.name, sl.location.address,
            sl.completed, sl.createdAt, SIZE(sl.items)
        FROM ShoppingList sl
        WHERE sl.user.id = :userId
        ORDER BY sl.createdAt DESC
    """)
    fun findSummariesByUserId(userId: Long): List<ShoppingListSummaryResponse>

    @Query("""
    SELECT COUNT(DISTINCT sl.user.id)
    FROM ShoppingList sl
    WHERE sl.location.id = :locationId
      AND sl.createdAt >= :since
""")
    fun countDistinctUsersSince(locationId: Long, since: LocalDateTime): Long

    @Query("""
    SELECT sl.totalPrice
    FROM ShoppingList sl
    WHERE sl.location.id = :locationId
    ORDER BY sl.createdAt DESC
    LIMIT 10
""")
    fun findLast10TotalPrices(locationId: Long): List<BigDecimal>

    @Query("""
    SELECT
        sli.product.id,
        sli.product.name,
        SUM(sli.cartQuantity)
    FROM ShoppingListItem sli
    WHERE sli.shoppingList.location.id = :locationId
    GROUP BY sli.product.id, sli.product.name
    ORDER BY SUM(sli.cartQuantity) DESC
    LIMIT 5
""")
    fun findTop5Products(locationId: Long): List<TopProductResponse>
}
