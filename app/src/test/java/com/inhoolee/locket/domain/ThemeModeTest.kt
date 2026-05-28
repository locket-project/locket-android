package com.inhoolee.locket.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun fromWireMapsKnownValues() {
        assertEquals(ThemeMode.System, ThemeMode.fromWire("system"))
        assertEquals(ThemeMode.Light, ThemeMode.fromWire("light"))
        assertEquals(ThemeMode.Dark, ThemeMode.fromWire("dark"))
    }

    @Test
    fun fromWireDefaultsToSystem() {
        assertEquals(ThemeMode.System, ThemeMode.fromWire(null))
        assertEquals(ThemeMode.System, ThemeMode.fromWire(""))
        assertEquals(ThemeMode.System, ThemeMode.fromWire("unknown"))
    }
}
