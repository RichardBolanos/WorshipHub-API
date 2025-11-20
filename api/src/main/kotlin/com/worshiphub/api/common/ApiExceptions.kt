package com.worshiphub.api.common

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class BadRequestException(message: String) : ResponseStatusException(HttpStatus.BAD_REQUEST, message)
class NotFoundException(message: String) : ResponseStatusException(HttpStatus.NOT_FOUND, message)
class ConflictException(message: String) : ResponseStatusException(HttpStatus.CONFLICT, message)