package preq.web.dto.request

import preq.enum.LocationType

data class BusinessRegisterRequest(
    // User credentials
    val email: String,
    val password: String,
    val ownerName: String,
    val ownerLastName: String,
    val businessPhone: String,
    val cuit: String? = null,
    // Claim existing location
    val locationId: Long? = null,
    // OR create new location
    val locationName: String? = null,
    val locationAddress: String? = null,
    val locationType: LocationType? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
