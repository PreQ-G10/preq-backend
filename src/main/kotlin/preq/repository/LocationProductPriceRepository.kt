package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import preq.model.LocationProductPrice
import preq.web.dto.projection.LocationPriceResult
import preq.web.dto.projection.PriceStats
import preq.web.dto.projection.TopLocationResult

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
        nativeQuery = true,
    )
    fun getPriceStats(
        @Param("productId") productId: Long,
    ): PriceStats

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
        nativeQuery = true,
    )
    fun getTopLocations(
        @Param("productId") productId: Long,
    ): List<TopLocationResult>

    @Query(
        value = """
        SELECT 
            l.id as locationId,
            l.name as name,
            l.address as address,
            l.latitude as latitude,
            l.longitude as longitude,
            AVG(lpp.price) as avgPrice
        FROM location_product_price lpp
        JOIN location l ON l.id = lpp.location_id
        WHERE lpp.product_id = :productId
        GROUP BY l.id, l.name, l.address, l.latitude, l.longitude
    """,
        nativeQuery = true,
    )
    fun getLocationPricesForProduct(
        @Param("productId") productId: Long,
    ): List<LocationPriceResult>

    @Query(
        value = """
        SELECT 
            l.id as locationId,
            l.name as name,
            l.address as address,
            l.latitude as latitude,
            l.longitude as longitude,
            AVG(lpp.price) as avgPrice
        FROM location_product_price lpp
        JOIN location l ON l.id = lpp.location_id
        WHERE lpp.product_id = :productId
        AND ST_DistanceSphere(
            ST_SetSRID(ST_MakePoint(l.longitude, l.latitude), 4326),
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)
        ) <= :radiusMeters
        GROUP BY
            l.id,
            l.name,
            l.address,
            l.latitude,
            l.longitude
       """,
        nativeQuery = true,
    )
    fun getLocationPricesForProductInArea(
        @Param("productId") productId: Long,
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
        @Param("radiusMeters") radiusMeters: Double,
    ): List<LocationPriceResult>

    @Query(
        value = """
        SELECT AVG(price) FROM location_product_price
        WHERE product_id = :productId
    """,
        nativeQuery = true,
    )
    fun getGlobalAvgPrice(
        @Param("productId") productId: Long,
    ): Double?

    fun findByProductIdOrderByReportedAtDesc(productId: Long): List<LocationProductPrice>
}
