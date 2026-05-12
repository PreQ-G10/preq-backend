package preq.web.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import preq.service.HeatmapService
import preq.service.PriceService
import preq.web.dto.request.HeatMapRequest
import preq.web.dto.request.ReportProductPriceRequest
import preq.web.dto.response.HeatmapPointResponse
import preq.web.dto.response.LocationProductPriceResponse
import preq.web.dto.response.PriceSummaryResponse

@RestController
@RequestMapping("/api/prices")
class ProductPriceController(
    private val priceService: PriceService,
    private val heatmapService: HeatmapService,
) {
    @PostMapping
    fun report(
        @RequestBody request: ReportProductPriceRequest,
    ): LocationProductPriceResponse = LocationProductPriceResponse.from(priceService.reportPrice(request))

    @GetMapping("/{productId}")
    fun getSummary(
        @PathVariable productId: Long,
    ): PriceSummaryResponse = priceService.getPriceSummary(productId)

    @GetMapping("/{productId}/heatmap")
    fun getHeatmapData(
        @PathVariable productId: Long,
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam radius: Double,
    ): List<HeatmapPointResponse> = heatmapService.getHeatmapDataForProduct(productId, latitude, longitude, radius)

}
