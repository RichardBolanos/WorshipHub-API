package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.organization.Team
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

// @Repository
// interface TeamRepository : JpaRepository<Team, UUID> {
//     fun findByChurchId(churchId: UUID): List<Team>
// }