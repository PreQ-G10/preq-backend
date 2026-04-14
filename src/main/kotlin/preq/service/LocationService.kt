package preq.service

import org.springframework.stereotype.Service
import preq.enum.LocationType
import preq.model.Location
import preq.repository.LocationRepository
import preq.web.dto.LocationResponse

@Service
class LocationService(private val locationRepository: LocationRepository) {

    fun search(name: String): List<LocationResponse> {
        return locationRepository.searchByName(name).map { LocationResponse.from(it) }
    }

    fun create(name: String, address: String, type: LocationType): Location {
        return locationRepository.save(Location().apply {
            this.name = name
            this.address = address
            this.type = type
        })
    }
}