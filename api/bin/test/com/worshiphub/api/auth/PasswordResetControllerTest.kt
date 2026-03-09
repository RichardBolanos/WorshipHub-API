import com.worshiphub.api.auth.PasswordResetController
import com.worshiphub.api.auth.ForgotPasswordRequest
import com.worshiphub.api.auth.ResetPasswordRequest
import com.worshiphub.application.auth.PasswordResetResult
import com.worshiphub.application.auth.PasswordResetService
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetControllerTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mvc: MockMvc

    private val passwordResetService: PasswordResetService = mockk()

    private val passwordResetController = PasswordResetController(passwordResetService)

    @Before
    fun setup() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `forgot password - success`() {
        // Given
        every { passwordResetService.initiatePasswordReset(any()) } returns Unit

        val request = ForgotPasswordRequest("test@example.com")

        // When
        val result = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auth/password/forgot")
               .contentType(MediaType.APPLICATION_JSON)
               .content(asJsonString(request))
        )

        // Then
        result.andExpect(MockMvcResultMatchers.status().isOk)
           .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("If the email exists, a password reset link has been sent"))
    }

    @Test
    fun `validate reset token - success`() {
        // Given
        every { passwordResetService.validateResetToken(any()) } returns PasswordResetResult.Success

        // When
        val result = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/auth/password/reset/123/validate")
        )

        // Then
        result.andExpect(MockMvcResultMatchers.status().isOk)
           .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Token is valid"))
    }

    @Test
    fun `validate reset token - invalid token`() {
        // Given
        every { passwordResetService.validateResetToken(any()) } returns PasswordResetResult.InvalidToken

        // When
        val result = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/auth/password/reset/123/validate")
        )

        // Then
        result.andExpect(MockMvcResultMatchers.status().isBadRequest)
           .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Invalid reset token"))
    }

    @Test
    fun `reset password - success`() {
        // Given
        every { passwordResetService.resetPassword(any(), any()) } returns PasswordResetResult.Success

        val request = ResetPasswordRequest("123", "newpassword")

        // When
        val result = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auth/password/reset")
               .contentType(MediaType.APPLICATION_JSON)
               .content(asJsonString(request))
        )

        // Then
        result.andExpect(MockMvcResultMatchers.status().isOk)
           .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Password reset successfully"))
    }

    private fun asJsonString(obj: Any): String {
        return ObjectMapper().writeValueAsString(obj)
    }
}