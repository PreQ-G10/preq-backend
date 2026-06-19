package preq.web.dto.response

import preq.enum.ClaimStatus
import preq.enum.LocationType
import preq.model.Location

data class LocationSearchResponse(
    val id: Long,
    val name: String,
    val address: String,
    val type: LocationType,
    val claimStatus: ClaimStatus,
) {
    companion object {
        fun from(location: Location) = LocationSearchResponse(
            id = location.id,
            name = location.name,
            address = location.address,
            type = location.type,
            claimStatus = location.claimStatus,
        )
    }
}