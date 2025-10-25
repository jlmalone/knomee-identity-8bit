package com.knomee.identity.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.math.BigInteger
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.knomee.identity.theme.RetroColors
import com.knomee.identity.theme.RetroTypography
import com.knomee.identity.theme.getIdentityTierColor
import kotlinx.coroutines.delay

enum class Screen {
    TITLE,
    IDENTITY_STATUS,
    CLAIM_VERIFICATION,
    VOUCH_SYSTEM,
    ACTIVE_CLAIMS,
    ORACLE_PANEL,
    SETTINGS
}

@Composable
fun MainScreen() {
    val viewModel = remember { com.knomee.identity.viewmodel.IdentityViewModel() }
    var currentScreen by remember { mutableStateOf(Screen.TITLE) }

    // Use blockchain state from ViewModel
    val currentTier = viewModel.currentTier
    val isConnected = viewModel.isConnected
    val currentAddress = viewModel.currentAddress

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onDispose()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RetroColors.ScreenBackground)
            .padding(16.dp)
    ) {
        // Top status bar (NES-style)
        StatusBar(
            tier = currentTier,
            isConnected = isConnected
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main content area with CRT border effect
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(4.dp, RetroColors.BorderColor, RoundedCornerShape(8.dp))
                .background(RetroColors.WindowBackground, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            when (currentScreen) {
                Screen.TITLE -> TitleScreen(
                    onNavigate = { currentScreen = it }
                )
                Screen.IDENTITY_STATUS -> IdentityStatusScreen(
                    tier = currentTier,
                    onBack = { currentScreen = Screen.TITLE }
                )
                Screen.CLAIM_VERIFICATION -> ClaimVerificationScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.TITLE }
                )
                Screen.VOUCH_SYSTEM -> VouchSystemScreen(
                    viewModel = viewModel,
                    onViewClaims = { currentScreen = Screen.ACTIVE_CLAIMS },
                    onBack = { currentScreen = Screen.TITLE }
                )
                Screen.ACTIVE_CLAIMS -> ActiveClaimsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.VOUCH_SYSTEM }
                )
                Screen.ORACLE_PANEL -> OraclePanelScreen(
                    onBack = { currentScreen = Screen.TITLE }
                )
                Screen.SETTINGS -> SettingsScreen(
                    isConnected = isConnected,
                    currentAddress = currentAddress,
                    blockNumber = viewModel.blockNumber,
                    chainId = viewModel.chainId,
                    onConnectionToggle = {
                        if (isConnected) {
                            viewModel.disconnect()
                        } else {
                            viewModel.connect(useTestAccount = true)
                            // Set contract addresses from .env
                            viewModel.setContractAddresses(
                                governance = "0x5FbDB2315678afecb367f032d93F642f64180aa3",
                                registry = "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512",
                                consensus = "0x9fE46736679d2D9a65F0992F2272dE9f3c7fa6e0"
                            )
                        }
                    },
                    onBack = { currentScreen = Screen.TITLE }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom info bar
        BottomBar()
    }
}

@Composable
fun StatusBar(tier: String, isConnected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(RetroColors.NESDarkGray, RoundedCornerShape(4.dp))
            .border(2.dp, RetroColors.NESGray, RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Identity tier display
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "STATUS:",
                style = RetroTypography.caption,
                color = RetroColors.NESWhite
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(getIdentityTierColor(tier), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = tier,
                style = RetroTypography.caption,
                color = getIdentityTierColor(tier)
            )
        }

        // Connection status with blinking indicator
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "NETWORK:",
                style = RetroTypography.caption,
                color = RetroColors.NESWhite
            )
            Spacer(modifier = Modifier.width(8.dp))
            BlinkingIndicator(isConnected)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isConnected) "ONLINE" else "OFFLINE",
                style = RetroTypography.caption,
                color = if (isConnected) RetroColors.Success else RetroColors.Error
            )
        }
    }
}

@Composable
fun BlinkingIndicator(isOn: Boolean) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            visible = !visible
        }
    }

    Box(
        modifier = Modifier
            .size(12.dp)
            .background(
                if (isOn && visible) RetroColors.Success else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .border(
                2.dp,
                if (isOn) RetroColors.Success else RetroColors.Error,
                RoundedCornerShape(6.dp)
            )
    )
}

@Composable
fun BottomBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(RetroColors.NESDarkGray, RoundedCornerShape(4.dp))
            .border(2.dp, RetroColors.NESGray, RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🎮 KNOMEE IDENTITY PROTOCOL v0.1 | PHASE 1: IDENTITY CONSENSUS LAYER",
            style = RetroTypography.caption,
            color = RetroColors.NESGray
        )
    }
}

@Composable
fun TitleScreen(onNavigate: (Screen) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated title
        AnimatedTitle()

        Spacer(modifier = Modifier.height(48.dp))

        // Menu options
        RetroMenuButton("◆ IDENTITY STATUS", onClick = { onNavigate(Screen.IDENTITY_STATUS) })
        Spacer(modifier = Modifier.height(16.dp))
        RetroMenuButton("◆ CLAIM VERIFICATION", onClick = { onNavigate(Screen.CLAIM_VERIFICATION) })
        Spacer(modifier = Modifier.height(16.dp))
        RetroMenuButton("◆ VOUCH SYSTEM", onClick = { onNavigate(Screen.VOUCH_SYSTEM) })
        Spacer(modifier = Modifier.height(16.dp))
        RetroMenuButton("◆ ORACLE PANEL", onClick = { onNavigate(Screen.ORACLE_PANEL) })
        Spacer(modifier = Modifier.height(16.dp))
        RetroMenuButton("◆ SETTINGS", onClick = { onNavigate(Screen.SETTINGS) })

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "PRESS ENTER TO SELECT",
            style = RetroTypography.caption,
            color = RetroColors.NESGray
        )
    }
}

@Composable
fun AnimatedTitle() {
    val infiniteTransition = rememberInfiniteTransition()
    val colorIndex by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val colors = listOf(
        RetroColors.PrimaryID,
        RetroColors.LinkedID,
        RetroColors.Oracle,
        RetroColors.GreyGhost
    )
    val currentColor = colors[colorIndex.toInt() % colors.size]

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "KNOMEE",
            style = RetroTypography.title,
            color = currentColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "IDENTITY PROTOCOL",
            style = RetroTypography.heading,
            color = RetroColors.NESWhite
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "DECENTRALIZED • SYBIL-RESISTANT • WEB3",
            style = RetroTypography.caption,
            color = RetroColors.NESGray
        )
    }
}

@Composable
fun RetroMenuButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(400.dp)
            .height(48.dp)
            .border(3.dp, RetroColors.BorderColor, RoundedCornerShape(4.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = RetroColors.WindowBackground,
            contentColor = RetroColors.NESWhite,
            disabledContainerColor = RetroColors.NESDarkGray,
            disabledContentColor = RetroColors.NESGray
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = RetroTypography.button,
            textAlign = TextAlign.Center
        )
    }
}
