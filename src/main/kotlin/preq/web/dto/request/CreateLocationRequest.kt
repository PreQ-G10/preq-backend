package preq.web.dto.request

import preq.enum.LocationType

data class CreateLocationRequest(
    val name: String,
    val address: String,
    val type: LocationType,
)
