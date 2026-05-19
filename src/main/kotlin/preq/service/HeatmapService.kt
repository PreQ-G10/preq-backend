package preq.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import preq.repository.LocationProductPriceRepository
import preq.web.dto.response.HeatmapPointResponse
import java.math.BigDecimal

@Service
class HeatmapService(
    private val locationProductPriceRepository: LocationProductPriceRepository,
    @Value("\${preq.trust.minimum-score}") private val minimumTrustScore: Double,
) {
    fun getHeatmapDataForProduct(
        productId: Long,
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
    ): List<HeatmapPointResponse> =
        locationProductPriceRepository
            .getLocationPricesForProductInArea(productId, latitude, longitude, radiusMeters, minimumTrustScore)
            .map {
                HeatmapPointResponse(
                    locationId = it.getLocationId(),
                    name = it.getName(),
                    address = it.getAddress(),
                    latitude = it.getLatitude(),
                    longitude = it.getLongitude(),
                    avgPrice = BigDecimal(it.getAvgPrice()),
                )
            }
}
