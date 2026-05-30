package preq.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import preq.enum.PriceValidationType
import preq.enum.ReportScore
import preq.model.Location
import preq.model.LocationProductPrice
import preq.model.PriceValidation
import preq.model.User
import preq.repository.LocationProductPriceRepository
import preq.repository.LocationRepository
import preq.repository.PriceValidationRepository
import preq.repository.ProductRepository
import preq.repository.UserRepository
import preq.web.dto.request.DisputePriceRequest
import preq.web.dto.request.ReportProductPriceRequest
import preq.web.dto.response.ConfirmPriceResponse
import preq.web.dto.response.PendingValidationResponse
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
    private val priceValidationRepository: PriceValidationRepository,
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

        val allPrices = locationProductPriceRepository.findValidByProductIdOrderByReportedAtDesc(request.productId)
        val weightedAverage = computeWeightedPrice(allPrices)
        val totalReports = locationProductPriceRepository.countValidByProductId(request.productId)

        val (locationConfidence, score) = applyIngestionRules(request, user, location, weightedAverage, totalReports)

        if (ReportScore.fromScore(score) != ReportScore.INVALID) {
            val wasAboveThreshold = user.trustScore >= minimumTrustScore
            user.trustScore = (user.trustScore + 0.01 * user.recoveryMultiplier).coerceIn(0.0, 1.0)
            if (wasAboveThreshold && user.trustScore < minimumTrustScore) {
                user.recoveryMultiplier *= 0.5
            }
            userRepository.save(user)

            product.updateMinMaxPrice(request.price)
            productRepository.save(product)
        }

        return locationProductPriceRepository.save(
            LocationProductPrice().apply {
                this.product = product
                this.location = location
                this.user = user
                this.price = request.price
                this.reportedAt = LocalDateTime.now()
                this.locationConfidence = locationConfidence
                this.score = score
            },
        )
    }

    fun getPendingValidation(
        user: User,
        latitude: Double,
        longitude: Double,
    ): List<PendingValidationResponse> {
        if (user.trustScore < minimumTrustScore) return emptyList()
        return locationProductPriceRepository
            .findPendingValidationNearby(user.id, latitude, longitude, proximityMeters)
            .map { PendingValidationResponse.from(it) }
    }

    fun confirmPrice(
        reportId: Long,
        confirmer: User,
    ): ConfirmPriceResponse {
        if (confirmer.trustScore < minimumTrustScore) throw IllegalStateException("User trust score too low to confirm")

        val report =
            locationProductPriceRepository.findById(reportId).orElseThrow {
                NoSuchElementException("Report $reportId not found")
            }

        if (priceValidationRepository.existsByReportIdAndUserId(reportId, confirmer.id)) {
            return ConfirmPriceResponse()
        }

        // R9 — update score proportionally to confirmer's trust score
        report.score = (report.score + (1 - report.score) * confirmer.trustScore * 0.3).coerceIn(0.0, 1.0)
        locationProductPriceRepository.save(report)

        confirmer.trustScore = (confirmer.trustScore + 0.01 * confirmer.recoveryMultiplier).coerceIn(0.0, 1.0)
        userRepository.save(confirmer)

        priceValidationRepository.save(
            PriceValidation().apply {
                this.report = report
                this.user = confirmer
                this.type = PriceValidationType.CONFIRM
            },
        )

        return ConfirmPriceResponse()
    }

    fun disputePrice(
        reportId: Long,
        disputer: User,
        request: DisputePriceRequest,
    ): LocationProductPrice {
        if (disputer.trustScore < minimumTrustScore) throw IllegalStateException("User trust score too low to dispute")

        val report =
            locationProductPriceRepository.findById(reportId).orElseThrow {
                NoSuchElementException("Report $reportId not found")
            }

        if (priceValidationRepository.existsByReportIdAndUserId(reportId, disputer.id)) {
            throw IllegalStateException("Already disputed this report")
        }

        // R12 — reduce report score proportionally to price difference
        val deviation = abs(request.alternativePrice.toDouble() - report.price.toDouble()) / report.price.toDouble()
        report.score = (report.score - deviation * 0.3).coerceIn(0.0, 1.0)
        locationProductPriceRepository.save(report)

        // R12 — penalize original reporter
        val reporter = report.user!!
        val wasAboveThreshold = reporter.trustScore >= minimumTrustScore
        reporter.trustScore = (reporter.trustScore - deviation * 0.1).coerceIn(0.0, 1.0)
        if (wasAboveThreshold && reporter.trustScore < minimumTrustScore) {
            reporter.recoveryMultiplier *= 0.5
        }
        userRepository.save(reporter)

        // store disputer's alternative price through full ingestion pipeline
        val alternativePriceReport =
            reportPrice(
                ReportProductPriceRequest(
                    productId = report.product!!.id,
                    locationId = report.location!!.id,
                    price = request.alternativePrice,
                    userLatitude = request.userLatitude,
                    userLongitude = request.userLongitude,
                ),
                disputer,
            )

        priceValidationRepository.save(
            PriceValidation().apply {
                this.report = report
                this.user = disputer
                this.type = PriceValidationType.DISPUTE
            },
        )

        return alternativePriceReport
    }

    fun getPriceSummary(productId: Long): PriceSummaryResponse {
        productRepository.findById(productId).orElseThrow {
            NoSuchElementException("Product $productId not found")
        }
        val stats = locationProductPriceRepository.getPriceStats(productId)
        val topLocations = locationProductPriceRepository.getTopLocations(productId)
        val allPrices = locationProductPriceRepository.findValidByProductIdOrderByReportedAtDesc(productId)
        val weightedPrice = computeWeightedPrice(allPrices)
        return PriceSummaryResponse.from(stats, topLocations, weightedPrice)
    }

    private fun applyIngestionRules(
        request: ReportProductPriceRequest,
        user: User,
        location: Location,
        weightedAverage: Double?,
        totalReports: Long,
    ): Pair<Double, Double> {
        val skipThresholdCheck = totalReports < coldStartMinimumReports

        var locationConfidence = 1.0
        val userLat = request.userLatitude
        val userLng = request.userLongitude
        if (userLat != null && userLng != null && location.hasCoordinates()) {
            val distanceMeters = locationProductPriceRepository.getDistanceMeters(userLat, userLng, location.id)
            if (distanceMeters > proximityMeters) {
                locationConfidence *= (proximityMeters / distanceMeters).coerceIn(0.0, 1.0)
            }
        }

        if (!skipThresholdCheck && weightedAverage != null) {
            val deviation = (request.price.toDouble() - weightedAverage) / weightedAverage
            if (abs(deviation) > thresholdPercentage) {
                val penalty = deviation * deviation
                val wasAboveThreshold = user.trustScore >= minimumTrustScore
                user.trustScore = (user.trustScore - penalty).coerceIn(0.0, 1.0)
                if (wasAboveThreshold && user.trustScore < minimumTrustScore) {
                    user.recoveryMultiplier *= 0.5
                }
                userRepository.save(user)
                return Pair(locationConfidence, 0.0)
            }
        }

        val deviationPenalty =
            if (!skipThresholdCheck && weightedAverage != null) {
                val deviation = abs((request.price.toDouble() - weightedAverage) / weightedAverage)
                deviation / thresholdPercentage
            } else {
                0.0
            }
        val initialScore =
            locationConfidence *
                (0.5 + user.trustScore * 0.5) *
                (1 - deviationPenalty * 0.3)

        return Pair(locationConfidence, initialScore.coerceIn(0.0, 1.0))
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
