package com.gabrieltagama.menuplanner.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.ui.text.label
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.core.ui.theme.colors

/**
 * Shared visual vocabulary of dishes: an icon and color pair per dish type (avatar), a colored
 * pill per heaviness (chip) and a small heaviness dot for compact places like calendar cells.
 */
val DishType.icon: ImageVector
    get() = when (this) {
        DishType.STARTER -> Icons.Filled.RamenDining
        DishType.MAIN -> Icons.Filled.DinnerDining
        DishType.SINGLE -> Icons.Filled.LunchDining
        DishType.DESSERT -> Icons.Filled.Icecream
    }

@Composable
fun DishType.containerColor(): Color = with(MaterialTheme.colorScheme) {
    when (this@containerColor) {
        DishType.STARTER -> secondaryContainer
        DishType.MAIN -> primaryContainer
        DishType.SINGLE -> tertiaryContainer
        DishType.DESSERT -> surfaceContainerHighest
    }
}

@Composable
fun DishType.contentColor(): Color = with(MaterialTheme.colorScheme) {
    when (this@contentColor) {
        DishType.STARTER -> onSecondaryContainer
        DishType.MAIN -> onPrimaryContainer
        DishType.SINGLE -> onTertiaryContainer
        DishType.DESSERT -> onSurfaceVariant
    }
}

@Composable
fun DishTypeAvatar(type: DishType, modifier: Modifier = Modifier, size: Dp = 40.dp) = Box(
    modifier = modifier.size(size).background(type.containerColor(), CircleShape),
    contentAlignment = Alignment.Center
) {
    Icon(imageVector = type.icon, contentDescription = null, tint = type.contentColor(), modifier = Modifier.size(size * 0.55f))
}

@Composable
fun HeavinessDot(heaviness: Heaviness, modifier: Modifier = Modifier, size: Dp = 8.dp) =
    Box(modifier = modifier.size(size).background(heaviness.colors().accent, CircleShape))

@Composable
fun HeavinessChip(heaviness: Heaviness, modifier: Modifier = Modifier) {
    val colors = heaviness.colors()
    Row(
        modifier = modifier
            .background(colors.container, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeavinessDot(heaviness = heaviness, size = 6.dp)
        Text(
            text = heaviness.label(),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DishVisualsPreview() = MenuPlannerTheme {
    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        DishType.entries.forEach { DishTypeAvatar(type = it) }
        Heaviness.entries.forEach { HeavinessChip(heaviness = it) }
    }
}
