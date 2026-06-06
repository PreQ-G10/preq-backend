package preq.web.dto.response

data class TopLocationResponse(
    val id: Long,
    val name: String,
    val address: String,
    val avgPrice: Double,
    val reportCount: Int,
) {
    companion object {
        fun from(row: Array<Any>) =
            TopLocationResponse(
                id = row [0] as Long,
                name = row[1] as String,
                address = row[2] as String,
                avgPrice = (row[3] as Number).toDouble(),
                reportCount = (row[4] as Number).toInt(),
            )
    }
}
