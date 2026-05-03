package com.example.dogmap.data.preferences

import org.junit.Assert.*
import org.junit.Test

class NotificationSettingsTest {

    @Test fun `default values`() {
        val settings = NotificationSettings()
        assertTrue(settings.globalEnabled)
        assertTrue(settings.socialEnabled)
        assertTrue(settings.remindersEnabled)
        assertEquals(-1, settings.dndStartHour)
        assertEquals(-1, settings.dndEndHour)
    }

    @Test fun `constructor stores provided values`() {
        val settings = NotificationSettings(
            globalEnabled = false,
            socialEnabled = false,
            remindersEnabled = false,
            dndStartHour = 22,
            dndEndHour = 8
        )
        assertFalse(settings.globalEnabled)
        assertFalse(settings.socialEnabled)
        assertFalse(settings.remindersEnabled)
        assertEquals(22, settings.dndStartHour)
        assertEquals(8, settings.dndEndHour)
    }

    @Test fun `copy changes only globalEnabled`() {
        val original = NotificationSettings()
        val copy = original.copy(globalEnabled = false)
        assertFalse(copy.globalEnabled)
        assertTrue(copy.socialEnabled)
        assertTrue(copy.remindersEnabled)
        assertEquals(-1, copy.dndStartHour)
        assertEquals(-1, copy.dndEndHour)
    }

    @Test fun `copy changes only socialEnabled`() {
        val original = NotificationSettings()
        val copy = original.copy(socialEnabled = false)
        assertTrue(copy.globalEnabled)
        assertFalse(copy.socialEnabled)
        assertTrue(copy.remindersEnabled)
    }

    @Test fun `copy changes only remindersEnabled`() {
        val original = NotificationSettings()
        val copy = original.copy(remindersEnabled = false)
        assertTrue(copy.globalEnabled)
        assertTrue(copy.socialEnabled)
        assertFalse(copy.remindersEnabled)
    }

    @Test fun `copy changes dnd hours`() {
        val original = NotificationSettings()
        val copy = original.copy(dndStartHour = 23, dndEndHour = 7)
        assertEquals(23, copy.dndStartHour)
        assertEquals(7, copy.dndEndHour)
    }

    @Test fun `equality based on all fields`() {
        val a = NotificationSettings(globalEnabled = true, socialEnabled = false, remindersEnabled = true, dndStartHour = 22, dndEndHour = 8)
        val b = NotificationSettings(globalEnabled = true, socialEnabled = false, remindersEnabled = true, dndStartHour = 22, dndEndHour = 8)
        assertEquals(a, b)
    }

    @Test fun `inequality when one field differs`() {
        val a = NotificationSettings(dndStartHour = 22)
        val b = NotificationSettings(dndStartHour = 23)
        assertNotEquals(a, b)
    }

    @Test fun `hashCode consistent with equality`() {
        val a = NotificationSettings(dndStartHour = 22, dndEndHour = 8)
        val b = NotificationSettings(dndStartHour = 22, dndEndHour = 8)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
