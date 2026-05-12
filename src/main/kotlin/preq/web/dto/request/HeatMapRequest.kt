package preq.web.dto.request

data class HeatMapRequest (
    val productId: Long,
    val latitude: Double,
    val longitude: Double,
    val distance: Double,
)
