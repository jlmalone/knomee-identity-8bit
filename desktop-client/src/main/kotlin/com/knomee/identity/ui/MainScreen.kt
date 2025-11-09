package com.knomee.identity.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key as ComposeKey
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.knomee.identity.blockchain.ClaimData
import com.knomee.identity.blockchain.GovernanceParams
import com.knomee.identity.blockchain.IdentityData
import com.knomee.identity.theme.RetroColors
import com.knomee.identity.theme.RetroTypography
import com.knomee.identity.theme.getIdentityTierColor
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.math.min

private data class SpotInfo(
    val id: String,
    val title: String,
    val description: String,
    val color: Color
)

private data class PixelArt(
    val rows: List<String>,
    val palette: Map<Char, Color>
)

private const val SPRITE_SCALE = 2
private const val GOLDEN_RATIO = 1.61803398875f

private data class TileInfo(
    val fill: Color,
    val border: Color? = null,
    val blocking: Boolean = false,
    val spot: SpotInfo? = null,
    val sprite: PixelArt? = null
)

private data class CitySpot(
    val id: String,
    val title: String,
    val description: String,
    val tile: IntOffset,
    val color: Color
) {
    val center: Offset
        get() = Offset(tile.x + 0.5f, tile.y + 0.5f)
}

private data class Enemy(
    var position: Offset,
    val color: Color
)

private data class TurnLogEntry(val turn: Int, val message: String)

private data class NpcAgent(val name: String, val role: NpcRole)

private enum class NpcRole {
    PRIMARY,
    ORACLE,
    CHALLENGER,
    SYBIL
}

data class EncounterRoom(
    val id: String,
    val description: String,
    val position: Offset,
    var resolved: Boolean = false
)

private enum class FacingDirection {
    UP, DOWN, LEFT, RIGHT
}

private enum class SoundEffect {
    STEP,
    INTERACT
}

private val defaultTileInfo = TileInfo(
    fill = RetroColors.ScreenBackground,
    border = null,
    blocking = false,
    spot = null,
    sprite = null
)

private val wallSprite = PixelArt(
    rows = listOf(
        "########",
        "#==##==#",
        "########",
        "#==##==#",
        "########",
        "#==##==#",
        "########",
        "#==##==#"
    ),
    palette = mapOf(
        '#' to RetroColors.NESDarkGray,
        '=' to RetroColors.NESGray
    )
)

private val buildingSprite = PixelArt(
    rows = listOf(
        "MMMMMMMM",
        "M......M",
        "MYY..YYM",
        "M......M",
        "MYY..YYM",
        "M......M",
        "M......M",
        "MMMMMMMM"
    ),
    palette = mapOf(
        'M' to RetroColors.BorderColor,
        'Y' to RetroColors.PrimaryID
    )
)

private val plazaSprite = PixelArt(
    rows = listOf(
        "........",
        "..++..++",
        "........",
        "++..++..",
        "........",
        "..++..++",
        "........",
        "++..++.."
    ),
    palette = mapOf(
        '+' to RetroColors.LinkedID.copy(alpha = 0.5f)
    )
)

private val bridgeSprite = PixelArt(
    rows = listOf(
        "........",
        "~~..~~..",
        "..~~..~~",
        "~~..~~..",
        "..~~..~~",
        "~~..~~..",
        "..~~..~~",
        "........"
    ),
    palette = mapOf(
        '~' to RetroColors.LinkedID
    )
)

private val oracleSprite = PixelArt(
    rows = listOf(
        "..OOOO..",
        ".O....O.",
        "O......O",
        "O.OOOO.O",
        "O.OOOO.O",
        "O......O",
        ".O....O.",
        "..OOOO.."
    ),
    palette = mapOf(
        'O' to RetroColors.Oracle
    )
)

private val guildSprite = PixelArt(
    rows = listOf(
        "HHHHHHHH",
        "H......H",
        "H..HH..H",
        "H.HHHH.H",
        "H..HH..H",
        "H.HHHH.H",
        "H......H",
        "HHHHHHHH"
    ),
    palette = mapOf(
        'H' to RetroColors.LinkedID
    )
)

private val pillarSprite = PixelArt(
    rows = listOf(
        "..RRRR..",
        "..R..R..",
        "..R..R..",
        "..R..R..",
        "..R..R..",
        "..R..R..",
        "..R..R..",
        "..RRRR.."
    ),
    palette = mapOf(
        'R' to RetroColors.Error
    )
)

private val networkSprite = PixelArt(
    rows = listOf(
        "..N..N..",
        ".N....N.",
        "N..NN..N",
        "..N..N..",
        "..N..N..",
        "N..NN..N",
        ".N....N.",
        "..N..N.."
    ),
    palette = mapOf(
        'N' to RetroColors.Info
    )
)

private val stageSprite = PixelArt(
    rows = listOf(
        "SSSSSSSS",
        "S......S",
        "S..SS..S",
        "S.SSSS.S",
        "S..SS..S",
        "S.SSSS.S",
        "S......S",
        "SSSSSSSS"
    ),
    palette = mapOf(
        'S' to RetroColors.PrimaryID
    )
)

private val kioskSprite = PixelArt(
    rows = listOf(
        "..KKKK..",
        ".K....K.",
        "K.KK.K.K",
        "K.K..K.K",
        "K.KK.K.K",
        "K.K..K.K",
        ".K....K.",
        "..KKKK.."
    ),
    palette = mapOf(
        'K' to RetroColors.BorderColor
    )
)

private val greyGhostSprite = PixelArt(
    rows = listOf(
        "..GGGG..",
        ".G....G.",
        "GG.GG.GG",
        "GGGGGGGG",
        "GGGGGGGG",
        "G.GGG.GG",
        "G......G",
        ".GGGGGG."
    ),
    palette = mapOf(
        'G' to RetroColors.GreyGhost
    )
)

private val teleportSprite = PixelArt(
    rows = listOf(
        "..TTTT..",
        ".T....T.",
        "T..TT..T",
        "T.TTTT.T",
        "T..TT..T",
        ".T....T.",
        "..TTTT..",
        "...TT..."
    ),
    palette = mapOf(
        'T' to RetroColors.Success
    )
)

private val dockSprite = PixelArt(
    rows = listOf(
        "DDDDDDDD",
        "D......D",
        "D.DD.D.D",
        "D......D",
        "D.DD.D.D",
        "D......D",
        "D......D",
        "DDDDDDDD"
    ),
    palette = mapOf(
        'D' to RetroColors.Warning
    )
)

private val identityCityLegend = mapOf(
    '.' to defaultTileInfo.copy(sprite = plazaSprite),
    '#' to TileInfo(
        fill = RetroColors.NESDarkGray,
        border = RetroColors.NESGray,
        blocking = true,
        sprite = wallSprite
    ),
    'B' to TileInfo(
        fill = RetroColors.WindowBackground,
        border = RetroColors.NESGray,
        blocking = true,
        sprite = buildingSprite
    ),
    '~' to TileInfo(
        fill = RetroColors.LinkedID.copy(alpha = 0.35f),
        border = RetroColors.LinkedID,
        blocking = false,
        sprite = bridgeSprite
    ),
    'O' to TileInfo(
        fill = RetroColors.Oracle.copy(alpha = 0.25f),
        border = RetroColors.Oracle,
        blocking = false,
        spot = SpotInfo(
            id = "oracle_tower",
            title = "Oracle Tower",
            description = "High-trust validators coordinate here. Reputation is earned, not bought.",
            color = RetroColors.Oracle
        ),
        sprite = oracleSprite
    ),
    'H' to TileInfo(
        fill = RetroColors.WindowBackground,
        border = RetroColors.LinkedID,
        blocking = false,
        spot = SpotInfo(
            id = "guild_hall",
            title = "Guild Hall",
            description = "Primary IDs gather to vouch, review claims, and keep the city Sybil-free.",
            color = RetroColors.LinkedID
        ),
        sprite = guildSprite
    ),
    'P' to TileInfo(
        fill = RetroColors.Error.copy(alpha = 0.25f),
        border = RetroColors.Error,
        blocking = false,
        spot = SpotInfo(
            id = "challenge_pillar",
            title = "Challenge Pillar",
            description = "Stake KNOW to challenge duplicates. Justice needs courage and collateral.",
            color = RetroColors.Error
        ),
        sprite = pillarSprite
    ),
    'N' to TileInfo(
        fill = RetroColors.Info.copy(alpha = 0.25f),
        border = RetroColors.Info,
        blocking = false,
        spot = SpotInfo(
            id = "network_node",
            title = "Network Relay",
            description = "Link secondary identities here. Every verified connection strengthens your web-of-trust score.",
            color = RetroColors.Info
        ),
        sprite = networkSprite
    ),
    'S' to TileInfo(
        fill = RetroColors.PrimaryID.copy(alpha = 0.2f),
        border = RetroColors.PrimaryID,
        blocking = false,
        spot = SpotInfo(
            id = "consensus_stage",
            title = "Consensus Stage",
            description = "Live votes play out here. Watch the bar fill as Primaries and Oracles weigh in.",
            color = RetroColors.PrimaryID
        ),
        sprite = stageSprite
    ),
    'K' to TileInfo(
        fill = RetroColors.BorderColor.copy(alpha = 0.25f),
        border = RetroColors.BorderColor,
        blocking = false,
        spot = SpotInfo(
            id = "knowledge_kiosk",
            title = "Knowledge Kiosk",
            description = "Browse protocol lore and tokenomics. Claude keeps the scrolls fresh.",
            color = RetroColors.BorderColor
        ),
        sprite = kioskSprite
    ),
    'G' to TileInfo(
        fill = RetroColors.GreyGhost.copy(alpha = 0.25f),
        border = RetroColors.GreyGhost,
        blocking = false,
        spot = SpotInfo(
            id = "grey_ghost_hideout",
            title = "Grey Ghost Hideout",
            description = "Unverified wanderers linger here, dreaming of their first blue check.",
            color = RetroColors.GreyGhost
        ),
        sprite = greyGhostSprite
    ),
    'T' to TileInfo(
        fill = RetroColors.Success.copy(alpha = 0.25f),
        border = RetroColors.Success,
        blocking = false,
        spot = SpotInfo(
            id = "teleport_gate",
            title = "Teleport Gate",
            description = "Fast-travel pad to other districts (coming soon). Requires Primary ID clearance.",
            color = RetroColors.Success
        ),
        sprite = teleportSprite
    ),
    'D' to TileInfo(
        fill = RetroColors.Warning.copy(alpha = 0.25f),
        border = RetroColors.Warning,
        blocking = false,
        spot = SpotInfo(
            id = "dockyard",
            title = "Dockyard",
            description = "Incoming shipments of retro pixels and governance upgrades.",
            color = RetroColors.Warning
        ),
        sprite = dockSprite
    )
)

private val identityCityLayout = listOf(
    "############################",
    "#....B.......O......T......#",
    "#....B..~~~..#####..T......#",
    "#....B..~~~..#...#..H......#",
    "#..........~~~#...#..P.....#",
    "#..#######~~~.#...#..P.....#",
    "#..#.....#...#...#######...#",
    "#..#..N..#####........S....#",
    "#..#.........S.............#",
    "#..###############....K....#",
    "#....G.........B.....K.....#",
    "#.............K.......D....#",
    "############################"
)

private val identityCitySpots: List<CitySpot> = identityCityLayout.flatMapIndexed { y, row ->
    row.mapIndexedNotNull { x, char ->
        val info = identityCityLegend[char]?.spot ?: return@mapIndexedNotNull null
        CitySpot(
            id = info.id,
            title = info.title,
            description = info.description,
            tile = IntOffset(x, y),
            color = info.color
        )
    }
}

@Composable
private fun IdentityCityHud(
    tier: String,
    address: String?,
    identityData: IdentityData?,
    knomeeBalance: BigInteger?,
    votingWeight: BigInteger?,
    activeClaims: List<ClaimData>,
    governanceParams: GovernanceParams?,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val votingDisplay = votingWeight?.toString()
        ?: identityData?.getVotingWeight()?.toString()
        ?: "0"
    val reputationText = formatNumber(identityData?.reputationScore)
    val knowBalanceText = formatTokenBalance(knomeeBalance)
    val linkedIds = identityData?.linkedAccounts?.size ?: 0
    val vouchesGiven = identityData?.vouchesGiven ?: 0
    val vouchesReceived = identityData?.vouchesReceived ?: 0
    val activeCount = activeClaims.count { it.isActive() }
    val underChallenge = identityData?.underChallenge == true
    val activeClaimsColor = when {
        underChallenge -> RetroColors.Error
        activeCount > 0 -> RetroColors.Warning
        else -> RetroColors.Success
    }
    val networkLabel = if (isConnected) "ONLINE" else "OFFLINE"
    val networkColor = if (isConnected) RetroColors.Success else RetroColors.Error

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = RetroColors.NESDarkGray.copy(alpha = 0.85f),
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, RetroColors.BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Wallet",
                        style = RetroTypography.caption,
                        color = RetroColors.NESGray
                    )
                    Text(
                        text = formatAddress(address),
                        style = RetroTypography.body,
                        color = RetroColors.NESWhite
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Network $networkLabel",
                        style = RetroTypography.caption,
                        color = networkColor
                    )
                    Text(
                        text = "Tier ${formatTier(tier)}",
                        style = RetroTypography.caption,
                        color = getIdentityTierColor(tier)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            val chips = listOf(
                Triple("Vote", votingDisplay, RetroColors.Oracle),
                Triple("Rep", reputationText, RetroColors.PrimaryID),
                Triple("KNOW", knowBalanceText, RetroColors.LinkedID),
                Triple("Linked", linkedIds.toString(), RetroColors.LinkedID),
                Triple("Vouches", "$vouchesGiven / $vouchesReceived", RetroColors.NESWhite),
                Triple("Active", activeCount.toString(), activeClaimsColor)
            )
            chips.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { (label, value, color) ->
                        StatChip(label, value, color)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            if (underChallenge) {
                Text(
                    text = "⚠ Challenges pending this turn.",
                    style = RetroTypography.caption,
                    color = RetroColors.Warning
                )
            }
        }
    }
}

@Composable
private fun TurnController(
    turnNumber: Int,
    onAdvanceTurn: () -> Unit,
    logEntries: List<TurnLogEntry>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, RetroColors.BorderColor, RoundedCornerShape(6.dp))
            .background(RetroColors.WindowBackground, RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Turn $turnNumber",
                    style = RetroTypography.heading,
                    color = RetroColors.LinkedID
                )
                Text(
                    text = "Advance time to process claims, distribute rewards, and spawn NPC actions.",
                    style = RetroTypography.caption,
                    color = RetroColors.NESGray
                )
            }
            Button(
                onClick = onAdvanceTurn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RetroColors.PrimaryID,
                    contentColor = RetroColors.NESWhite
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Advance Turn", style = RetroTypography.button)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .border(1.dp, RetroColors.NESDarkGray, RoundedCornerShape(4.dp))
                .background(RetroColors.NESBlack.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            if (logEntries.isEmpty()) {
                Text(
                    text = "No events yet. Advance a turn to kick off the simulation.",
                    style = RetroTypography.caption,
                    color = RetroColors.NESGray
                )
            } else {
                LazyColumn {
                    items(logEntries.takeLast(8)) { entry ->
                        Text(
                            text = "Turn ${entry.turn}: ${entry.message}",
                            style = RetroTypography.caption,
                            color = RetroColors.NESWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HudStat(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = RetroTypography.caption,
            color = RetroColors.NESGray
        )
        Text(
            text = value.ifEmpty { "--" },
            style = RetroTypography.heading,
            color = valueColor
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        color = RetroColors.WindowBackground.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label.uppercase(), style = RetroTypography.caption, color = RetroColors.NESGray)
            Text(value.ifEmpty { "--" }, style = RetroTypography.button, color = color)
        }
    }
}

@Composable
private fun MiniMap(
    mapData: List<String>,
    playerPosition: Offset,
    activeSpot: CitySpot?
) {
    val mapHeight = mapData.size
    val mapWidth = mapData.maxOf { it.length }
    val tileSize = 6.dp
    val density = LocalDensity.current
    val tileSizePx = with(density) { tileSize.toPx() }

    Canvas(
        modifier = Modifier.size(tileSize * mapWidth, tileSize * mapHeight)
    ) {
        for (y in 0 until mapHeight) {
            val row = mapData[y]
            for (x in 0 until mapWidth) {
                val info = tileInfoFor(row[x])
                drawRect(
                    color = info.fill.copy(alpha = 0.9f),
                    topLeft = Offset(x * tileSizePx, y * tileSizePx),
                    size = Size(tileSizePx, tileSizePx)
                )
                info.sprite?.let { sprite ->
                    drawPixelArt(sprite, Offset(x * tileSizePx, y * tileSizePx), tileSizePx)
                }
            }
        }

        activeSpot?.let { spot ->
            val topLeft = Offset(spot.tile.x * tileSizePx, spot.tile.y * tileSizePx)
            drawRect(
                color = spot.color,
                topLeft = topLeft,
                size = Size(tileSizePx, tileSizePx)
            )
        }

        drawCircle(
            color = RetroColors.PrimaryID,
            radius = tileSizePx * 0.4f,
            center = Offset(playerPosition.x * tileSizePx, playerPosition.y * tileSizePx)
        )
    }
}

@Composable
private fun MiniMapCard(
    mapData: List<String>,
    playerPosition: Offset,
    activeSpot: CitySpot?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = RetroColors.NESDarkGray.copy(alpha = 0.85f),
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, RetroColors.BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Mini Map",
                style = RetroTypography.caption,
                color = RetroColors.NESWhite
            )
            MiniMap(
                mapData = mapData,
                playerPosition = playerPosition,
                activeSpot = activeSpot
            )
        }
    }
}

@Composable
private fun SpotActionPanel(
    spot: CitySpot,
    actions: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = RetroColors.WindowBackground.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, spot.color),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = spot.title.uppercase(Locale.getDefault()),
                style = RetroTypography.caption,
                color = spot.color
            )
            actions.forEach { (label, action) ->
                Button(
                    onClick = action,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = spot.color.copy(alpha = 0.8f),
                        contentColor = RetroColors.NESBlack
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(label, style = RetroTypography.button)
                }
            }
            Text(
                text = "Press ENTER to trigger the first action.",
                style = RetroTypography.caption,
                color = RetroColors.NESGray
            )
        }
    }
}

private fun SnapshotStateMap<ComposeKey, Boolean>.isPressed(key: ComposeKey): Boolean = this[key] == true

private fun isWalkable(x: Float, y: Float, map: List<String>): Boolean {
    val tileX = floor(x).toInt()
    val tileY = floor(y).toInt()

    if (tileY !in map.indices) return false

    val row = map[tileY]
    if (tileX < 0 || tileX >= row.length) return false

    val info = tileInfoFor(row[tileX])
    return !info.blocking
}

private fun formatAddress(address: String?): String {
    if (address.isNullOrBlank()) return "--"
    return if (address.length <= 10) address else "${address.take(6)}...${address.takeLast(4)}"
}

private fun formatTier(tier: String): String =
    tier.lowercase(Locale.US)
        .split('_')
        .joinToString(" ") { part ->
            part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }

private fun formatBasisPoints(value: BigInteger?): String {
    if (value == null) return "--"
    val percentage = value.toDouble() / 100.0
    return String.format(Locale.US, "%.0f%%", percentage)
}

private fun formatTokenBalance(balance: BigInteger?): String {
    if (balance == null) return "--"
    val decimals = BigDecimal("1000000000000000000")
    val formatted = BigDecimal(balance)
        .divide(decimals, 2, RoundingMode.DOWN)
        .stripTrailingZeros()
        .toPlainString()
    return "$formatted KNOW"
}

private fun formatNumber(value: BigInteger?): String {
    if (value == null) return "--"
    return NumberFormat.getIntegerInstance(Locale.US).format(value)
}

private fun triggerSound(effect: SoundEffect) {
    // Placeholder for retro SFX hook once audio engine is wired.
}

private fun DrawScope.drawPixelArt(sprite: PixelArt, topLeft: Offset, tileSizePx: Float) {
    val rows = sprite.rows
    if (rows.isEmpty()) return
    val scaledRows = rows.size * SPRITE_SCALE
    val pixelSize = tileSizePx / scaledRows
    rows.forEachIndexed { rowIndex, row ->
        row.forEachIndexed { colIndex, char ->
            val color = sprite.palette[char] ?: return@forEachIndexed
            repeat(SPRITE_SCALE) { dy ->
                repeat(SPRITE_SCALE) { dx ->
                    drawRect(
                        color = color,
                        topLeft = Offset(
                            topLeft.x + (colIndex * SPRITE_SCALE + dx) * pixelSize,
                            topLeft.y + (rowIndex * SPRITE_SCALE + dy) * pixelSize
                        ),
                        size = Size(pixelSize, pixelSize)
                    )
                }
            }
        }
    }
}

private fun tileInfoFor(char: Char): TileInfo = identityCityLegend[char] ?: defaultTileInfo

private fun isFirstPersonWall(x: Int, y: Int): Boolean {
    if (x !in 0 until firstPersonMapWidth || y !in 0 until firstPersonMapHeight) return true
    return firstPersonLayout[y][x] != 0
}

private fun collidesWithWall(x: Float, y: Float): Boolean {
    val cellX = floor(x).toInt()
    val cellY = floor(y).toInt()
    return isFirstPersonWall(cellX, cellY)
}

private fun Float.format(digits: Int): String = "%.${digits}f".format(Locale.US, this)

private fun shadeColor(color: Color, factor: Float): Color {
    val clamped = factor.coerceIn(0f, 1f)
    return Color(
        red = (color.red * clamped).coerceIn(0f, 1f),
        green = (color.green * clamped).coerceIn(0f, 1f),
        blue = (color.blue * clamped).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}

private fun distance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

private fun simulateNpcTurn(turn: Int): List<String> {
    val rng = Random(turn)
    val events = mutableListOf<String>()
    val shuffled = npcAgents.shuffled(rng)
    shuffled.take(rng.nextInt(2, npcAgents.size)).forEach { agent ->
        val action = when (agent.role) {
            NpcRole.PRIMARY -> if (rng.nextBoolean()) {
                "submitted a new linked ID request."
            } else {
                "vouched on an open claim."
            }
            NpcRole.ORACLE -> if (rng.nextBoolean()) {
                "opened a duplicate investigation."
            } else {
                "boosted quorum on a pending primary claim."
            }
            NpcRole.CHALLENGER -> "staked KNOW to challenge a suspicious address."
            NpcRole.SYBIL -> "attempted to sneak in a fake claim."
        }
        events += "${agent.name} ${action}"
    }
    if (rng.nextInt(100) < 15) {
        events += "System audit uncovered inconsistent votes; cooldown applied."
    }
    return events
}

private fun findNearbyCitySpot(position: Offset, spots: List<CitySpot>, radius: Float = 0.75f): CitySpot? {
    val radiusSquared = radius * radius
    var closest: CitySpot? = null
    var bestDistance = Float.MAX_VALUE

    for (spot in spots) {
        val center = spot.center
        val dx = position.x - center.x
        val dy = position.y - center.y
        val dist = dx * dx + dy * dy

        if (dist < bestDistance) {
            bestDistance = dist
            closest = spot
        }
    }

    return if (closest != null && bestDistance <= radiusSquared) closest else null
}

private val firstPersonLayout = arrayOf(
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(1, 0, 0, 0, 2, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 3, 0, 0, 0, 3, 0, 0, 1),
    intArrayOf(1, 0, 3, 0, 0, 0, 3, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 4, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 4, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 5, 0, 0, 0, 5, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
)

private val firstPersonColors = mapOf(
    1 to RetroColors.NESDarkGray,
    2 to RetroColors.PrimaryID,
    3 to RetroColors.LinkedID,
    4 to RetroColors.Oracle,
    5 to RetroColors.Warning
)

private val firstPersonMapWidth = firstPersonLayout[0].size
private val firstPersonMapHeight = firstPersonLayout.size
private val demoEnemies = listOf(
    Enemy(position = Offset(5.5f, 5.5f), color = RetroColors.Error),
    Enemy(position = Offset(2.5f, 6.5f), color = RetroColors.PrimaryID)
)

private val npcAgents = listOf(
    NpcAgent("Astra", NpcRole.ORACLE),
    NpcAgent("Kento", NpcRole.PRIMARY),
    NpcAgent("Mira", NpcRole.CHALLENGER),
    NpcAgent("Glim", NpcRole.SYBIL),
    NpcAgent("Nova", NpcRole.PRIMARY)
)

class WorldState {
    var turn by mutableStateOf(1)
    val log = mutableStateListOf<TurnLogEntry>()
    val encounters = mutableStateListOf(
        EncounterRoom("enc_oracle", "Review a high-priority claim", Offset(2.5f, 3.5f)),
        EncounterRoom("enc_challenge", "Investigate duplicate evidence", Offset(6.5f, 5.5f)),
        EncounterRoom("enc_network", "Link a new identity shard", Offset(4.5f, 7.5f))
    )

    fun logEvent(message: String, turnOverride: Int? = null) {
        val entry = TurnLogEntry(turnOverride ?: turn, message)
        log += entry
        while (log.size > 32) {
            log.removeAt(0)
        }
    }

    fun advanceTurn() {
        turn += 1
        val events = simulateNpcTurn(turn)
        if (events.isEmpty()) {
            logEvent("Quiet turn. Parameters holding steady.", turn)
        } else {
            events.forEach { logEvent(it, turn) }
        }
        // refresh unresolved encounters occasionally
        encounters.forEach { if (it.resolved && turn % 3 == 0) it.resolved = false }
    }
}

enum class Screen {
    TITLE,
    IDENTITY_STATUS,
    CLAIM_VERIFICATION,
    VOUCH_SYSTEM,
    ACTIVE_CLAIMS,
    MY_VOUCHES,
    CLAIM_REWARDS,
    ORACLE_PANEL,
    IDENTITY_CITY,
    FIRST_PERSON,
    SETTINGS
}

@Composable
fun MainScreen() {
    val viewModel = remember { com.knomee.identity.viewmodel.IdentityViewModel() }
    var currentScreen by remember { mutableStateOf(Screen.TITLE) }
    val worldState = remember { WorldState() }

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
                Screen.IDENTITY_CITY -> IdentityCityScreen(
                    onBack = { currentScreen = Screen.TITLE },
                    worldState = worldState,
                    onOpenActiveClaims = { currentScreen = Screen.ACTIVE_CLAIMS },
                    onOpenChallenge = {
                        currentScreen = Screen.CLAIM_VERIFICATION
                    },
                    onOpenVouchSystem = { currentScreen = Screen.VOUCH_SYSTEM },
                    tier = currentTier,
                    address = currentAddress,
                    identityData = viewModel.identityData,
                    knomeeBalance = viewModel.knomeeTokenBalance,
                    votingWeight = viewModel.votingWeight,
                    activeClaims = viewModel.activeClaims,
                    governanceParams = viewModel.governanceParams,
                    isConnected = isConnected
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
                    onViewMyVouches = { currentScreen = Screen.MY_VOUCHES },
                    onViewRewards = { currentScreen = Screen.CLAIM_REWARDS },
                    onBack = { currentScreen = Screen.TITLE }
                )
                Screen.ACTIVE_CLAIMS -> ActiveClaimsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.VOUCH_SYSTEM }
                )
                Screen.MY_VOUCHES -> MyVouchesScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.VOUCH_SYSTEM }
                )
                Screen.CLAIM_REWARDS -> ClaimRewardsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.VOUCH_SYSTEM }
                )
                Screen.ORACLE_PANEL -> OraclePanelScreen(
                    onBack = { currentScreen = Screen.TITLE }
                )
                Screen.FIRST_PERSON -> FirstPersonDemoScreen(
                    onBack = { currentScreen = Screen.TITLE },
                    worldState = worldState
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
        RetroMenuButton("◆ IDENTITY CITY", onClick = { onNavigate(Screen.IDENTITY_CITY) })
        Spacer(modifier = Modifier.height(16.dp))
        RetroMenuButton("◆ FIRST PERSON DEMO", onClick = { onNavigate(Screen.FIRST_PERSON) })
        Spacer(modifier = Modifier.height(16.dp))
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
fun IdentityCityScreen(
    onBack: () -> Unit,
    worldState: WorldState,
    onOpenActiveClaims: () -> Unit,
    onOpenChallenge: () -> Unit,
    onOpenVouchSystem: () -> Unit,
    tier: String,
    address: String?,
    identityData: IdentityData?,
    knomeeBalance: BigInteger?,
    votingWeight: BigInteger?,
    activeClaims: List<ClaimData>,
    governanceParams: GovernanceParams?,
    isConnected: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    val pressedKeys = remember { mutableStateMapOf<ComposeKey, Boolean>() }
    var playerPosition by remember { mutableStateOf(Offset(2.5f, 2.5f)) }

    val mapData = remember { identityCityLayout }
    val mapHeight = mapData.size
    val mapWidth = mapData.maxOf { it.length }
    val spots = remember { identityCitySpots }
    var activeSpot by remember { mutableStateOf<CitySpot?>(null) }
    var dialogueSpot by remember { mutableStateOf<CitySpot?>(null) }
    var promptBlink by remember { mutableStateOf(true) }
    val trail = remember { mutableStateListOf<Offset>() }
    var facingDirection by remember { mutableStateOf(FacingDirection.UP) }
    var stepTick by remember { mutableStateOf(0) }
    var isMoving by remember { mutableStateOf(false) }
    var stepAccumulator by remember { mutableStateOf(0f) }
    var sparkLife by remember { mutableStateOf(0f) }
    var trailFadeTimer by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(650)
            promptBlink = !promptBlink
        }
    }

    LaunchedEffect(dialogueSpot) {
        if (dialogueSpot == null) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameNanos { it }
        val speedTilesPerSecond = 4f

        while (true) {
            val frameTime = withFrameNanos { it }
            val frameDelta = (frameTime - lastFrameTime).coerceAtMost(60_000_000L)
            val deltaSeconds = frameDelta / 1_000_000_000f
            lastFrameTime = frameTime

            val up = pressedKeys.isPressed(ComposeKey.DirectionUp) || pressedKeys.isPressed(ComposeKey.W)
            val down = pressedKeys.isPressed(ComposeKey.DirectionDown) || pressedKeys.isPressed(ComposeKey.S)
            val left = pressedKeys.isPressed(ComposeKey.DirectionLeft) || pressedKeys.isPressed(ComposeKey.A)
            val right = pressedKeys.isPressed(ComposeKey.DirectionRight) || pressedKeys.isPressed(ComposeKey.D)

            if (sparkLife > 0f) {
                sparkLife = (sparkLife - deltaSeconds * 1.5f).coerceAtLeast(0f)
            }

            var dx = 0f
            var dy = 0f

            if (left && !right) dx -= 1f
            if (right && !left) dx += 1f
            if (up && !down) dy -= 1f
            if (down && !up) dy += 1f

            val rawDx = dx
            val rawDy = dy
            val movingNow = rawDx != 0f || rawDy != 0f

            if (movingNow) {
                val magnitude = sqrt(dx * dx + dy * dy)
                if (magnitude > 0f) {
                    dx /= magnitude
                    dy /= magnitude
                }

                val moveX = dx * speedTilesPerSecond * deltaSeconds
                val moveY = dy * speedTilesPerSecond * deltaSeconds

                var newX = playerPosition.x
                var newY = playerPosition.y

                if (moveX != 0f) {
                    val candidateX = newX + moveX
                    if (isWalkable(candidateX, newY, mapData)) {
                        newX = candidateX
                    }
                }

                if (moveY != 0f) {
                    val candidateY = newY + moveY
                    if (isWalkable(newX, candidateY, mapData)) {
                        newY = candidateY
                    }
                }

                val margin = 1.5f
                playerPosition = Offset(
                    x = newX.coerceIn(margin, mapWidth - margin),
                    y = newY.coerceIn(margin, mapHeight - margin)
                )

                trail.add(playerPosition)
                if (trail.size > 16) {
                    trail.removeAt(0)
                }

                val absX = kotlin.math.abs(rawDx)
                val absY = kotlin.math.abs(rawDy)
                facingDirection = when {
                    absX > absY && rawDx > 0f -> FacingDirection.RIGHT
                    absX > absY && rawDx < 0f -> FacingDirection.LEFT
                    rawDy > 0f -> FacingDirection.DOWN
                    else -> FacingDirection.UP
                }

                stepTick = (stepTick + 1) % 12
                stepAccumulator += deltaSeconds
                if (stepAccumulator >= 0.28f) {
                    triggerSound(SoundEffect.STEP)
                    stepAccumulator = 0f
                }

                trailFadeTimer = 0f
            } else {
                stepAccumulator = 0f
                if (stepTick != 0) stepTick = 0
                trailFadeTimer += deltaSeconds
                if (trail.isNotEmpty() && trailFadeTimer >= 0.1f) {
                    trail.removeAt(0)
                    trailFadeTimer = 0f
                }
            }

            isMoving = movingNow

            val nearestSpot = findNearbyCitySpot(playerPosition, spots)
            if (nearestSpot?.id != activeSpot?.id) {
                activeSpot = nearestSpot
            }
            if (dialogueSpot != null && (nearestSpot == null || nearestSpot.id != dialogueSpot?.id)) {
                dialogueSpot = null
            }
        }
    }

    fun runSpotAction(spot: CitySpot, label: String, action: () -> Unit) {
        action()
        worldState.logEvent("You selected \"$label\" at ${spot.title}.")
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "IDENTITY CITY - EXPLORE",
                    style = RetroTypography.heading,
                    color = RetroColors.LinkedID
                )
                Text(
                    text = activeSpot?.let { "Nearby: ${it.title}" }
                        ?: "Roam the district to discover consensus hotspots.",
                    style = RetroTypography.caption,
                    color = RetroColors.NESGray
                )
            }

            Button(
                onClick = onBack,
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RetroColors.WindowBackground,
                    contentColor = RetroColors.NESWhite
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "◀ BACK TO MENU",
                    style = RetroTypography.button
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(0.72f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                val availableWidth = maxWidth
                val availableHeight = maxHeight
                val boardWidth = if (availableWidth < availableHeight * GOLDEN_RATIO.toFloat()) {
                    availableWidth
                } else {
                    availableHeight * GOLDEN_RATIO.toFloat()
                }
                val boardHeight = boardWidth / GOLDEN_RATIO.toFloat()
                val boardModifier = Modifier
                    .size(boardWidth, boardHeight)
                    .border(3.dp, RetroColors.BorderColor, RoundedCornerShape(6.dp))
                    .background(RetroColors.NESBlack, RoundedCornerShape(6.dp))

                Box(
                    modifier = boardModifier
                ) {
                    val actionsForSpot: (CitySpot) -> List<Pair<String, () -> Unit>> = { spot ->
                        val baseActions = when (spot.id) {
                            "oracle_tower" -> listOf("Review Active Claims" to onOpenActiveClaims)
                        "challenge_pillar" -> listOf("Open Challenge Workflow" to onOpenChallenge)
                        "network_node" -> listOf("Open Vouch System" to onOpenVouchSystem)
                        else -> emptyList()
                    }
                    baseActions.map { (label, handler) ->
                        label to { runSpotAction(spot, label, handler) }
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            fun updateKey(key: ComposeKey, isDown: Boolean): Boolean {
                                if (isDown) {
                                    pressedKeys[key] = true
                                } else {
                                    pressedKeys.remove(key)
                                }
                                return true
                            }

                            when (event.key) {
                                ComposeKey.DirectionUp, ComposeKey.W -> updateKey(event.key, event.type == KeyEventType.KeyDown)
                                ComposeKey.DirectionDown, ComposeKey.S -> updateKey(event.key, event.type == KeyEventType.KeyDown)
                                ComposeKey.DirectionLeft, ComposeKey.A -> updateKey(event.key, event.type == KeyEventType.KeyDown)
                                ComposeKey.DirectionRight, ComposeKey.D -> updateKey(event.key, event.type == KeyEventType.KeyDown)
                                ComposeKey.Spacebar -> {
                                    if (event.type == KeyEventType.KeyDown) {
                                        val opening = dialogueSpot == null && activeSpot != null
                                        dialogueSpot = if (dialogueSpot == null) activeSpot else null
                                        if (opening) {
                                            sparkLife = 1f
                                            triggerSound(SoundEffect.INTERACT)
                                        }
                                    }
                                    true
                                }
                                ComposeKey.Enter -> {
                                    if (event.type == KeyEventType.KeyDown) {
                                        val targetSpot = dialogueSpot ?: activeSpot
                                        val actions = targetSpot?.let { actionsForSpot(it) }.orEmpty()
                                        if (actions.isNotEmpty()) {
                                            actions.first().second()
                                            dialogueSpot = null
                                            focusRequester.requestFocus()
                                            true
                                        } else if (dialogueSpot != null) {
                                            dialogueSpot = null
                                            true
                                        } else {
                                            false
                                        }
                                    } else {
                                        true
                                    }
                                }
                                ComposeKey.Escape -> {
                                    if (event.type == KeyEventType.KeyUp && dialogueSpot != null) {
                                        dialogueSpot = null
                                    } else if (event.type == KeyEventType.KeyUp) {
                                        onBack()
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                ) {
                    val tileSizePx = min(size.width / mapWidth, size.height / mapHeight)
                    val boardWidth = tileSizePx * mapWidth
                    val boardHeight = tileSizePx * mapHeight
                    val origin = Offset(
                        x = (size.width - boardWidth) / 2f,
                        y = (size.height - boardHeight) / 2f
                    )

                    for (y in 0 until mapHeight) {
                        val row = mapData[y]
                        for (x in 0 until mapWidth) {
                            val info = tileInfoFor(row[x])
                            val topLeft = Offset(origin.x + x * tileSizePx, origin.y + y * tileSizePx)
                            val tileSize = Size(tileSizePx, tileSizePx)

                            drawRect(
                                color = info.fill,
                                topLeft = topLeft,
                                size = tileSize
                            )

                            info.sprite?.let { sprite ->
                                drawPixelArt(sprite, topLeft, tileSizePx)
                            }

                            info.border?.let { borderColor ->
                                drawRect(
                                    color = borderColor,
                                    topLeft = topLeft,
                                    size = tileSize,
                                    style = Stroke(width = 3f)
                                )
                            }
                        }
                    }

                    val highlightSpot = dialogueSpot ?: activeSpot
                    highlightSpot?.let { spot ->
                        val highlightTopLeft = Offset(
                            origin.x + spot.tile.x * tileSizePx,
                            origin.y + spot.tile.y * tileSizePx
                        )
                        drawRect(
                            color = spot.color.copy(alpha = 0.25f),
                            topLeft = highlightTopLeft,
                            size = Size(tileSizePx, tileSizePx)
                        )
                        drawRect(
                            color = spot.color,
                            topLeft = highlightTopLeft,
                            size = Size(tileSizePx, tileSizePx),
                            style = Stroke(width = 4f)
                        )
                    }

                    for (x in 0..mapWidth) {
                        drawLine(
                            color = RetroColors.NESDarkGray,
                            start = Offset(origin.x + x * tileSizePx, origin.y),
                            end = Offset(origin.x + x * tileSizePx, origin.y + boardHeight),
                            strokeWidth = 1f
                        )
                    }

                    for (y in 0..mapHeight) {
                        drawLine(
                            color = RetroColors.NESDarkGray,
                            start = Offset(origin.x, origin.y + y * tileSizePx),
                            end = Offset(origin.x + boardWidth, origin.y + y * tileSizePx),
                            strokeWidth = 1f
                        )
                    }

                    if (trail.isNotEmpty()) {
                        trail.forEachIndexed { index, point ->
                            val progress = (index + 1).toFloat() / trail.size
                            drawCircle(
                                color = RetroColors.PrimaryID.copy(alpha = 0.12f * progress),
                                radius = tileSizePx * 0.25f * progress,
                                center = Offset(origin.x + point.x * tileSizePx, origin.y + point.y * tileSizePx)
                            )
                        }
                    }

                    val playerX = origin.x + playerPosition.x * tileSizePx
                    val playerY = origin.y + playerPosition.y * tileSizePx
                    val bobOffsetPx = if (isMoving) {
                        if (stepTick < 6) tileSizePx * 0.05f else -tileSizePx * 0.05f
                    } else 0f
                    val adjustedPlayerY = playerY + bobOffsetPx

                    drawRoundRect(
                        color = RetroColors.PrimaryID,
                        topLeft = Offset(playerX - tileSizePx * 0.35f, adjustedPlayerY - tileSizePx * 0.35f),
                        size = Size(tileSizePx * 0.7f, tileSizePx * 0.7f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(tileSizePx * 0.1f, tileSizePx * 0.1f)
                    )

                    val facingVector = when (facingDirection) {
                        FacingDirection.UP -> Offset(0f, -tileSizePx * 0.4f)
                        FacingDirection.DOWN -> Offset(0f, tileSizePx * 0.4f)
                        FacingDirection.LEFT -> Offset(-tileSizePx * 0.4f, 0f)
                        FacingDirection.RIGHT -> Offset(tileSizePx * 0.4f, 0f)
                    }

                    drawLine(
                        color = RetroColors.NESWhite,
                        start = Offset(playerX, adjustedPlayerY),
                        end = Offset(playerX + facingVector.x, adjustedPlayerY + facingVector.y),
                        strokeWidth = 4f
                    )

                    if (sparkLife > 0f) {
                        val alpha = sparkLife.coerceIn(0f, 1f)
                        val sparkRadius = tileSizePx * (0.55f + (1f - sparkLife) * 0.35f)
                        val sparkColor = RetroColors.BorderColor.copy(alpha = 0.5f * alpha)
                        val center = Offset(playerX, adjustedPlayerY)

                        drawCircle(
                            color = sparkColor,
                            radius = sparkRadius,
                            center = center,
                            style = Stroke(width = 3f)
                        )

                        drawLine(
                            color = sparkColor,
                            start = Offset(center.x - sparkRadius / 2f, center.y),
                            end = Offset(center.x + sparkRadius / 2f, center.y),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = sparkColor,
                            start = Offset(center.x, center.y - sparkRadius / 2f),
                            end = Offset(center.x, center.y + sparkRadius / 2f),
                            strokeWidth = 2f
                        )
                    }
                }

                val inlineSpot = activeSpot
                val inlineActions = inlineSpot?.let { actionsForSpot(it) }.orEmpty()
                if (inlineSpot != null && dialogueSpot == null && inlineActions.isNotEmpty()) {
                    SpotActionPanel(
                        spot = inlineSpot,
                        actions = inlineActions,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .widthIn(max = 220.dp)
                    )
                }

                dialogueSpot?.let { spot ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                        color = RetroColors.WindowBackground.copy(alpha = 0.96f),
                        tonalElevation = 12.dp,
                        border = BorderStroke(2.dp, spot.color)
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 420.dp)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = spot.title,
                                style = RetroTypography.heading,
                                color = spot.color
                            )
                            Text(
                                text = spot.description,
                                style = RetroTypography.body,
                                color = RetroColors.NESWhite
                            )
                            val contextualActions = actionsForSpot(spot)
                            if (contextualActions.isNotEmpty()) {
                                contextualActions.forEach { (label, action) ->
                                    Button(
                                        onClick = action,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = spot.color.copy(alpha = 0.8f),
                                            contentColor = RetroColors.NESBlack
                                        ),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(label, style = RetroTypography.button)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Press SPACE or ENTER to close",
                                    style = RetroTypography.caption,
                                    color = RetroColors.NESGray
                                )
                                Button(
                                    onClick = {
                                        dialogueSpot = null
                                        focusRequester.requestFocus()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = spot.color.copy(alpha = 0.85f),
                                        contentColor = RetroColors.NESBlack
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Close",
                                        style = RetroTypography.button
                                    )
                                }
                            }
                        }
                    }
                }

                if (activeSpot != null && dialogueSpot == null && promptBlink) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = RetroColors.NESDarkGray.copy(alpha = 0.9f),
                        tonalElevation = 4.dp
                    ) {
                        Text(
                            text = "Press SPACE to interact with ${activeSpot!!.title}",
                            style = RetroTypography.caption,
                            color = RetroColors.NESWhite,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            }

            Column(
                modifier = Modifier
                    .weight(0.28f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IdentityCityHud(
                    tier = tier,
                    address = address,
                    identityData = identityData,
                    knomeeBalance = knomeeBalance,
                    votingWeight = votingWeight,
                    activeClaims = activeClaims,
                    governanceParams = governanceParams,
                    isConnected = isConnected,
                    modifier = Modifier.fillMaxWidth()
                )
                TurnController(
                    turnNumber = worldState.turn,
                    onAdvanceTurn = { worldState.advanceTurn() },
                    logEntries = worldState.log,
                    modifier = Modifier.fillMaxWidth()
                )
                MiniMapCard(
                    mapData = mapData,
                    playerPosition = playerPosition,
                    activeSpot = activeSpot,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Move with Arrow Keys or WASD. SPACE to interact. ESC to leave the district.",
                style = RetroTypography.caption,
                color = RetroColors.NESGray,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Legend: # Walls | B Buildings | ~ Pathway | O Oracle | H Guild Hall | P Challenge | N Network | S Stage | K Knowledge | G Grey Ghost | T Teleport | D Dockyard",
                style = RetroTypography.caption,
                color = RetroColors.NESGray,
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
fun FirstPersonDemoScreen(onBack: () -> Unit, worldState: WorldState) {
    val focusRequester = remember { FocusRequester() }
    val pressedKeys = remember { mutableStateMapOf<ComposeKey, Boolean>() }
    var playerPosition by remember { mutableStateOf(Offset(3.5f, 3.5f)) }
    var playerAngle by remember { mutableStateOf(0f) }
    var activeEncounter by remember { mutableStateOf<EncounterRoom?>(null) }
    val wanderRng = remember { Random(System.currentTimeMillis()) }
    val enemies = remember {
        mutableStateListOf<Enemy>().apply {
            addAll(demoEnemies.map { it.copy(position = it.position) })
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        var lastFrame = withFrameNanos { it }
        val moveSpeed = 2.8f
        val rotationSpeed = 1.8f

        while (true) {
            val frameTime = withFrameNanos { it }
            val deltaSeconds = ((frameTime - lastFrame).coerceAtMost(64_000_000L)) / 1_000_000_000f
            lastFrame = frameTime

            val forward = pressedKeys.isPressed(ComposeKey.W) || pressedKeys.isPressed(ComposeKey.DirectionUp)
            val backward = pressedKeys.isPressed(ComposeKey.S) || pressedKeys.isPressed(ComposeKey.DirectionDown)
            val strafeLeft = pressedKeys.isPressed(ComposeKey.A)
            val strafeRight = pressedKeys.isPressed(ComposeKey.D)
            val turnLeft = pressedKeys.isPressed(ComposeKey.DirectionLeft) || pressedKeys.isPressed(ComposeKey.Q)
            val turnRight = pressedKeys.isPressed(ComposeKey.DirectionRight) || pressedKeys.isPressed(ComposeKey.E)

            var newAngle = playerAngle
            if (turnLeft && !turnRight) {
                newAngle -= rotationSpeed * deltaSeconds
            } else if (turnRight && !turnLeft) {
                newAngle += rotationSpeed * deltaSeconds
            }

            val dirX = cos(newAngle)
            val dirY = sin(newAngle)
            val perpX = -dirY
            val perpY = dirX

            var newPosition = playerPosition

            if (forward) {
                val nextX = newPosition.x + dirX * moveSpeed * deltaSeconds
                val nextY = newPosition.y + dirY * moveSpeed * deltaSeconds
                if (!collidesWithWall(nextX, newPosition.y)) {
                    newPosition = newPosition.copy(x = nextX)
                }
                if (!collidesWithWall(newPosition.x, nextY)) {
                    newPosition = newPosition.copy(y = nextY)
                }
            }

            if (backward) {
                val nextX = newPosition.x - dirX * moveSpeed * deltaSeconds
                val nextY = newPosition.y - dirY * moveSpeed * deltaSeconds
                if (!collidesWithWall(nextX, newPosition.y)) {
                    newPosition = newPosition.copy(x = nextX)
                }
                if (!collidesWithWall(newPosition.x, nextY)) {
                    newPosition = newPosition.copy(y = nextY)
                }
            }

            if (strafeLeft && !strafeRight) {
                val nextX = newPosition.x + perpX * moveSpeed * deltaSeconds
                val nextY = newPosition.y + perpY * moveSpeed * deltaSeconds
                if (!collidesWithWall(nextX, newPosition.y)) {
                    newPosition = newPosition.copy(x = nextX)
                }
                if (!collidesWithWall(newPosition.x, nextY)) {
                    newPosition = newPosition.copy(y = nextY)
                }
            } else if (strafeRight && !strafeLeft) {
                val nextX = newPosition.x - perpX * moveSpeed * deltaSeconds
                val nextY = newPosition.y - perpY * moveSpeed * deltaSeconds
                if (!collidesWithWall(nextX, newPosition.y)) {
                    newPosition = newPosition.copy(x = nextX)
                }
                if (!collidesWithWall(newPosition.x, nextY)) {
                    newPosition = newPosition.copy(y = nextY)
                }
            }

            playerAngle = newAngle
            playerPosition = newPosition

            val encounter = worldState.encounters.firstOrNull { room ->
                !room.resolved && distance(room.position, newPosition) < 0.9f
            }
            activeEncounter = encounter

            enemies.forEach { enemy ->
                val jitter = Offset(
                    (wanderRng.nextFloat() - 0.5f) * deltaSeconds * 0.6f,
                    (wanderRng.nextFloat() - 0.5f) * deltaSeconds * 0.6f
                )
                val candidate = enemy.position + jitter
                if (!collidesWithWall(candidate.x, enemy.position.y)) {
                    enemy.position = enemy.position.copy(x = candidate.x)
                }
                if (!collidesWithWall(enemy.position.x, candidate.y)) {
                    enemy.position = enemy.position.copy(y = candidate.y)
                }
            }
        }
    }

    fun resolveEncounter(success: Boolean) {
        val current = activeEncounter ?: return
        current.resolved = true
        if (success) {
            worldState.logEvent("Resolved encounter: ${current.description}")
        } else {
            worldState.logEvent("Skipped encounter: ${current.description}")
        }
        activeEncounter = null
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FIRST PERSON DEMO",
                    style = RetroTypography.heading,
                    color = RetroColors.LinkedID
                )
                Text(
                    text = "WASD to move, arrows/QE to rotate, ESC to exit",
                    style = RetroTypography.caption,
                    color = RetroColors.NESGray
                )
            }
            Button(
                onClick = onBack,
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RetroColors.WindowBackground,
                    contentColor = RetroColors.NESWhite
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "◀ BACK",
                    style = RetroTypography.button
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

    Surface(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
            color = RetroColors.NESBlack,
            tonalElevation = 8.dp,
            border = BorderStroke(3.dp, RetroColors.BorderColor)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        fun updateKey(key: ComposeKey, isDown: Boolean): Boolean {
                            if (isDown) {
                                pressedKeys[key] = true
                            } else {
                                pressedKeys.remove(key)
                            }
                            return true
                        }

                        when (event.key) {
                            ComposeKey.W, ComposeKey.S, ComposeKey.A, ComposeKey.D,
                            ComposeKey.Q, ComposeKey.E,
                            ComposeKey.DirectionUp, ComposeKey.DirectionDown,
                            ComposeKey.DirectionLeft, ComposeKey.DirectionRight -> {
                                updateKey(event.key, event.type == KeyEventType.KeyDown)
                            }
                            ComposeKey.Spacebar -> {
                                if (activeEncounter != null) {
                                    resolveEncounter(true)
                                    true
                                } else false
                            }
                            ComposeKey.Escape -> {
                                if (event.type == KeyEventType.KeyUp) {
                                    onBack()
                                }
                                true
                            }
                            ComposeKey.Enter -> {
                                if (activeEncounter != null && event.type == KeyEventType.KeyDown) {
                                    resolveEncounter(true)
                                    true
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val halfHeight = height / 2f

                drawRect(
                    color = RetroColors.NESDarkGray,
                    size = Size(width, halfHeight)
                )
                drawRect(
                    color = RetroColors.ScreenBackground,
                    topLeft = Offset(0f, halfHeight),
                    size = Size(width, halfHeight)
                )
                val tile = 8f
                for (y in 0 until (halfHeight / tile).toInt()) {
                    val alpha = 0.08f * (y % 2)
                    drawRect(
                        color = RetroColors.NESGray.copy(alpha = alpha),
                        topLeft = Offset(0f, halfHeight + y * tile),
                        size = Size(width, tile)
                    )
                }

                val dirX = cos(playerAngle)
                val dirY = sin(playerAngle)
                val planeX = -dirY * 0.66f
                val planeY = dirX * 0.66f

                val rayCount = width.toInt().coerceIn(160, 480)
                val columnWidth = width / rayCount
                val depthBuffer = FloatArray(rayCount)

                for (column in 0 until rayCount) {
                    val cameraX = 2f * column / rayCount - 1f
                    val rayDirX = dirX + planeX * cameraX
                    val rayDirY = dirY + planeY * cameraX

                    var mapX = floor(playerPosition.x).toInt()
                    var mapY = floor(playerPosition.y).toInt()

                    val deltaDistX = if (rayDirX == 0f) Float.MAX_VALUE else abs(1f / rayDirX)
                    val deltaDistY = if (rayDirY == 0f) Float.MAX_VALUE else abs(1f / rayDirY)

                    var stepX: Int
                    var stepY: Int
                    var sideDistX: Float
                    var sideDistY: Float

                    if (rayDirX < 0f) {
                        stepX = -1
                        sideDistX = (playerPosition.x - mapX) * deltaDistX
                    } else {
                        stepX = 1
                        sideDistX = (mapX + 1f - playerPosition.x) * deltaDistX
                    }
                    if (rayDirY < 0f) {
                        stepY = -1
                        sideDistY = (playerPosition.y - mapY) * deltaDistY
                    } else {
                        stepY = 1
                        sideDistY = (mapY + 1f - playerPosition.y) * deltaDistY
                    }

                    var hit = false
                    var side = 0
                    var wallId = 0
                    var iterations = 0

                    while (!hit && iterations < 64) {
                        if (sideDistX < sideDistY) {
                            sideDistX += deltaDistX
                            mapX += stepX
                            side = 0
                        } else {
                            sideDistY += deltaDistY
                            mapY += stepY
                            side = 1
                        }
                        if (mapX in 0 until firstPersonMapWidth && mapY in 0 until firstPersonMapHeight) {
                            wallId = firstPersonLayout[mapY][mapX]
                            if (wallId > 0) {
                                hit = true
                            }
                        } else {
                            hit = true
                        }
                        iterations++
                    }

                    if (!hit) continue

                    val perpWallDist = if (side == 0) {
                        sideDistX - deltaDistX
                    } else {
                        sideDistY - deltaDistY
                    }.coerceAtLeast(0.0001f)
                    depthBuffer[column] = perpWallDist

                    val lineHeight = height / perpWallDist
                    val drawStart = (halfHeight - lineHeight / 2f).coerceIn(0f, height)
                    val drawEnd = (halfHeight + lineHeight / 2f).coerceIn(0f, height)
                    val columnX = column * columnWidth

                    val baseColor = firstPersonColors[wallId] ?: RetroColors.NESGray
                    val hitCoord = if (side == 0) {
                        playerPosition.y + perpWallDist * rayDirY
                    } else {
                        playerPosition.x + perpWallDist * rayDirX
                    }
                    val textureCoord = hitCoord - floor(hitCoord)
                    val stripe = ((textureCoord * 8f).toInt() % 2 == 0)
                    val distanceShade = if (side == 1) 0.7f else 1f
                    val stripeShade = if (stripe) 1f else 0.85f
                    val shadedColor = shadeColor(baseColor, distanceShade * stripeShade / (1f + perpWallDist * 0.05f))

                    drawRect(
                        color = shadedColor,
                        topLeft = Offset(columnX, drawStart),
                        size = Size(columnWidth + 1f, drawEnd - drawStart)
                    )
                }

                // Render enemies (billboard sprites)
                val invDet = 1f / (planeX * dirY - dirX * planeY)

                val spriteEntities = buildList {
                    enemies.forEach { add(it.position to it.color) }
                    worldState.encounters.filter { !it.resolved }.forEach {
                        add(it.position to RetroColors.Info)
                    }
                }.sortedByDescending { (pos, _) ->
                    val dx = pos.x - playerPosition.x
                    val dy = pos.y - playerPosition.y
                    dx * dx + dy * dy
                }

                spriteEntities.forEach { (pos, spriteColor) ->
                    val spriteX = pos.x - playerPosition.x
                    val spriteY = pos.y - playerPosition.y

                    val transformX = invDet * (dirY * spriteX - dirX * spriteY)
                    val transformY = invDet * (-planeY * spriteX + planeX * spriteY)
                    if (transformY <= 0f) return@forEach

                    val spriteScreenX = width / 2f * (1f + transformX / transformY)
                    val spriteScale = (height / transformY).coerceAtMost(height * 1.5f)
                    val spriteHeight = spriteScale
                    val spriteWidth = spriteScale * 0.6f

                    val drawStartY = (halfHeight - spriteHeight / 2f).coerceIn(0f, height)
                    val drawEndY = (halfHeight + spriteHeight / 2f).coerceIn(0f, height)
                    val drawStartX = (spriteScreenX - spriteWidth / 2f).coerceIn(0f, width)
                    val drawEndX = (spriteScreenX + spriteWidth / 2f).coerceIn(0f, width)

                    val centerColumn = ((spriteScreenX / columnWidth).toInt()).coerceIn(0, rayCount - 1)
                    if (transformY < depthBuffer[centerColumn]) {
                    val spriteColor = shadeColor(spriteColor, (1.2f / transformY).coerceIn(0.35f, 1f))
                    drawRoundRect(
                        color = spriteColor,
                        topLeft = Offset(drawStartX, drawStartY),
                        size = Size(drawEndX - drawStartX, drawEndY - drawStartY),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(spriteWidth * 0.25f, spriteWidth * 0.25f)
                        )
                        drawCircle(
                            color = RetroColors.NESWhite.copy(alpha = 0.6f),
                            radius = (drawEndX - drawStartX) * 0.12f,
                            center = Offset(spriteScreenX, drawStartY + spriteHeight * 0.35f)
                        )
                    }
                }

                // Crosshair
                val center = Offset(width / 2f, height / 2f)
                drawLine(
                    color = RetroColors.NESWhite,
                    start = center + Offset(-6f, 0f),
                    end = center + Offset(6f, 0f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = RetroColors.NESWhite,
                    start = center + Offset(0f, -6f),
                    end = center + Offset(0f, 6f),
                    strokeWidth = 2f
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Pos: (${playerPosition.x.format(2)}, ${playerPosition.y.format(2)}) | Angle: ${(playerAngle * 57.2958f).format(1)}°",
            style = RetroTypography.caption,
            color = RetroColors.NESGray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        activeEncounter?.let { encounter ->
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                color = RetroColors.WindowBackground.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, RetroColors.Info)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Encounter",
                        style = RetroTypography.heading,
                        color = RetroColors.Info
                    )
                    Text(
                        text = encounter.description,
                        style = RetroTypography.body,
                        color = RetroColors.NESWhite
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { resolveEncounter(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RetroColors.Success,
                                contentColor = RetroColors.NESBlack
                            )
                        ) {
                            Text("Approve", style = RetroTypography.button)
                        }
                        Button(
                            onClick = { resolveEncounter(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RetroColors.Warning,
                                contentColor = RetroColors.NESBlack
                            )
                        ) {
                            Text("Skip", style = RetroTypography.button)
                        }
                    }
                    Text(
                        text = "Press SPACE/ENTER to approve instantly.",
                        style = RetroTypography.caption,
                        color = RetroColors.NESGray
                    )
                }
            }
        }
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
