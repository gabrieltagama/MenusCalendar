package com.gabrieltagama.menuplanner.core.data.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the local PIN verification against the stored SHA-256 digest.
 */
class PinPolicyTest {

    @Test
    fun `correct PIN matches`() = assertTrue(PinPolicy.matches("1234"))

    @Test
    fun `wrong PIN does not match`() = assertFalse(PinPolicy.matches("0000"))

    @Test
    fun `empty PIN does not match`() = assertFalse(PinPolicy.matches(""))

    @Test
    fun `PIN with surrounding whitespace does not match`() = assertFalse(PinPolicy.matches(" 1234 "))

    @Test
    fun `longer PIN with valid prefix does not match`() = assertFalse(PinPolicy.matches("12345"))

    @Test
    fun `raw digest text does not match`() =
        assertFalse(PinPolicy.matches("03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"))
}
