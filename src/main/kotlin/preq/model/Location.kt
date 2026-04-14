package preq.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import preq.enum.LocationType

@Entity
@Table(name = "location")
class Location : BaseEntity() {

    var name: String = ""
    var address: String = ""

    @Enumerated(EnumType.STRING)
    var type: LocationType = LocationType.OTHER

    var latitude: Double? = null
    var longitude: Double? = null

    @OneToMany(mappedBy = "location", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val prices: MutableList<LocationProductPrice> = mutableListOf()

    fun hasCoordinates() = latitude != null && longitude != null
}