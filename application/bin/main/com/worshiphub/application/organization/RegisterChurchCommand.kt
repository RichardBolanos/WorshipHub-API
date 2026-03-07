package com.worshiphub.application.organization

/**
 * Command for registering a new church.
 * 
 * @property name Church name
 * @property address Church address
 * @property email Contact email
 */
data class RegisterChurchCommand(
    val name: String,
    val address: String,
    val email: String
)