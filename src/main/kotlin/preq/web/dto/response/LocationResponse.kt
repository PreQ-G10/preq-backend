package preq.web.dto.response

import preq.enum.LocationType
import preq.model.Location

data class LocationResponse(
    val id: Long,
    val name: String,
    val address: String,
    val type: LocationType,
) {
    companion object {
        fun from(location: Location) =
            LocationResponse(
                id = location.id,
                name = location.name,
                address = location.address,
                type = location.type,
            )
    }
}
