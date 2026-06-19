package preq.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import preq.enum.ReportSource
import preq.exceptions.ProductNotInCatalogueException
import preq.model.Location
import preq.model.LocationProductPrice
import preq.model.User
import preq.repository.LocationProductPriceRepository
import preq.repository.LocationRepository
import preq.repository.ProductRepository
import preq.web.dto.request.CatalogueRequest
import preq.web.dto.request.DeleteFromCatalogueRequest
import preq.web.dto.request.UpdateCataloguePriceRequest
import preq.web.dto.response.CatalogueItemResponse
import preq.web.dto.response.DeleteFromCatalogueResponse
import java.time.LocalDateTime

@Service
class BusinessCatalogueService(
    private val locationRepository: LocationRepository,
    private val locationProductPriceRepository: LocationProductPriceRepository,
    private val productRepository: ProductRepository,
) {
    fun getCatalogue(business: User): List<CatalogueItemResponse> {
        val location = getLocation(business)
        return locationProductPriceRepository
            .findByLocationAndSource(location, ReportSource.BUSINESS_CATALOGUE)
            .map { CatalogueItemResponse.from(it) }
    }

    fun addToCatalogue(
        user: User,
        request: CatalogueRequest,
    ): CatalogueItemResponse {
        val location = getLocation(user)
        val product =
            productRepository
                .findById(request.productId)
                .orElseThrow { EntityNotFoundException("Product ${request.productId} not found") }

        val existing =
            locationProductPriceRepository
                .findByLocationAndProductAndSource(location, product, ReportSource.BUSINESS_CATALOGUE)

        val entry =
            existing ?: LocationProductPrice().also {
                it.location = location
                it.product = product
                it.user = user
                it.source = ReportSource.BUSINESS_CATALOGUE
            }

        entry.price = request.price
        entry.reportedAt = LocalDateTime.now()

        return CatalogueItemResponse.from(locationProductPriceRepository.save(entry))
    }

    fun updatePrices(
        user: User,
        request: UpdateCataloguePriceRequest,
    ): List<CatalogueItemResponse> {
        val location = getLocation(user)

        return request.updates.map { update ->
            val product =
                productRepository
                    .findById(update.productId)
                    .orElseThrow { EntityNotFoundException("Product ${update.productId} not found") }

            val entry =
                locationProductPriceRepository
                    .findByLocationAndProductAndSource(location, product, ReportSource.BUSINESS_CATALOGUE)
                    ?: throw EntityNotFoundException("Product ${update.productId} not in catalogue")

            entry.price = update.price
            entry.reportedAt = LocalDateTime.now()

            CatalogueItemResponse.from(locationProductPriceRepository.save(entry))
        }
    }

    @Transactional
    fun deleteFromCatalogue(
        user: User,
        request: DeleteFromCatalogueRequest,
    ): DeleteFromCatalogueResponse {
        val location = getLocation(user)

        val existingIds =
            locationProductPriceRepository
                .findByLocationAndProductIdInAndSource(location, request.productIds, ReportSource.BUSINESS_CATALOGUE)
                .map { it.product!!.id }
                .toSet()

        val missingIds = request.productIds.toSet() - existingIds
        if (missingIds.isNotEmpty()) {
            throw ProductNotInCatalogueException(missingIds)
        }

        locationProductPriceRepository.deleteByLocationAndProductIdInAndSource(
            location,
            request.productIds,
            ReportSource.BUSINESS_CATALOGUE,
        )

        return DeleteFromCatalogueResponse(deletedProductIds = request.productIds)
    }

    private fun getLocation(user: User): Location =
        locationRepository.findByClaimedBy(user)
            ?: throw EntityNotFoundException("No location found for this business account")
}
