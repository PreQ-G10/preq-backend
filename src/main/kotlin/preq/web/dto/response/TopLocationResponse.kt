package preq.web.dto.response

import preq.web.dto.projection.TopLocationResult

data class TopLocationResponse(
    val id: Long,
    val name: String,
    val address: String,
    val avgPrice: Double,
    val reportCount: Int,
) {
    companion object {
        fun from(row: TopLocationResult) =
            TopLocationResponse(
                id = row.getId(),
                name = row.getName(),
                address = row.getAddress(),
                avgPrice = row.getAvgPrice(),
                reportCount = row.getReportCount(),
            )
    }
}
