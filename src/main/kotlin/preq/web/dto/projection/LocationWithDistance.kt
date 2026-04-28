package preq.web.dto.projection

interface LocationWithDistance {
    fun getId(): Long

    fun getName(): String

    fun getAddress(): String

    fun getType(): String

    fun getDistanceMeters(): Double
}
