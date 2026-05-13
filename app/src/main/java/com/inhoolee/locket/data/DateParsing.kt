package com.inhoolee.locket.data

import java.time.Instant

fun parseSupabaseInstant(value: String?): Instant =
    value?.let {
        runCatching { Instant.parse(it) }.getOrNull()
    } ?: Instant.now()

fun isoNow(): String = Instant.now().toString()
