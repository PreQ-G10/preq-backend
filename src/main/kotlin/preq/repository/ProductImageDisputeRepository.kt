package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import preq.model.ProductImageDispute
import preq.web.dto.projection.ImageDisputeCountResult

interface ProductImageDisputeRepository : JpaRepository<ProductImageDispute, Long> {
    fun existsByImageIdAndUserId(
        imageId: Long,
        userId: Long,
    ): Boolean

    fun countByImageId(imageId: Long): Long

    @Query(
        """
        SELECT pid.image.id AS imageId, COUNT(pid) AS count
        FROM ProductImageDispute pid
        WHERE pid.image.id IN :imageIds
        GROUP BY pid.image.id
    """,
    )
    fun countByImageIdIn(imageIds: List<Long>): List<ImageDisputeCountResult>
}
