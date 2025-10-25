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
import com.knomee.identity.theme.RetroColors
import com.knomee.identity.theme.RetroTypography
import com.knomee.identity.viewmodel.IdentityViewModel

/**
 * Vote record for tracking user's voting history
 */
data class VoteRecord(
    val claim: ClaimData,
    val votedFor: Boolean, // true = voted for, false = voted against
    val stakeAmount: Double,
    val timestamp: Long
)

/**
 * Screen showing user's voting history
 */
@Composable
fun MyVouchesScreen(
    viewModel: IdentityViewModel,
    onBack: () -> Unit
) {
    // For now, filter active claims to show only those we can infer votes for
    // In a full implementation, we'd query VouchSubmitted events for current user
    val allClaims = viewModel.activeClaims
    var selectedFilter by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader("MY VOUCHES")

        Spacer(modifier = Modifier.height(16.dp))

        // Stats summary
        StatsCard(
            totalVouches = allClaims.size,
            votesFor = allClaims.count { it.weightedFor > it.weightedAgainst },
            votesAgainst = allClaims.count { it.weightedAgainst > it.weightedFor },
            pending = allClaims.count { it.isActive() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filter tabs
        FilterTabs(
            selected = selectedFilter,
            onSelect = { selectedFilter = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Vote history list
        if (allClaims.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NO VOTES YET",
                        style = RetroTypography.heading,
                        color = RetroColors.NESGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Visit Active Claims to start voting",
                        style = RetroTypography.body,
                        color = RetroColors.NESGray
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
                items(allClaims.filter { claim ->
                    when (selectedFilter) {
                        "Pending" -> claim.isActive()
                        "Resolved" -> !claim.isActive()
                        else -> true
                    }
                }) { claim ->
                    VoteHistoryCard(claim)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RetroBackButton(onBack)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Stats card showing voting statistics
 */
@Composable
fun StatsCard(
    totalVouches: Int,
    votesFor: Int,
    votesAgainst: Int,
    pending: Int
) {
    Box(
        modifier = Modifier
            .width(600.dp)
            .background(RetroColors.CardBackground, RoundedCornerShape(8.dp))
            .border(3.dp, RetroColors.BorderColor, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("TOTAL", totalVouches.toString(), RetroColors.BorderColor)
            StatItem("SUPPORT", votesFor.toString(), RetroColors.Success)
            StatItem("OPPOSE", votesAgainst.toString(), RetroColors.Error)
            StatItem("PENDING", pending.toString(), RetroColors.Info)
        }
    }
}

/**
 * Individual stat item
 */
@Composable
fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = RetroTypography.title,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = RetroTypography.caption,
            color = RetroColors.NESGray
        )
    }
}

/**
 * Filter tabs for vote history
 */
@Composable
fun FilterTabs(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("All", "Pending", "Resolved").forEach { filter ->
            FilterTab(
                text = filter,
                selected = selected == filter,
                onClick = { onSelect(filter) }
            )
        }
    }
}

/**
 * Individual filter tab
 */
@Composable
fun FilterTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(150.dp)
            .height(36.dp)
            .border(
                2.dp,
                if (selected) RetroColors.BorderColor else RetroColors.NESGray,
                RoundedCornerShape(4.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) RetroColors.BorderColor.copy(alpha = 0.3f)
                else RetroColors.WindowBackground,
            contentColor = if (selected) RetroColors.BorderColor else RetroColors.NESGray
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = RetroTypography.button
        )
    }
}

/**
 * Card showing individual vote history entry
 */
@Composable
fun VoteHistoryCard(claim: ClaimData) {
    // Infer vote direction from claim data (simplified for demo)
    val votedFor = claim.weightedFor > claim.weightedAgainst
    val isResolved = !claim.isActive()

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
                // Vote direction badge
                Box(
                    modifier = Modifier
                        .background(
                            if (votedFor) RetroColors.Success.copy(alpha = 0.2f)
                            else RetroColors.Error.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            2.dp,
                            if (votedFor) RetroColors.Success else RetroColors.Error,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (votedFor) "✓ SUPPORTED" else "✗ OPPOSED",
                        style = RetroTypography.caption,
                        color = if (votedFor) RetroColors.Success else RetroColors.Error
                    )
                }

                // Status badge
                if (isResolved) {
                    Box(
                        modifier = Modifier
                            .background(RetroColors.Info.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .border(2.dp, RetroColors.Info, RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "RESOLVED",
                            style = RetroTypography.caption,
                            color = RetroColors.Info
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(RetroColors.BorderColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .border(2.dp, RetroColors.BorderColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PENDING",
                            style = RetroTypography.caption,
                            color = RetroColors.BorderColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Claim type and ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = claim.claimType.displayName,
                    style = RetroTypography.heading,
                    color = RetroColors.NESWhite
                )
                Text(
                    text = "ID: ${claim.claimId}",
                    style = RetroTypography.caption,
                    color = RetroColors.NESGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Claimant
            Text(
                text = "By: ${claim.claimant.take(10)}...${claim.claimant.takeLast(6)}",
                style = RetroTypography.caption,
                color = RetroColors.NESGray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Current voting status
            VotingProgressBar(claim)

            Spacer(modifier = Modifier.height(8.dp))

            // Vote counts
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
                    text = "${(claim.getProgress() * 100).toInt()}% consensus",
                    style = RetroTypography.caption,
                    color = RetroColors.BorderColor
                )
            }
        }
    }
}
