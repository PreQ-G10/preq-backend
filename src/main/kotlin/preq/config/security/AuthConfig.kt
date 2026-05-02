package preq.config.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import preq.repository.UserRepository

@Configuration
open class AuthConfig(
    private val userRepository: UserRepository,
) {
    @Bean
    open fun userDetailsService(): UserDetailsService =
        UserDetailsService { email ->
            userRepository
                .findByEmail(email)
                .map { user ->
                    org.springframework.security.core.userdetails.User
                        .withUsername(user.email)
                        .password(user.password)
                        .roles(user.role.name)
                        .build()
                }.orElseThrow { UsernameNotFoundException("User not found: $email") }
        }

    @Bean
    open fun authenticationProvider(): AuthenticationProvider =
        DaoAuthenticationProvider().apply {
            setUserDetailsService(userDetailsService())
            setPasswordEncoder(passwordEncoder())
        }

    @Bean
    open fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
