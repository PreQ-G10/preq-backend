package preq.web.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import preq.repository.UserRepository
import preq.service.CartService
import preq.web.dto.request.CartCompareRequest
import preq.web.dto.response.CartCompareResponse
import java.security.Principal

@RestController
@RequestMapping("/api/cart")
class CartController(
    private val cartService: CartService,
    private val userRepository: UserRepository,
) {
    @PostMapping("/compare")
    fun compare(
        @RequestBody request: CartCompareRequest,
        principal: Principal,
    ): CartCompareResponse {
        val user = userRepository.findByEmail(principal.name).orElseThrow()
        val enrichedRequest = if (request.userLatitude == null && user.addressLocation != null) {
            request.copy(
                userLatitude = user.addressLocation!!.y,
                userLongitude = user.addressLocation!!.x,
            )
        } else request
        return cartService.compare(enrichedRequest)
    }
}