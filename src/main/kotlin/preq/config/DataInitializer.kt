package preq.config

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import preq.enum.LocationType
import preq.model.Location
import preq.model.LocationProductPrice
import preq.model.Product
import preq.repository.LocationProductPriceRepository
import preq.repository.LocationRepository
import preq.repository.ProductRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.random.Random

@Component
class DataInitializer(
    private val productRepository: ProductRepository,
    private val locationRepository: LocationRepository,
    private val locationProductPriceRepository: LocationProductPriceRepository,
) : ApplicationRunner {
    private val rng = Random(42)
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    // ─────────────────────────────────────────────────────────
    // Reference prices — used to generate realistic price reports
    // ─────────────────────────────────────────────────────────

    data class ProductReference(
        val name: String,
        val brand: String,
        val referencePrice: Int,
    )

    private val productReferences =
        listOf(
            ProductReference("Sprite", "Sprite", 3350),
            ProductReference("Coca Cola", "Coca Cola", 3400),
            ProductReference("Coca Cola", "Coca Cola", 1800),
            ProductReference("Levite", "Villa del Sur", 2200),
            ProductReference("Baggio Pronto", "Baggio", 2000),
            ProductReference("Aquarius", "Aquarius", 2625),
            ProductReference("Fernet Branca", "Branca", 17100),
            ProductReference("Alma Mora Syrah", "Alma Mora", 5118),
            ProductReference("Finca Las Moras Syrah", "Finca Las Moras", 5400),
            ProductReference("Aceite de Girasol", "Cañuelas", 3775),
            ProductReference("Harina 000", "Caserita", 950),
            ProductReference("Harina 000", "Cañuelas", 1100),
            ProductReference("Pure de Tomate", "Arcor", 1840),
            ProductReference("Arroz Doble Carolina", "Molinos Ala", 1850),
            ProductReference("Yerba Suave", "Unión", 5510),
            ProductReference("Café Dolca Suave", "Nescafé", 13800),
            ProductReference("Café Tostado Molido", "Cabrales", 14267),
            ProductReference("Yerba Mate", "Playadito", 2827),
            ProductReference("Mate Cocido", "Taragüi", 1470),
            ProductReference("Edulcorante Sweet", "Hileret", 3375),
            ProductReference("Mermelada de Frutilla", "Arcor", 4260),
            ProductReference("Pasta de Maní Natural", "Maní King", 4650),
            ProductReference("Pan Artesano Con Masa Madre", "Bimbo", 7500),
            ProductReference("Leche Descremada Proteica", "La Serenisima", 2755),
            ProductReference("Rapiditas Light", "Bimbo", 3673),
            ProductReference("Leche de Almendra", "La Serenisima", 4413),
            ProductReference("Galletitas Pepas", "Trio", 3617),
            ProductReference("Galletitas Avena, Chia y Lino", "Frutigran", 2880),
            ProductReference("Galletitas Oreo Original", "Oreo", 2560),
            ProductReference("Surtidas Diversion", "Arcor", 2825),
            ProductReference("Surtido", "Bagley", 3175),
            ProductReference("Queso Clásico", "La Serenisima", 3773),
            ProductReference("Yogurisimo Griego Natural Sin Endulzar", "La Serenisima", 4243),
            ProductReference("Dulce De Leche Repostero", "La Serenisima", 4329),
            ProductReference("Salchichas Vienissima", "Vienissima", 3113),
            ProductReference("Salchichas Viena Clasica", "Paty", 2840),
            ProductReference("Franuí Amargo", "Franuí", 8450),
            ProductReference("Papas Noisette Clasicas", "McCain", 16462),
            ProductReference("Paty Clasico", "Paty", 8625),
            ProductReference("Chocolatada", "Cindor", 6722),
            ProductReference("Finlandia Light", "La Serenisima", 5100),
            ProductReference("Finlandia Clasico", "La Serenisima", 4989),
        )

    // ─────────────────────────────────────────────────────────
    // Location seed data
    // ─────────────────────────────────────────────────────────

    data class LocationSeed(
        val name: String,
        val address: String,
        val type: LocationType,
        val latitude: Double,
        val longitude: Double,
    )

    private val locationSeeds =
        listOf(
            LocationSeed("Carrefour", "Av Dardo Rocha 849", LocationType.SUPERMARKET, -34.7144208, -58.2979084),
            LocationSeed("Carrefour Express", "Av Mitre 573", LocationType.SUPERMARKET, -34.7202972, -58.2550920),
            LocationSeed("Carrefour Express", "12 de Octubre 520", LocationType.SUPERMARKET, -34.7298602, -58.2638667),
            LocationSeed("Jumbo", "Av Calchaquí 3950", LocationType.SUPERMARKET, -34.7582935, -58.2746276),
            LocationSeed("Jumbo", "Av Mitre 1075", LocationType.SUPERMARKET, -34.7283425, -58.2492215),
            LocationSeed("Dia", "Belgrano 388", LocationType.SUPERMARKET, -34.7117208, -58.2822597),
            LocationSeed("Dia", "Lamadrid 141 Bis", LocationType.SUPERMARKET, -34.7156588, -58.2732754),
            LocationSeed("Coto", "Av Dardo Rocha 251", LocationType.SUPERMARKET, -34.7185918, -58.2913681),
            LocationSeed("Coto", "Humberto Primo 165", LocationType.SUPERMARKET, -34.7248423, -58.2555763),
            LocationSeed("Coto", "Av Hipólito Yrigoyen 380", LocationType.SUPERMARKET, -34.7197752, -58.2622978),
            LocationSeed("Coto", "Av 12 de Octubre 3054", LocationType.SUPERMARKET, -34.7423815, -58.2895529),
            LocationSeed("Test", "Av Test 123", LocationType.SUPERMARKET, -34.75426616419045, -58.282812872353965),
            LocationSeed(
                "Cwentro de Estudiantes CyT",
                "Rodriguez Saenz Peña 352",
                LocationType.STORE,
                -34.705967337092886,
                -58.27784788792694,
            ),
        )

    // ─────────────────────────────────────────────────────────
    // Runner
    // ─────────────────────────────────────────────────────────

    override fun run(args: ApplicationArguments) {
        println("DataInitializer: Seeding locations and prices...")
        val locations = createLocations()
        val products = productRepository.findAll()
        createPriceReports(products, locations)
        println("DataInitializer: Done. ${locations.size} locations, prices generated for ${products.size} products.")
    }

    // ─────────────────────────────────────────────────────────
    // Step 1 — Locations
    // ─────────────────────────────────────────────────────────

    private fun createLocations(): List<Location> {
        if (locationRepository.count() > 0) {
            println("DataInitializer: Locations already seeded, skipping.")
            return locationRepository.findAll()
        }

        return locationSeeds.map { seed ->
            locationRepository.save(
                Location().apply {
                    name = seed.name
                    address = seed.address
                    type = seed.type
                    latitude = seed.latitude
                    longitude = seed.longitude
                    coordinates =
                        geometryFactory.createPoint(
                            Coordinate(seed.longitude, seed.latitude),
                        )
                },
            )
        }
    }

    // ─────────────────────────────────────────────────────────
    // Step 2 — Price reports with dynamic dates
    // ─────────────────────────────────────────────────────────

    private fun createPriceReports(
        products: List<Product>,
        locations: List<Location>,
    ) {
        products.forEach { product ->
            val reference = findReference(product) ?: return@forEach

            val locationCount =
                when {
                    productReferences.count { it.name == product.name && it.brand == product.brand } >= 3 -> rng.nextInt(5, 9)
                    else -> rng.nextInt(3, 7)
                }

            locations.shuffled(rng).take(locationCount).forEach { location ->
                val reportCount = rng.nextInt(1, 4)
                generateDates(reportCount).forEach { date ->
                    val daysAgo =
                        java.time.temporal.ChronoUnit.DAYS
                            .between(date, LocalDateTime.now())
                            .toInt()
                    val inflationMultiplier = 1.0 + (daysAgo / 90.0) * 0.08
                    val baseWithInflation = (reference.referencePrice * inflationMultiplier).toInt()
                    val noise = rng.nextInt(-30, 31) * 10
                    val finalPrice = (baseWithInflation + noise).coerceAtLeast(100)

                    locationProductPriceRepository.save(
                        LocationProductPrice().apply {
                            this.product = product
                            this.location = location
                            this.price = BigDecimal(finalPrice)
                            this.reportedAt = date
                        },
                    )
                }
            }
        }
    }

    private fun findReference(product: Product): ProductReference? =
        productReferences.firstOrNull {
            it.name == product.name && it.brand == product.brand
        }

    private fun generateDates(count: Int): List<LocalDateTime> =
        (0 until count)
            .map { LocalDateTime.now().minusDays(rng.nextLong(1, 91)) }
            .sortedBy { it }
}
