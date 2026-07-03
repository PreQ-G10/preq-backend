package preq.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import preq.model.User
import preq.repository.LocationRepository
import preq.repository.ProductRepository
import preq.repository.ShoppingListRepository
import preq.web.dto.request.SaveShoppingListRequest
import preq.web.dto.response.ShoppingListResponse
import jakarta.persistence.EntityNotFoundException
import preq.model.ShoppingList
import preq.model.ShoppingListItem
import preq.repository.ShoppingListItemRepository
import preq.web.dto.request.UpdateShoppingListRequest
import preq.web.dto.response.BusinessMetricsResponse
import preq.web.dto.response.DeleteShoppingListResponse
import preq.web.dto.response.ShoppingListSummaryResponse
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@Transactional
class ShoppingListService(
    private val shoppingListRepository: ShoppingListRepository,
    private val shoppingListItemRepository: ShoppingListItemRepository,
    private val locationRepository: LocationRepository,
    private val productRepository: ProductRepository,
) {
    fun save(request: SaveShoppingListRequest, user: User): ShoppingListResponse {
        val location = locationRepository.findById(request.locationId)
            .orElseThrow { EntityNotFoundException("Location ${request.locationId} not found") }

        val list = ShoppingList().apply {
            this.user = user
            this.location = location
            this.totalPrice = request.items.fold(BigDecimal.ZERO) { acc, item ->
                acc + item.unitPrice.multiply(item.cartQuantity.toBigDecimal())
            }
        }

        request.items.forEach { itemReq ->
            val product = productRepository.findById(itemReq.productId)
                .orElseThrow { EntityNotFoundException("Product ${itemReq.productId} not found") }
            list.items.add(ShoppingListItem().apply {
                this.shoppingList = list
                this.product = product
                this.cartQuantity = itemReq.cartQuantity.coerceAtLeast(1)
                this.unitPrice = itemReq.unitPrice
            })
        }

        return ShoppingListResponse.from(shoppingListRepository.save(list))
    }

    fun update(id: Long, request: UpdateShoppingListRequest, user: User): ShoppingListResponse {
        val list = shoppingListRepository.findByIdAndUserId(id, user.id)
            ?: throw EntityNotFoundException("Shopping list $id not found")

        list.completed = request.completed

        request.items.forEach { itemReq ->
            val item = shoppingListItemRepository.findByIdAndShoppingListId(itemReq.itemId, id)
                ?: throw EntityNotFoundException("Item ${itemReq.itemId} not found in list $id")
            item.checkedQuantity = itemReq.checkedQuantity.coerceIn(0, item.cartQuantity)
        }

        return ShoppingListResponse.from(shoppingListRepository.save(list))
    }

    @Transactional(readOnly = true)
    fun getAll(user: User): List<ShoppingListSummaryResponse> =
        shoppingListRepository.findSummariesByUserId(user.id)

    @Transactional(readOnly = true)
    fun getById(id: Long, user: User): ShoppingListResponse {
        val list = shoppingListRepository.findByIdAndUserId(id, user.id)
            ?: throw EntityNotFoundException("Shopping list $id not found")
        return ShoppingListResponse.from(list)
    }

    @Transactional(readOnly = true)
    fun getBusinessMetrics(user: User): BusinessMetricsResponse {
        val location = locationRepository.findByClaimedById(user.id)
            ?: throw EntityNotFoundException("No claimed location found for user ${user.id}")

        val since = LocalDateTime.now().minus(30, ChronoUnit.DAYS)

        val uniqueUsers = shoppingListRepository.countDistinctUsersSince(location.id, since)

        val last10Prices = shoppingListRepository.findLast10TotalPrices(location.id)
        val averagePrice = if (last10Prices.isEmpty()) null
        else last10Prices.fold(BigDecimal.ZERO, BigDecimal::add)
            .divide(last10Prices.size.toBigDecimal(), 2, RoundingMode.HALF_UP)

        val topProducts = shoppingListRepository.findTop5Products(location.id)

        return BusinessMetricsResponse(
            uniqueUsersLast30Days = uniqueUsers,
            averagePriceLast10Lists = averagePrice,
            topProducts = topProducts
        )
    }

    fun delete(id: Long, user: User): DeleteShoppingListResponse {
        val list = shoppingListRepository.findByIdAndUserId(id, user.id)
            ?: throw EntityNotFoundException("Shopping list $id not found")
        shoppingListRepository.delete(list)
        return DeleteShoppingListResponse(list.id)
    }
}