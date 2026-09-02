package com.heartbeat.sms

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsSenderTest {
    @Test
    fun parsesNumbersIgnoringBlankLinesAndWhitespace() {
        val raw = "+79001112233\n\n  +79004445566 \n\n"
        assertEquals(listOf("+79001112233", "+79004445566"), parseNumbers(raw))
    }

    @Test
    fun emptyInputYieldsEmptyList() {
        assertEquals(emptyList<String>(), parseNumbers(""))
    }
}
