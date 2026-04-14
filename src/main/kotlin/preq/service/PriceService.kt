package preq.service

import org.springframework.stereotype.Service
import preq.model.LocationProductPrice
import preq.repository.LocationProductPriceRepository
import preq.repository.LocationRepository
import preq.repository.ProductRepository
import preq.web.dto.PriceSummaryResponse
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class PriceService(
    private val locationProductPriceRepository: LocationProductPriceRepository,
    private val productRepository: ProductRepository,
    private val locationRepository: LocationRepository,
) {
    fun reportPrice(
        productId: Long,
        locationId: Long,
        price: BigDecimal,
    ): LocationProductPrice {
        val product = productRepository.findById(productId).orElseThrow()
        val location = locationRepository.findById(locationId).orElseThrow()
        return locationProductPriceRepository.save(
            LocationProductPrice().apply {
                this.product = product
                this.location = location
                this.price = price
                this.reportedAt = LocalDateTime.now()
            },
        )
    }

    fun getPriceSummary(productId: Long): PriceSummaryResponse {
        val stats = locationProductPriceRepository.getPriceStats(productId)
        val topLocations = locationProductPriceRepository.getTopLocations(productId)
        val allPrices = locationProductPriceRepository.findByProductIdOrderByReportedAtDesc(productId)
        val weightedPrice = computeWeightedPrice(allPrices)

        return PriceSummaryResponse.from(stats, topLocations, weightedPrice)
    }

    private fun computeWeightedPrice(prices: List<LocationProductPrice>): Double {
        if (prices.isEmpty()) return 0.0
        val decayFactor = 0.01
        var weightedSum = 0.0
        var totalWeight = 0.0
        prices.forEach { report ->
            val weight = Math.exp(-decayFactor * report.ageInDays())
            weightedSum += report.price.toDouble() * weight
            totalWeight += weight
        }
        return if (totalWeight == 0.0) 0.0 else weightedSum / totalWeight
    }
}
