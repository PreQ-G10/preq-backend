package preq.web.controller

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import preq.model.User
import preq.repository.UserRepository
import preq.service.ProductImageDisputeService
import java.security.Principal

@RestController
@RequestMapping("api/images")
class ProductImageController(
    private val disputeService: ProductImageDisputeService,
    private val userRepository: UserRepository,
) {
    private fun user(principal: Principal): User = userRepository.findByEmail(principal.name).orElseThrow()

    @PostMapping("/{imageId}/dispute")
    fun disputeImage(
        @PathVariable imageId: Long,
        principal: Principal,
    ): Map<String, String> {
        val user = user(principal)
        val status = disputeService.disputeImage(imageId, user)
        return mapOf("status" to status.name)
    }
}
