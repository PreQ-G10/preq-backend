package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import preq.model.LocationProductPrice

@Repository
interface LocationProductPriceRepository : JpaRepository<LocationProductPrice, Long> {

    @Query(
        value = """
            SELECT 
                AVG(price) as avgPrice,
                MAX(price) as maxPrice,
                MIN(price) as minPrice
            FROM location_product_price
            WHERE product_id = :productId
        """,
        nativeQuery = true
    )
    fun getPriceStats(@Param("productId") productId: Long): Array<Any>

    @Query(
        value = """
            SELECT l.name, l.address, AVG(lpp.price) as avgPrice, COUNT(*) as reportCount
            FROM location_product_price lpp
            JOIN location l ON l.id = lpp.location_id
            WHERE lpp.product_id = :productId
            GROUP BY l.id, l.name, l.address
            ORDER BY reportCount DESC
            LIMIT 5
        """,
        nativeQuery = true
    )
    fun getTopLocations(@Param("productId") productId: Long): List<Array<Any>>

    fun findByProductIdOrderByReportedAtDesc(productId: Long): List<LocationProductPrice>
}