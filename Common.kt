@file:OptIn(ExperimentalMaterial3Api::class)

package com.yourname.iprotation2.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/** Shared screen frame: top bar + snackbar + optional FAB. Insets are handled by the top bar / nav bar. */
@Composable
fun ScreenScaffold(
    title: String,
    snackbarHostState: SnackbarHostState? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                actions = actions
            )
        },
        snackbarHost = { if (snackbarHostState != null) SnackbarHost(snackbarHostState) },
        floatingActionButton = floatingActionButton,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = { padding -> content(PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())) }
    )
}
