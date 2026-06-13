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
import preq.web.dto.response.LocationProductPriceResponse
import preq.web.dto.response.PendingValidationResponse
import preq.web.dto.response.PriceHistoryPointResponse
import preq.web.dto.response.PriceSummaryResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

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
    @Value("\${preq.prices.average-monthly-inflation}") private val averageMonthlyInflation: Double,
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
        val weightedAverage = LocationProductPrice.computeInflationAdjustedPrice(allPrices, averageMonthlyInflation)
        val totalReports = locationProductPriceRepository.countValidByProductId(request.productId)

        val (locationConfidence, score) = applyIngestionRules(request, user, location, weightedAverage, totalReports)

        if (ReportScore.fromScore(score) != ReportScore.INVALID) {
            user.adjustTrustScore(0.01 * user.recoveryMultiplier, minimumTrustScore)
            userRepository.save(user)
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
        if (!user.canValidatePrices(minimumTrustScore)) return emptyList()
        return locationProductPriceRepository
            .findPendingValidationNearby(user.id, latitude, longitude, proximityMeters)
            .map { PendingValidationResponse.from(it) }
    }

    fun confirmPrice(
        reportId: Long,
        confirmer: User,
    ): ConfirmPriceResponse {
        if (!confirmer.canValidatePrices(minimumTrustScore)) throw IllegalStateException("User trust score too low to confirm")

        val report =
            locationProductPriceRepository.findById(reportId).orElseThrow {
                NoSuchElementException("Report $reportId not found")
            }

        if (priceValidationRepository.existsByReportIdAndUserId(reportId, confirmer.id)) {
            return ConfirmPriceResponse()
        }

        report.applyConfirmationBoost(confirmer.trustScore)
        locationProductPriceRepository.save(report)

        confirmer.adjustTrustScore(0.01 * confirmer.recoveryMultiplier, minimumTrustScore)
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
        if (!disputer.canValidatePrices(minimumTrustScore)) throw IllegalStateException("User trust score too low to dispute")

        val report =
            locationProductPriceRepository.findById(reportId).orElseThrow {
                NoSuchElementException("Report $reportId not found")
            }

        if (priceValidationRepository.existsByReportIdAndUserId(reportId, disputer.id)) {
            throw IllegalStateException("Already disputed this report")
        }

        val deviation = abs(request.alternativePrice.toDouble() - report.price.toDouble()) / report.price.toDouble()

        report.applyDisputePenalty(deviation)
        locationProductPriceRepository.save(report)

        val reporter = report.user!!
        reporter.adjustTrustScore(-deviation * 0.1, minimumTrustScore)
        userRepository.save(reporter)

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
        val weightedPrice = LocationProductPrice.computeInflationAdjustedPrice(allPrices, averageMonthlyInflation)
        val weightedByConfidencePrice = LocationProductPrice.computeDecayWeightedPrice(allPrices)
        return PriceSummaryResponse.from(stats, topLocations, weightedPrice, weightedByConfidencePrice)
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
            if (user.applyPricePenaltyIfNeeded(deviation, thresholdPercentage, minimumTrustScore)) {
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

        val score = LocationProductPrice.computeInitialScore(locationConfidence, user.trustScore, deviationPenalty)

        return Pair(locationConfidence, score)
    }

    fun getPricesFromLocation(
        productId: Long,
        locationId: Long,
    ): List<LocationProductPriceResponse> =
        locationProductPriceRepository
            .getLocationPricesForProductInLocation(productId, locationId)
            .map { LocationProductPriceResponse.from(it) }

    fun getPriceHistory(productId: Long): List<PriceHistoryPointResponse> {
        val since = LocalDateTime.now().minus(180, ChronoUnit.DAYS)
        return locationProductPriceRepository.findWeeklyAverages(productId, since)
            .map { row ->
                PriceHistoryPointResponse.from(
                    weekStart = row.getWeekStart().toLocalDate(),
                    avgPrice = row.getAvgPrice(),
                )
            }
    }
}