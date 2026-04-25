package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import preq.model.Location
import preq.web.dto.projection.LocationWithDistance

@Repository
interface LocationRepository : JpaRepository<Location, Long> {
    @Query(
        """
        SELECT l 
        FROM Location l 
        WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :name, '%')) 
            OR LOWER(l.address) LIKE LOWER(CONCAT('%', :name, '%'))
        """,
    )
    fun searchByName(
        @Param("name") name: String,
    ): List<Location>

    @Query(
        """
        SELECT l.*, ST_Distance(
            l.coordinates::geography,
            ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
        ) AS distance_meters
        FROM location l
        WHERE l.coordinates IS NOT NULL
        AND ST_DWithin(
            l.coordinates::geography,
            ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
            :maxDistanceMeters
        )
        ORDER BY distance_meters ASC
        LIMIT 1
        """,
        nativeQuery = true,
    )
    fun findWithinRange(
        lat: Double,
        lon: Double,
        maxDistanceMeters: Double,
    ): LocationWithDistance?
}
