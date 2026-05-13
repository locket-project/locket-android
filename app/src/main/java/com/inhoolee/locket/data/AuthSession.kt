package com.inhoolee.locket.data

import java.time.Clock
import java.time.Instant

data class AuthSession(
    val accessToken: String,
    val refreshToken: String?,
    val userId: String,
    val email: String?,
    val expiresAt: Instant?
) {
    fun isExpired(clock: Clock = Clock.systemUTC()): Boolean =
        expiresAt?.let { it <= Instant.now(clock).plusSeconds(60) } ?: false
}
