package preq.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import preq.enum.ImageDisputeStatus
import preq.model.User
import preq.repository.ProductImageDisputeRepository
import preq.repository.ProductImageRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.DataIntegrityViolationException
import preq.model.ProductImageDispute

@Service
class ProductImageDisputeService(
    private val disputeRepository: ProductImageDisputeRepository,
    private val imageRepository: ProductImageRepository,
) {
    @Transactional
    fun disputeImage(imageId: Long, user: User): ImageDisputeStatus {
        if (disputeRepository.existsByImageIdAndUserId(imageId, user.id)) {
            return ImageDisputeStatus.ALREADY_DISPUTED
        }

        val image = imageRepository.findById(imageId)
            .orElseThrow { EntityNotFoundException("Image $imageId not found") }

        try {
            disputeRepository.save(
                ProductImageDispute().apply {
                    this.image = image
                    this.user = user
                }
            )
        } catch (ex: DataIntegrityViolationException) {
            return ImageDisputeStatus.ALREADY_DISPUTED
        }

        return ImageDisputeStatus.FIRST_DISPUTE
    }
}