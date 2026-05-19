package preq.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import preq.enum.PriceValidity
import preq.model.Location
import preq.model.LocationProductPrice
import preq.model.User
import preq.repository.LocationProductPriceRepository
import preq.repository.LocationRepository
import preq.repository.ProductRepository
import preq.repository.UserRepository
import preq.web.dto.request.ReportProductPriceRequest
import preq.web.dto.response.PriceSummaryResponse
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.exp

@Service
class PriceService(
    private val locationProductPriceRepository: LocationProductPriceRepository,
    private val productRepository: ProductRepository,
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository,
    @Value("\${preq.prices.threshold-percentage}") private val thresholdPercentage: Double,
    @Value("\${preq.prices.proximity-meters}") private val proximityMeters: Double,
    @Value("\${preq.prices.cold-start-minimum-reports}") private val coldStartMinimumReports: Int,
    @Value("\${preq.trust.minimum-score}") private val minimumTrustScore: Double,
) {
    fun reportPrice(
        request: ReportProductPriceRequest,
        user: User,
    ): LocationProductPrice {
        val product =
            productRepository.findById(request.productId).orElseThrow {
                NoSuchElementException("Product ${request.productId} not found")
            }
        val location =
            locationRepository.findById(request.locationId).orElseThrow {
                NoSuchElementException("Location ${request.locationId} not found")
            }

        val allPrices = locationProductPriceRepository.findValidByProductIdOrderByReportedAtDesc(request.productId, minimumTrustScore)
        val weightedAverage = computeWeightedPrice(allPrices)
        val totalReports = locationProductPriceRepository.countValidByProductId(request.productId, minimumTrustScore)

        val (locationConfidence, priceValidity) = applyIngestionRules(request, user, location, weightedAverage, totalReports)

        // R11 — trust recovery only on valid reports
        if (priceValidity == PriceValidity.VALID) {
            val wasAboveThreshold = user.trustScore >= minimumTrustScore
            user.trustScore = (user.trustScore + 0.01 * user.recoveryMultiplier).coerceIn(0.0, 1.0)
            if (wasAboveThreshold && user.trustScore < minimumTrustScore) {
                user.recoveryMultiplier *= 0.5
            }
            userRepository.save(user)
        }

        return locationProductPriceRepository.save(
            LocationProductPrice().apply {
                this.product = product
                this.location = location
                this.user = user
                this.price = request.price
                this.reportedAt = LocalDateTime.now()
                this.locationConfidence = locationConfidence
                this.priceValidity = priceValidity
            },
        )
    }

    fun getPriceSummary(productId: Long): PriceSummaryResponse {
        productRepository.findById(productId).orElseThrow {
            NoSuchElementException("Product $productId not found")
        }
        val stats = locationProductPriceRepository.getPriceStats(productId, minimumTrustScore)
        val topLocations = locationProductPriceRepository.getTopLocations(productId, minimumTrustScore)
        val allPrices = locationProductPriceRepository.findValidByProductIdOrderByReportedAtDesc(productId, minimumTrustScore)
        val weightedPrice = computeWeightedPrice(allPrices)
        return PriceSummaryResponse.from(stats, topLocations, weightedPrice)
    }

    private fun applyIngestionRules(
        request: ReportProductPriceRequest,
        user: User,
        location: Location,
        weightedAverage: Double?,
        totalReports: Long,
    ): Pair<Double, PriceValidity> {
        // R4 — cold start: skip R1 if not enough reports
        val skipThresholdCheck = totalReports < coldStartMinimumReports

        // R1 — threshold check
        if (!skipThresholdCheck && weightedAverage != null) {
            val deviation = (request.price.toDouble() - weightedAverage) / weightedAverage
            if (abs(deviation) > thresholdPercentage) {
                // R10 — proportional penalty, applied silently
                val penalty = deviation * deviation
                val wasAboveThreshold = user.trustScore >= minimumTrustScore
                user.trustScore = (user.trustScore - penalty).coerceIn(0.0, 1.0)
                if (wasAboveThreshold && user.trustScore < minimumTrustScore) {
                    user.recoveryMultiplier *= 0.5
                }
                userRepository.save(user)
                return Pair(1.0, PriceValidity.INVALID)
            }
        }

        // R3 — location coherence: reduce locationConfidence if user is too far from reported location
        var locationConfidence = 1.0
        val userLat = request.userLatitude
        val userLng = request.userLongitude
        if (userLat != null && userLng != null && location.hasCoordinates()) {
            val distanceMeters = locationProductPriceRepository.getDistanceMeters(userLat, userLng, location.id)
            if (distanceMeters > proximityMeters) {
                locationConfidence *= (proximityMeters / distanceMeters).coerceIn(0.0, 1.0)
            }
        }

        return Pair(locationConfidence, PriceValidity.VALID)
    }

    private fun computeWeightedPrice(prices: List<LocationProductPrice>): Double? {
        if (prices.isEmpty()) return null
        val decayFactor = 0.01
        var weightedSum = 0.0
        var totalWeight = 0.0
        prices.forEach { report ->
            val weight = exp(-decayFactor * report.ageInDays()) * report.locationConfidence
            weightedSum += report.price.toDouble() * weight
            totalWeight += weight
        }
        return if (totalWeight == 0.0) 0.0 else weightedSum / totalWeight
    }
}
