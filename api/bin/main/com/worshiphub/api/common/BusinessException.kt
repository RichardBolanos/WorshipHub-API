package com.worshiphub.api.common

import org.springframework.http.HttpStatus

sealed class BusinessException(
    message: String,
    val errorCode: String,
    val httpStatus: HttpStatus
) : RuntimeException(message) {
    
    class UserNotFound(userId: String) : BusinessException(
        "User not found: $userId",
        "USER_NOT_FOUND",
        HttpStatus.NOT_FOUND
    )
    
    class ChurchNotFound(churchId: String) : BusinessException(
        "Church not found: $churchId",
        "CHURCH_NOT_FOUND", 
        HttpStatus.NOT_FOUND
    )
    
    class TeamNotFound(teamId: String) : BusinessException(
        "Team not found: $teamId",
        "TEAM_NOT_FOUND",
        HttpStatus.NOT_FOUND
    )
    
    class SongNotFound(songId: String) : BusinessException(
        "Song not found: $songId",
        "SONG_NOT_FOUND",
        HttpStatus.NOT_FOUND
    )
    
    class DuplicateEmail(email: String) : BusinessException(
        "User with email already exists: $email",
        "DUPLICATE_EMAIL",
        HttpStatus.CONFLICT
    )
    
    class InvalidPermission(action: String) : BusinessException(
        "Insufficient permissions for action: $action",
        "INVALID_PERMISSION",
        HttpStatus.FORBIDDEN
    )
    
    class InvalidBusinessRule(rule: String) : BusinessException(
        "Business rule violation: $rule",
        "INVALID_BUSINESS_RULE",
        HttpStatus.BAD_REQUEST
    )
}