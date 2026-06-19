package preq.web.dto.response

import preq.enum.ClaimStatus
import preq.model.Location
import preq.model.User

data class BusinessProfileResponse(
    val ownerName: String,
    val ownerLastName: String,
    val email: String,
    val businessPhone: String,
    val cuit: String?,
    val locationName: String,
    val locationAddress: String,
    val claimStatus: ClaimStatus,
) {
    companion object {
        fun from(user: User, location: Location) = BusinessProfileResponse(
            ownerName = user.name,
            ownerLastName = user.lastName,
            email = user.email,
            businessPhone = location.businessPhone ?: "",
            cuit = location.cuit,
            locationName = location.name,
            locationAddress = location.address,
            claimStatus = location.claimStatus,
        )
    }
}