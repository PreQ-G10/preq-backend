package preq.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import org.locationtech.jts.geom.Point
import preq.enum.UserRole

@Entity
@Table(name = "users")
class User : BaseEntity() {
    @NotBlank
    @Column(nullable = false)
    lateinit var name: String

    @NotBlank
    @Column(name = "last_name", nullable = false)
    lateinit var lastName: String

    @Column(nullable = true)
    var address: String? = null

    @Column(nullable = true, columnDefinition = "geometry(Point, 4326)")
    var addressLocation: Point? = null

    @NotBlank
    @Column(nullable = false, unique = true)
    lateinit var email: String

    @NotBlank
    @Column(nullable = false)
    lateinit var password: String

    @Column(name = "trust_score", nullable = false)
    var trustScore: Double = 0.5

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.USER
}
