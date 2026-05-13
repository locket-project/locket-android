package com.inhoolee.locket.data

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionTest {
    private val clock = Clock.fixed(Instant.parse("2026-05-12T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun sessionWithoutExpiryIsValid() {
        val session = session(expiresAt = null)

        assertFalse(session.isExpired(clock))
    }

    @Test
    fun sessionWithinRefreshWindowIsExpired() {
        val session = session(expiresAt = Instant.parse("2026-05-12T00:00:30Z"))

        assertTrue(session.isExpired(clock))
    }

    @Test
    fun sessionBeyondRefreshWindowIsValid() {
        val session = session(expiresAt = Instant.parse("2026-05-12T00:02:00Z"))

        assertFalse(session.isExpired(clock))
    }

    private fun session(expiresAt: Instant?) = AuthSession(
        accessToken = "access",
        refreshToken = "refresh",
        userId = "user",
        email = "user@example.com",
        expiresAt = expiresAt
    )
}
