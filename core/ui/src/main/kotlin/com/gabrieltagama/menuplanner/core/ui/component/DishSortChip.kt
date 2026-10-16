package com.gabrieltagama.menuplanner.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrieltagama.menuplanner.core.domain.model.DishSort
import com.gabrieltagama.menuplanner.core.ui.R
import com.gabrieltagama.menuplanner.core.ui.text.label

/**
 * Chip that shows the current dish order and opens a menu with every DishSort to pick one. It
 * looks selected whenever the order is not the default one (by name).
 */
@Composable
fun DishSortChip(sort: DishSort, onSortChange: (DishSort) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        FilterChip(
            selected = sort != DishSort.NAME,
            onClick = { expanded = true },
            label = { Text(sort.label()) },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.dish_sort_title), modifier = Modifier.size(18.dp))
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DishSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label()) },
                    leadingIcon = { RadioButton(selected = option == sort, onClick = null) },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
