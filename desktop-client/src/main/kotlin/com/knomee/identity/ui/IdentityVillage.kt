package com.knomee.identity.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.knomee.identity.theme.RetroColors
import com.knomee.identity.theme.RetroTypography
import com.knomee.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.math.min

/**
 * IDENTITY VILLAGE - Interactive Reputation Building Game
 *
 * Players interact with 30 villagers, each with unique personalities.
 * Build reputation by answering questions and helping villagers.
 * Complete levels by achieving reputation thresholds.
 */

// ============================================================================
// DATA MODELS
// ============================================================================

private data class PixelArt(
    val rows: List<String>,
    val palette: Map<Char, Color>
)

private data class TileInfo(
    val fill: Color,
    val border: Color? = null,
    val blocking: Boolean = false,
    val sprite: PixelArt? = null
)

private enum class VillagerPersonality {
    GENEROUS,      // Easy to please, gives reputation quickly
    FRIENDLY,      // Moderately easy, cheerful
    NEUTRAL,       // Standard interactions
    WITHDRAWN,     // Harder to gain reputation, quiet
    SKEPTICAL,     // Requires multiple interactions
    DEMANDING      // Very particular, high standards
}

private data class Villager(
    val id: String,
    val name: String,
    val personality: VillagerPersonality,
    val tile: IntOffset,
    val sprite: PixelArt,
    val greetings: List<String>,
    val questions: List<VillagerQuestion>,
    val color: Color
) {
    val center: Offset
        get() = Offset(tile.x + 0.5f, tile.y + 0.5f)
}

private data class VillagerQuestion(
    val question: String,
    val answers: List<Answer>,
    val context: String = ""
)

private data class Answer(
    val text: String,
    val reputationGain: Int,
    val response: String
)

private data class VillagerReputation(
    val villagerId: String,
    var reputation: Int = 0,
    var interactionCount: Int = 0,
    var questionsAnswered: Int = 0
)

private data class GameLevel(
    val number: Int,
    val name: String,
    val requiredReputation: Int,
    val description: String
)

// ============================================================================
// GAME STATE
// ============================================================================

private data class VillageGameState(
    var playerPosition: Offset = Offset(15f, 20f),
    var level: Int = 1,
    var totalReputation: Int = 0,
    val villagerReputations: MutableMap<String, VillagerReputation> = mutableMapOf(),
    var activeVillager: Villager? = null,
    var currentDialogue: String? = null,
    var currentQuestion: VillagerQuestion? = null,
    var showLevelComplete: Boolean = false,
    val trail: MutableList<Offset> = mutableListOf()
)

// ============================================================================
// VILLAGE MAP
// ============================================================================

private const val SPRITE_SCALE = 2
private const val GOLDEN_RATIO = 1.61803398875f

// 32x24 village map with 30 villager locations marked by numbers
private val villageLayout = listOf(
    "################################",
    "#~~~~~~~~~~................~~~#",
    "#~~VVV~~~~~.BBB...BBB......~~~#",
    "#~~VVV~~~~~.BBB...BBB......####",
    "#~~VVV~~~~~................#..#",
    "#~~........TTT.............#..#",
    "#..........TTT.........BBB.#..#",
    "#..BBB.....TTT.........BBB.....#",
    "#..BBB.................BBB.....#",
    "#..BBB.........................#",
    "#..............................#",
    "#...HHH..........####..........#",
    "#...HHH..........####.....FFF..#",
    "#...HHH..........####.....FFF..#",
    "#........................FFF...#",
    "#..BBB...WWW...................#",
    "#..BBB...WWW.......SSS.........#",
    "#..BBB...WWW.......SSS.........#",
    "#..................SSS.........#",
    "#..............................#",
    "#.....PP...................RRR.#",
    "#.....PP...GGG.............RRR.#",
    "#.....PP...GGG.............RRR.#",
    "################################"
)

// Legend: ~ = water, V = village hall, T = tavern, H = house, B = building,
//         F = farm, W = well, S = smithy, P = pub, G = garden, R = residence

// ============================================================================
// SPRITES
// ============================================================================

private val villagerSprite1 = PixelArt(
    rows = listOf(
        "  ####  ",
        " #OOOO# ",
        "#OOOOOO#",
        " #OOOO# ",
        "  #SS#  ",
        " #SSSS# ",
        " #S##S# ",
        " ##  ## "
    ),
    palette = mapOf(
        '#' to Color(0xFF0F0F0F),
        'O' to Color(0xFFD8A070),
        'S' to Color(0xFF0058F8)
    )
)

private val villagerSprite2 = PixelArt(
    rows = listOf(
        "  ####  ",
        " #OOOO# ",
        "#OOOOOO#",
        " #OOOO# ",
        "  #GG#  ",
        " #GGGG# ",
        " #G##G# ",
        " ##  ## "
    ),
    palette = mapOf(
        '#' to Color(0xFF0F0F0F),
        'O' to Color(0xFFD8A070),
        'G' to Color(0xFF00D800)
    )
)

private val villagerSprite3 = PixelArt(
    rows = listOf(
        "  ####  ",
        " #OOOO# ",
        "#OOOOOO#",
        " #OOOO# ",
        "  #RR#  ",
        " #RRRR# ",
        " #R##R# ",
        " ##  ## "
    ),
    palette = mapOf(
        '#' to Color(0xFF0F0F0F),
        'O' to Color(0xFFD8A070),
        'R' to Color(0xFFD82800)
    )
)

private val villagerSprite4 = PixelArt(
    rows = listOf(
        "  ####  ",
        " #OOOO# ",
        "#OOOOOO#",
        " #OOOO# ",
        "  #PP#  ",
        " #PPPP# ",
        " #P##P# ",
        " ##  ## "
    ),
    palette = mapOf(
        '#' to Color(0xFF0F0F0F),
        'O' to Color(0xFFD8A070),
        'P' to Color(0xFFA800A8)
    )
)

private val treeSprite = PixelArt(
    rows = listOf(
        "  GGG   ",
        " GGGGG  ",
        "GGGGGGG ",
        " GGGGG  ",
        "  GGG   ",
        "   B    ",
        "   B    ",
        "   B    "
    ),
    palette = mapOf(
        'G' to Color(0xFF00D800),
        'B' to Color(0xFF905000)
    )
)

private val buildingSprite = PixelArt(
    rows = listOf(
        " ###### ",
        "########",
        "########",
        "#WW##WW#",
        "#WW##WW#",
        "########",
        "########",
        "########"
    ),
    palette = mapOf(
        '#' to Color(0xFF905000),
        'W' to Color(0xFF3CBCFC)
    )
)

private val waterSprite = PixelArt(
    rows = listOf(
        "~~~~~~~~",
        "~~~~~  ~",
        "~~~~    ",
        "~~~   ~~",
        "~~  ~~~~",
        "~  ~~~~~",
        "   ~~~~~",
        "~~~~~~~~"
    ),
    palette = mapOf(
        '~' to Color(0xFF3CBCFC),
        ' ' to Color(0xFF0058F8)
    )
)

// ============================================================================
// VILLAGER DEFINITIONS
// ============================================================================

private fun createVillagers(): List<Villager> {
    val sprites = listOf(villagerSprite1, villagerSprite2, villagerSprite3, villagerSprite4)

    return listOf(
        // GENEROUS VILLAGERS (1-5)
        Villager(
            id = "elder_mara",
            name = "Elder Mara",
            personality = VillagerPersonality.GENEROUS,
            tile = IntOffset(4, 3),
            sprite = sprites[0],
            greetings = listOf(
                "Welcome, dear child! I've been expecting you.",
                "Ah, a new face in our village! How wonderful!"
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "In our village, we believe identity is built through...",
                    answers = listOf(
                        Answer("Trust and community verification", 15, "Exactly! You understand our ways."),
                        Answer("Government documents", 5, "That's one way, but we value community more."),
                        Answer("Wealth and status", 3, "Oh dear, identity isn't about possessions.")
                    )
                ),
                VillagerQuestion(
                    question = "What makes someone truly part of a community?",
                    answers = listOf(
                        Answer("Their actions and relationships", 15, "You have a wise heart!"),
                        Answer("Living there a long time", 10, "Time helps, but it's not everything."),
                        Answer("Being born there", 5, "Birthplace matters less than belonging.")
                    )
                )
            ),
            color = RetroColors.Success
        ),

        Villager(
            id = "baker_tom",
            name = "Baker Tom",
            personality = VillagerPersonality.GENEROUS,
            tile = IntOffset(10, 6),
            sprite = sprites[1],
            greetings = listOf(
                "Fresh bread for a fresh face! Welcome!",
                "Hello there! Would you like to try my bread?"
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "What's the secret to good bread?",
                    answers = listOf(
                        Answer("Patience and care", 12, "Yes! Just like building trust!"),
                        Answer("Expensive ingredients", 8, "Quality helps, but technique matters more."),
                        Answer("Speed", 4, "No no, you can't rush good bread!")
                    )
                ),
                VillagerQuestion(
                    question = "If someone claims to be a master baker, how do you verify?",
                    answers = listOf(
                        Answer("Taste their bread and ask others", 15, "Perfect! Trust but verify!"),
                        Answer("Check their certificate", 10, "Papers can be faked, bread doesn't lie."),
                        Answer("Believe them immediately", 3, "Too trusting! We need proof.")
                    )
                )
            ),
            color = Color(0xFFD8A070)
        ),

        Villager(
            id = "farmer_jane",
            name = "Farmer Jane",
            personality = VillagerPersonality.FRIENDLY,
            tile = IntOffset(27, 12),
            sprite = sprites[2],
            greetings = listOf(
                "Good day! The crops are looking fine.",
                "Hello! Nice weather we're having."
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do you know if seeds are authentic?",
                    answers = listOf(
                        Answer("Test them and see what grows", 12, "Wise! Actions prove identity."),
                        Answer("Trust the seller's word", 6, "Risky! Better to verify."),
                        Answer("Price indicates quality", 4, "Not always true, friend.")
                    )
                ),
                VillagerQuestion(
                    question = "What makes a good neighbor?",
                    answers = listOf(
                        Answer("Someone who helps when needed", 14, "You'd fit right in here!"),
                        Answer("Someone who minds their business", 8, "We prefer community here."),
                        Answer("Someone who pays well", 3, "That's not what community means!")
                    )
                )
            ),
            color = Color(0xFF00D800)
        ),

        Villager(
            id = "musician_rey",
            name = "Musician Rey",
            personality = VillagerPersonality.FRIENDLY,
            tile = IntOffset(10, 17),
            sprite = sprites[3],
            greetings = listOf(
                "Music is the language of the soul!",
                "Would you like to hear a tune?"
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "What makes a musician authentic?",
                    answers = listOf(
                        Answer("Their unique voice and style", 13, "Yes! Identity shines through art!"),
                        Answer("Technical perfection", 9, "Skill matters, but soul more so."),
                        Answer("Fame and recognition", 5, "Popularity isn't authenticity.")
                    )
                ),
                VillagerQuestion(
                    question = "If two people claim the same song, how do you decide?",
                    answers = listOf(
                        Answer("Ask witnesses and check records", 15, "Perfect! Community consensus!"),
                        Answer("Whoever plays it better", 8, "Not quite, the creator matters."),
                        Answer("Whoever claimed it first", 10, "Time helps, but we need more proof.")
                    )
                )
            ),
            color = Color(0xFFFCE020)
        ),

        Villager(
            id = "gardener_lily",
            name = "Gardener Lily",
            personality = VillagerPersonality.GENEROUS,
            tile = IntOffset(11, 22),
            sprite = sprites[0],
            greetings = listOf(
                "The flowers are blooming beautifully!",
                "Welcome to my garden, friend!"
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do different plants coexist in a garden?",
                    answers = listOf(
                        Answer("Each has its space, all are valued", 15, "Beautiful! Like identities in community!"),
                        Answer("Strongest plants dominate", 4, "No! We nurture all plants here."),
                        Answer("Only similar plants together", 7, "Diversity makes gardens stronger!")
                    )
                )
            ),
            color = Color(0xFFFC74FC)
        ),

        // NEUTRAL VILLAGERS (6-15)
        Villager(
            id = "blacksmith_grok",
            name = "Blacksmith Grok",
            personality = VillagerPersonality.NEUTRAL,
            tile = IntOffset(19, 17),
            sprite = sprites[1],
            greetings = listOf(
                "Busy day at the forge.",
                "Need something made?"
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How can you tell quality metalwork?",
                    answers = listOf(
                        Answer("Test it under stress", 10, "Smart. Proof through trials."),
                        Answer("Look at the shine", 6, "Appearance can deceive."),
                        Answer("Ask the maker", 8, "Good start, but verify too.")
                    )
                ),
                VillagerQuestion(
                    question = "What if someone claims to have made your work?",
                    answers = listOf(
                        Answer("Show my unique maker's mark", 12, "Good! Identity markers matter."),
                        Answer("Fight them", 2, "Violence solves nothing!"),
                        Answer("Let the community judge", 14, "Wise! Consensus protects truth.")
                    )
                )
            ),
            color = Color(0xFF9D9D9D)
        ),

        Villager(
            id = "merchant_sara",
            name = "Merchant Sara",
            personality = VillagerPersonality.NEUTRAL,
            tile = IntOffset(10, 13),
            sprite = sprites[2],
            greetings = listOf(
                "Looking to trade?",
                "I have goods from far lands."
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do you verify trading partners?",
                    answers = listOf(
                        Answer("Check their reputation with others", 11, "Smart business practice!"),
                        Answer("Trust everyone", 3, "Too risky in trade!"),
                        Answer("Only trade with family", 6, "Too limiting, but understandable.")
                    )
                ),
                VillagerQuestion(
                    question = "What builds trust in commerce?",
                    answers = listOf(
                        Answer("Consistent honest dealings", 13, "Exactly! Reputation is earned."),
                        Answer("Lowest prices", 7, "Price isn't everything."),
                        Answer("Fancy appearance", 4, "Looks can be deceiving!")
                    )
                )
            ),
            color = Color(0xFF3CBCFC)
        ),

        Villager(
            id = "teacher_alden",
            name = "Teacher Alden",
            tile = IntOffset(5, 12),
            personality = VillagerPersonality.NEUTRAL,
            sprite = sprites[3],
            greetings = listOf(
                "Knowledge is power.",
                "Always eager to learn?"
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do you verify someone's expertise?",
                    answers = listOf(
                        Answer("Test their knowledge and check with peers", 12, "Thorough approach!"),
                        Answer("Look at their credentials", 9, "Helpful, but not sufficient."),
                        Answer("Assume based on age", 5, "Age doesn't guarantee wisdom.")
                    )
                ),
                VillagerQuestion(
                    question = "What's more important: what you know or who vouches for you?",
                    answers = listOf(
                        Answer("Both matter equally", 14, "Nuanced thinking! I like it."),
                        Answer("Only knowledge matters", 8, "Knowledge alone isn't recognized."),
                        Answer("Only reputation matters", 7, "Reputation without skill is hollow.")
                    )
                )
            ),
            color = Color(0xFF0058F8)
        ),

        Villager(
            id = "weaver_nora",
            name = "Weaver Nora",
            personality = VillagerPersonality.NEUTRAL,
            tile = IntOffset(15, 16),
            sprite = sprites[0],
            greetings = listOf(
                "Each thread has its place.",
                "The loom never lies."
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "What makes a pattern unique?",
                    answers = listOf(
                        Answer("The specific combination and technique", 11, "Yes! Like identity signatures."),
                        Answer("The colors used", 7, "Colors help, but pattern is key."),
                        Answer("The size", 5, "Size varies, patterns identify.")
                    )
                )
            ),
            color = Color(0xFFA800A8)
        ),

        Villager(
            id = "hunter_finn",
            name = "Hunter Finn",
            personality = VillagerPersonality.NEUTRAL,
            tile = IntOffset(25, 6),
            sprite = sprites[1],
            greetings = listOf(
                "The forest provides.",
                "Tracking anything?"
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do you identify an animal's track?",
                    answers = listOf(
                        Answer("Unique pattern, size, and gait", 10, "Exactly like identity markers!"),
                        Answer("Just the size", 6, "Too simple, many share size."),
                        Answer("Where you found it", 7, "Location helps but isn't enough.")
                    )
                )
            ),
            color = Color(0xFF905000)
        ),

        Villager(
            id = "healer_maya",
            name = "Healer Maya",
            personality = VillagerPersonality.FRIENDLY,
            tile = IntOffset(18, 8),
            sprite = sprites[2],
            greetings = listOf(
                "Health is our true wealth.",
                "Feeling well, I hope?"
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do you identify the right herb?",
                    answers = listOf(
                        Answer("Multiple characteristics and testing", 12, "Careful verification saves lives!"),
                        Answer("Color alone", 4, "Dangerous! Many herbs look similar."),
                        Answer("Smell", 8, "Important, but not sufficient.")
                    )
                )
            ),
            color = Color(0xFF00D800)
        ),

        Villager(
            id = "innkeeper_boris",
            name = "Innkeeper Boris",
            personality = VillagerPersonality.NEUTRAL,
            tile = IntOffset(10, 5),
            sprite = sprites[3],
            greetings = listOf(
                "Welcome to the Tavern!",
                "Room or just a meal?"
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do you handle strangers?",
                    answers = listOf(
                        Answer("Welcome all, but watch carefully", 11, "Balanced approach!"),
                        Answer("Turn them away", 5, "We're friendlier than that."),
                        Answer("Trust completely", 6, "Too naive for an innkeeper!")
                    )
                )
            ),
            color = Color(0xFFD8A070)
        ),

        Villager(
            id = "carpenter_ed",
            name = "Carpenter Ed",
            personality = VillagerPersonality.NEUTRAL,
            tile = IntOffset(7, 8),
            sprite = sprites[0],
            greetings = listOf(
                "Measure twice, cut once.",
                "Building something?"
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "What makes a structure trustworthy?",
                    answers = listOf(
                        Answer("Strong foundation and verified materials", 10, "Foundation like identity verification!"),
                        Answer("Speed of construction", 4, "Rushed work fails!"),
                        Answer("Appearance", 7, "Looks matter, but strength more.")
                    )
                )
            ),
            color = Color(0xFF905000)
        ),

        Villager(
            id = "shepherd_kai",
            name = "Shepherd Kai",
            personality = VillagerPersonality.NEUTRAL,
            tile = IntOffset(20, 3),
            sprite = sprites[1],
            greetings = listOf(
                "The flock is calm today.",
                "Know your sheep by name?"
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do you tell your sheep apart?",
                    answers = listOf(
                        Answer("Each has unique features I know well", 11, "Individual recognition! Like identity!"),
                        Answer("They all look the same", 3, "You're not looking close enough!"),
                        Answer("I mark them with paint", 9, "Marks help, but I know them deeper.")
                    )
                )
            ),
            color = Color(0xFFFFFFFF)
        ),

        Villager(
            id = "potter_zara",
            name = "Potter Zara",
            personality = VillagerPersonality.NEUTRAL,
            tile = IntOffset(7, 16),
            sprite = sprites[2],
            greetings = listOf(
                "Clay takes the shape you give it.",
                "Looking for pottery?"
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "Can two pots be identical?",
                    answers = listOf(
                        Answer("No, each has subtle unique marks", 12, "Yes! Like human identity!"),
                        Answer("Yes, if using molds", 7, "Even molded pots vary slightly."),
                        Answer("Only machine-made ones", 10, "True, handmade is always unique.")
                    )
                )
            ),
            color = Color(0xFF905000)
        ),

        // WITHDRAWN VILLAGERS (16-22)
        Villager(
            id = "scribe_mortimer",
            name = "Scribe Mortimer",
            personality = VillagerPersonality.WITHDRAWN,
            tile = IntOffset(4, 21),
            sprite = sprites[3],
            greetings = listOf(
                "...Yes?",
                "I'm busy with records."
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do you verify written records?",
                    answers = listOf(
                        Answer("Cross-reference multiple sources", 9, "Adequate methodology."),
                        Answer("Trust the oldest record", 7, "Age doesn't guarantee accuracy."),
                        Answer("Believe the writer", 4, "Insufficient verification.")
                    )
                ),
                VillagerQuestion(
                    question = "What if records contradict?",
                    answers = listOf(
                        Answer("Investigate thoroughly and seek consensus", 11, "...Acceptable answer."),
                        Answer("Keep both versions", 6, "Indecisive."),
                        Answer("Trust the official one", 8, "Officials can err too.")
                    )
                )
            ),
            color = Color(0xFF9D9D9D)
        ),

        Villager(
            id = "hermit_silas",
            name = "Hermit Silas",
            personality = VillagerPersonality.WITHDRAWN,
            tile = IntOffset(28, 2),
            sprite = sprites[0],
            greetings = listOf(
                "...",
                "What do you want?"
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "Why do you avoid people?",
                    answers = listOf(
                        Answer("I respect your privacy and won't pry", 10, "...Thank you."),
                        Answer("You must be hiding something", 2, "Leave me alone!"),
                        Answer("Everyone needs community", 6, "Not everyone.")
                    )
                ),
                VillagerQuestion(
                    question = "How does one prove they're who they say?",
                    answers = listOf(
                        Answer("Consistent actions over time", 11, "...You understand."),
                        Answer("Papers and documents", 7, "Papers can be forged."),
                        Answer("What others say about them", 8, "Words are cheap.")
                    )
                )
            ),
            color = Color(0xFF0F0F0F)
        ),

        Villager(
            id = "librarian_helen",
            name = "Librarian Helen",
            personality = VillagerPersonality.WITHDRAWN,
            tile = IntOffset(2, 6),
            sprite = sprites[1],
            greetings = listOf(
                "Shh. This is a library.",
                "Keep your voice down."
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do you catalog books?",
                    answers = listOf(
                        Answer("By multiple attributes for cross-referencing", 9, "*nods approvingly*"),
                        Answer("Alphabetically only", 6, "Too simple."),
                        Answer("By color", 3, "That's absurd.")
                    )
                )
            ),
            color = Color(0xFF0058F8)
        ),

        Villager(
            id = "night_watch_grim",
            name = "Night Watch Grim",
            personality = VillagerPersonality.WITHDRAWN,
            tile = IntOffset(30, 22),
            sprite = sprites[2],
            greetings = listOf(
                "State your business.",
                "I'm watching you."
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do you spot trouble?",
                    answers = listOf(
                        Answer("Watch patterns and behaviors over time", 10, "You'd make a decent guard."),
                        Answer("Judge by appearance", 5, "Superficial."),
                        Answer("Everyone's suspicious", 7, "Cynical, but I understand.")
                    )
                )
            ),
            color = Color(0xFF0F0F0F)
        ),

        Villager(
            id = "astronomer_luna",
            name = "Astronomer Luna",
            personality = VillagerPersonality.WITHDRAWN,
            tile = IntOffset(15, 4),
            sprite = sprites[3],
            greetings = listOf(
                "The stars are clearer than people.",
                "Hmm?"
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do you identify stars?",
                    answers = listOf(
                        Answer("Position, brightness, and relation to others", 10, "Like identity in networks."),
                        Answer("They're all the same", 3, "Clearly you don't observe."),
                        Answer("By mythology names", 7, "Names come after identification.")
                    )
                )
            ),
            color = Color(0xFFFCE020)
        ),

        Villager(
            id = "tax_collector_ruth",
            name = "Tax Collector Ruth",
            personality = VillagerPersonality.WITHDRAWN,
            tile = IntOffset(25, 20),
            sprite = sprites[0],
            greetings = listOf(
                "Time to pay up.",
                "Your taxes are due."
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do you prevent fraud?",
                    answers = listOf(
                        Answer("Detailed records and cross-verification", 9, "Competent answer."),
                        Answer("Trust people", 4, "Naive."),
                        Answer("Harsh penalties", 6, "Punishment alone doesn't prevent.")
                    )
                )
            ),
            color = Color(0xFF9D9D9D)
        ),

        Villager(
            id = "old_witch_agatha",
            name = "Old Witch Agatha",
            personality = VillagerPersonality.WITHDRAWN,
            tile = IntOffset(2, 15),
            sprite = sprites[1],
            greetings = listOf(
                "Hehehehe...",
                "What brings you to my hut?"
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do you tell a real witch from a fake?",
                    answers = listOf(
                        Answer("Test their knowledge and observe results", 10, "Clever child..."),
                        Answer("Witches aren't real", 5, "*cackles* Are you sure?"),
                        Answer("By their appearance", 4, "Appearances deceive, dearie.")
                    )
                )
            ),
            color = Color(0xFFA800A8)
        ),

        // SKEPTICAL VILLAGERS (23-27)
        Villager(
            id = "judge_marcus",
            name = "Judge Marcus",
            personality = VillagerPersonality.SKEPTICAL,
            tile = IntOffset(12, 12),
            sprite = sprites[2],
            greetings = listOf(
                "I'll be the judge of your character.",
                "Convince me you're trustworthy."
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "What is the basis of justice?",
                    answers = listOf(
                        Answer("Evidence, fairness, and community wisdom", 8, "Mmm. Acceptable."),
                        Answer("The law books", 6, "Law without wisdom is tyranny."),
                        Answer("Punishment", 3, "Justice is more than vengeance.")
                    )
                ),
                VillagerQuestion(
                    question = "How many witnesses make testimony credible?",
                    answers = listOf(
                        Answer("Quality matters more than quantity", 9, "...You may have wisdom."),
                        Answer("At least three", 7, "Numbers help, but can lie too."),
                        Answer("One truthful person", 6, "One can be mistaken.")
                    )
                )
            ),
            color = Color(0xFF0058F8)
        ),

        Villager(
            id = "master_craftsman_viktor",
            name = "Master Craftsman Viktor",
            personality = VillagerPersonality.SKEPTICAL,
            tile = IntOffset(20, 20),
            sprite = sprites[3],
            greetings = listOf(
                "Prove your worth.",
                "Everyone claims to be skilled these days."
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "What makes a master?",
                    answers = listOf(
                        Answer("Years of work verified by peers and results", 8, "You understand standards."),
                        Answer("Self-declaration", 2, "Hah! Arrogance."),
                        Answer("Teaching others", 7, "Part of it, but mastery first.")
                    )
                ),
                VillagerQuestion(
                    question = "How do you handle imposters?",
                    answers = listOf(
                        Answer("Test their skills publicly", 9, "Public verification. Good."),
                        Answer("Ignore them", 4, "They damage all our reputations!"),
                        Answer("Report to authorities", 7, "Authorities don't know craft.")
                    )
                )
            ),
            color = Color(0xFF905000)
        ),

        Villager(
            id = "merchant_guild_leader_sharon",
            name = "Guild Leader Sharon",
            personality = VillagerPersonality.SKEPTICAL,
            tile = IntOffset(15, 8),
            sprite = sprites[0],
            greetings = listOf(
                "New merchants must prove themselves.",
                "Our guild has standards."
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "Why do guilds exist?",
                    answers = listOf(
                        Answer("To verify quality and prevent fraud", 8, "Exactly our purpose."),
                        Answer("To control prices", 5, "That's a side effect."),
                        Answer("To exclude competition", 3, "We maintain standards, not monopolies.")
                    )
                ),
                VillagerQuestion(
                    question = "What's the guild's role in identity?",
                    answers = listOf(
                        Answer("Collective vouching for members' legitimacy", 9, "You grasp our function."),
                        Answer("Issuing certificates", 6, "Papers alone aren't enough."),
                        Answer("No relation to identity", 2, "You understand nothing.")
                    )
                )
            ),
            color = Color(0xFF3CBCFC)
        ),

        Villager(
            id = "veteran_commander_stone",
            name = "Commander Stone",
            personality = VillagerPersonality.SKEPTICAL,
            tile = IntOffset(30, 6),
            sprite = sprites[1],
            greetings = listOf(
                "Prove your loyalty first.",
                "I don't trust easily."
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do you identify friend from foe?",
                    answers = listOf(
                        Answer("Actions under pressure reveal truth", 8, "Battle-tested wisdom."),
                        Answer("Uniforms and symbols", 6, "Easily faked."),
                        Answer("What they say", 3, "Words mean nothing in war.")
                    )
                ),
                VillagerQuestion(
                    question = "What makes a soldier trustworthy?",
                    answers = listOf(
                        Answer("Consistent behavior and peer vouching", 9, "You'd make a good officer."),
                        Answer("Following orders", 7, "Obedience isn't enough."),
                        Answer("Years of service", 6, "Time helps, but traitors can wait.")
                    )
                )
            ),
            color = Color(0xFFD82800)
        ),

        Villager(
            id = "alchemist_zephyr",
            name = "Alchemist Zephyr",
            personality = VillagerPersonality.SKEPTICAL,
            tile = IntOffset(23, 16),
            sprite = sprites[2],
            greetings = listOf(
                "Science demands proof.",
                "I deal in facts, not claims."
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "How do you verify an experiment?",
                    answers = listOf(
                        Answer("Reproduce results independently", 8, "Scientific method. Good."),
                        Answer("Trust the researcher", 3, "Peer review exists for reason."),
                        Answer("Check their credentials", 6, "Credentials don't guarantee truth.")
                    )
                ),
                VillagerQuestion(
                    question = "What's more reliable: observation or theory?",
                    answers = listOf(
                        Answer("Both together, verified repeatedly", 9, "Ah, you think like a scientist."),
                        Answer("Pure observation", 7, "Without theory, observation is blind."),
                        Answer("Pure theory", 5, "Without data, theory is speculation.")
                    )
                )
            ),
            color = Color(0xFFA800A8)
        ),

        // DEMANDING VILLAGERS (28-30)
        Villager(
            id = "grand_architect_solomon",
            name = "Grand Architect Solomon",
            personality = VillagerPersonality.DEMANDING,
            tile = IntOffset(27, 22),
            sprite = sprites[3],
            greetings = listOf(
                "I expect excellence.",
                "Mediocrity is unacceptable."
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "What's the foundation of great architecture?",
                    answers = listOf(
                        Answer("Vision, skill, and community approval", 7, "Barely adequate answer."),
                        Answer("Beauty", 4, "Beauty without function fails."),
                        Answer("Cost", 3, "You think like an accountant, not an artist.")
                    )
                ),
                VillagerQuestion(
                    question = "How do you handle criticism of your work?",
                    answers = listOf(
                        Answer("Welcome it from qualified peers, verify validity", 8, "Hmm. You may have potential."),
                        Answer("Ignore critics", 3, "Arrogance leads to failure."),
                        Answer("Change based on all feedback", 5, "You must filter noise from wisdom.")
                    )
                ),
                VillagerQuestion(
                    question = "Why should I trust your identity claim?",
                    answers = listOf(
                        Answer("My actions, peer vouches, and verifiable history", 9, "...Compelling evidence. Proceed."),
                        Answer("You should just believe me", 2, "Absurd!"),
                        Answer("I have documents", 5, "Documents are starting point, not proof.")
                    )
                )
            ),
            color = RetroColors.NESYellow
        ),

        Villager(
            id = "high_priestess_seraphina",
            name = "High Priestess Seraphina",
            personality = VillagerPersonality.DEMANDING,
            tile = IntOffset(4, 12),
            sprite = sprites[0],
            greetings = listOf(
                "Your soul must be pure.",
                "I see through deception."
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "What is the nature of truth?",
                    answers = listOf(
                        Answer("Truth is consistent across perspectives and time", 7, "A worthy answer, but incomplete."),
                        Answer("Truth is relative", 4, "Dangerous relativism."),
                        Answer("Truth is what community agrees", 6, "Consensus helps reveal, not create truth.")
                    )
                ),
                VillagerQuestion(
                    question = "How do you prove spiritual authenticity?",
                    answers = listOf(
                        Answer("Consistent ethical actions witnessed by community", 8, "You show wisdom."),
                        Answer("Faith alone", 5, "Faith without works is hollow."),
                        Answer("Religious knowledge", 6, "Knowledge without virtue means nothing.")
                    )
                ),
                VillagerQuestion(
                    question = "What is identity's relationship to truth?",
                    answers = listOf(
                        Answer("True identity is who you consistently are, verified by community", 9, "...You have passed my test."),
                        Answer("Identity is self-defined", 5, "Partially true, but insufficient."),
                        Answer("Identity is assigned", 4, "Too passive. We co-create identity.")
                    )
                )
            ),
            color = RetroColors.NESWhite
        ),

        Villager(
            id = "oracle_of_the_well",
            name = "Oracle of the Well",
            personality = VillagerPersonality.DEMANDING,
            tile = IntOffset(10, 16),
            sprite = sprites[1],
            greetings = listOf(
                "The waters show all truths.",
                "You seek wisdom? Prove worthy."
            ),
            questions = listOf(
                VillagerQuestion(
                    question = "What is the paradox of identity?",
                    answers = listOf(
                        Answer("We are both individual and defined by relationships", 7, "You see one layer..."),
                        Answer("There is no paradox", 4, "Simplistic thinking."),
                        Answer("Identity doesn't exist", 3, "Nihilism isn't wisdom.")
                    )
                ),
                VillagerQuestion(
                    question = "How can identity be both fixed and changing?",
                    answers = listOf(
                        Answer("Core self persists while context and growth evolve", 8, "Deeper understanding emerges..."),
                        Answer("Identity never changes", 5, "Rigid thinking."),
                        Answer("Identity is pure flux", 5, "Chaos without continuity.")
                    )
                ),
                VillagerQuestion(
                    question = "Why does the village need identity verification?",
                    answers = listOf(
                        Answer("Trust enables cooperation; verification sustains trust", 9, "The waters reveal your wisdom. You understand."),
                        Answer("To control people", 3, "You confuse tools with intentions."),
                        Answer("It's traditional", 5, "Tradition without understanding is empty ritual.")
                    )
                )
            ),
            color = RetroColors.NESSkyBlue
        )
    )
}

// ============================================================================
// GAME LEVELS
// ============================================================================

private val gameLevels = listOf(
    GameLevel(1, "Newcomer", 100, "Introduce yourself to the village"),
    GameLevel(2, "Familiar Face", 250, "Build relationships with neighbors"),
    GameLevel(3, "Trusted Member", 450, "Earn the community's trust"),
    GameLevel(4, "Village Elder", 700, "Become a pillar of the community")
)

// ============================================================================
// TILE MAP LEGEND
// ============================================================================

private fun getTileInfo(char: Char): TileInfo {
    return when (char) {
        '.' -> TileInfo(
            fill = Color(0xFF00A800),
            border = null,
            blocking = false
        )
        '#' -> TileInfo(
            fill = Color(0xFF905000),
            border = Color(0xFF0F0F0F),
            blocking = true,
            sprite = buildingSprite
        )
        '~' -> TileInfo(
            fill = Color(0xFF3CBCFC),
            border = null,
            blocking = true,
            sprite = waterSprite
        )
        'B', 'V', 'T', 'H', 'F', 'W', 'S', 'P', 'G', 'R' -> TileInfo(
            fill = Color(0xFF905000),
            border = Color(0xFF0F0F0F),
            blocking = true,
            sprite = buildingSprite
        )
        else -> TileInfo(
            fill = Color(0xFF00A800),
            border = null,
            blocking = false
        )
    }
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

private fun isWalkable(x: Float, y: Float, map: List<String>): Boolean {
    val tileX = floor(x).toInt()
    val tileY = floor(y).toInt()

    if (tileY !in map.indices || tileX !in map[tileY].indices) {
        return false
    }

    val tileInfo = getTileInfo(map[tileY][tileX])
    return !tileInfo.blocking
}

private fun findNearbyVillager(
    playerPos: Offset,
    villagers: List<Villager>,
    radius: Float = 1.2f
): Villager? {
    return villagers
        .map { it to sqrt((it.center.x - playerPos.x) * (it.center.x - playerPos.x) +
                          (it.center.y - playerPos.y) * (it.center.y - playerPos.y)) }
        .filter { it.second <= radius }
        .minByOrNull { it.second }
        ?.first
}

private fun getPersonalityDescription(personality: VillagerPersonality): String {
    return when (personality) {
        VillagerPersonality.GENEROUS -> "Warm and welcoming"
        VillagerPersonality.FRIENDLY -> "Cheerful and open"
        VillagerPersonality.NEUTRAL -> "Practical and fair"
        VillagerPersonality.WITHDRAWN -> "Reserved and quiet"
        VillagerPersonality.SKEPTICAL -> "Cautious and questioning"
        VillagerPersonality.DEMANDING -> "Exacting and rigorous"
    }
}

// ============================================================================
// MAIN COMPOSABLE
// ============================================================================

@Composable
fun IdentityVillageScreen(
    viewModel: IdentityViewModel,
    onBack: () -> Unit
) {
    val villagers = remember { createVillagers() }
    val gameState = remember { VillageGameState() }

    // Initialize reputation tracking
    LaunchedEffect(Unit) {
        villagers.forEach { villager ->
            gameState.villagerReputations[villager.id] = VillagerReputation(villager.id)
        }
    }

    val focusRequester = remember { FocusRequester() }
    var frameCount by remember { mutableStateOf(0) }
    val pressedKeys = remember { mutableMapOf<ComposeKey, Boolean>() }

    // Check level completion
    LaunchedEffect(gameState.totalReputation) {
        val currentLevel = gameLevels.find { it.number == gameState.level }
        if (currentLevel != null && gameState.totalReputation >= currentLevel.requiredReputation) {
            gameState.showLevelComplete = true
        }
    }

    // Game loop
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()

        var lastFrameTime = withFrameNanos { it }

        while (true) {
            val frameTime = withFrameNanos { it }
            val deltaSeconds = (frameTime - lastFrameTime) / 1_000_000_000f
            lastFrameTime = frameTime

            frameCount++

            // Movement
            var dx = 0f
            var dy = 0f

            if (pressedKeys[ComposeKey.W] == true || pressedKeys[ComposeKey.DirectionUp] == true) dy -= 1f
            if (pressedKeys[ComposeKey.S] == true || pressedKeys[ComposeKey.DirectionDown] == true) dy += 1f
            if (pressedKeys[ComposeKey.A] == true || pressedKeys[ComposeKey.DirectionLeft] == true) dx -= 1f
            if (pressedKeys[ComposeKey.D] == true || pressedKeys[ComposeKey.DirectionRight] == true) dx += 1f

            if (dx != 0f || dy != 0f) {
                val len = sqrt(dx * dx + dy * dy)
                dx /= len
                dy /= len

                val speedTilesPerSecond = 4f
                val candidateX = gameState.playerPosition.x + dx * speedTilesPerSecond * deltaSeconds
                val candidateY = gameState.playerPosition.y + dy * speedTilesPerSecond * deltaSeconds

                if (isWalkable(candidateX, candidateY, villageLayout)) {
                    gameState.playerPosition = Offset(candidateX, candidateY)
                    gameState.trail.add(gameState.playerPosition)
                    if (gameState.trail.size > 16) {
                        gameState.trail.removeAt(0)
                    }
                }
            }

            delay(16) // ~60 FPS
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RetroColors.NESBlack)
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
                        if (event.type == KeyEventType.KeyDown && gameState.activeVillager == null) {
                            // Find nearby villager and interact
                            val nearbyVillager = findNearbyVillager(gameState.playerPosition, villagers)
                            if (nearbyVillager != null) {
                                gameState.activeVillager = nearbyVillager
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F0F0F),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "IDENTITY VILLAGE",
                        style = RetroTypography.title,
                        color = RetroColors.NESYellow
                    )

                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RetroColors.Error
                        )
                    ) {
                        Text("EXIT", style = RetroTypography.button)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                // Main game area
                Box(
                    modifier = Modifier
                        .weight(GOLDEN_RATIO)
                        .fillMaxHeight()
                ) {
                    VillageGameCanvas(
                        gameState = gameState,
                        villagers = villagers,
                        frameCount = frameCount
                    )
                }

                // Right sidebar - HUD
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    color = Color(0xFF1A1A1A)
                ) {
                    VillageHUD(
                        gameState = gameState,
                        villagers = villagers,
                        viewModel = viewModel
                    )
                }
            }
        }

        // Dialogue overlay
        gameState.activeVillager?.let { villager ->
            VillagerDialogue(
                villager = villager,
                gameState = gameState,
                onClose = {
                    gameState.activeVillager = null
                    gameState.currentDialogue = null
                    gameState.currentQuestion = null
                }
            )
        }

        // Level complete overlay
        if (gameState.showLevelComplete) {
            LevelCompleteOverlay(
                gameState = gameState,
                onContinue = {
                    gameState.level++
                    gameState.showLevelComplete = false
                }
            )
        }
    }
}

@Composable
private fun VillageGameCanvas(
    gameState: VillageGameState,
    villagers: List<Villager>,
    frameCount: Int
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current

        Canvas(modifier = Modifier.fillMaxSize()) {
            val tileSize = min(size.width / villageLayout[0].length, size.height / villageLayout.size)

            // Draw map
            villageLayout.forEachIndexed { y, row ->
                row.forEachIndexed { x, char ->
                    val tileInfo = getTileInfo(char)
                    val left = x * tileSize
                    val top = y * tileSize

                    // Draw tile
                    drawRect(
                        color = tileInfo.fill,
                        topLeft = Offset(left, top),
                        size = Size(tileSize, tileSize)
                    )

                    // Draw sprite if exists
                    tileInfo.sprite?.let { sprite ->
                        drawPixelArt(sprite, Offset(left, top), tileSize / 8f)
                    }

                    // Draw border
                    tileInfo.border?.let { borderColor ->
                        drawRect(
                            color = borderColor,
                            topLeft = Offset(left, top),
                            size = Size(tileSize, tileSize),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }

            // Draw villagers
            villagers.forEach { villager ->
                val vx = villager.tile.x * tileSize
                val vy = villager.tile.y * tileSize

                drawPixelArt(villager.sprite, Offset(vx, vy), tileSize / 8f)

                // Draw colored indicator above villager (instead of text)
                drawCircle(
                    color = villager.color,
                    radius = tileSize * 0.15f,
                    center = Offset(vx + tileSize / 2f, vy - tileSize * 0.3f)
                )
            }

            // Draw trail
            gameState.trail.forEachIndexed { index, pos ->
                val alpha = index.toFloat() / gameState.trail.size
                drawCircle(
                    color = Color.Yellow.copy(alpha = alpha * 0.3f),
                    radius = tileSize * 0.15f,
                    center = Offset(pos.x * tileSize, pos.y * tileSize)
                )
            }

            // Draw player
            val px = gameState.playerPosition.x * tileSize
            val py = gameState.playerPosition.y * tileSize

            // Player circle
            drawCircle(
                color = Color.Yellow,
                radius = tileSize * 0.4f,
                center = Offset(px, py)
            )

            // Player outline
            drawCircle(
                color = Color(0xFF0F0F0F),
                radius = tileSize * 0.4f,
                center = Offset(px, py),
                style = Stroke(width = 3f)
            )

            // Find nearby villager
            val nearbyVillager = findNearbyVillager(gameState.playerPosition, villagers)
            nearbyVillager?.let { villager ->
                // Draw interaction indicator
                val vx = villager.center.x * tileSize
                val vy = villager.center.y * tileSize

                val blinkAlpha = if ((frameCount / 30) % 2 == 0) 1f else 0.5f

                drawCircle(
                    color = Color.White.copy(alpha = blinkAlpha),
                    radius = tileSize * 0.6f,
                    center = Offset(vx, vy),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

@Composable
private fun VillageHUD(
    gameState: VillageGameState,
    villagers: List<Villager>,
    viewModel: IdentityViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Level info
        val currentLevel = gameLevels.find { it.number == gameState.level }
        if (currentLevel != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Level ${currentLevel.number}: ${currentLevel.name}",
                        style = RetroTypography.heading,
                        color = RetroColors.NESYellow
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        currentLevel.description,
                        style = RetroTypography.body,
                        color = RetroColors.NESWhite
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress bar
                    val progress = gameState.totalReputation.toFloat() / currentLevel.requiredReputation
                    LinearProgressIndicator(
                        progress = progress.coerceAtMost(1f),
                        modifier = Modifier.fillMaxWidth(),
                        color = RetroColors.Success,
                        trackColor = Color(0xFF404040)
                    )
                    Text(
                        "${gameState.totalReputation} / ${currentLevel.requiredReputation}",
                        style = RetroTypography.caption,
                        color = RetroColors.NESWhite,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Total reputation
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Reputation", style = RetroTypography.body, color = RetroColors.NESWhite)
                Text(
                    "${gameState.totalReputation}",
                    style = RetroTypography.heading,
                    color = RetroColors.Success
                )
            }
        }

        // Villager reputation list
        Text(
            "Villager Relationships",
            style = RetroTypography.heading,
            color = RetroColors.NESYellow
        )

        // Top 10 villagers by reputation
        val topVillagers = gameState.villagerReputations.entries
            .sortedByDescending { it.value.reputation }
            .take(10)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            topVillagers.forEach { (villagerId, rep) ->
                val villager = villagers.find { it.id == villagerId }
                if (villager != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    villager.name,
                                    style = RetroTypography.body,
                                    color = villager.color
                                )
                                Text(
                                    getPersonalityDescription(villager.personality),
                                    style = RetroTypography.caption,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                "+${rep.reputation}",
                                style = RetroTypography.body,
                                color = RetroColors.Success
                            )
                        }
                    }
                }
            }
        }

        // Instructions
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Controls",
                    style = RetroTypography.heading,
                    color = RetroColors.NESYellow
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("WASD / Arrows: Move", style = RetroTypography.caption, color = RetroColors.NESWhite)
                Text("SPACE: Talk to villager", style = RetroTypography.caption, color = RetroColors.NESWhite)
            }
        }
    }
}

@Composable
private fun VillagerDialogue(
    villager: Villager,
    gameState: VillageGameState,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(600.dp)
                .padding(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1A1A1A),
            border = BorderStroke(3.dp, villager.color)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    villager.name,
                    style = RetroTypography.title,
                    color = villager.color
                )

                Text(
                    getPersonalityDescription(villager.personality),
                    style = RetroTypography.body,
                    color = Color.Gray
                )

                Divider(color = villager.color)

                // Current dialogue or question
                if (gameState.currentQuestion != null) {
                    // Show question
                    val question = gameState.currentQuestion!!

                    if (question.context.isNotEmpty()) {
                        Text(
                            question.context,
                            style = RetroTypography.body,
                            color = RetroColors.NESWhite
                        )
                    }

                    Text(
                        question.question,
                        style = RetroTypography.heading,
                        color = RetroColors.NESYellow
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Answer options
                    question.answers.forEach { answer ->
                        Button(
                            onClick = {
                                // Process answer
                                val rep = gameState.villagerReputations[villager.id]!!
                                rep.reputation += answer.reputationGain
                                rep.questionsAnswered++
                                gameState.totalReputation += answer.reputationGain

                                // Show response
                                gameState.currentDialogue = answer.response
                                gameState.currentQuestion = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2A2A2A)
                            )
                        ) {
                            Text(
                                answer.text,
                                style = RetroTypography.body,
                                color = RetroColors.NESWhite
                            )
                        }
                    }
                } else if (gameState.currentDialogue != null) {
                    // Show response
                    Text(
                        gameState.currentDialogue!!,
                        style = RetroTypography.body,
                        color = RetroColors.NESWhite
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            // Check if there are more questions
                            val rep = gameState.villagerReputations[villager.id]!!
                            if (rep.questionsAnswered < villager.questions.size) {
                                gameState.currentQuestion = villager.questions[rep.questionsAnswered]
                                gameState.currentDialogue = null
                            } else {
                                onClose()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = villager.color
                        )
                    ) {
                        Text("Continue", style = RetroTypography.button)
                    }
                } else {
                    // Show greeting
                    val greeting = villager.greetings.random()
                    Text(
                        greeting,
                        style = RetroTypography.body,
                        color = RetroColors.NESWhite
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val rep = gameState.villagerReputations[villager.id]!!

                    if (rep.questionsAnswered < villager.questions.size) {
                        Button(
                            onClick = {
                                rep.interactionCount++
                                gameState.currentQuestion = villager.questions[rep.questionsAnswered]
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = villager.color
                            )
                        ) {
                            Text("Talk", style = RetroTypography.button)
                        }
                    } else {
                        Text(
                            "You've completed all interactions with ${villager.name}!",
                            style = RetroTypography.body,
                            color = RetroColors.Success
                        )
                    }
                }

                Divider(color = villager.color)

                // Reputation with this villager
                val rep = gameState.villagerReputations[villager.id]!!
                Text(
                    "Reputation: +${rep.reputation} (${rep.questionsAnswered}/${villager.questions.size} questions)",
                    style = RetroTypography.caption,
                    color = Color.Gray
                )

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RetroColors.Error
                    )
                ) {
                    Text("Close", style = RetroTypography.button)
                }
            }
        }
    }
}

@Composable
private fun LevelCompleteOverlay(
    gameState: VillageGameState,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(500.dp)
                .padding(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1A1A1A),
            border = BorderStroke(3.dp, RetroColors.Success)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "LEVEL COMPLETE!",
                    style = RetroTypography.title,
                    color = RetroColors.Success
                )

                val currentLevel = gameLevels.find { it.number == gameState.level }
                if (currentLevel != null) {
                    Text(
                        "You've completed: ${currentLevel.name}",
                        style = RetroTypography.heading,
                        color = RetroColors.NESYellow
                    )
                }

                Text(
                    "Total Reputation: ${gameState.totalReputation}",
                    style = RetroTypography.body,
                    color = RetroColors.NESWhite
                )

                Divider(color = RetroColors.Success)

                val nextLevel = gameLevels.find { it.number == gameState.level + 1 }
                if (nextLevel != null) {
                    Text(
                        "Next Level: ${nextLevel.name}",
                        style = RetroTypography.heading,
                        color = RetroColors.NESYellow
                    )
                    Text(
                        "Required Reputation: ${nextLevel.requiredReputation}",
                        style = RetroTypography.body,
                        color = RetroColors.NESWhite
                    )

                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RetroColors.Success
                        )
                    ) {
                        Text("Continue", style = RetroTypography.button)
                    }
                } else {
                    Text(
                        "CONGRATULATIONS!",
                        style = RetroTypography.title,
                        color = RetroColors.Success
                    )
                    Text(
                        "You've completed all levels!",
                        style = RetroTypography.heading,
                        color = RetroColors.NESYellow
                    )
                }
            }
        }
    }
}

// Helper function to draw pixel art
private fun DrawScope.drawPixelArt(art: PixelArt, topLeft: Offset, pixelSize: Float) {
    art.rows.forEachIndexed { y, row ->
        row.forEachIndexed { x, char ->
            art.palette[char]?.let { color ->
                drawRect(
                    color = color,
                    topLeft = Offset(topLeft.x + x * pixelSize, topLeft.y + y * pixelSize),
                    size = Size(pixelSize, pixelSize)
                )
            }
        }
    }
}
