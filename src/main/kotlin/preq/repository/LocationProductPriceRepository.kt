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
                AVG(lpp.price) as avgPrice,
                MAX(lpp.price) as maxPrice,
                MIN(lpp.price) as minPrice
            FROM location_product_price lpp
            JOIN users u ON u.id = lpp.user_id
            WHERE lpp.product_id = :productId
            AND lpp.price_validity = 'VALID'
            AND u.trust_score >= :minimumTrustScore
        """,
        nativeQuery = true,
    )
    fun getPriceStats(
        @Param("productId") productId: Long,
        @Param("minimumTrustScore") minimumTrustScore: Double,
    ): PriceStats

    @Query(
        value = """
            SELECT l.name, l.address, AVG(lpp.price) as avgPrice, COUNT(*) as reportCount
            FROM location_product_price lpp
            JOIN location l ON l.id = lpp.location_id
            JOIN users u ON u.id = lpp.user_id
            WHERE lpp.product_id = :productId
            AND lpp.price_validity = 'VALID'
            AND u.trust_score >= :minimumTrustScore
            GROUP BY l.id, l.name, l.address
            ORDER BY reportCount DESC
            LIMIT 5
        """,
        nativeQuery = true,
    )
    fun getTopLocations(
        @Param("productId") productId: Long,
        @Param("minimumTrustScore") minimumTrustScore: Double,
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
            JOIN users u ON u.id = lpp.user_id
            WHERE lpp.product_id = :productId
            AND lpp.price_validity = 'VALID'
            AND u.trust_score >= :minimumTrustScore
            GROUP BY l.id, l.name, l.address, l.latitude, l.longitude
        """,
        nativeQuery = true,
    )
    fun getLocationPricesForProduct(
        @Param("productId") productId: Long,
        @Param("minimumTrustScore") minimumTrustScore: Double,
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
            JOIN users u ON u.id = lpp.user_id
            WHERE lpp.product_id = :productId
            AND lpp.price_validity = 'VALID'
            AND u.trust_score >= :minimumTrustScore
            AND ST_DistanceSphere(
                ST_SetSRID(ST_MakePoint(l.longitude, l.latitude), 4326),
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)
            ) <= :radiusMeters
            GROUP BY l.id, l.name, l.address, l.latitude, l.longitude
        """,
        nativeQuery = true,
    )
    fun getLocationPricesForProductInArea(
        @Param("productId") productId: Long,
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
        @Param("radiusMeters") radiusMeters: Double,
        @Param("minimumTrustScore") minimumTrustScore: Double,
    ): List<LocationPriceResult>

    @Query(
        value = """
            SELECT AVG(lpp.price)
            FROM location_product_price lpp
            JOIN users u ON u.id = lpp.user_id
            WHERE lpp.product_id = :productId
            AND lpp.price_validity = 'VALID'
            AND u.trust_score >= :minimumTrustScore
        """,
        nativeQuery = true,
    )
    fun getGlobalAvgPrice(
        @Param("productId") productId: Long,
        @Param("minimumTrustScore") minimumTrustScore: Double,
    ): Double?

    @Query(
        value = """
            SELECT lpp.* FROM location_product_price lpp
            JOIN users u ON u.id = lpp.user_id
            WHERE lpp.product_id = :productId
            AND lpp.price_validity = 'VALID'
            AND u.trust_score >= :minimumTrustScore
            ORDER BY lpp.reported_at DESC
        """,
        nativeQuery = true,
    )
    fun findValidByProductIdOrderByReportedAtDesc(
        @Param("productId") productId: Long,
        @Param("minimumTrustScore") minimumTrustScore: Double,
    ): List<LocationProductPrice>

    @Query(
        value = """
            SELECT COUNT(*) FROM location_product_price lpp
            JOIN users u ON u.id = lpp.user_id
            WHERE lpp.product_id = :productId
            AND lpp.price_validity = 'VALID'
            AND u.trust_score >= :minimumTrustScore
        """,
        nativeQuery = true,
    )
    fun countValidByProductId(
        @Param("productId") productId: Long,
        @Param("minimumTrustScore") minimumTrustScore: Double,
    ): Long

    @Query(
        value = """
            SELECT ST_DistanceSphere(
                ST_SetSRID(ST_MakePoint(:userLng, :userLat), 4326),
                coordinates
            )
            FROM location
            WHERE id = :locationId
        """,
        nativeQuery = true,
    )
    fun getDistanceMeters(
        @Param("userLat") userLat: Double,
        @Param("userLng") userLng: Double,
        @Param("locationId") locationId: Long,
    ): Double
}
