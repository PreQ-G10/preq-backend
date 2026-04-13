package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import preq.model.Location

@Repository
interface LocationRepository : JpaRepository<Location, Long> {

    @Query("""
        SELECT l 
        FROM Location l 
        WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :name, '%')) 
            OR LOWER(l.address) LIKE LOWER(CONCAT('%', :name, '%'))
        """
    )
    fun searchByName(@Param("name") name: String): List<Location>
}