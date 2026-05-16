package preq.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import org.locationtech.jts.geom.Point
import preq.enum.LocationType

@Entity
@Table(name = "location")
class Location : BaseEntity() {
    @NotBlank
    var name: String = ""

    @NotBlank
    var address: String = ""

    @Enumerated(EnumType.STRING)
    var type: LocationType = LocationType.OTHER

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    var latitude: Double? = null

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    var longitude: Double? = null

    @Column(columnDefinition = "geometry(Point, 4326)")
    var coordinates: Point? = null

    @OneToMany(mappedBy = "location", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val prices: MutableList<LocationProductPrice> = mutableListOf()

    fun hasCoordinates() = latitude != null && longitude != null
}
