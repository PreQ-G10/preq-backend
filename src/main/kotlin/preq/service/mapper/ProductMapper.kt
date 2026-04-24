package preq.service.mapper

import org.springframework.stereotype.Component
import preq.model.Product
import preq.model.ProductImage
import preq.web.dto.response.OpenFoodFactsProductResponse

@Component
class ProductMapper {
    fun fromApi(
        barcode: String,
        api: OpenFoodFactsProductResponse,
    ): Product {
        val product = Product()
        val productImg = ProductImage()

        productImg.apply {
            this.product = product
            this.imageUrl = api.image_front_url ?: ""
        }

        product.apply {
            this.barcode = barcode
            this.name = api.product_name ?: "Unknown"
            this.brand = api.brands ?: "Unknown"
            this.quantity = api.product_quantity ?: 0.toBigDecimal()
            this.quantityType = api.product_quantity_unit ?: "Unknown"
            this.images.add(productImg)
        }

        return product
    }
}
