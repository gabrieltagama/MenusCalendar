package com.gabrieltagama.menuplanner.feature.calendar.month

import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * AutoFillMessageFormatter over Spanish texts that mimic the plurals resources: empty month,
 * missing dishes, plain fill and every optional part of the summary.
 */
class AutoFillMessageFormatterTest {

    private val texts = object : AutoFillTexts {
        override val nothingToFill = "No hay días vacíos en este mes"
        override val notEnoughDishes = "No hay platos suficientes: crea primeros y segundos o platos únicos"
        override fun filled(count: Int) = if (count == 1) "Rellenado 1 día" else "Rellenados $count días"
        override fun repeatedDish(count: Int) = if (count == 1) "1 repite plato" else "$count repiten plato"
        override fun heavyStreak(count: Int) = "$count con dos días pesados seguidos"
        override fun unfilled(count: Int) = "$count sin rellenar (faltan platos)"
    }

    private val formatter = AutoFillMessageFormatter(texts)

    @Test
    fun `nothing filled and nothing missing means no empty days`() =
        assertEquals("No hay días vacíos en este mes", formatter.format(AutoFillResult(0, 0, 0, 0)))

    @Test
    fun `nothing filled with empty days means not enough dishes`() =
        assertEquals(
            "No hay platos suficientes: crea primeros y segundos o platos únicos",
            formatter.format(AutoFillResult(0, 0, 0, 12))
        )

    @Test
    fun `plain fill shows only filled days`() =
        assertEquals("Rellenados 18 días", formatter.format(AutoFillResult(18, 0, 0, 0)))

    @Test
    fun `single filled day uses singular`() =
        assertEquals("Rellenado 1 día", formatter.format(AutoFillResult(1, 0, 0, 0)))

    @Test
    fun `all optional parts are appended in order`() =
        assertEquals(
            "Rellenados 18 días, 2 repiten plato, 1 con dos días pesados seguidos, 3 sin rellenar (faltan platos)",
            formatter.format(AutoFillResult(18, 2, 1, 3))
        )

    @Test
    fun `zero counts are omitted`() =
        assertEquals("Rellenados 5 días, 1 repite plato", formatter.format(AutoFillResult(5, 1, 0, 0)))
}
