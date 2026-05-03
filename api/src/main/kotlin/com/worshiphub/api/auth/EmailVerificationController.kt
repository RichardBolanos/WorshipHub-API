package com.worshiphub.api.auth

import com.worshiphub.api.common.MessageResponse
import com.worshiphub.application.auth.EmailVerificationResult
import com.worshiphub.application.auth.EmailVerificationService
import com.worshiphub.security.SecurityContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@Tag(name = "Email Verification", description = "Email verification operations")
@RestController
@RequestMapping("/api/v1/auth/email")
class EmailVerificationController(
    private val emailVerificationService: EmailVerificationService,
    private val securityContext: SecurityContext
) {

    @Operation(
        summary = "Send email verification",
        description = "Sends verification email to the authenticated user"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Verification email sent"),
        ApiResponse(responseCode = "400", description = "Email already verified"),
        ApiResponse(responseCode = "401", description = "User not authenticated"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @PostMapping("/send-verification")
    @PreAuthorize("isAuthenticated()")
    fun sendEmailVerification(): ResponseEntity<MessageResponse> {
        val userId = securityContext.getCurrentUserId()
        
        return when (val result = emailVerificationService.sendEmailVerification(userId)) {
            is EmailVerificationResult.Success -> 
                ResponseEntity.ok(MessageResponse("Verification email sent successfully"))
            is EmailVerificationResult.AlreadyVerified -> 
                ResponseEntity.badRequest().body(MessageResponse("Email is already verified"))
            is EmailVerificationResult.UserNotFound -> 
                ResponseEntity.notFound().build()
            else -> ResponseEntity.badRequest().body(MessageResponse("Failed to send verification email"))
        }
    }

    @Operation(
        summary = "Verify email address",
        description = "Verifies user email using the token sent via email. Returns an HTML page."
    )
    @SecurityRequirements // No security required — public endpoint
    @GetMapping("/verify/{token}", produces = ["text/html"])
    fun verifyEmail(
        @Parameter(description = "Email verification token", required = true)
        @PathVariable token: String
    ): ResponseEntity<String> {
        return when (emailVerificationService.verifyEmail(token)) {
            is EmailVerificationResult.Success ->
                ResponseEntity.ok(buildVerificationPage(
                    success = true,
                    title = "¡Correo verificado!",
                    message = "Tu cuenta ha sido activada correctamente. Ya puedes iniciar sesión en la aplicación móvil."
                ))
            is EmailVerificationResult.AlreadyVerified ->
                ResponseEntity.ok(buildVerificationPage(
                    success = true,
                    title = "Correo ya verificado",
                    message = "Tu dirección de correo electrónico ya fue verificada anteriormente. Puedes iniciar sesión en la aplicación."
                ))
            is EmailVerificationResult.TokenExpired ->
                ResponseEntity.ok(buildVerificationPage(
                    success = false,
                    title = "Enlace expirado",
                    message = "Este enlace de verificación ha expirado. Por favor, inicia sesión en la app y solicita un nuevo correo de verificación."
                ))
            is EmailVerificationResult.TokenAlreadyUsed ->
                ResponseEntity.ok(buildVerificationPage(
                    success = false,
                    title = "Enlace ya utilizado",
                    message = "Este enlace de verificación ya fue utilizado. Si aún no puedes iniciar sesión, contacta a soporte."
                ))
            is EmailVerificationResult.InvalidToken ->
                ResponseEntity.ok(buildVerificationPage(
                    success = false,
                    title = "Enlace inválido",
                    message = "El enlace de verificación no es válido. Asegúrate de haber usado el enlace correcto del último correo que recibiste."
                ))
            is EmailVerificationResult.UserNotFound ->
                ResponseEntity.ok(buildVerificationPage(
                    success = false,
                    title = "Usuario no encontrado",
                    message = "No se encontró una cuenta asociada a este enlace de verificación."
                ))
        }
    }

    private fun buildVerificationPage(success: Boolean, title: String, message: String): String {
        val accentColor = if (success) "#22c55e" else "#f97316"
        val icon = if (success) "✓" else "✕"
        val iconBg = if (success) "rgba(34,197,94,0.15)" else "rgba(249,115,22,0.15)"
        val iconBorder = if (success) "rgba(34,197,94,0.3)" else "rgba(249,115,22,0.3)"

        return """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$title - WorshipHub</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&display=swap" rel="stylesheet">
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: 'Inter', sans-serif;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            background: linear-gradient(135deg, #0a0a1a 0%, #1a1b3a 50%, #252659 100%);
            padding: 24px;
        }
        .card {
            background: rgba(255,255,255,0.05);
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 24px;
            padding: 48px 40px;
            max-width: 480px;
            width: 100%;
            text-align: center;
            backdrop-filter: blur(20px);
            box-shadow: 0 8px 64px rgba(0,0,0,0.5), 0 0 0 1px rgba(255,255,255,0.05) inset;
            animation: fadeSlideUp 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
        }
        @keyframes fadeSlideUp {
            from { opacity: 0; transform: translateY(24px); }
            to   { opacity: 1; transform: translateY(0); }
        }
        .logo {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 12px;
            margin-bottom: 36px;
        }
        .logo-icon {
            width: 44px; height: 44px;
            border-radius: 12px;
            background: linear-gradient(135deg, #7c3aed, #a855f7);
            display: flex; align-items: center; justify-content: center;
            font-size: 22px;
        }
        .logo-text {
            font-size: 22px; font-weight: 800;
            background: linear-gradient(135deg, #a855f7, #c084fc);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
        }
        .status-icon {
            width: 80px; height: 80px;
            border-radius: 50%;
            background: $iconBg;
            border: 2px solid $iconBorder;
            display: flex; align-items: center; justify-content: center;
            font-size: 36px;
            color: $accentColor;
            margin: 0 auto 28px;
            animation: pulse 2s ease-in-out infinite;
        }
        @keyframes pulse {
            0%, 100% { box-shadow: 0 0 0 0 ${accentColor}33; }
            50%       { box-shadow: 0 0 0 12px ${accentColor}00; }
        }
        h1 {
            font-size: 26px; font-weight: 800;
            color: #f1f5f9;
            margin-bottom: 14px;
            line-height: 1.2;
        }
        p {
            font-size: 16px;
            color: #94a3b8;
            line-height: 1.6;
        }
        .divider {
            margin: 32px auto;
            height: 1px;
            width: 60%;
            background: linear-gradient(90deg, transparent, rgba(168,85,247,0.4), transparent);
        }
        .footer {
            font-size: 13px;
            color: #64748b;
        }
        .footer strong {
            color: #a855f7;
            font-weight: 600;
        }
    </style>
</head>
<body>
    <div class="card">
        <div class="logo">
            <div class="logo-icon">⛪</div>
            <span class="logo-text">WorshipHub</span>
        </div>
        <div class="status-icon">$icon</div>
        <h1>$title</h1>
        <p>$message</p>
        <div class="divider"></div>
        <p class="footer">¿Necesitas ayuda? Contacta a <strong>soporte@worshiphub.com</strong></p>
    </div>
</body>
</html>
        """.trimIndent()
    }

    @Operation(
        summary = "Resend email verification",
        description = "Resends verification email to the user with the given email address"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Verification email sent"),
        ApiResponse(responseCode = "400", description = "Email already verified or failed to send"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @SecurityRequirements // No security required — public endpoint
    @PostMapping("/resend")
    fun resendEmailVerification(@RequestBody request: ResendEmailVerificationRequest): ResponseEntity<MessageResponse> {
        return when (emailVerificationService.resendEmailVerificationByEmail(request.email)) {
            is EmailVerificationResult.Success -> 
                ResponseEntity.ok(MessageResponse("Verification email sent successfully"))
            is EmailVerificationResult.AlreadyVerified -> 
                ResponseEntity.badRequest().body(MessageResponse("Email is already verified"))
            is EmailVerificationResult.UserNotFound -> 
                ResponseEntity.notFound().build()
            else -> ResponseEntity.badRequest().body(MessageResponse("Failed to send verification email"))
        }
    }
}

data class ResendEmailVerificationRequest(
    val email: String
)