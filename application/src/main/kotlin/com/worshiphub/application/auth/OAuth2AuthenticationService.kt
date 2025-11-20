package com.worshiphub.application.auth

import com.worshiphub.domain.auth.repository.InvitationTokenRepository
import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.repository.ChurchRepository
import com.worshiphub.domain.organization.repository.UserRepository
// Removed direct dependency on JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class OAuth2AuthenticationService(
    private val userRepository: UserRepository,
    private val invitationTokenRepository: InvitationTokenRepository,
    private val churchRepository: ChurchRepository,
    private val jwtTokenService: JwtTokenService
) {

    fun handleGoogleLogin(email: String, fullName: String, givenName: String, familyName: String): OAuth2LoginResult {
        return try {
            // Validate inputs
            require(email.isNotBlank() && email.contains("@")) { "Valid email is required" }
            
            // Check if user already exists
            val existingUser = userRepository.findByEmail(email)
            if (existingUser != null) {
                val token = jwtTokenService.generateToken(
                    existingUser.id.toString(), 
                    existingUser.churchId.toString(), 
                    listOf(existingUser.role.name)
                )
                return OAuth2LoginResult.Success(
                    token = token,
                    userId = existingUser.id.toString(),
                    pendingInvitations = emptyList()
                )
            }

            // Check for pending invitations
            val pendingInvitations = invitationTokenRepository.findValidInvitationsByEmail(email)
            
            if (pendingInvitations.isNotEmpty()) {
                val invitationDtos = pendingInvitations.mapNotNull { invitation ->
                    try {
                        val church = churchRepository.findById(invitation.churchId)
                        val inviter = userRepository.findById(invitation.invitedBy)
                        
                        PendingInvitationDto(
                            id = invitation.id.toString(),
                            churchName = church?.name?.take(100) ?: "Unknown Church",
                            role = invitation.role.name,
                            invitedBy = "${inviter?.firstName?.take(50) ?: ""} ${inviter?.lastName?.take(50) ?: ""}".trim(),
                            expiresAt = invitation.expiresAt.toString()
                        )
                    } catch (e: Exception) {
                        null // Skip invalid invitations
                    }
                }
                OAuth2LoginResult.PendingInvitations(invitationDtos)
            } else {
                OAuth2LoginResult.NoInvitations
            }
        } catch (e: Exception) {
            OAuth2LoginResult.NoInvitations
        }
    }

    fun acceptInvitationAfterOAuth(invitationId: String, email: String): OAuth2LoginResult {
        return try {
            // Validate inputs
            require(invitationId.isNotBlank()) { "Invitation ID is required" }
            require(email.isNotBlank() && email.contains("@")) { "Valid email is required" }
            
            val invitationUuid = try {
                UUID.fromString(invitationId)
            } catch (e: IllegalArgumentException) {
                return OAuth2LoginResult.NoInvitations
            }

            val invitation = invitationTokenRepository.findById(invitationUuid)
                ?: return OAuth2LoginResult.NoInvitations

            if (!invitation.isValid() || invitation.email != email) {
                return OAuth2LoginResult.NoInvitations
            }

            // Check if user already exists (race condition protection)
            if (userRepository.existsByEmail(email)) {
                return OAuth2LoginResult.NoInvitations
            }

            // Create user account with validated data
            val user = User(
                email = invitation.email,
                firstName = invitation.firstName.take(50),
                lastName = invitation.lastName.take(50),
                passwordHash = "", // No password needed for OAuth users
                churchId = invitation.churchId,
                role = invitation.role,
                isActive = true,
                isEmailVerified = true // Email verified by Google
            )

            val savedUser = userRepository.save(user)

            // Mark invitation as used
            invitationTokenRepository.save(invitation.markAsUsed())

            // Generate JWT token with proper parameters
            val token = jwtTokenService.generateToken(
                savedUser.id.toString(), 
                savedUser.churchId.toString(), 
                listOf(savedUser.role.name)
            )

            OAuth2LoginResult.Success(
                token = token,
                userId = savedUser.id.toString(),
                pendingInvitations = emptyList()
            )
        } catch (e: Exception) {
            OAuth2LoginResult.NoInvitations
        }
    }
}

sealed class OAuth2LoginResult {
    data class Success(
        val token: String,
        val userId: String,
        val pendingInvitations: List<PendingInvitationDto>
    ) : OAuth2LoginResult()
    
    data class PendingInvitations(
        val invitations: List<PendingInvitationDto>
    ) : OAuth2LoginResult()
    
    object NoInvitations : OAuth2LoginResult()
}

data class PendingInvitationDto(
    val id: String,
    val churchName: String,
    val role: String,
    val invitedBy: String,
    val expiresAt: String
)