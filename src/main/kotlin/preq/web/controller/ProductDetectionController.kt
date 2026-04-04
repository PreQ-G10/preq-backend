package preq.web.controller

import org.springframework.http.ResponseEntity
import preq.service.ProductDetectionService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import preq.web.dto.ProductDetectionResponse

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = ["*"])
class ProductDetectionController(
    private val productDetectionService: ProductDetectionService
) {

    @PostMapping("/detect")
    fun detectProduct(@RequestParam image: MultipartFile): ResponseEntity<FloatArray?> {
        print("Trying to obtain embedding")

        val embedding = productDetectionService.generateEmbedding(image)
        return ResponseEntity.ok(embedding)
    }

    @PostMapping("/compare")
    fun compareProducts(@RequestParam image1: MultipartFile, image2: MultipartFile): ResponseEntity<Float> {
        val embedding1 = productDetectionService.generateEmbedding(image1)
        val embedding2 = productDetectionService.generateEmbedding(image2)

        val similarity = productDetectionService.cosineSimilarity(embedding1, embedding2)
        return ResponseEntity.ok(similarity)
    }

}