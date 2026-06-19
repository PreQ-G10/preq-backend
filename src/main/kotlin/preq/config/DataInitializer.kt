package preq.config

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import preq.enum.LocationType
import preq.enum.ReportScore
import preq.enum.ReportSource
import preq.enum.UserRole
import preq.model.Location
import preq.model.LocationProductPrice
import preq.model.Product
import preq.model.User
import preq.repository.LocationProductPriceRepository
import preq.repository.LocationRepository
import preq.repository.ProductRepository
import preq.repository.UserRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.random.Random

@Component
class DataInitializer(
    private val productRepository: ProductRepository,
    private val locationRepository: LocationRepository,
    private val locationProductPriceRepository: LocationProductPriceRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {
    private val rng = Random(42)
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    // ─────────────────────────────────────────────────────────
    // User tiers
    // ─────────────────────────────────────────────────────────

    enum class UserTier(
        val trustScore: Double,
        val recoveryMultiplier: Double,
        val avgScore: Double, // average report score for this tier
        val scoreVariance: Double, // how much scores vary
    ) {
        HIGH(0.85, 1.0, 0.92, 0.06),
        GOOD(0.65, 1.0, 0.80, 0.08),
        MID(0.45, 1.0, 0.65, 0.10),
        BORDERLINE(0.30, 0.75, 0.50, 0.12),
        LOW(0.15, 0.5, 0.30, 0.15),
        BAD(0.05, 0.25, 0.10, 0.08),
    }

    // ─────────────────────────────────────────────────────────
    // Seed names
    // ─────────────────────────────────────────────────────────

    private val seedNames =
        listOf(
            "Mateo",
            "Valentina",
            "Santiago",
            "Sofía",
            "Nicolás",
            "Martina",
            "Tomás",
            "Lucía",
            "Benjamín",
            "Emma",
            "Lucas",
            "Camila",
            "Facundo",
            "Florencia",
            "Ignacio",
            "Agustina",
            "Joaquín",
            "Rocío",
            "Sebastián",
            "Pilar",
            "Marcos",
            "Julieta",
            "Andrés",
            "Valeria",
            "Bruno",
            "Milagros",
            "Ezequiel",
            "Natalia",
            "Ramiro",
            "Clara",
        )

    private val seedLastNames =
        listOf(
            "García",
            "Martínez",
            "López",
            "González",
            "Rodríguez",
            "Fernández",
            "Pérez",
            "Sánchez",
            "Romero",
            "Torres",
            "Díaz",
            "Álvarez",
            "Ruiz",
            "Moreno",
            "Muñoz",
            "Alonso",
            "Gutiérrez",
            "Navarro",
            "Molina",
            "Domínguez",
            "Gil",
            "Vázquez",
            "Serrano",
            "Blanco",
            "Ramírez",
            "Herrera",
            "Medina",
            "Suárez",
            "Castro",
            "Ortega",
        )

    // Owners for the seeded business accounts — one per claimed location
    private val businessOwnerNames =
        listOf(
            "Carlos" to "Pereyra",
            "Marisa" to "Acosta",
            "Diego" to "Funes",
            "Patricia" to "Bazán",
        )

    // ─────────────────────────────────────────────────────────
    // Reference prices
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
                "Centro de Estudiantes CyT",
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
        println("DataInitializer: Seeding...")
        val locations = createLocations()
        val users = createUsers()
        val products = productRepository.findAll()
        createPriceReports(products, locations, users)
        val businessUsers = createBusinessAccounts(locations)
        createBusinessCatalogue(products, businessUsers)
        println(
            "DataInitializer: Done. ${locations.size} locations, ${users.size} users, " +
                "${businessUsers.size} business accounts, prices generated for ${products.size} products.",
        )
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
                    coordinates = geometryFactory.createPoint(Coordinate(seed.longitude, seed.latitude))
                },
            )
        }
    }

    // ─────────────────────────────────────────────────────────
    // Step 2 — Users
    // ─────────────────────────────────────────────────────────

    private fun createUsers(): List<User> {
        if (userRepository.count() > 0) {
            println("DataInitializer: Users already seeded, skipping.")
            return userRepository.findAll()
        }

        val tierDistribution =
            listOf(
                UserTier.HIGH to 5,
                UserTier.GOOD to 7,
                UserTier.MID to 7,
                UserTier.BORDERLINE to 5,
                UserTier.LOW to 4,
                UserTier.BAD to 2,
            )

        val users = mutableListOf<User>()
        var index = 1

        tierDistribution.forEach { (tier, count) ->
            repeat(count) {
                val trustVariance = rng.nextDouble(-0.05, 0.05)
                users.add(
                    userRepository.save(
                        User().apply {
                            name = seedNames[index - 1]
                            lastName = seedLastNames[index - 1]
                            email = "seed$index@preq.app"
                            password = passwordEncoder.encode("password")
                            role = UserRole.USER
                            trustScore = (tier.trustScore + trustVariance).coerceIn(0.0, 1.0)
                            recoveryMultiplier = tier.recoveryMultiplier
                        },
                    ),
                )
                index++
            }
        }

        println("DataInitializer: Created ${users.size} seeded users.")
        return users
    }

    // ─────────────────────────────────────────────────────────
    // Step 3 — Price reports
    // ─────────────────────────────────────────────────────────

    private fun createPriceReports(
        products: List<Product>,
        locations: List<Location>,
        users: List<User>,
    ) {
        products.forEach { product ->
            val reference = findReference(product) ?: return@forEach

            val locationCount =
                when {
                    productReferences.count { it.name == product.name && it.brand == product.brand } >= 3 -> rng.nextInt(5, 9)
                    else -> rng.nextInt(3, 7)
                }

            var currentMinPrice: BigDecimal? = null
            var currentMaxPrice: BigDecimal? = null

            locations.shuffled(rng).take(locationCount).forEach { location ->
                val reportCount = rng.nextInt(2, 6)
                generateDates(reportCount).forEach { date ->
                    val user = users.random(rng)
                    val tier = tierForScore(user.trustScore)

                    val daysAgo =
                        java.time.temporal.ChronoUnit.DAYS
                            .between(date, LocalDateTime.now())
                            .toInt()
                    val inflationMultiplier = 1.0 + (daysAgo / 90.0) * 0.08
                    val baseWithInflation = (reference.referencePrice * inflationMultiplier).toInt()

                    // Score-driven price noise — low score reports deviate more
                    val scoreNoise = rng.nextDouble(-0.1, 0.1)
                    val reportScore = (tier.avgScore + scoreNoise).coerceIn(0.0, 1.0)

                    val noise =
                        if (reportScore >= ReportScore.VALID_MIN) {
                            rng.nextInt(-30, 31) * 10
                        } else if (reportScore >= ReportScore.PENDING_MIN) {
                            rng.nextInt(-150, 151) * 10
                        } else {
                            rng.nextInt(45, 80) * 100 * if (rng.nextBoolean()) 1 else -1
                        }
                    val finalPrice = (baseWithInflation + noise).coerceAtLeast(100)

                    if (currentMinPrice == null || BigDecimal(finalPrice) < currentMinPrice) {
                        currentMinPrice = BigDecimal(finalPrice)
                    }
                    if (currentMaxPrice == null || BigDecimal(finalPrice) > currentMaxPrice) {
                        currentMaxPrice = BigDecimal(finalPrice)
                    }

                    // locationConfidence varies realistically
                    val locationConfidence = (0.7 + rng.nextDouble(0.0, 0.3)).coerceIn(0.0, 1.0)

                    locationProductPriceRepository.save(
                        LocationProductPrice().apply {
                            this.product = product
                            this.location = location
                            this.user = user
                            this.price = BigDecimal(finalPrice)
                            this.reportedAt = date
                            this.locationConfidence = locationConfidence
                            this.score = reportScore
                        },
                    )
                }
            }
            productRepository.save(product)
        }
    }

    // ─────────────────────────────────────────────────────────
    // Step 4 — Business accounts
    // ─────────────────────────────────────────────────────────

    private fun createBusinessAccounts(locations: List<Location>): List<User> {
        val existingBusinessUsers = userRepository.findAll().filter { it.role == UserRole.BUSINESS }
        if (existingBusinessUsers.isNotEmpty()) {
            println("DataInitializer: Business accounts already seeded, skipping.")
            return existingBusinessUsers
        }

        // Claim a handful of real (non-"Test") locations, one business per location.
        val claimable = locations.filter { it.name != "Test" }.shuffled(rng).take(businessOwnerNames.size)

        val businessUsers =
            claimable.mapIndexed { i, location ->
                val (firstName, lastNameValue) = businessOwnerNames[i]
                val user =
                    userRepository.save(
                        User().apply {
                            name = firstName
                            lastName = lastNameValue
                            email = "business$i@preq.app"
                            password = passwordEncoder.encode("password")
                            role = UserRole.BUSINESS
                            trustScore = 1.0
                            recoveryMultiplier = 1.0
                        },
                    )
                location.claimedBy = user
                locationRepository.save(location)
                user
            }

        println("DataInitializer: Created ${businessUsers.size} business accounts, each claiming one location.")
        return businessUsers
    }

    // ─────────────────────────────────────────────────────────
    // Step 5 — Business catalogue prices
    // ─────────────────────────────────────────────────────────

    private fun createBusinessCatalogue(
        products: List<Product>,
        businessUsers: List<User>,
    ) {
        if (businessUsers.isEmpty()) return

        val existingCatalogueEntries =
            locationProductPriceRepository.findAll().count { it.source == ReportSource.BUSINESS_CATALOGUE }
        if (existingCatalogueEntries > 0) {
            println("DataInitializer: Business catalogue already seeded, skipping.")
            return
        }

        var created = 0

        businessUsers.forEach { business ->
            val location = business.let { locationRepository.findByClaimedBy(it) } ?: return@forEach

            // Each business lists a subset of the catalogue — not every product.
            val catalogueProducts = products.shuffled(rng).take(rng.nextInt(10, products.size.coerceAtMost(25) + 1))

            catalogueProducts.forEach { product ->
                val reference = findReference(product)
                val basePrice = reference?.referencePrice ?: rng.nextInt(500, 15000)

                // Business-listed prices are direct and trustworthy — small noise only.
                val noise = rng.nextInt(-20, 21) * 10
                val finalPrice = (basePrice + noise).coerceAtLeast(100)

                locationProductPriceRepository.save(
                    LocationProductPrice().apply {
                        this.product = product
                        this.location = location
                        this.user = business
                        this.price = BigDecimal(finalPrice)
                        this.reportedAt = LocalDateTime.now().minusDays(rng.nextLong(0, 14))
                        this.locationConfidence = 1.0
                        this.score = 1.0
                        this.source = ReportSource.BUSINESS_CATALOGUE
                    },
                )
                created++
            }
        }

        println("DataInitializer: Created $created business catalogue price entries across ${businessUsers.size} businesses.")
    }

    private fun tierForScore(score: Double): UserTier =
        when {
            score >= 0.75 -> UserTier.HIGH
            score >= 0.55 -> UserTier.GOOD
            score >= 0.35 -> UserTier.MID
            score >= 0.25 -> UserTier.BORDERLINE
            score >= 0.10 -> UserTier.LOW
            else -> UserTier.BAD
        }

    private fun findReference(product: Product): ProductReference? =
        productReferences.firstOrNull { it.name == product.name && it.brand == product.brand }

    private fun generateDates(count: Int): List<LocalDateTime> =
        (0 until count)
            .map { LocalDateTime.now().minusDays(rng.nextLong(1, 91)) }
            .sortedBy { it }
}
