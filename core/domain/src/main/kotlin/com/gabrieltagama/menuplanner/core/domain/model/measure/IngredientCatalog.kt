package com.gabrieltagama.menuplanner.core.domain.model.measure

import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit

/**
 * Estimated data of common ingredients used to convert kitchen measures: whether the product is
 * liquid (measured in millilitres) or solid (grams), what one unit weighs, and the amount that a
 * pinch or "to taste" stands for. Lookups go by IngredientKey, so plurals and accents do not
 * matter. Unknown ingredients use the defaults and take their substance from the other lines.
 */
enum class Substance(val standardUnit: MeasureUnit) {
    SOLID(MeasureUnit.GRAM),
    LIQUID(MeasureUnit.MILLILITER)
}

data class IngredientProfile(
    val substance: Substance,
    val amountPerUnit: Double? = null,
    val toTasteAmount: Double? = null
)

object IngredientCatalog {
    const val DEFAULT_AMOUNT_PER_UNIT = 100.0
    const val DEFAULT_TO_TASTE_AMOUNT = 10.0

    fun profileOf(key: IngredientKey): IngredientProfile? = profiles[key]

    private val profiles: Map<IngredientKey, IngredientProfile> = listOf(
        liquid("Aceite de oliva", toTaste = 10.0),
        liquid("Aceite", toTaste = 10.0),
        liquid("Aceite para freír", toTaste = 250.0),
        liquid("Vinagre", toTaste = 10.0),
        liquid("Leche"),
        liquid("Nata para montar"),
        liquid("Vino blanco"),
        liquid("Vino tinto"),
        liquid("Anís"),
        liquid("Caldo"),
        liquid("Caldo de pollo"),
        liquid("Caldo de pescado"),
        liquid("Caldo de verduras"),
        liquid("Caldo de cocido"),
        solid("Sal", toTaste = 5.0),
        solid("Sal gruesa", toTaste = 10.0),
        solid("Pimienta", toTaste = 2.0),
        solid("Perejil", toTaste = 10.0),
        solid("Canela molida", toTaste = 2.0),
        solid("Nuez moscada", toTaste = 1.0),
        solid("Colorante o azafrán", toTaste = 1.0),
        solid("Orégano", toTaste = 2.0),
        solid("Azúcar", toTaste = 5.0),
        solid("Azúcar glas", toTaste = 10.0),
        solid("Queso rallado", toTaste = 30.0),
        solid("Ralladura de limón", toTaste = 5.0),
        solid("Ralladura de limón o naranja", toTaste = 5.0),
        solid("Alioli", toTaste = 30.0),
        solid("Kétchup y mostaza", toTaste = 30.0),
        solid("Huevo", perUnit = 60.0),
        solid("Yema de huevo", perUnit = 20.0),
        solid("Cebolla", perUnit = 150.0),
        solid("Cebolla grande", perUnit = 250.0),
        solid("Patata", perUnit = 200.0),
        solid("Tomate", perUnit = 150.0),
        solid("Zanahoria", perUnit = 80.0),
        solid("Pimiento rojo", perUnit = 200.0),
        solid("Pimiento verde", perUnit = 150.0),
        solid("Diente de ajo", perUnit = 5.0),
        solid("Puerro", perUnit = 150.0),
        solid("Calabacín", perUnit = 250.0),
        solid("Limón", perUnit = 120.0),
        solid("Lechuga", perUnit = 400.0),
        solid("Repollo", perUnit = 1000.0),
        solid("Brócoli", perUnit = 400.0),
        solid("Apio", perUnit = 40.0),
        solid("Plátano", perUnit = 120.0),
        solid("Chorizo", perUnit = 200.0),
        solid("Morcilla", perUnit = 150.0),
        solid("Salchicha", perUnit = 50.0),
        solid("Pechuga de pollo", perUnit = 250.0),
        solid("Loncha de queso", perUnit = 20.0),
        solid("Loncha de jamón york", perUnit = 20.0),
        solid("Rebanada de pan de molde", perUnit = 25.0),
        solid("Pan", perUnit = 250.0),
        solid("Atún en lata", perUnit = 80.0),
        solid("Bote de tomate frito", perUnit = 400.0),
        solid("Sobre de levadura", perUnit = 16.0),
        solid("Quesito", perUnit = 16.0),
        solid("Galleta maría", perUnit = 6.0),
        solid("Hoja de laurel", perUnit = 0.2),
        solid("Rama de canela", perUnit = 3.0)
    ).toMap()

    private fun liquid(name: String, perUnit: Double? = null, toTaste: Double? = null) =
        IngredientKey.of(name) to IngredientProfile(Substance.LIQUID, perUnit, toTaste)

    private fun solid(name: String, perUnit: Double? = null, toTaste: Double? = null) =
        IngredientKey.of(name) to IngredientProfile(Substance.SOLID, perUnit, toTaste)
}
