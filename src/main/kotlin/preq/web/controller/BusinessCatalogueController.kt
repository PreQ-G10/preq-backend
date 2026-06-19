package preq.web.controller

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import preq.model.User
import preq.repository.UserRepository
import preq.service.BusinessCatalogueService
import preq.web.dto.request.CatalogueRequest
import preq.web.dto.request.DeleteFromCatalogueRequest
import preq.web.dto.request.UpdateCataloguePriceRequest
import preq.web.dto.response.CatalogueItemResponse
import preq.web.dto.response.DeleteFromCatalogueResponse
import java.security.Principal

@RestController
@RequestMapping("/api/business/catalogue")
class BusinessCatalogueController(
    private val catalogueService: BusinessCatalogueService,
    private val userRepository: UserRepository,
) {
    private fun getUser(principal: Principal): User =
        userRepository.findByEmail(principal.name).orElseThrow()

    @GetMapping
    fun getCatalogue(principal: Principal): List<CatalogueItemResponse> =
        catalogueService.getCatalogue(getUser(principal))

    @PostMapping
    fun addToCatalogue(
        @RequestBody request: CatalogueRequest,
        principal: Principal,
    ): CatalogueItemResponse =
        catalogueService.addToCatalogue(getUser(principal), request)

    @PutMapping
    fun updatePrices(
        @RequestBody request: UpdateCataloguePriceRequest,
        principal: Principal,
    ): List<CatalogueItemResponse> =
        catalogueService.updatePrices(getUser(principal), request)

    @DeleteMapping
    fun deleteFromCatalogue(
        @RequestBody request: DeleteFromCatalogueRequest,
        principal: Principal,
    ): DeleteFromCatalogueResponse =
        catalogueService.deleteFromCatalogue(getUser(principal), request)
}