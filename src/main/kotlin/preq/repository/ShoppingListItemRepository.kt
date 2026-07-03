package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import preq.model.ShoppingListItem

@Repository
interface ShoppingListItemRepository : JpaRepository<ShoppingListItem, Long> {
    fun findByIdAndShoppingListId(
        id: Long,
        shoppingListId: Long,
    ): ShoppingListItem?
}
