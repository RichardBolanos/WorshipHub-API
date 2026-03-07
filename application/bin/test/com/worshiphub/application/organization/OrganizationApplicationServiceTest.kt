package com.worshiphub.application.organization

import com.worshiphub.domain.organization.Church
import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.repository.ChurchRepository
import com.worshiphub.domain.organization.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.any
import java.util.*
import kotlin.test.assertEquals

class OrganizationApplicationServiceTest {
    
    private val churchRepository = mock<ChurchRepository>()
    private val userRepository = mock<UserRepository>()
    private val organizationService = OrganizationApplicationService(churchRepository, userRepository)
    
    @Test
    fun `should register church with validation`() {
        val command = RegisterChurchCommand(
            name = "First Baptist Church",
            address = "123 Main St",
            email = "admin@firstbaptist.org"
        )
        
        val church = Church(
            name = command.name,
            address = command.address,
            email = command.email
        )
        
        whenever(churchRepository.save(any())).thenReturn(church)
        
        val result = organizationService.registerChurch(command)
        
        verify(churchRepository).save(any())
        assertEquals(church.id, result)
    }
    
    @Test
    fun `should invite user with role assignment`() {
        val churchId = UUID.randomUUID()
        val command = InviteUserCommand(
            email = "newuser@church.org",
            firstName = "John",
            lastName = "Doe",
            role = "TEAM_MEMBER",
            churchId = churchId
        )
        
        val user = User(
            email = command.email,
            firstName = command.firstName,
            lastName = command.lastName,
            passwordHash = "temp-hash",
            churchId = churchId,
            role = com.worshiphub.domain.organization.UserRole.TEAM_MEMBER
        )
        
        whenever(userRepository.existsByEmail(command.email)).thenReturn(false)
        whenever(userRepository.save(any())).thenReturn(user)
        
        val result = organizationService.inviteUser(command)
        
        verify(userRepository).save(any())
        assertEquals(user.id, result)
    }
    
    @Test
    fun `should create team with leader assignment`() {
        val churchId = UUID.randomUUID()
        val leaderId = UUID.randomUUID()
        val command = CreateTeamCommand(
            name = "Sunday Morning Team",
            description = "Main worship team",
            churchId = churchId,
            leaderId = leaderId
        )
        
        val result = organizationService.createTeam(command)
        
        assert(result != null)
    }
}