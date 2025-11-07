package com.knomee.identity.ui

import androidx.compose.ui.graphics.Color
import com.knomee.identity.theme.RetroColors

/**
 * Improved 16x16 pixel art sprites for the cyberpunk identity game
 * Each sprite is 16 characters wide and 16 lines tall
 */

// ═══════════════════════════════════════════════════════════════════════
// PLAYER CHARACTER SPRITES - 16x16 PIXELS
// ═══════════════════════════════════════════════════════════════════════

/**
 * GREY GHOST (Tier 0 - Unverified)
 * Visual: Tattered hoodie, glitchy appearance, uncertain identity
 */
val greyGhostIdleDown = PixelArt(
    rows = listOf(
        "                ",
        "   ...HHHH...   ",
        "  .H.hhhhhh.H.  ",
        "  Hh..hXXh..hH  ",
        "  Hh.hXXXXh.hH  ",
        "  .H..XXXX..H.  ",
        "   ...hhhh...   ",
        "    GGGGGGGG    ",
        "   GGGGGGGGGG   ",
        "  GG.GGGGGG.GG  ",
        "  GGG......GGG  ",
        "   GGGG..GGGG   ",
        "    GGG..GGG    ",
        "    LL    RR    ",
        "    LL    RR    ",
        "   LLL    RRR   "
    ),
    palette = mapOf(
        'H' to Color(0xFF3A3A3A), // Dark hood
        'h' to Color(0xFF505050), // Hood shadow
        'X' to Color(0xFF8B0000), // Unverified red eyes
        '.' to Color(0xFF606060), // Glitch pixels
        'G' to Color(0xFF404040), // Grey body
        'L' to Color(0xFF353535), // Left leg
        'R' to Color(0xFF353535)  // Right leg
    )
)

val greyGhostWalkDown1 = PixelArt(
    rows = listOf(
        "                ",
        "   ...HHHH...   ",
        "  .H.hhhhhh.H.  ",
        "  Hh..hXXh..hH  ",
        "  Hh.hXXXXh.hH  ",
        "  .H..XXXX..H.  ",
        "   ...hhhh...   ",
        "    GGGGGGGG    ",
        "   GGGGGGGGGG   ",
        "  GG.GGGGGG.GG  ",
        "  GGG......GGG  ",
        "   GGGG..GGGG   ",
        "    GG    GG    ",
        "   LLL     RR   ",
        "   LL      RR   ",
        "  LLL      RR   "
    ),
    palette = mapOf(
        'H' to Color(0xFF3A3A3A),
        'h' to Color(0xFF505050),
        'X' to Color(0xFF8B0000),
        '.' to Color(0xFF606060),
        'G' to Color(0xFF404040),
        'L' to Color(0xFF353535),
        'R' to Color(0xFF353535)
    )
)

val greyGhostWalkDown2 = PixelArt(
    rows = listOf(
        "                ",
        "   ...HHHH...   ",
        "  .H.hhhhhh.H.  ",
        "  Hh..hXXh..hH  ",
        "  Hh.hXXXXh.hH  ",
        "  .H..XXXX..H.  ",
        "   ...hhhh...   ",
        "    GGGGGGGG    ",
        "   GGGGGGGGGG   ",
        "  GG.GGGGGG.GG  ",
        "  GGG......GGG  ",
        "   GGGG..GGGG   ",
        "    GG    GG    ",
        "    LL   RRR    ",
        "    LL   RR     ",
        "    LL   RRR    "
    ),
    palette = mapOf(
        'H' to Color(0xFF3A3A3A),
        'h' to Color(0xFF505050),
        'X' to Color(0xFF8B0000),
        '.' to Color(0xFF606060),
        'G' to Color(0xFF404040),
        'L' to Color(0xFF353535),
        'R' to Color(0xFF353535)
    )
)

val greyGhostWalkUp = PixelArt(
    rows = listOf(
        "                ",
        "   ..HHHHHH..   ",
        "  .HHhhhhhhhH.  ",
        "  Hhh.hhhh.hhH  ",
        "  Hh........hH  ",
        "  .H........H.  ",
        "   ..HHHHHH..   ",
        "    GGGGGGGG    ",
        "   GGGGGGGGGG   ",
        "  GGGGGGGGGGGG  ",
        "  GG........GG  ",
        "   GGGG..GGGG   ",
        "    GGG..GGG    ",
        "    LL    RR    ",
        "    LL    RR    ",
        "   LLL    RRR   "
    ),
    palette = mapOf(
        'H' to Color(0xFF3A3A3A),
        'h' to Color(0xFF505050),
        '.' to Color(0xFF606060),
        'G' to Color(0xFF404040),
        'L' to Color(0xFF353535),
        'R' to Color(0xFF353535)
    )
)

val greyGhostWalkRight = PixelArt(
    rows = listOf(
        "                ",
        "    ...HHH..    ",
        "   .H.hhhhh.    ",
        "   Hh.hXXXh.    ",
        "   Hh.hXXXh..   ",
        "   .H..XXX...   ",
        "    ..hhhh..    ",
        "     GGGGGGG    ",
        "    GGGGGGGGG   ",
        "   GGG.GGGGGG   ",
        "   GGGGG...GG   ",
        "    GGGGGG..    ",
        "     GGGG       ",
        "     LLL  RR    ",
        "     LL   RR    ",
        "    LLL   RRR   "
    ),
    palette = mapOf(
        'H' to Color(0xFF3A3A3A),
        'h' to Color(0xFF505050),
        'X' to Color(0xFF8B0000),
        '.' to Color(0xFF606060),
        'G' to Color(0xFF404040),
        'L' to Color(0xFF353535),
        'R' to Color(0xFF353535)
    )
)

/**
 * PRIMARY ID (Tier 2 - Verified Citizen)
 * Visual: Sleek cyber-suit, confident posture, cyan accents
 */
val primaryIdIdleDown = PixelArt(
    rows = listOf(
        "                ",
        "     FFFFFF     ",
        "    FSSSSSF     ",
        "   FScCCCcsF    ",
        "   FScCCCCsF    ",
        "    FSCCCSF     ",
        "     FFFFF      ",
        "    BBBBBBB     ",
        "   BCCCCCCCB    ",
        "  BCwCCCCCwCB   ",
        "  BC........CB  ",
        "   BCCB..BCCB   ",
        "    BCB..BCB    ",
        "    LLL  RRR    ",
        "    LLL  RRR    ",
        "   LLLL  RRRR   "
    ),
    palette = mapOf(
        'F' to Color(0xFF2C2C2C), // Face/helmet dark
        'S' to Color(0xFF505050), // Face shadow
        'c' to RetroColors.PrimaryID.copy(alpha = 0.7f), // Cyan eyes
        'C' to RetroColors.PrimaryID, // Cyan bright
        'B' to Color(0xFF1A1A2E), // Body dark
        'w' to Color(0xFFFFFFFF), // White highlights
        '.' to RetroColors.PrimaryID.copy(alpha = 0.3f), // Suit lines
        'L' to Color(0xFF16213E), // Legs
        'R' to Color(0xFF16213E)
    )
)

/**
 * ORACLE (Tier 3 - Elite Validator)
 * Visual: Purple robes, floating crown, ethereal presence
 */
val oracleIdleDown = PixelArt(
    rows = listOf(
        "   PP  pp  PP   ",
        "  PPPP..ppPPPP  ",
        "   P..PP..P..P  ",
        "   .FFFFFFFFFF  ",
        "  FFoOOOOOooFF  ",
        "  FoOOOOOOOOoF  ",
        "   FoOOOOOoF    ",
        "    FFFFFF      ",
        "    RRRRRRRR    ",
        "   RPRRRRRRPR   ",
        "  RPPPP..PPPPR  ",
        "  RP........PR  ",
        "   RRRR..RRRR   ",
        "    RRR..RRR    ",
        "    LL    RR    ",
        "   LLL    RRR   "
    ),
    palette = mapOf(
        'P' to RetroColors.Oracle, // Purple crown
        'p' to RetroColors.Oracle.copy(alpha = 0.6f), // Fading crown
        '.' to RetroColors.Oracle.copy(alpha = 0.3f), // Ethereal glow
        'F' to Color(0xFF2C2C2C), // Face
        'o' to Color(0xFFFFD700), // Gold third eye
        'O' to RetroColors.Oracle.copy(alpha = 0.9f), // Eye glow
        'R' to Color(0xFF4A148C), // Purple robes
        'L' to Color(0xFF6A1B9A), // Robe legs
        'R' to Color(0xFF6A1B9A)
    )
)

// ═══════════════════════════════════════════════════════════════════════
// NPC SPRITES - 16x16 PIXELS
// ═══════════════════════════════════════════════════════════════════════

/**
 * OLD MAN DECKARD - Grey Ghost quest giver
 * Former Oracle, now helps newcomers in Greytown
 */
val npcDeckard = PixelArt(
    rows = listOf(
        "                ",
        "     .WWW.      ",
        "    WFFFFFW     ",
        "   WFfFFFfFW    ",
        "   WFfFFFfFW    ",
        "    WFFFFFWW    ",
        "     WWWWWBW    ",
        "    CCCCCCCW    ",
        "   CCCCCCCCCW   ",
        "  CCCCCCCCCCCW  ",
        "  CCC......CCW  ",
        "   CCCC..CCCC   ",
        "    CCC..CCC    ",
        "    LL    RR    ",
        "    LLL   RRR   ",
        "   LLLL   RRRR  "
    ),
    palette = mapOf(
        'W' to Color(0xFFE0E0E0), // White hair/beard
        'F' to Color(0xFFC4A484), // Weathered face
        'f' to Color(0xFF8B7355), // Face shadows
        'B' to Color(0xFF654321), // Brown neck
        'C' to Color(0xFF5C4033), // Brown trench coat
        '.' to Color(0xFF3E2723), // Coat details
        'L' to Color(0xFF4A3728), // Left leg
        'R' to Color(0xFF4A3728)  // Right leg
    )
)

/**
 * GUILD MASTER CHEN - Primary Plaza quest giver
 * Wise validator, runs the verification guild
 */
val npcGuildMaster = PixelArt(
    rows = listOf(
        "                ",
        "      RRR       ",
        "     RRRRR      ",
        "    RaFFFaR     ",
        "    RaFFFaR     ",
        "     RFFFR      ",
        "      RRR       ",
        "     GGGGG      ",
        "    GGGGGGG     ",
        "   GGwGGGGwGG   ",
        "  GGGG....GGGG  ",
        "   GGGG..GGGG   ",
        "    GGG..GGG    ",
        "    LLL  RRR    ",
        "    LLL  RRR    ",
        "   LLLL  RRRR   "
    ),
    palette = mapOf(
        'R' to Color(0xFF8B0000), // Red ceremonial hat
        'F' to Color(0xFFD4A574), // Asian skin tone
        'a' to Color(0xFF2C2C2C), // Dark eyes
        'G' to RetroColors.PrimaryID.copy(alpha = 0.8f), // Guild robes
        'w' to Color(0xFFFFD700), // Gold emblem
        '.' to RetroColors.PrimaryID.copy(alpha = 0.4f), // Robe details
        'L' to RetroColors.PrimaryID.copy(alpha = 0.6f),
        'R' to RetroColors.PrimaryID.copy(alpha = 0.6f)
    )
)

/**
 * SYBIL BOT - Enemy type
 * Obviously fake, mass-produced appearance
 */
val enemySybilBot = PixelArt(
    rows = listOf(
        "                ",
        "   !!!XXXX!!!   ",
        "  !XXXXXXXXXXX  ",
        "  XXrRRRRRrXX   ",
        "  XXrRRRRRrXX   ",
        "   XXRRRRRXX    ",
        "    XXXXXXX     ",
        "    MMMMMMM     ",
        "   MMMMMMMM     ",
        "  MMMMMMMMMMM   ",
        "  MMM......MMM  ",
        "   MMMM..MMMM   ",
        "    MMM..MMM    ",
        "    LL    RR    ",
        "   LLL    RRR   ",
        "  LLLL    RRRR  "
    ),
    palette = mapOf(
        '!' to Color(0xFFFF0000).copy(alpha = 0.5f), // Error glitch
        'X' to Color(0xFF808080), // Metal head
        'r' to Color(0xFFFF0000), // Red bot eyes
        'R' to Color(0xFF8B0000), // Dark red
        'M' to Color(0xFF696969), // Metal body
        '.' to Color(0xFF404040), // Panel lines
        'L' to Color(0xFF505050), // Metal legs
        'R' to Color(0xFF505050)
    )
)

/**
 * DATA BROKER - Grey Market NPC
 * Sells information and identity proofs
 */
val npcDataBroker = PixelArt(
    rows = listOf(
        "                ",
        "    .SSSSSS.    ",
        "   SSSSSSSSS    ",
        "   SFFFFFFFFS   ",
        "   SFcccccFS    ",
        "    SFFFFFFS    ",
        "     SSSSS      ",
        "    JJJJJJJ     ",
        "   JJJJJJJJJ    ",
        "  JJnJJJJJnJJ   ",
        "  JJJ......JJJ  ",
        "   JJJJ..JJJJ   ",
        "    JJJ..JJJ    ",
        "    LL    RR    ",
        "    LLL   RRR   ",
        "   LLLL   RRRR  "
    ),
    palette = mapOf(
        '.' to Color(0xFF1A1A1A), // Shadow
        'S' to Color(0xFF00FF00), // Green visor
        'F' to Color(0xFF8B7355), // Face beneath
        'c' to Color(0xFF00FF00).copy(alpha = 0.7f), // Visor glare
        'J' to Color(0xFF2C2C2C), // Black jacket
        'n' to Color(0xFF00FFFF), // Cyan implants
        'L' to Color(0xFF1A1A1A), // Legs
        'R' to Color(0xFF1A1A1A)
    )
)

// ═══════════════════════════════════════════════════════════════════════
// BUILDING SPRITES - 32x32 PIXELS
// ═══════════════════════════════════════════════════════════════════════

/**
 * GUILD HALL - Primary Plaza landmark
 * Grand building where verification happens
 */
val buildingGuildHall = PixelArt(
    rows = listOf(
        "                                ",
        "        CCCCCCCCCCCCCC          ",
        "       CGGGGGGGGGGGGGC          ",
        "       CG..........GGC          ",
        "      CG............GC          ",
        "      CG..WWWWWWW...GC          ",
        "     CG..WWWWWWWWW..GC          ",
        "     CG..WWWWWWWWW..GC          ",
        "    BBBBBBBBBBBBBBBBBBBB        ",
        "    B..................B        ",
        "    B..DD........DD....B        ",
        "    B..DD........DD....B        ",
        "    B..DD........DD....B        ",
        "    B..................B        ",
        "    B..DD........DD....B        ",
        "    B..DD........DD....B        ",
        "    B..DD........DD....B        ",
        "    B..................B        ",
        "    BBBBBBBBBDDDDBBBBBBB        ",
        "    BBBBBBBBDDDDDDBBBBBB        ",
        "           DDDDDDDD             ",
        "           DDDDDDDD             ",
        "                                "
    ),
    palette = mapOf(
        'C' to RetroColors.PrimaryID, // Cyan roof
        'G' to RetroColors.PrimaryID.copy(alpha = 0.6f), // Roof gradient
        'W' to Color(0xFFFFFFFF), // White columns
        'B' to Color(0xFF8B7355), // Brown brick
        'D' to Color(0xFF654321), // Dark wood door
        '.' to Color(0xFF3E2723)  // Shadows
    )
)

/**
 * ORACLE TOWER - Imposing skyscraper
 * Elite validator headquarters
 */
val buildingOracleTower = PixelArt(
    rows = listOf(
        "        PPPPPPPPPPPP            ",
        "        P..........P            ",
        "        P.oo....oo.P            ",
        "        P..........P            ",
        "        PPPPPPPPPPPP            ",
        "         WWWWWWWWWW             ",
        "         W........W             ",
        "         W.WW..WW.W             ",
        "         W........W             ",
        "         WWWWWWWWWW             ",
        "         WWWWWWWWWW             ",
        "         W........W             ",
        "         W.WW..WW.W             ",
        "         W........W             ",
        "         WWWWWWWWWW             ",
        "         WWWWWWWWWW             ",
        "         W........W             ",
        "         W.WW..WW.W             ",
        "         W........W             ",
        "         WWWWWWWWWW             ",
        "        BBBBBBBBBBBB            ",
        "        B..........B            ",
        "        BBBBDDDDBBBB            "
    ),
    palette = mapOf(
        'P' to RetroColors.Oracle, // Purple penthouse
        'o' to Color(0xFFFFD700), // Gold windows
        'W' to Color(0xFFE0E0E0), // White/grey walls
        'B' to Color(0xFF3A3A3A), // Dark base
        'D' to Color(0xFF1A1A1A), // Door
        '.' to RetroColors.Oracle.copy(alpha = 0.2f) // Purple glow
    )
)

// ═══════════════════════════════════════════════════════════════════════
// ENVIRONMENTAL OBJECTS - 16x16 PIXELS
// ═══════════════════════════════════════════════════════════════════════

/**
 * TERMINAL - Interactive info/save point
 */
val objectTerminal = PixelArt(
    rows = listOf(
        "                ",
        "   TTTTTTTTTT   ",
        "  TGGGGGGGGGT   ",
        "  TG>>>>>>>>GT  ",
        "  TG>ccc...cGT  ",
        "  TG>c..ccc.GT  ",
        "  TG>ccc..c.GT  ",
        "  TG>...ccc.GT  ",
        "  TG>>>>>>>>GT  ",
        "  TGGGGGGGGGT   ",
        "   TTTTTTTTTT   ",
        "      TTT       ",
        "     TTTTT      ",
        "    TTTTTTT     ",
        "   BBBBBBBBB    ",
        "   BBBBBBBBB    "
    ),
    palette = mapOf(
        'T' to Color(0xFF2C2C2C), // Terminal frame
        'G' to Color(0xFF1A1A1A), // Screen dark
        '>' to Color(0xFF00FF41), // Green scan lines
        'c' to Color(0xFF39FF14), // Green text
        '.' to Color(0xFF00FF41).copy(alpha = 0.3f), // Glow
        'B' to Color(0xFF505050)  // Base
    )
)

/**
 * NEON SIGN - Environmental decoration
 */
val objectNeonSign = PixelArt(
    rows = listOf(
        "NNNNNNNNNNNNNNNN",
        "N..............N",
        "N.CC..KK..CC...N",
        "N.CC.KKKK.CC...N",
        "N.CC.KK.KK.CC..N",
        "N.CC.KK..KKKK..N",
        "N..............N",
        "N.OO..WW...WW..N",
        "N.OO..WW...WW..N",
        "N.OO..WW.W.WW..N",
        "N.OO..WWWWWWW..N",
        "N..............N",
        "NNNNNNNNNNNNNNNN",
        "       ||       ",
        "       ||       ",
        "       BB       "
    ),
    palette = mapOf(
        'N' to Color(0xFF2C2C2C), // Sign frame
        'C' to RetroColors.PrimaryID, // Cyan "KNO"
        'K' to RetroColors.PrimaryID,
        'O' to Color(0xFFFF00FF), // Magenta "W"
        'W' to Color(0xFFFF00FF),
        '.' to Color(0xFF1A1A1A), // Background
        '|' to Color(0xFF505050), // Pole
        'B' to Color(0xFF3A3A3A)  // Base
    )
)

// ═══════════════════════════════════════════════════════════════════════
// ANIMATION HELPERS
// ═══════════════════════════════════════════════════════════════════════

data class CharacterAnimation(
    val idleDown: PixelArt,
    val walkDown: List<PixelArt>,
    val walkUp: List<PixelArt>,
    val walkRight: List<PixelArt>,
    val walkLeft: List<PixelArt>? = null // null means mirror walkRight
)

val greyGhostAnimation = CharacterAnimation(
    idleDown = greyGhostIdleDown,
    walkDown = listOf(greyGhostIdleDown, greyGhostWalkDown1, greyGhostIdleDown, greyGhostWalkDown2),
    walkUp = listOf(greyGhostWalkUp),
    walkRight = listOf(greyGhostWalkRight),
    walkLeft = null // Will be mirrored from walkRight
)

// Helper to mirror sprite horizontally
fun PixelArt.mirrorHorizontal(): PixelArt {
    return PixelArt(
        rows = rows.map { it.reversed() },
        palette = palette
    )
}
