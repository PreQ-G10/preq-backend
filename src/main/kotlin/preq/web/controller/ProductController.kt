package preq.web.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import preq.enum.FieldContestStatus
import preq.model.User
import preq.repository.UserRepository
import preq.service.ProductService
import preq.web.dto.request.ContestProductFieldRequest
import preq.web.dto.request.CreateProductRequest
import preq.web.dto.response.BarcodeDetectionResponse
import preq.web.dto.response.NearbyOffersResponse
import preq.web.dto.response.ProductResponse
import preq.web.dto.response.ProductSearchWithPriceResponse
import java.security.Principal

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService,
    private val userRepository: UserRepository,
) {
    private fun user(principal: Principal): User = userRepository.findByEmail(principal.name).orElseThrow()

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long,
    ): ProductResponse = ProductResponse.from(productService.getById(id))

    @GetMapping("/search")
    fun searchByName(
        @RequestParam name: String,
    ): List<ProductSearchWithPriceResponse> = productService.searchByName(name)

    @PostMapping
    fun create(
        @RequestBody request: CreateProductRequest,
    ): ProductResponse = ProductResponse.from(productService.create(request))

    @PostMapping("/{productId}/image", consumes = ["multipart/form-data"])
    fun uploadImage(
        @PathVariable productId: Long,
        @RequestPart("file") file: MultipartFile,
        principal: Principal,
    ): ProductResponse = ProductResponse.from(productService.addImage(productId, file, user(principal)))

    @PostMapping("/{productId}/confirm-image", consumes = ["multipart/form-data"])
    fun confirmImage(
        @PathVariable productId: Long,
        @RequestPart("file") file: MultipartFile,
        @RequestParam similarity: Double,
        principal: Principal,
    ): ProductResponse {
        val product = productService.confirmMatch(productId, file, similarity, user(principal))
        return ProductResponse.from(product)
    }

    @GetMapping("/barcode/{barcode}")
    fun getByBarcode(
        @PathVariable barcode: String,
        principal: Principal,
    ): BarcodeDetectionResponse = productService.getOrCreateByBarcode(barcode, user(principal))

    @PostMapping("/{id}/resolve-barcode-collision")
    fun resolveBarcodeCollision(
        @PathVariable id: Long,
        @RequestParam barcode: String,
        @RequestParam confirm: Boolean,
        principal: Principal,
    ): ProductResponse = ProductResponse.from(productService.resolveBarcodeCollision(id, barcode, confirm, user(principal)))

    @PostMapping("/{id}/contestField")
    fun contestField(
        @PathVariable id: Long,
        @RequestBody field: ContestProductFieldRequest,
        principal: Principal,
    ): FieldContestStatus = productService.contestField(id, field, user(principal))

    @GetMapping("/offers-nearby")
    fun getNearbyOffers(principal: Principal): NearbyOffersResponse = productService.getNearbyOffers(user(principal))
}
