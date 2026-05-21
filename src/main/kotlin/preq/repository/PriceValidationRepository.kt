package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import preq.model.PriceValidation

@Repository
interface PriceValidationRepository : JpaRepository<PriceValidation, Long> {
    fun existsByReportIdAndUserId(
        reportId: Long,
        userId: Long,
    ): Boolean
}
