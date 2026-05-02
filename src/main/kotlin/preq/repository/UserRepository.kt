package preq.repository

import org.springframework.data.jpa.repository.JpaRepository
import preq.model.User
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>

    fun existsByEmail(email: String): Boolean
}
