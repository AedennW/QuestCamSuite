package com.camerasuite.quest

import com.camerasuite.quest.storage.FileNamer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNamerTest {

    private val vars = FileNamer.Vars(
        mode = "PHOTO", ext = "jpg", date = "2026-02-14", time = "21-30-05", counter = 7
    )

    @Test
    fun `default template renders all tokens`() {
        val out = FileNamer.render(FileNamer.DEFAULT_TEMPLATE, vars)
        assertEquals("QCS_photo_2026-02-14_21-30-05_007.jpg", out)
    }

    @Test
    fun `blank template falls back to default`() {
        val out = FileNamer.render("", vars)
        assertTrue(out.endsWith(".jpg"))
        assertTrue(out.contains("2026-02-14"))
    }

    @Test
    fun `extension is never duplicated`() {
        val out = FileNamer.render("holiday.jpg", vars)
        assertEquals("holiday.jpg", out)
    }

    @Test
    fun `unsafe characters are sanitised`() {
        val out = FileNamer.render("my trip/{mode}: shot", vars)
        assertTrue(out.matches(Regex("[A-Za-z0-9._-]+")))
        assertTrue(out.endsWith(".jpg"))
    }

    @Test
    fun `counter is padded to three digits`() {
        val out = FileNamer.render("n_{counter}", vars.copy(counter = 42))
        assertEquals("n_042.jpg", out)
    }
}
