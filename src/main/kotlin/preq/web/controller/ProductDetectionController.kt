package preq.web.controller

import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import preq.service.ProductService
import preq.web.dto.ProductDetectionResponse

@RestController
@RequestMapping("/api/detection")
@CrossOrigin(origins = ["*"])
class ProductDetectionController(
    private val productService: ProductService,
) {

    @PostMapping("/image", consumes = ["multipart/form-data"])
    fun detectByImage(
        @RequestPart("file") file: MultipartFile
    ): List<ProductDetectionResponse> {
        return productService.detect(file)
    }

}