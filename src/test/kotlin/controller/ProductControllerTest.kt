package controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import preq.Application
import preq.model.Product
import preq.service.ProductService
import preq.web.controller.ProductController

@WebMvcTest(ProductController::class)
@ContextConfiguration(classes = [Application::class])
class ProductControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc
    @MockBean
    lateinit var productService: ProductService

    @Test
    fun `GET search returns matching products`() {
        val products = listOf(
            Product().apply { name = "Pasta de Maní"; brand = "Maní King" }
        )
        whenever(productService.searchByName("maní")).thenReturn(products)

        mockMvc.perform(get("/api/products/search").param("name", "maní"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Pasta de Maní"))
    }

    @Test
    fun `GET search returns empty list when no results`() {
        whenever(productService.searchByName("xyznonexistent")).thenReturn(emptyList())

        mockMvc.perform(get("/api/products/search").param("name", "xyznonexistent"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `POST confirm image returns updated product`() {
        val product = Product().apply {
            name = "Pasta de Maní"
            brand = "Maní King"
        }
        whenever(productService.confirmMatch(eq(1L), any(), eq(0.92))).thenReturn(product)

        mockMvc.perform(
            multipart("/api/products/1/confirm-image")
                .file("file", ByteArray(1))
                .param("similarity", "0.92")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Pasta de Maní"))
    }
}