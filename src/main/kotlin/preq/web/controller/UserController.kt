package preq.web.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import preq.service.UserService
import preq.web.dto.request.UpdateUserRequest
import preq.web.dto.response.UserProfileResponse
import java.security.Principal

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/me")
    fun getProfile(principal: Principal): UserProfileResponse =
        userService.getProfile(principal.name)

    @PutMapping("/me")
    fun updateProfile(
        @RequestBody request: UpdateUserRequest,
        principal: Principal,
    ): UserProfileResponse = userService.updateProfile(principal.name, request)
}