package com.knomee.identity.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.knomee.identity.blockchain.ClaimData
import com.knomee.identity.theme.RetroColors
import com.knomee.identity.theme.RetroTypography
import com.knomee.identity.viewmodel.IdentityViewModel
import java.math.BigInteger

/**
 * Screen displaying all active claims with voting interface
 */
@Composable
fun ActiveClaimsScreen(
    viewModel: IdentityViewModel,
    onBack: () -> Unit
) {
    val activeClaims = viewModel.activeClaims
    var selectedClaim by remember { mutableStateOf<ClaimData?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader("ACTIVE CLAIMS")

        Spacer(modifier = Modifier.height(16.dp))

        if (activeClaims.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NO ACTIVE CLAIMS",
                        style = RetroTypography.heading,
                        color = RetroColors.NESGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Create a claim to get started",
                        style = RetroTypography.body,
                        color = RetroColors.NESGray
                    )
                }
            }
        } else {
            // Claims list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeClaims) { claim ->
                    ClaimCard(
                        claim = claim,
                        onClick = { selectedClaim = claim },
                        onVoteFor = { viewModel.vouchFor(claim.claimId, 0.01) },
                        onVoteAgainst = { /* TODO */ },
                        isPending = viewModel.isTransactionPending
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Refresh button
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RetroMenuButton(
                text = "REFRESH",
                onClick = { viewModel.loadActiveClaims() },
                enabled = !viewModel.isTransactionPending
            )
            RetroBackButton(onBack)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Claim detail dialog
    selectedClaim?.let { claim ->
        ClaimDetailDialog(
            claim = claim,
            onDismiss = { selectedClaim = null },
            onVoteFor = {
                viewModel.vouchFor(claim.claimId, 0.01)
                selectedClaim = null
            },
            onVoteAgainst = {
                // TODO: Implement vouchAgainst
                selectedClaim = null
            }
        )
    }
}

/**
 * Individual claim card with voting buttons
 */
@Composable
fun ClaimCard(
    claim: ClaimData,
    onClick: () -> Unit,
    onVoteFor: () -> Unit,
    onVoteAgainst: () -> Unit,
    isPending: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, RetroColors.BorderColor, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = RetroColors.CardBackground
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Claim type and ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = claim.claimType.displayName,
                    style = RetroTypography.heading,
                    color = RetroColors.BorderColor
                )
                Text(
                    text = "ID: ${claim.claimId}",
                    style = RetroTypography.caption,
                    color = RetroColors.NESGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Claimant address
            Text(
                text = "By: ${claim.claimant.take(10)}...${claim.claimant.takeLast(6)}",
                style = RetroTypography.body,
                color = RetroColors.NESWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Justification preview
            Text(
                text = claim.justification,
                style = RetroTypography.caption,
                color = RetroColors.NESGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            VotingProgressBar(claim)

            Spacer(modifier = Modifier.height(12.dp))

            // Voting stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "For: ${claim.weightedFor}",
                    style = RetroTypography.caption,
                    color = RetroColors.Success
                )
                Text(
                    text = "Against: ${claim.weightedAgainst}",
                    style = RetroTypography.caption,
                    color = RetroColors.Error
                )
                Text(
                    text = "${(claim.getProgress() * 100).toInt()}%",
                    style = RetroTypography.caption,
                    color = RetroColors.BorderColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Voting buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onVoteFor,
                    enabled = !isPending,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .padding(end = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RetroColors.Success.copy(alpha = 0.3f),
                        contentColor = RetroColors.Success
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("✓ SUPPORT", style = RetroTypography.caption)
                }

                Button(
                    onClick = onVoteAgainst,
                    enabled = !isPending,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .padding(start = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RetroColors.Error.copy(alpha = 0.3f),
                        contentColor = RetroColors.Error
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("✗ OPPOSE", style = RetroTypography.caption)
                }
            }
        }
    }
}

/**
 * Voting progress bar showing weighted votes
 */
@Composable
fun VotingProgressBar(claim: ClaimData) {
    val progress = claim.getProgress().toFloat().coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(RetroColors.NESDarkGray, RoundedCornerShape(4.dp))
            .border(2.dp, RetroColors.NESGray, RoundedCornerShape(4.dp))
    ) {
        // Progress fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(RetroColors.Success, RoundedCornerShape(4.dp))
        )

        // Center text
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "CONSENSUS: ${(progress * 100).toInt()}%",
                style = RetroTypography.caption,
                color = RetroColors.NESWhite
            )
        }
    }
}

/**
 * Detailed claim view dialog
 */
@Composable
fun ClaimDetailDialog(
    claim: ClaimData,
    onDismiss: () -> Unit,
    onVoteFor: () -> Unit,
    onVoteAgainst: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(600.dp)
                .background(RetroColors.WindowBackground, RoundedCornerShape(8.dp))
                .border(4.dp, RetroColors.BorderColor, RoundedCornerShape(8.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "CLAIM DETAILS",
                style = RetroTypography.heading,
                color = RetroColors.BorderColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Claim info
            DetailRow("Type:", claim.claimType.displayName)
            DetailRow("Status:", claim.status.displayName)
            DetailRow("Claimant:", claim.claimant)
            claim.targetAddress?.let { DetailRow("Target:", it) }
            claim.platform?.let { DetailRow("Platform:", it) }
            DetailRow("Stake:", "${claim.stakeAmount.toDouble() / 1e18} ETH")

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Justification:",
                style = RetroTypography.body,
                color = RetroColors.NESWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RetroColors.CardBackground, RoundedCornerShape(4.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = claim.justification,
                    style = RetroTypography.caption,
                    color = RetroColors.NESGray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Voting stats
            VotingProgressBar(claim)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Votes For", style = RetroTypography.caption, color = RetroColors.Success)
                    Text("${claim.vouchesFor}", style = RetroTypography.body, color = RetroColors.NESWhite)
                    Text("Weight: ${claim.weightedFor}", style = RetroTypography.caption, color = RetroColors.NESGray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Votes Against", style = RetroTypography.caption, color = RetroColors.Error)
                    Text("${claim.vouchesAgainst}", style = RetroTypography.body, color = RetroColors.NESWhite)
                    Text("Weight: ${claim.weightedAgainst}", style = RetroTypography.caption, color = RetroColors.NESGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RetroDialogButton("SUPPORT", RetroColors.Success, onVoteFor)
                RetroDialogButton("OPPOSE", RetroColors.Error, onVoteAgainst)
                RetroDialogButton("CLOSE", RetroColors.NESGray, onDismiss)
            }
        }
    }
}

/**
 * Detail row for claim information
 */
@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = RetroTypography.body,
            color = RetroColors.NESGray
        )
        Text(
            text = value,
            style = RetroTypography.caption,
            color = RetroColors.NESWhite,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
