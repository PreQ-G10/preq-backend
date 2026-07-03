package preq.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "shopping_list")
class ShoppingList : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    lateinit var location: Location

    @Column(nullable = false)
    var completed: Boolean = false

    @Column(nullable = false, precision = 12, scale = 2)
    var totalPrice: BigDecimal = BigDecimal.ZERO

    @OneToMany(mappedBy = "shoppingList", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<ShoppingListItem> = mutableListOf()
}
