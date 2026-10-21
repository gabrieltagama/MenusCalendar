package com.gabrieltagama.menuplanner.core.domain.text

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Shared Spanish text rules: folding case and accents, collapsing whitespace and splitting words.
 */
class SpanishTextTest {

    @Test
    fun `folds case and accents in one step`() =
        assertEquals("pure de calabacin", "PURÉ de Calabacín".foldedCaseAndAccents())

    @Test
    fun `keeps the letter n with tilde as n`() =
        assertEquals("anade", "Añade".foldedCaseAndAccents())

    @Test
    fun `collapses inner and outer whitespace`() =
        assertEquals("Aceite de oliva", "  Aceite \t de   oliva ".collapsedSpaces())

    @Test
    fun `splits words ignoring extra whitespace`() =
        assertEquals(listOf("diente", "de", "ajo"), " diente  de\najo ".words())
}
