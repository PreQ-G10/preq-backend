package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import preq.model.Product

@Repository
interface ProductRepository : JpaRepository<Product, Long> {
    fun findByBarcode(barcode: String): Product?

    @Query(
        """
        SELECT p 
        FROM Product p 
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) 
            OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :name, '%'))
        """,
    )
    fun searchByName(
        @Param("name") name: String,
    ): List<Product>

    @Query(
        """
        SELECT p 
        FROM Product p 
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :text, '%')) 
            OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :text, '%'))
        """,
    )
    fun findByOcrText(
        @Param("text") text: String,
    ): List<Product>
}
