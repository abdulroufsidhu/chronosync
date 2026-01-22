package com.chronosync.dto.common

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

data class ErrorResponse(
    val success: Boolean,
    val message: String,
    val errors: List<String>? = null
)
