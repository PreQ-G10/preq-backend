package preq.web.dto.response

import preq.enum.LocationType
import preq.web.dto.projection.NearbyOfferResult
import java.math.BigDecimal

data class NearbyOfferResponse(
    val product: ProductResponse,
    val location: LocationResponse,
    val distanceMeters: Double,
    val price: BigDecimal,
    val averagePrice: BigDecimal,
) {
    companion object {
        fun from(r: NearbyOfferResult) =
            NearbyOfferResponse(
                product =
                    ProductResponse(
                        id = r.getProductId(),
                        name = r.getProductName(),
                        brand = r.getProductBrand(),
                        quantity = r.getProductQuantity(),
                        quantityType = r.getProductQuantityType(),
                        barcode = r.getProductBarcode(),
                        images = emptyList(),
                    ),
                location =
                    LocationResponse(
                        id = r.getLocationId(),
                        name = r.getLocationName(),
                        address = r.getLocationAddress(),
                        type = LocationType.valueOf(r.getLocationType()),
                    ),
                distanceMeters = r.getDistanceMeters(),
                price = r.getMinPrice(),
                averagePrice = r.getAvgPrice(),
            )
    }
}
