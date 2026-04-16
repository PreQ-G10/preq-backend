package preq.config

import com.pgvector.PGvector
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
open class PGvectorConfig(private val dataSource: DataSource) {

    @PostConstruct
    fun init() {
        val hikari = dataSource as com.zaxxer.hikari.HikariDataSource
        repeat(hikari.maximumPoolSize) {
            try {
                hikari.connection.use { conn ->
                    conn.unwrap(org.postgresql.PGConnection::class.java)
                        .addDataType("vector", PGvector::class.java)
                }
            } catch (e: Exception) {
                println("Warning: ${e.message}")
            }
        }
        println("PGvector registered on all pool connections")
    }
}