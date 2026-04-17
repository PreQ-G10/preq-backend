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
import preq.service.ProductService
import preq.web.dto.request.CreateProductRequest
import preq.web.dto.response.ProductResponse

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService,
) {
    @GetMapping("/search")
    fun searchByName(
        @RequestParam name: String,
    ): List<ProductResponse> = productService.searchByName(name).map { ProductResponse.from(it) }

    @PostMapping
    fun create(
        @RequestBody request: CreateProductRequest,
    ): ProductResponse = ProductResponse.from(productService.create(request))

    @PostMapping("/{productId}/confirm-image", consumes = ["multipart/form-data"])
    fun confirmImage(
        @PathVariable productId: Long,
        @RequestPart("file") file: MultipartFile,
        @RequestParam similarity: Double,
    ): ProductResponse {
        val product = productService.confirmMatch(productId, file, similarity)
        return ProductResponse.from(product)
    }
}
