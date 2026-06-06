package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import preq.enum.ReportScore
import preq.model.LocationProductPrice
import preq.web.dto.projection.LocationPriceResult
import preq.web.dto.projection.NearbyOfferResult
import preq.web.dto.projection.PriceStats
import preq.web.dto.projection.TopLocationResult
import preq.web.dto.response.LocationProductPriceResponse
import java.math.BigDecimal

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
            AND lpp.score >= :validThreshold
        """,
        nativeQuery = true,
    )
    fun getPriceStats(
        @Param("productId") productId: Long,
        @Param("validThreshold") validThreshold: Double = ReportScore.VALID_MIN,
    ): PriceStats

    @Query(
        value = """
            SELECT l.id as id, l.name, l.address, AVG(lpp.price) as avgPrice, COUNT(*) as reportCount
            FROM location_product_price lpp
            JOIN location l ON l.id = lpp.location_id
            JOIN users u ON u.id = lpp.user_id
            WHERE lpp.product_id = :productId
            AND lpp.score >= :validThreshold
            GROUP BY l.id, l.name, l.address
            ORDER BY reportCount DESC
            LIMIT 5
        """,
        nativeQuery = true,
    )
    fun getTopLocations(
        @Param("productId") productId: Long,
        @Param("validThreshold") validThreshold: Double = ReportScore.VALID_MIN,
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
            AND lpp.score >= :validThreshold
            GROUP BY l.id, l.name, l.address, l.latitude, l.longitude
        """,
        nativeQuery = true,
    )
    fun getLocationPricesForProduct(
        @Param("productId") productId: Long,
        @Param("validThreshold") validThreshold: Double = ReportScore.VALID_MIN,
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
            AND lpp.score >= :validThreshold
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
        @Param("validThreshold") validThreshold: Double = ReportScore.VALID_MIN,
    ): List<LocationPriceResult>

    @Query(
        value = """
            SELECT AVG(lpp.price)
            FROM location_product_price lpp
            JOIN users u ON u.id = lpp.user_id
            WHERE lpp.product_id = :productId
            AND lpp.score >= :validThreshold
        """,
        nativeQuery = true,
    )
    fun getGlobalAvgPrice(
        @Param("productId") productId: Long,
        @Param("validThreshold") validThreshold: Double = ReportScore.VALID_MIN,
    ): Double?

    @Query(
        value = """
            SELECT lpp.* FROM location_product_price lpp
            JOIN users u ON u.id = lpp.user_id
            WHERE lpp.product_id = :productId
            AND lpp.score >= :validThreshold
            ORDER BY lpp.reported_at DESC
        """,
        nativeQuery = true,
    )
    fun findValidByProductIdOrderByReportedAtDesc(
        @Param("productId") productId: Long,
        @Param("validThreshold") validThreshold: Double = ReportScore.VALID_MIN,
    ): List<LocationProductPrice>

    @Query(
        value = """
            SELECT COUNT(*) FROM location_product_price lpp
            JOIN users u ON u.id = lpp.user_id
            WHERE lpp.product_id = :productId
            AND lpp.score >= :validThreshold
        """,
        nativeQuery = true,
    )
    fun countValidByProductId(
        @Param("productId") productId: Long,
        @Param("validThreshold") validThreshold: Double = ReportScore.VALID_MIN,
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

    @Query(
        value = """
            SELECT lpp.* FROM location_product_price lpp
            JOIN location l ON l.id = lpp.location_id
            WHERE lpp.score >= :pendingMin
            AND lpp.score < :validMin
            AND ST_DistanceSphere(
                ST_SetSRID(ST_MakePoint(l.longitude, l.latitude), 4326),
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)
            ) <= :radiusMeters
            AND lpp.id NOT IN (
                SELECT pv.report_id FROM price_validations pv WHERE pv.user_id = :userId
            )
            AND lpp.reported_at >= NOW() - INTERVAL '15 days'
            AND lpp.user_id <> :userId
            ORDER BY lpp.reported_at DESC
        """,
        nativeQuery = true,
    )
    fun findPendingValidationNearby(
        @Param("userId") userId: Long,
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
        @Param("radiusMeters") radiusMeters: Double,
        @Param("pendingMin") pendingMin: Double = ReportScore.PENDING_MIN,
        @Param("validMin") validMin: Double = ReportScore.VALID_MIN,
    ): List<LocationProductPrice>

    @Query(
        value = """
            SELECT 
                p.id              AS productId,
                p.name            AS productName,
                p.brand           AS productBrand,
                p.quantity        AS productQuantity,
                p.quantity_type   AS productQuantityType,
                p.min_price       AS productMinPrice,
                p.max_price       AS productMaxPrice,
                p.barcode         AS productBarcode,
                l.id              AS locationId,
                l.name            AS locationName,
                l.address         AS locationAddress,
                l.type            AS locationType,
                MIN(lpp.price)    AS minPrice,
                AVG(lpp.price)    AS avgPrice,
                ST_DistanceSphere(
                    ST_SetSRID(ST_MakePoint(l.longitude, l.latitude), 4326),
                    ST_SetSRID(ST_MakePoint(:userLng, :userLat), 4326)
                )                 AS distanceMeters
            FROM location_product_price lpp
            JOIN product p  ON p.id  = lpp.product_id
            JOIN location l ON l.id  = lpp.location_id
            WHERE lpp.reported_at >= NOW() - INTERVAL '30 days'
              AND lpp.score >= :validThreshold
              AND ST_DistanceSphere(
                    ST_SetSRID(ST_MakePoint(l.longitude, l.latitude), 4326),
                    ST_SetSRID(ST_MakePoint(:userLng, :userLat), 4326)
                  ) <= :radiusMeters
            GROUP BY
                p.id, p.name, p.brand, p.quantity, p.quantity_type,
                p.min_price, p.max_price, p.barcode,
                l.id, l.name, l.address, l.type
            HAVING MIN(lpp.price) <= AVG(lpp.price) * (1.0 - :thresholdFraction)
            ORDER BY (AVG(lpp.price) - MIN(lpp.price)) DESC
        """,
        nativeQuery = true,
    )
    fun findNearbyOffers(
        @Param("userLat") userLat: Double,
        @Param("userLng") userLng: Double,
        @Param("radiusMeters") radiusMeters: Double,
        @Param("thresholdFraction") thresholdFraction: BigDecimal,
        @Param("validThreshold") validThreshold: Double = ReportScore.VALID_MIN,
    ): List<NearbyOfferResult>

    @Query(
        """
            SELECT lpp.* FROM location_product_price lpp
            JOIN location l ON l.id = lpp.location_id
            WHERE lpp.product_id = :productId
                AND l.id = :locationId
                AND lpp.score >= :validThreshold
            ORDER BY lpp.reported_at DESC
        """,
        nativeQuery = true,
    )
    fun getLocationPricesForProductInLocation(
        @Param("productId") productId: Long,
        @Param("locationId") locationId: Long,
        @Param("validThreshold") validThreshold: Double = ReportScore.VALID_MIN,
    ): List<LocationProductPrice>
}
