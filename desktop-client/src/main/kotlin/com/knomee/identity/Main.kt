package com.knomee.identity

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.knomee.identity.theme.RetroColors
import com.knomee.identity.theme.RetroTheme
import com.knomee.identity.theme.RetroTypography
import com.knomee.identity.theme.getIdentityTierColor
import com.knomee.identity.ui.MainScreen

fun main() = application {
    val windowState = rememberWindowState(width = 1024.dp, height = 768.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "KNOMEE IDENTITY PROTOCOL v0.1",
        state = windowState
    ) {
        RetroTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = RetroColors.ScreenBackground
            ) {
                MainScreen()
            }
        }
    }
}

@Composable
@Preview
fun App() {
    RetroTheme {
        MainScreen()
    }
}
