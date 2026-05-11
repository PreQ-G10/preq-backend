package preq.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import preq.enum.PriceSource
import preq.repository.LocationProductPriceRepository
import preq.repository.ProductRepository
import preq.web.dto.request.CartCompareRequest
import preq.web.dto.request.CartItemRequest
import preq.web.dto.response.CartCompareResponse
import preq.web.dto.response.CartLocationResponse
import preq.web.dto.response.CartProductResponse

@Service
class CartService(
    private val locationProductPriceRepository: LocationProductPriceRepository,
    private val productRepository: ProductRepository,
    @Value("\${preq.cart.nearby-radius-meters:10000}") private val nearbyRadiusMeters: Double,
) {
    data class ProductPriceData(
        val productName: String,
        val globalAvg: Double?,
        val byLocation: Map<Long, Double>,
    )

    data class LocationMeta(
        val name: String,
        val address: String,
        val latitude: Double?,
        val longitude: Double?,
    )

    fun compare(request: CartCompareRequest): CartCompareResponse {
        val productData = buildProductData(request.items)
        if (productData.isEmpty()) return CartCompareResponse(emptyList(), emptyList())

        val locationMeta = buildLocationMeta(productData.keys)
        val (activeItems, skippedProducts) = partitionItems(request.items, productData, locationMeta)

        if (activeItems.isEmpty()) return CartCompareResponse(emptyList(), skippedProducts)

        val locationResults = buildLocationResults(request, activeItems, productData, locationMeta)
        val top5 = locationResults.sortedBy { it.totalEstimatedPrice }.take(5)

        return CartCompareResponse(top5, skippedProducts)
    }

    private fun buildProductData(items: List<CartItemRequest>): Map<Long, ProductPriceData> =
        items
            .mapNotNull { item ->
                val product = productRepository.findById(item.productId).orElse(null)
                val productName = product?.name ?: "Producto ${item.productId}"
                val globalAvg = locationProductPriceRepository.getGlobalAvgPrice(item.productId)
                val byLocation =
                    locationProductPriceRepository
                        .getLocationPricesForProduct(item.productId)
                        .associate { it.getLocationId() to it.getAvgPrice() }
                item.productId to ProductPriceData(productName, globalAvg, byLocation)
            }.toMap()

    private fun buildLocationMeta(productIds: Set<Long>): Map<Long, LocationMeta> {
        val meta = mutableMapOf<Long, LocationMeta>()
        productIds.forEach { productId ->
            locationProductPriceRepository.getLocationPricesForProduct(productId).forEach {
                meta.putIfAbsent(
                    it.getLocationId(),
                    LocationMeta(it.getName(), it.getAddress(), it.getLatitude(), it.getLongitude()),
                )
            }
        }
        return meta
    }

    private fun partitionItems(
        items: List<CartItemRequest>,
        productData: Map<Long, ProductPriceData>,
        locationMeta: Map<Long, LocationMeta>,
    ): Pair<List<CartItemRequest>, List<String>> {
        val active = mutableListOf<CartItemRequest>()
        val skipped = mutableListOf<String>()

        items.forEach { item ->
            val data = productData[item.productId]
            if (data == null || isGloballyDiscarded(data, locationMeta)) {
                skipped.add(data?.productName ?: "Producto ${item.productId}")
            } else {
                active.add(item)
            }
        }

        return active to skipped
    }

    private fun isGloballyDiscarded(
        data: ProductPriceData,
        locationMeta: Map<Long, LocationMeta>,
    ): Boolean {
        if (data.globalAvg != null) return false
        if (data.byLocation.isNotEmpty()) return false
        return locationMeta.values.none { meta ->
            if (meta.latitude == null || meta.longitude == null) return@none false
            locationMeta.values.any { other ->
                if (other.latitude == null || other.longitude == null) return@any false
                val dist = haversineMeters(meta.latitude, meta.longitude, other.latitude, other.longitude)
                dist <= nearbyRadiusMeters && data.byLocation.isNotEmpty()
            }
        }
    }

    private fun buildLocationResults(
        request: CartCompareRequest,
        activeItems: List<CartItemRequest>,
        productData: Map<Long, ProductPriceData>,
        locationMeta: Map<Long, LocationMeta>,
    ): List<CartLocationResponse> =
        locationMeta.entries.mapNotNull { (locationId, meta) ->
            val distanceMeters = resolveDistance(request, meta)
            val productResults =
                activeItems.map { item ->
                    resolveProductResult(item, productData[item.productId]!!, locationId, meta)
                }
            val total =
                productResults
                    .filter { it.priceSource != PriceSource.NO_DATA }
                    .sumOf { it.totalPrice }

            CartLocationResponse(
                locationId = locationId,
                name = meta.name,
                address = meta.address,
                totalEstimatedPrice = total,
                distanceMeters = distanceMeters,
                products = productResults,
            )
        }

    private fun resolveDistance(
        request: CartCompareRequest,
        meta: LocationMeta,
    ): Double? {
        if (request.userLatitude == null || request.userLongitude == null) return null
        if (meta.latitude == null || meta.longitude == null) return null
        return haversineMeters(request.userLatitude, request.userLongitude, meta.latitude, meta.longitude)
    }

    private fun resolveProductResult(
        item: CartItemRequest,
        data: ProductPriceData,
        locationId: Long,
        meta: LocationMeta,
    ): CartProductResponse {
        val reportedPrice = data.byLocation[locationId]
        if (reportedPrice != null) {
            return buildProductResult(item, data.productName, reportedPrice, PriceSource.REPORTED)
        }

        val nearbyAvg = resolveNearbyFallback(data, meta)
        if (nearbyAvg != null) {
            return buildProductResult(item, data.productName, nearbyAvg, PriceSource.NEARBY_FALLBACK)
        }

        if (data.globalAvg != null) {
            return buildProductResult(item, data.productName, data.globalAvg, PriceSource.GLOBAL_FALLBACK)
        }

        return buildProductResult(item, data.productName, 0.0, PriceSource.NO_DATA)
    }

    private fun resolveNearbyFallback(
        data: ProductPriceData,
        meta: LocationMeta,
    ): Double? {
        if (meta.latitude == null || meta.longitude == null) return null
        val nearbyPrices =
            data.byLocation.entries.mapNotNull { (otherLocationId, price) ->
                if (meta.latitude == null || meta.longitude == null) return@mapNotNull null
                price
            }
        return if (nearbyPrices.isNotEmpty()) nearbyPrices.average() else null
    }

    private fun buildProductResult(
        item: CartItemRequest,
        name: String,
        unitPrice: Double,
        source: PriceSource,
    ): CartProductResponse =
        CartProductResponse(
            productId = item.productId,
            name = name,
            quantity = item.quantity,
            unitPrice = unitPrice,
            totalPrice = unitPrice * item.quantity,
            priceSource = source,
        )

    private fun haversineMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a =
            Math.pow(Math.sin(dLat / 2), 2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.pow(Math.sin(dLon / 2), 2.0)
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}
