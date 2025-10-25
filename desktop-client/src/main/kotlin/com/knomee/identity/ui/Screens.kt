package com.knomee.identity.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.knomee.identity.theme.RetroColors
import com.knomee.identity.theme.RetroTypography
import com.knomee.identity.theme.getIdentityTierColor

// ============ Identity Status Screen ============

@Composable
fun IdentityStatusScreen(tier: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader("IDENTITY STATUS")

        Spacer(modifier = Modifier.height(32.dp))

        // Large tier display
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(getIdentityTierColor(tier), RoundedCornerShape(8.dp))
                .border(4.dp, RetroColors.BorderColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = tier,
                    style = RetroTypography.title,
                    color = if (tier == "ORACLE") RetroColors.NESBlack else RetroColors.NESWhite
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = getTierDescription(tier),
                    style = RetroTypography.caption,
                    color = if (tier == "ORACLE") RetroColors.NESBlack else RetroColors.NESWhite,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Stats panel
        StatsPanel(tier)

        Spacer(modifier = Modifier.weight(1f))

        RetroBackButton(onBack)
    }
}

@Composable
fun StatsPanel(tier: String) {
    Column(
        modifier = Modifier
            .width(600.dp)
            .background(RetroColors.CardBackground, RoundedCornerShape(8.dp))
            .border(2.dp, RetroColors.NESGray, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        StatRow("Voting Weight:", getVotingWeight(tier))
        StatRow("Linked Accounts:", "0")
        StatRow("Total Vouches:", "0")
        StatRow("Reputation Score:", "0")
        StatRow("Claims Created:", "0")
        StatRow("Verified At:", "Not verified")
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = RetroTypography.body,
            color = RetroColors.NESGray
        )
        Text(
            text = value,
            style = RetroTypography.body,
            color = RetroColors.NESWhite
        )
    }
}

// ============ Claim Verification Screen ============

@Composable
fun ClaimVerificationScreen(
    viewModel: com.knomee.identity.viewmodel.IdentityViewModel,
    onBack: () -> Unit
) {
    var showPrimaryDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader("CLAIM VERIFICATION")

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Request identity verification and link accounts",
            style = RetroTypography.body,
            color = RetroColors.NESGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Transaction status
        viewModel.transactionStatus?.let { status ->
            Box(
                modifier = Modifier
                    .width(500.dp)
                    .background(RetroColors.Success.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .border(2.dp, RetroColors.Success, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = status,
                    style = RetroTypography.body,
                    color = RetroColors.Success,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Claim type buttons
        RetroMenuButton(
            text = "REQUEST PRIMARY ID",
            onClick = { showPrimaryDialog = true },
            enabled = !viewModel.isTransactionPending
        )
        Spacer(modifier = Modifier.height(16.dp))
        RetroMenuButton(
            text = "LINK SECONDARY ACCOUNT",
            onClick = { showLinkDialog = true },
            enabled = !viewModel.isTransactionPending
        )
        Spacer(modifier = Modifier.height(16.dp))
        RetroMenuButton("VIEW MY CLAIMS", onClick = { /* TODO */ })

        Spacer(modifier = Modifier.weight(1f))

        InfoBox(
            "STAKING REQUIRED:\n" +
            "• Link: 0.01 ETH (51% consensus)\n" +
            "• Primary: 0.03 ETH (67% consensus)\n" +
            "• Win: Get refund + rewards\n" +
            "• Lose: Stake slashed (10%-30%)"
        )

        Spacer(modifier = Modifier.height(16.dp))

        RetroBackButton(onBack)
    }

    // Dialogs
    if (showPrimaryDialog) {
        RequestPrimaryIDDialog(
            onSubmit = { justification, stake ->
                viewModel.requestPrimaryID(justification, stake)
            },
            onDismiss = { showPrimaryDialog = false }
        )
    }

    if (showLinkDialog) {
        LinkSecondaryAccountDialog(
            onSubmit = { primary, platform, justification, stake ->
                viewModel.linkSecondaryAccount(primary, platform, justification, stake)
            },
            onDismiss = { showLinkDialog = false }
        )
    }
}

// ============ Vouch System Screen ============

@Composable
fun VouchSystemScreen(
    viewModel: com.knomee.identity.viewmodel.IdentityViewModel,
    onViewClaims: () -> Unit,
    onViewMyVouches: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader("VOUCH SYSTEM")

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Support or challenge identity claims",
            style = RetroTypography.body,
            color = RetroColors.NESGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Quick stats
        Box(
            modifier = Modifier
                .width(500.dp)
                .background(RetroColors.CardBackground, RoundedCornerShape(8.dp))
                .border(2.dp, RetroColors.BorderColor, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "ACTIVE CLAIMS: ${viewModel.activeClaims.size}",
                    style = RetroTypography.body,
                    color = RetroColors.BorderColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your voting weight: ${if (viewModel.identityData?.isPrimary() == true) "1" else "0"}",
                    style = RetroTypography.caption,
                    color = RetroColors.NESGray
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        RetroMenuButton("VIEW ACTIVE CLAIMS", onClick = onViewClaims)
        Spacer(modifier = Modifier.height(16.dp))
        RetroMenuButton("MY VOUCHES", onClick = onViewMyVouches)
        Spacer(modifier = Modifier.height(16.dp))
        RetroMenuButton("CLAIM REWARDS", onClick = { /* TODO */ })

        Spacer(modifier = Modifier.weight(1f))

        InfoBox(
            "WEIGHTED VOTING:\n" +
            "• Primary ID: Weight = 1\n" +
            "• Oracle: Weight = 100\n" +
            "• Consensus auto-resolves when threshold met"
        )

        Spacer(modifier = Modifier.height(16.dp))

        RetroBackButton(onBack)
    }
}

// ============ Oracle Panel Screen ============

@Composable
fun OraclePanelScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader("ORACLE PANEL")

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .background(RetroColors.Oracle, RoundedCornerShape(40.dp))
                .border(4.dp, RetroColors.BorderColor, RoundedCornerShape(40.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🌟",
                style = RetroTypography.title,
                color = RetroColors.NESBlack
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ORACLE ACCESS REQUIRED",
            style = RetroTypography.heading,
            color = RetroColors.Oracle,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        InfoBox(
            "Oracles are trusted verifiers with 100x voting weight.\n\n" +
            "Powers:\n" +
            "• Instant consensus resolution\n" +
            "• Challenge duplicate identities\n" +
            "• Governance participation\n\n" +
            "Oracles must first achieve Primary ID status,\n" +
            "then be granted Oracle role by contract owner."
        )

        Spacer(modifier = Modifier.weight(1f))

        RetroBackButton(onBack)
    }
}

// ============ Settings Screen ============

@Composable
fun SettingsScreen(
    isConnected: Boolean,
    currentAddress: String?,
    blockNumber: java.math.BigInteger?,
    chainId: java.math.BigInteger?,
    onConnectionToggle: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader("SETTINGS")

        Spacer(modifier = Modifier.height(32.dp))

        // Network connection
        SettingRow(
            label = "Network Connection:",
            value = if (isConnected) "CONNECTED" else "DISCONNECTED"
        )
        Button(
            onClick = onConnectionToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) RetroColors.Error else RetroColors.Success
            )
        ) {
            Text(
                text = if (isConnected) "DISCONNECT" else "CONNECT",
                style = RetroTypography.button
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Blockchain info
        SettingRow("Chain ID:", chainId?.toString() ?: "Not connected")
        SettingRow("Block:", blockNumber?.toString() ?: "N/A")
        if (currentAddress != null) {
            SettingRow("Wallet:", "${currentAddress.take(6)}...${currentAddress.takeLast(4)}")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Contract addresses (placeholder - will be set after deployment)
        SettingRow("Registry:", "Not deployed")
        SettingRow("Consensus:", "Not deployed")
        SettingRow("Governance:", "Not deployed")

        Spacer(modifier = Modifier.height(24.dp))

        // God Mode (testing)
        InfoBox(
            "🎮 GOD MODE (TESTING)\n\n" +
            "Time warp enabled for testing.\n" +
            "Can fast-forward time to test cooldowns\n" +
            "and claim expiry.\n\n" +
            "⚠️ Must be disabled before mainnet!"
        )

        Spacer(modifier = Modifier.weight(1f))

        RetroBackButton(onBack)
    }
}

@Composable
fun SettingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .width(600.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = RetroTypography.body,
            color = RetroColors.NESGray
        )
        Text(
            text = value,
            style = RetroTypography.body,
            color = RetroColors.NESWhite
        )
    }
}

// ============ Shared Components ============

@Composable
fun ScreenHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(RetroColors.CardBackground, RoundedCornerShape(8.dp))
            .border(3.dp, RetroColors.BorderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = RetroTypography.heading,
            color = RetroColors.BorderColor
        )
    }
}

@Composable
fun InfoBox(text: String) {
    Box(
        modifier = Modifier
            .width(600.dp)
            .background(RetroColors.Info.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .border(2.dp, RetroColors.Info, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = text,
            style = RetroTypography.caption,
            color = RetroColors.Info,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RetroBackButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(40.dp)
            .border(2.dp, RetroColors.Error, RoundedCornerShape(4.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = RetroColors.WindowBackground,
            contentColor = RetroColors.Error
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "◀ BACK",
            style = RetroTypography.button
        )
    }
}

// ============ Helper Functions ============

fun getTierDescription(tier: String): String {
    return when (tier.uppercase()) {
        "GREYGHOST" -> "Unverified\nAnonymous"
        "LINKEDID" -> "Linked Account\nSecondary ID"
        "PRIMARYID" -> "Blue Checkmark\nVerified Human"
        "ORACLE" -> "Trusted Oracle\n100x Voting Power"
        else -> "Unknown"
    }
}

fun getVotingWeight(tier: String): String {
    return when (tier.uppercase()) {
        "GREYGHOST" -> "0 (Cannot vote)"
        "LINKEDID" -> "0 (Cannot vote)"
        "PRIMARYID" -> "1"
        "ORACLE" -> "100"
        else -> "0"
    }
}
