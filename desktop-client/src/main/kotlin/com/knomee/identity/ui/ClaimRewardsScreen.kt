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
import androidx.compose.ui.unit.dp
import com.knomee.identity.blockchain.ClaimData
import com.knomee.identity.blockchain.ClaimStatus
import com.knomee.identity.theme.RetroColors
import com.knomee.identity.theme.RetroTypography
import com.knomee.identity.viewmodel.IdentityViewModel

/**
 * Screen for claiming rewards from resolved claims
 */
@Composable
fun ClaimRewardsScreen(
    viewModel: IdentityViewModel,
    onBack: () -> Unit
) {
    // Filter for resolved claims only
    val resolvedClaims = viewModel.activeClaims.filter {
        it.status == ClaimStatus.APPROVED || it.status == ClaimStatus.REJECTED
    }

    // Calculate total claimable rewards (simplified)
    val totalRewards = resolvedClaims.size * 0.01 // Placeholder calculation

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader("CLAIM REWARDS")

        Spacer(modifier = Modifier.height(16.dp))

        // Total rewards card
        Box(
            modifier = Modifier
                .width(600.dp)
                .background(RetroColors.Success.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .border(3.dp, RetroColors.Success, RoundedCornerShape(8.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "TOTAL CLAIMABLE REWARDS",
                    style = RetroTypography.caption,
                    color = RetroColors.Success
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = String.format("%.4f ETH", totalRewards),
                    style = RetroTypography.title,
                    color = RetroColors.Success
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${resolvedClaims.size} resolved claims",
                    style = RetroTypography.caption,
                    color = RetroColors.NESGray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transaction status
        viewModel.transactionStatus?.let { status ->
            Box(
                modifier = Modifier
                    .width(600.dp)
                    .background(RetroColors.Info.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .border(2.dp, RetroColors.Info, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = status,
                    style = RetroTypography.body,
                    color = RetroColors.Info,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Info box
        InfoBox(
            "REWARD SYSTEM:\n" +
            "• Correct votes get stake refund + rewards\n" +
            "• Rewards come from slashed stakes\n" +
            "• Claim rewards individually per claim\n" +
            "• Gas fees apply to each claim"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Resolved claims list
        if (resolvedClaims.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NO REWARDS AVAILABLE",
                        style = RetroTypography.heading,
                        color = RetroColors.NESGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Rewards become available when claims resolve",
                        style = RetroTypography.body,
                        color = RetroColors.NESGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(resolvedClaims) { claim ->
                    RewardClaimCard(
                        claim = claim,
                        onClaim = { viewModel.claimRewards(claim.claimId) },
                        isPending = viewModel.isTransactionPending
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (resolvedClaims.isNotEmpty()) {
                RetroMenuButton(
                    text = "CLAIM ALL",
                    onClick = {
                        // TODO: Batch claim all rewards
                        resolvedClaims.forEach { claim ->
                            viewModel.claimRewards(claim.claimId)
                        }
                    },
                    enabled = !viewModel.isTransactionPending
                )
            }
            RetroBackButton(onBack)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Card for individual reward claim
 */
@Composable
fun RewardClaimCard(
    claim: ClaimData,
    onClaim: () -> Unit,
    isPending: Boolean
) {
    val isApproved = claim.status == ClaimStatus.APPROVED
    val estimatedReward = claim.stakeAmount.toDouble() / 1e18 * 1.1 // Stake + 10% reward

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, RetroColors.BorderColor, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = RetroColors.CardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = claim.claimType.displayName,
                    style = RetroTypography.heading,
                    color = RetroColors.NESWhite
                )

                // Status badge
                Box(
                    modifier = Modifier
                        .background(
                            if (isApproved) RetroColors.Success.copy(alpha = 0.2f)
                            else RetroColors.Error.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            2.dp,
                            if (isApproved) RetroColors.Success else RetroColors.Error,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isApproved) "✓ APPROVED" else "✗ REJECTED",
                        style = RetroTypography.caption,
                        color = if (isApproved) RetroColors.Success else RetroColors.Error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Claim details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Claim ID: ${claim.claimId}",
                        style = RetroTypography.caption,
                        color = RetroColors.NESGray
                    )
                    Text(
                        text = "Claimant: ${claim.claimant.take(10)}...",
                        style = RetroTypography.caption,
                        color = RetroColors.NESGray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Estimated Reward",
                        style = RetroTypography.caption,
                        color = RetroColors.NESGray
                    )
                    Text(
                        text = String.format("%.4f ETH", estimatedReward),
                        style = RetroTypography.body,
                        color = RetroColors.Success
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Voting stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Final: For ${claim.weightedFor} vs ${claim.weightedAgainst}",
                    style = RetroTypography.caption,
                    color = RetroColors.NESGray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Claim button
            Button(
                onClick = onClaim,
                enabled = !isPending,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .border(2.dp, RetroColors.Success, RoundedCornerShape(4.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RetroColors.Success.copy(alpha = 0.2f),
                    contentColor = RetroColors.Success,
                    disabledContainerColor = RetroColors.NESDarkGray,
                    disabledContentColor = RetroColors.NESGray
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (isPending) "PROCESSING..." else "CLAIM REWARD",
                    style = RetroTypography.button
                )
            }
        }
    }
}
