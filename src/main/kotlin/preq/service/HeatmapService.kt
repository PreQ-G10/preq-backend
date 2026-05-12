package preq.service

import org.springframework.stereotype.Service
import preq.repository.LocationProductPriceRepository
import preq.web.dto.request.HeatMapRequest
import preq.web.dto.response.HeatmapPointResponse
import java.math.BigDecimal

@Service
class HeatmapService(
    private val locationProductPriceRepository: LocationProductPriceRepository,
) {
    fun getHeatmapDataForProduct(
        productId: Long,
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
    ): List<HeatmapPointResponse> {
        return locationProductPriceRepository.getLocationPricesForProductInArea(productId, latitude, longitude, radiusMeters)
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
}
