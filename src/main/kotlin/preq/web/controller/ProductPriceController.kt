package preq.web.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import preq.repository.UserRepository
import preq.service.HeatmapService
import preq.service.PriceService
import preq.web.dto.request.DisputePriceRequest
import preq.web.dto.request.ReportProductPriceRequest
import preq.web.dto.response.ConfirmPriceResponse
import preq.web.dto.response.HeatmapPointResponse
import preq.web.dto.response.LocationProductPriceResponse
import preq.web.dto.response.PendingValidationResponse
import preq.web.dto.response.PriceHistoryPointResponse
import preq.web.dto.response.PriceSummaryResponse
import java.security.Principal

@RestController
@RequestMapping("/api/prices")
class ProductPriceController(
    private val priceService: PriceService,
    private val heatmapService: HeatmapService,
    private val userRepository: UserRepository,
) {
    @PostMapping
    fun report(
        @RequestBody request: ReportProductPriceRequest,
        principal: Principal,
    ): LocationProductPriceResponse {
        val user = userRepository.findByEmail(principal.name).orElseThrow()
        return LocationProductPriceResponse.from(priceService.reportPrice(request, user))
    }

    @GetMapping("/pending-validation")
    fun getPendingValidation(
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        principal: Principal,
    ): List<PendingValidationResponse> {
        val user = userRepository.findByEmail(principal.name).orElseThrow()
        return priceService.getPendingValidation(user, latitude, longitude)
    }

    @PostMapping("/{id}/confirm")
    fun confirm(
        @PathVariable id: Long,
        principal: Principal,
    ): ConfirmPriceResponse {
        val user = userRepository.findByEmail(principal.name).orElseThrow()
        return priceService.confirmPrice(id, user)
    }

    @PostMapping("/{id}/dispute")
    fun dispute(
        @PathVariable id: Long,
        @RequestBody request: DisputePriceRequest,
        principal: Principal,
    ): LocationProductPriceResponse {
        val user = userRepository.findByEmail(principal.name).orElseThrow()
        return LocationProductPriceResponse.from(priceService.disputePrice(id, user, request))
    }

    @GetMapping("/{productId}")
    fun getSummary(
        @PathVariable productId: Long,
        principal: Principal,
    ): PriceSummaryResponse {
        val user = userRepository.findByEmail(principal.name).orElseThrow()
        return priceService.getPriceSummary(productId, user)
    }

    @GetMapping("/{productId}/heatmap")
    fun getHeatmapData(
        @PathVariable productId: Long,
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam radius: Double,
    ): List<HeatmapPointResponse> = heatmapService.getHeatmapDataForProduct(productId, latitude, longitude, radius)

    @GetMapping("/{productId}/{locationId}")
    fun getPriceReportsFromLocation(
        @PathVariable productId: Long,
        @PathVariable locationId: Long,
    ): List<LocationProductPriceResponse> = priceService.getPricesFromLocation(productId, locationId)

    @GetMapping("/history/{productId}")
    fun getPriceHistory(
        @PathVariable productId: Long,
    ): List<PriceHistoryPointResponse> = priceService.getPriceHistory(productId)
}
