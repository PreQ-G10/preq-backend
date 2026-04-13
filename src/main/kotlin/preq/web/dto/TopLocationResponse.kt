package preq.web.dto

data class TopLocationResponse(
    val name: String,
    val address: String,
    val avgPrice: Double,
    val reportCount: Int
) {
    companion object {
        fun from(row: Array<Any>) = TopLocationResponse(
            name = row[0] as String,
            address = row[1] as String,
            avgPrice = (row[2] as Number).toDouble(),
            reportCount = (row[3] as Number).toInt()
        )
    }
}