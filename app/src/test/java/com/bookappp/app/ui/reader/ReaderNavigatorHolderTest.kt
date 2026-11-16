package com.bookappp.app.ui.reader

import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.readium.r2.navigator.Navigator

/** Guards the book guard that stops one book's locators reaching another book's page. */
class ReaderNavigatorHolderTest {

    private val navigator = mockk<Navigator>(relaxed = true)

    @After
    fun tearDown() {
        ReaderNavigatorHolder.detach(navigator)
    }

    @Test
    fun `navigator is only offered to the book it renders`() {
        ReaderNavigatorHolder.attach(bookId = 7L, navigator = navigator)

        assertSame(navigator, ReaderNavigatorHolder.navigatorFor(7L))
        assertNull(ReaderNavigatorHolder.navigatorFor(8L))
    }

    @Test
    fun `detaching one navigator keeps another book's navigator`() {
        ReaderNavigatorHolder.attach(bookId = 7L, navigator = navigator)
        val other = mockk<Navigator>(relaxed = true)
        ReaderNavigatorHolder.attach(bookId = 8L, navigator = other)

        ReaderNavigatorHolder.detach(navigator)

        assertSame(other, ReaderNavigatorHolder.navigatorFor(8L))
        assertNull(ReaderNavigatorHolder.navigatorFor(7L))
        ReaderNavigatorHolder.detach(other)
    }
}
