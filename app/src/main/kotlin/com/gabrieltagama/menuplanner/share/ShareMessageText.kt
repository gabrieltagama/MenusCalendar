package com.gabrieltagama.menuplanner.share

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gabrieltagama.menuplanner.R
import com.gabrieltagama.menuplanner.core.ui.text.message

/**
 * Spanish user-facing text for the outcome of an export or import.
 */
@Composable
fun ShareMessage.text(): String = when (this) {
    is ShareMessage.Imported -> stringResource(
        R.string.share_import_summary,
        summary.dishesAdded,
        summary.dishesUpdated,
        summary.daysAdded,
        summary.daysUpdated
    )
    is ShareMessage.Failed -> error.message()
    ShareMessage.ExportFailed -> stringResource(R.string.share_export_failed)
}
