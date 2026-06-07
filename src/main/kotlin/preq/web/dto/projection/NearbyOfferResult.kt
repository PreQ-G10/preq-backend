package preq.web.dto.projection

import java.math.BigDecimal

interface NearbyOfferResult {
    // product
    fun getProductId(): Long

    fun getProductName(): String

    fun getProductBrand(): String

    fun getProductQuantity(): BigDecimal

    fun getProductQuantityType(): String

    fun getProductMinPrice(): BigDecimal?

    fun getProductMaxPrice(): BigDecimal?

    fun getProductBarcode(): String?

    // location
    fun getLocationId(): Long

    fun getLocationName(): String

    fun getLocationAddress(): String

    fun getLocationType(): String

    // offer
    fun getMinPrice(): BigDecimal

    fun getAvgPrice(): BigDecimal

    fun getDistanceMeters(): Double
}
