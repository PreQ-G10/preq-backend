package preq.repository

import org.springframework.stereotype.Repository
import org.springframework.data.jpa.repository.JpaRepository
import preq.model.Product

@Repository
interface ProductRepository : JpaRepository<Product, Long> {
}