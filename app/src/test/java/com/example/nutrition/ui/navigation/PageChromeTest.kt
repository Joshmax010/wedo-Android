package com.example.nutrition.ui.navigation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class PageChromeTest {
    @Test fun navigatingToANewPageStartsWithAnExpandedTitle() {
        val chrome = PageChrome(8f)
        chrome.collapsed = true
        chrome.visible = false
        chrome.reset()
        assertFalse(chrome.collapsed)
        assertEquals(0f, chrome.titleCollapseFraction, 0.001f)
        assertTrue(chrome.visible)
    }

    @Test fun onlyConsumedContentScrollChangesTitleSize() {
        val chrome = PageChrome(8f)
        chrome.updateTitleScroll(20f, 40f)
        assertEquals(0.5f, chrome.titleCollapseFraction, 0.001f)
        chrome.scrollConnection.onPreScroll(Offset(0f, -16f), NestedScrollSource.UserInput)
        assertFalse(chrome.visible)
        assertEquals(0.5f, chrome.titleCollapseFraction, 0.001f)
    }

    @Test fun recycledLazyTitleStaysCollapsedWhenNavigationHides() {
        val chrome = PageChrome(8f)
        chrome.updateTitleScroll(Float.POSITIVE_INFINITY, 40f)
        chrome.scrollConnection.onPreScroll(Offset(0f, -16f), NestedScrollSource.UserInput)
        assertFalse(chrome.visible)
        assertTrue(chrome.collapsed)
        assertEquals(1f, chrome.titleCollapseFraction, 0.001f)
    }

    @Test fun returningToTheTopRestoresTitleAndNavigation() {
        val chrome = PageChrome(8f)
        chrome.updateTitleScroll(100f, 40f)
        chrome.scrollConnection.onPreScroll(Offset(0f, -16f), NestedScrollSource.UserInput)
        chrome.updateTitleScroll(0f, 40f)
        assertFalse(chrome.collapsed)
        assertEquals(0f, chrome.titleCollapseFraction, 0.001f)
        assertTrue(chrome.visible)
    }

    @Test fun restoredPageUsesItsSavedScrollPosition() {
        val chrome = PageChrome(8f)
        chrome.updateTitleScroll(Float.POSITIVE_INFINITY, 40f)
        chrome.reset()
        chrome.updateTitleScroll(60f, 40f)
        assertTrue(chrome.collapsed)
        assertEquals(1f, chrome.titleCollapseFraction, 0.001f)
    }

    @Test fun largerTextUsesALongerCollapseRangeAndClampsOverscroll() {
        val chrome = PageChrome(8f)
        chrome.updateTitleScroll(20f, 80f)
        assertEquals(0.25f, chrome.titleCollapseFraction, 0.001f)
        chrome.updateTitleScroll(-10f, 80f)
        assertEquals(0f, chrome.titleCollapseFraction, 0.001f)
        assertFalse(chrome.collapsed)
    }

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
