package preq.util.mapper

import preq.model.Product
import preq.model.ProductImage
import preq.web.dto.response.OpenFoodFactsProductResponse

object ProductMapper {
    fun fromOpenFoodFactsApiResponse(
        barcode: String,
        api: OpenFoodFactsProductResponse,
    ): Product {
        val product = Product()

        api.image_front_url?.takeIf { it.isNotBlank() }?.let { url ->
            val productImg = ProductImage().apply {
                this.product = product
                this.imageUrl = url
            }
            product.images.add(productImg)
        }

        product.apply {
            this.barcode = barcode
            this.name = api.product_name!!
            this.brand = api.brands!!
            this.quantity = api.product_quantity!!
            this.quantityType = api.product_quantity_unit!!
        }

        return product
    }
}
