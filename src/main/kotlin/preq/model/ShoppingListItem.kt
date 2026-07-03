package preq.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "shopping_list_item")
class ShoppingListItem : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_list_id", nullable = false)
    lateinit var shoppingList: ShoppingList

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    lateinit var product: Product

    @Column(nullable = false)
    var cartQuantity: Int = 1

    @Column(nullable = false)
    var checkedQuantity: Int = 0

    @Column(nullable = false, precision = 12, scale = 2)
    var unitPrice: BigDecimal = BigDecimal.ZERO
}
