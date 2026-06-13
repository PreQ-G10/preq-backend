package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import preq.enum.ReportScore
import preq.model.Product
import java.math.BigDecimal

@Repository
interface ProductRepository : JpaRepository<Product, Long> {
    fun findByBarcode(barcode: String): Product?

    @Query("""
        SELECT p,
           MAX(lpp.price) AS maxPrice,
           MIN(lpp.price) AS minPrice
        FROM Product p
        LEFT JOIN LocationProductPrice lpp ON lpp.product.id = p.id
        WHERE (
            LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))
            OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :name, '%'))
        ) AND lpp.score >= :validThreshold
        GROUP BY p
    """)
    fun searchByNameWithPrice(
        @Param("name") name: String,
        @Param("validThreshold") validThreshold: Double = ReportScore.VALID_MIN,
    ): List<Array<Any>>

    @Query(
        """
    SELECT * FROM product p
    WHERE p.barcode IS NULL
    AND p.quantity = :quantity
    AND p.quantity_type = :quantityType
    AND (
        similarity(unaccent(lower(p.name)), unaccent(lower(:name))) > 0.4
        OR similarity(unaccent(lower(p.brand)), unaccent(lower(:brand))) > 0.4
        OR similarity(unaccent(lower(p.name)), unaccent(lower(:brand))) > 0.4
        OR similarity(unaccent(lower(p.brand)), unaccent(lower(:name))) > 0.4
        OR similarity(
            unaccent(lower(concat(p.name, ' ', p.brand))), 
            unaccent(lower(concat(:name, ' ', :brand)))
        ) > 0.4
    )
    ORDER BY similarity(
        unaccent(lower(concat(p.name, ' ', p.brand))),
        unaccent(lower(concat(:name, ' ', :brand)))
    ) DESC
    """,
        nativeQuery = true,
    )
    fun findPotentialCollisions(
        name: String,
        brand: String,
        quantity: BigDecimal,
        quantityType: String,
    ): List<Product>
}
