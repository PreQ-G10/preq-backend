package controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import preq.Application
import preq.service.ProductService
import preq.web.controller.ProductDetectionController
import preq.web.dto.ProductDetectionResponse

@WebMvcTest(ProductDetectionController::class)
@ContextConfiguration(classes = [Application::class])
class ProductDetectionControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc
    @MockBean
    lateinit var productService: ProductService

    @Test
    fun `POST detect image returns 200 with results`() {
        val response = listOf(
            ProductDetectionResponse(
                productId = 1L,
                name = "Pasta de Maní",
                brand = "Maní King",
                quantity = 485,
                quantityType = "g",
                imageUrl = "https://cloudinary.com/image.jpg",
                similarity = 0.92,
                confident = true
            )
        )
        whenever(productService.detect(any())).thenReturn(response)

        mockMvc.perform(
            multipart("/api/detection/image").file("file", ByteArray(1))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Pasta de Maní"))
            .andExpect(jsonPath("$[0].similarity").value(0.92))
            .andExpect(jsonPath("$[0].confident").value(true))
    }
}