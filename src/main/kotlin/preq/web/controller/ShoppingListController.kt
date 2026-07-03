package preq.web.controller

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import preq.model.User
import preq.repository.UserRepository
import preq.service.ShoppingListService
import preq.web.dto.request.SaveShoppingListRequest
import preq.web.dto.request.UpdateShoppingListRequest
import preq.web.dto.response.BusinessMetricsResponse
import preq.web.dto.response.DeleteShoppingListResponse
import preq.web.dto.response.ShoppingListResponse
import preq.web.dto.response.ShoppingListSummaryResponse
import java.security.Principal

@RestController
@RequestMapping("/shopping-lists")
class ShoppingListController(
    private val service: ShoppingListService,
    private val userRepository: UserRepository,
) {
    private fun user(principal: Principal): User = userRepository.findByEmail(principal.name).orElseThrow()

    @PostMapping
    fun save(
        @RequestBody request: SaveShoppingListRequest,
        principal: Principal,
    ): ShoppingListResponse {
        val user = user(principal)
        return service.save(request, user)
    }

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: UpdateShoppingListRequest,
        principal: Principal,
    ): ShoppingListResponse {
        val user = user(principal)
        return service.update(id, request, user)
    }

    @GetMapping
    fun getAll(principal: Principal): List<ShoppingListSummaryResponse> {
        val user = user(principal)
        return service.getAll(user)
    }

    @GetMapping("/business-metrics")
    fun getBusinessMetrics(principal: Principal): BusinessMetricsResponse {
        val user = user(principal)
        return service.getBusinessMetrics(user)
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long,
        principal: Principal,
    ): ShoppingListResponse {
        val user = user(principal)
        return service.getById(id, user)
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
        principal: Principal,
    ): DeleteShoppingListResponse {
        val user = user(principal)
        return service.delete(id, user)
    }
}
