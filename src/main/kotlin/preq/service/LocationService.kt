package preq.service

import org.springframework.stereotype.Service
import preq.model.Location
import preq.repository.LocationRepository
import preq.web.dto.request.CreateLocationRequest

@Service
class LocationService(
    private val locationRepository: LocationRepository,
) {
    fun search(name: String): List<Location> = locationRepository.searchByName(name)

    fun create(request: CreateLocationRequest): Location =
        locationRepository.save(
            Location().apply {
                name = request.name
                address = request.address
                type = request.type
            },
        )
}
