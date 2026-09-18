package ch.ncavallini.polywear.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

/** Wear Material theme wrapper. Uses the default Wear color palette. */
@Composable
fun PolyWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
