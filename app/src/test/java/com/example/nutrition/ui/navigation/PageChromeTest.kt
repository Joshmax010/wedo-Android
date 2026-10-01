package com.example.nutrition.ui.navigation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class PageChromeTest {
    @Test fun navigationVisibilityDoesNotConsumeContentScroll() {
        val chrome = PageChrome(8f)
        assertEquals(Offset.Zero, chrome.scrollConnection.onPreScroll(Offset(0f, -16f), NestedScrollSource.UserInput))
        assertFalse(chrome.visible)
        assertEquals(Offset.Zero, chrome.scrollConnection.onPreScroll(Offset(0f, 16f), NestedScrollSource.UserInput))
        assertTrue(chrome.visible)
    }

    @Test fun smallDirectionChangesDoNotFlickerNavigation() {
        val chrome = PageChrome(8f)
        chrome.scrollConnection.onPreScroll(Offset(0f, -6f), NestedScrollSource.UserInput)
        chrome.scrollConnection.onPreScroll(Offset(0f, 2f), NestedScrollSource.UserInput)
        chrome.scrollConnection.onPreScroll(Offset(0f, -6f), NestedScrollSource.UserInput)
        assertTrue(chrome.visible)
        chrome.scrollConnection.onPreScroll(Offset(0f, -2f), NestedScrollSource.UserInput)
        assertFalse(chrome.visible)
    }

    @Test fun editingAndKeyboardKeepNavigationAvailable() {
        val chrome = PageChrome(8f)
        chrome.editing = true
        chrome.scrollConnection.onPreScroll(Offset(0f, -16f), NestedScrollSource.UserInput)
        assertTrue(chrome.visible)
        chrome.editing = false
        chrome.keyboardOpen = true
        chrome.scrollConnection.onPreScroll(Offset(0f, -16f), NestedScrollSource.UserInput)
        assertTrue(chrome.visible)
    }
}
