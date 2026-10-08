package com.adityaram.present.ui.importing

import androidx.compose.runtime.Composable

/**
 * DEBUG source set: provides the Claude Opus debug import button.
 * This function is resolved at compile time from src/debug/ in debug builds.
 */
fun debugImportContent(viewModel: ImportViewModel): (@Composable () -> Unit)? = {
    ClaudeDebugImportButton(viewModel = viewModel)
}
