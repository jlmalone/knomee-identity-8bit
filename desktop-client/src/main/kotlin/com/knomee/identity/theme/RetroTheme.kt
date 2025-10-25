package com.knomee.identity.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 8-bit NES/Nintendo inspired color palette
 * Based on classic NES color palette with identity tier colors
 */
object RetroColors {
    // NES Base Colors
    val NESBlack = Color(0xFF0F0F0F)
    val NESWhite = Color(0xFFFFFFFF)
    val NESGray = Color(0xFF7C7C7C)
    val NESDarkGray = Color(0xFF3C3C3C)

    // Identity Tier Colors (NES-inspired)
    val GreyGhost = Color(0xFF9D9D9D)        // Gray ghost - unverified
    val LinkedID = Color(0xFF3CBCFC)         // NES cyan - linked accounts
    val PrimaryID = Color(0xFF0058F8)        // NES blue - blue checkmark!
    val Oracle = Color(0xFFFCE020)           // NES gold/yellow - trusted oracle

    // UI Accent Colors
    val Success = Color(0xFF00D800)          // NES green
    val Error = Color(0xFFD82800)            // NES red
    val Warning = Color(0xFFF8B800)          // NES orange
    val Info = Color(0xFF00A8F8)             // NES light blue

    // Background layers (CRT screen effect)
    val ScreenBackground = Color(0xFF0F1419)  // Dark CRT background
    val WindowBackground = Color(0xFF1C2128)  // Slightly lighter
    val CardBackground = Color(0xFF2C3138)    // Card/panel background
    val BorderColor = Color(0xFF58A6FF)       // Bright border highlight
}

/**
 * 8-bit color scheme for Material3
 */
private val retroColorScheme = darkColorScheme(
    primary = RetroColors.PrimaryID,
    onPrimary = RetroColors.NESWhite,
    secondary = RetroColors.LinkedID,
    onSecondary = RetroColors.NESWhite,
    tertiary = RetroColors.Oracle,
    onTertiary = RetroColors.NESBlack,
    background = RetroColors.ScreenBackground,
    onBackground = RetroColors.NESWhite,
    surface = RetroColors.WindowBackground,
    onSurface = RetroColors.NESWhite,
    error = RetroColors.Error,
    onError = RetroColors.NESWhite
)

/**
 * Typography using monospace font for retro look
 * (In production, use a pixel font like "Press Start 2P")
 */
object RetroTypography {
    val title = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 2.sp
    )

    val heading = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 1.sp
    )

    val body = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    )

    val button = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 1.sp
    )

    val caption = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    )
}

/**
 * Main retro theme composable
 */
@Composable
fun RetroTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = retroColorScheme,
        content = content
    )
}

/**
 * Get color for identity tier
 */
fun getIdentityTierColor(tier: String): Color {
    return when (tier.uppercase()) {
        "GREYGHOST" -> RetroColors.GreyGhost
        "LINKEDID" -> RetroColors.LinkedID
        "PRIMARYID" -> RetroColors.PrimaryID
        "ORACLE" -> RetroColors.Oracle
        else -> RetroColors.GreyGhost
    }
}
