package controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import preq.Application
import preq.model.Product
import preq.model.User
import preq.repository.UserRepository
import preq.service.JwtService
import preq.service.ProductService
import preq.web.controller.ProductController
import preq.web.dto.response.ProductResponse
import preq.web.dto.response.ProductSearchWithPriceResponse
import java.math.BigDecimal
import java.security.Principal
import java.util.Optional

@WebMvcTest(ProductController::class)
@ContextConfiguration(classes = [Application::class])
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var productService: ProductService

    @MockitoBean
    lateinit var userRepository: UserRepository

    lateinit var principal: Principal
    lateinit var user: User

    @BeforeEach
    fun setup() {
        user = User().apply { email = "test@mail.com" }
        principal =
            Principal {
                user.email
            }

        whenever(
            userRepository.findByEmail(user.email),
        ).thenReturn(Optional.of(user))
    }

    @Test
    fun `GET search returns matching products`() {
        val response = ProductSearchWithPriceResponse(
            product = ProductResponse(
                id = 1L,
                name = "Pasta de Maní",
                brand = "Maní King",
                quantity = BigDecimal.ONE,
                quantityType = "kg",
                barcode = null,
                images = emptyList(),
            ),
            maxPrice = 1500.0,
            minPrice = 800.0,
        )
        whenever(productService.searchByName("maní")).thenReturn(listOf(response))

        mockMvc
            .perform(get("/api/products/search").param("name", "maní"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].product.name").value("Pasta de Maní"))
    }

    @Test
    fun `GET search returns empty list when no results`() {
        whenever(productService.searchByName("xyznonexistent")).thenReturn(emptyList())

        mockMvc
            .perform(get("/api/products/search").param("name", "xyznonexistent"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `POST confirm image returns updated product`() {
        val product =
            Product().apply {
                name = "Pasta de Maní"
                brand = "Maní King"
            }
        whenever(productService.confirmMatch(eq(1L), any(), eq(0.92), eq(user))).thenReturn(product)

        mockMvc
            .perform(
                multipart("/api/products/1/confirm-image")
                    .file("file", ByteArray(1))
                    .param("similarity", "0.92")
                    .principal(principal),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Pasta de Maní"))
    }
}
