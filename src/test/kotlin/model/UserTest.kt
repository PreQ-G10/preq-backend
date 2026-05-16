package model

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import preq.enum.UserRole
import preq.model.User

class UserTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setup() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    private fun validUser() = User().apply {
        name = "Franco"
        lastName = "Marengo"
        email = "franco@preq.com"
        password = "securepassword"
        trustScore = 0.5
        role = UserRole.USER
    }

    // ── valid ─────────────────────────────────────────────────────────────

    @Test
    fun `valid user passes validation`() {
        val violations = validator.validate(validUser())
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `valid user with address passes validation`() {
        val user = validUser().apply { address = "Av. Corrientes 1234, Buenos Aires" }
        val violations = validator.validate(user)
        assertTrue(violations.isEmpty())
    }

    // ── name ──────────────────────────────────────────────────────────────

    @Test
    fun `blank name fails validation`() {
        val user = validUser().apply { name = "   " }
        val violations = validator.validate(user)
        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `name with numbers fails validation`() {
        val user = validUser().apply { name = "Franco123" }
        val violations = validator.validate(user)
        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `name exceeding 50 characters fails validation`() {
        val user = validUser().apply { name = "a".repeat(51) }
        val violations = validator.validate(user)
        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `name at boundary 50 characters passes validation`() {
        val user = validUser().apply { name = "a".repeat(50) }
        val violations = validator.validate(user)
        assertTrue(violations.none { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `name with accents passes validation`() {
        val user = validUser().apply { name = "José María" }
        val violations = validator.validate(user)
        assertTrue(violations.none { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `name with hyphen passes validation`() {
        val user = validUser().apply { name = "María-José" }
        val violations = validator.validate(user)
        assertTrue(violations.none { it.propertyPath.toString() == "name" })
    }

    // ── lastName ──────────────────────────────────────────────────────────

    @Test
    fun `blank lastName fails validation`() {
        val user = validUser().apply { lastName = "   " }
        val violations = validator.validate(user)
        assertTrue(violations.any { it.propertyPath.toString() == "lastName" })
    }

    @Test
    fun `lastName with numbers fails validation`() {
        val user = validUser().apply { lastName = "Marengo123" }
        val violations = validator.validate(user)
        assertTrue(violations.any { it.propertyPath.toString() == "lastName" })
    }

    @Test
    fun `lastName exceeding 50 characters fails validation`() {
        val user = validUser().apply { lastName = "a".repeat(51) }
        val violations = validator.validate(user)
        assertTrue(violations.any { it.propertyPath.toString() == "lastName" })
    }

    @Test
    fun `lastName at boundary 50 characters passes validation`() {
        val user = validUser().apply { lastName = "a".repeat(50) }
        val violations = validator.validate(user)
        assertTrue(violations.none { it.propertyPath.toString() == "lastName" })
    }

    @Test
    fun `lastName with accents passes validation`() {
        val user = validUser().apply { lastName = "García" }
        val violations = validator.validate(user)
        assertTrue(violations.none { it.propertyPath.toString() == "lastName" })
    }

    // ── email ─────────────────────────────────────────────────────────────

    @Test
    fun `blank email fails validation`() {
        val user = validUser().apply { email = "   " }
        val violations = validator.validate(user)
        assertTrue(violations.any { it.propertyPath.toString() == "email" })
    }

    @Test
    fun `email without at sign fails validation`() {
        val user = validUser().apply { email = "francopreq.com" }
        val violations = validator.validate(user)
        assertTrue(violations.any { it.propertyPath.toString() == "email" })
    }

    @Test
    fun `email without domain fails validation`() {
        val user = validUser().apply { email = "franco@" }
        val violations = validator.validate(user)
        assertTrue(violations.any { it.propertyPath.toString() == "email" })
    }

    @Test
    fun `email without local part fails validation`() {
        val user = validUser().apply { email = "@preq.com" }
        val violations = validator.validate(user)
        assertTrue(violations.any { it.propertyPath.toString() == "email" })
    }

    @Test
    fun `valid email passes validation`() {
        val user = validUser().apply { email = "franco.marengo@preq.com.ar" }
        val violations = validator.validate(user)
        assertTrue(violations.none { it.propertyPath.toString() == "email" })
    }

    // ── password ──────────────────────────────────────────────────────────

    @Test
    fun `blank password fails validation`() {
        val user = validUser().apply { password = "   " }
        val violations = validator.validate(user)
        assertTrue(violations.any { it.propertyPath.toString() == "password" })
    }

    @Test
    fun `password below minimum length fails validation`() {
        val user = validUser().apply { password = "abcd" }
        val violations = validator.validate(user)
        assertTrue(violations.any { it.propertyPath.toString() == "password" })
    }

    @Test
    fun `password at minimum length 5 passes validation`() {
        val user = validUser().apply { password = "abcde" }
        val violations = validator.validate(user)
        assertTrue(violations.none { it.propertyPath.toString() == "password" })
    }

    @Test
    fun `long password passes validation`() {
        val user = validUser().apply { password = "a".repeat(128) }
        val violations = validator.validate(user)
        assertTrue(violations.none { it.propertyPath.toString() == "password" })
    }

    // ── trustScore ────────────────────────────────────────────────────────

    @Test
    fun `trustScore defaults to 0_5`() {
        val user = User().apply {
            name = "Franco"
            lastName = "Marengo"
            email = "franco@preq.com"
            password = "securepassword"
        }
        assertEquals(0.5, user.trustScore)
    }

    @Test
    fun `trustScore below 0 fails validation`() {
        val user = validUser().apply { trustScore = -0.01 }
        val violations = validator.validate(user)
        assertTrue(violations.any { it.propertyPath.toString() == "trustScore" })
    }

    @Test
    fun `trustScore above 1 fails validation`() {
        val user = validUser().apply { trustScore = 1.01 }
        val violations = validator.validate(user)
        assertTrue(violations.any { it.propertyPath.toString() == "trustScore" })
    }

    @Test
    fun `trustScore at boundary 0_0 passes validation`() {
        val user = validUser().apply { trustScore = 0.0 }
        val violations = validator.validate(user)
        assertTrue(violations.none { it.propertyPath.toString() == "trustScore" })
    }

    @Test
    fun `trustScore at boundary 1_0 passes validation`() {
        val user = validUser().apply { trustScore = 1.0 }
        val violations = validator.validate(user)
        assertTrue(violations.none { it.propertyPath.toString() == "trustScore" })
    }

    // ── role ──────────────────────────────────────────────────────────────

    @Test
    fun `role defaults to USER`() {
        val user = User().apply {
            name = "Franco"
            lastName = "Marengo"
            email = "franco@preq.com"
            password = "securepassword"
        }
        assertEquals(UserRole.USER, user.role)
    }

    // ── address ───────────────────────────────────────────────────────────

    @Test
    fun `address is null by default`() {
        val user = User().apply {
            name = "Franco"
            lastName = "Marengo"
            email = "franco@preq.com"
            password = "securepassword"
        }
        assertNull(user.address)
    }

    @Test
    fun `addressLocation is null by default`() {
        val user = User().apply {
            name = "Franco"
            lastName = "Marengo"
            email = "franco@preq.com"
            password = "securepassword"
        }
        assertNull(user.addressLocation)
    }
}