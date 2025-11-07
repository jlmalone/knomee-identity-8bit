package com.knomee.identity.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key as ComposeKey
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.knomee.identity.theme.RetroColors
import com.knomee.identity.theme.RetroTypography
import com.knomee.identity.utils.ValidationUtils
import com.knomee.identity.utils.logger
import kotlinx.coroutines.delay
import java.math.BigInteger

/**
 * Dragon Warrior / Dragon Quest style menu interaction system
 *
 * This is the CORE gameplay mechanism - exploration leads to menu interactions
 * that stake tokens and execute smart contract calls.
 */

// ═══════════════════════════════════════════════════════════════════════
// DATA MODELS
// ═══════════════════════════════════════════════════════════════════════

/**
 * Represents an NPC or location the player can interact with
 */
data class InteractableNPC(
    val id: String,
    val name: String,
    val sprite: PixelArt,
    val dialogue: List<DialogueNode>,
    val faction: NPCFaction,
    val location: String
)

enum class NPCFaction {
    VERIFIERS_GUILD,    // Believers in the protocol
    SYBIL_SYNDICATE,    // Antagonists
    ANALOG_RESISTANCE,  // Freedom fighters
    NEURAL_NETWORKERS,  // AI researchers
    GREY_MARKET,        // Data brokers
    NEUTRAL             // Regular citizens
}

/**
 * A node in the dialogue tree
 */
sealed class DialogueNode {
    abstract val id: String

    /**
     * NPC speaks, then shows choices
     */
    data class Speech(
        override val id: String,
        val text: String,
        val npcName: String,
        val choices: List<Choice>
    ) : DialogueNode()

    /**
     * Player narration or internal thought
     */
    data class Narration(
        override val id: String,
        val text: String,
        val nextNode: String?
    ) : DialogueNode()

    /**
     * Execute a smart contract action
     */
    data class SmartContractAction(
        override val id: String,
        val actionType: ContractActionType,
        val description: String,
        val requiredStake: Double?,
        val risk: String,
        val reward: String,
        val onConfirm: () -> Unit,
        val onSuccess: String?, // Next dialogue node
        val onFailure: String?  // Next dialogue node
    ) : DialogueNode()

    /**
     * End of conversation
     */
    data class End(
        override val id: String,
        val farewell: String?
    ) : DialogueNode()
}

/**
 * A choice the player can make
 */
data class Choice(
    val text: String,
    val nextNode: String,
    val requirement: ChoiceRequirement? = null,
    val cost: ChoiceCost? = null
)

/**
 * Requirements to show/enable a choice
 */
sealed class ChoiceRequirement {
    data class MinimumTier(val tier: Int) : ChoiceRequirement()
    data class MinimumReputation(val reputation: Long) : ChoiceRequirement()
    data class MinimumKNOW(val amount: Double) : ChoiceRequirement()
    data class QuestCompleted(val questId: String) : ChoiceRequirement()
}

/**
 * Cost of making a choice
 */
sealed class ChoiceCost {
    data class KNOW(val amount: Double) : ChoiceCost()
    data class Reputation(val amount: Long) : ChoiceCost()
}

/**
 * Types of smart contract actions
 */
enum class ContractActionType {
    REQUEST_PRIMARY_VERIFICATION,
    REQUEST_LINK_TO_PRIMARY,
    VOUCH_FOR_CLAIM,
    VOUCH_AGAINST_CLAIM,
    CHALLENGE_DUPLICATE,
    CLAIM_REWARDS,
    STAKE_TOKENS,
    UNSTAKE_TOKENS
}

// ═══════════════════════════════════════════════════════════════════════
// INTERACTION MENU COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════

/**
 * Main Dragon Warrior-style menu system
 */
@Composable
fun InteractionMenu(
    npc: InteractableNPC,
    currentNodeId: String,
    onChoiceSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    playerTier: Int,
    playerReputation: Long,
    playerKNOW: BigInteger?,
    modifier: Modifier = Modifier
) {
    val log = logger("InteractionMenu")
    val currentNode = remember(currentNodeId) {
        npc.dialogue.find { it.id == currentNodeId } ?: npc.dialogue.first()
    }

    var selectedChoiceIndex by remember { mutableStateOf(0) }
    var promptBlink by remember { mutableStateOf(true) }

    // Blinking cursor effect
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            promptBlink = !promptBlink
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            when (currentNode) {
                is DialogueNode.Speech -> SpeechMenu(
                    node = currentNode,
                    selectedIndex = selectedChoiceIndex,
                    onIndexChange = { selectedChoiceIndex = it },
                    onSelect = { choice ->
                        log.info("Player selected: ${choice.text}")
                        onChoiceSelected(choice.nextNode)
                    },
                    onCancel = onDismiss,
                    playerTier = playerTier,
                    playerReputation = playerReputation,
                    playerKNOW = playerKNOW,
                    promptBlink = promptBlink
                )

                is DialogueNode.SmartContractAction -> SmartContractActionMenu(
                    node = currentNode,
                    onConfirm = {
                        log.info("Executing contract action: ${currentNode.actionType}")
                        currentNode.onConfirm()
                    },
                    onCancel = onDismiss
                )

                is DialogueNode.Narration -> NarrationBox(
                    text = currentNode.text,
                    onContinue = {
                        currentNode.nextNode?.let { onChoiceSelected(it) } ?: onDismiss()
                    }
                )

                is DialogueNode.End -> {
                    currentNode.farewell?.let {
                        FarewellBox(text = it, onClose = onDismiss)
                    } ?: run {
                        LaunchedEffect(Unit) { onDismiss() }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// MENU COMPONENTS
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SpeechMenu(
    node: DialogueNode.Speech,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    onSelect: (Choice) -> Unit,
    onCancel: () -> Unit,
    playerTier: Int,
    playerReputation: Long,
    playerKNOW: BigInteger?,
    promptBlink: Boolean
) {
    Column(
        modifier = Modifier
            .width(600.dp)
            .background(RetroColors.WindowBackground, RoundedCornerShape(8.dp))
            .border(4.dp, RetroColors.BorderColor, RoundedCornerShape(8.dp))
            .padding(24.dp)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        ComposeKey.DirectionUp, ComposeKey.W -> {
                            onIndexChange((selectedIndex - 1 + node.choices.size) % node.choices.size)
                            true
                        }
                        ComposeKey.DirectionDown, ComposeKey.S -> {
                            onIndexChange((selectedIndex + 1) % node.choices.size)
                            true
                        }
                        ComposeKey.Enter, ComposeKey.Spacebar -> {
                            val choice = node.choices.getOrNull(selectedIndex)
                            if (choice != null && isChoiceAvailable(choice, playerTier, playerReputation, playerKNOW)) {
                                onSelect(choice)
                            }
                            true
                        }
                        ComposeKey.Escape -> {
                            onCancel()
                            true
                        }
                        else -> false
                    }
                } else false
            },
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // NPC Name Header
        Text(
            text = node.npcName.uppercase(),
            style = RetroTypography.heading,
            color = RetroColors.BorderColor,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Divider(color = RetroColors.BorderColor, thickness = 2.dp)

        // NPC Speech
        Text(
            text = node.text,
            style = RetroTypography.body,
            color = RetroColors.NESWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Choices
        node.choices.forEachIndexed { index, choice ->
            val isSelected = index == selectedIndex
            val isAvailable = isChoiceAvailable(choice, playerTier, playerReputation, playerKNOW)

            ChoiceItem(
                choice = choice,
                isSelected = isSelected,
                isAvailable = isAvailable,
                promptBlink = promptBlink && isSelected,
                onClick = { if (isAvailable) onSelect(choice) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Controls hint
        Text(
            text = "↑↓ NAVIGATE | ENTER SELECT | ESC CANCEL",
            style = RetroTypography.caption,
            color = RetroColors.NESGray,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChoiceItem(
    choice: Choice,
    isSelected: Boolean,
    isAvailable: Boolean,
    promptBlink: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        !isAvailable -> RetroColors.NESDarkGray.copy(alpha = 0.5f)
        isSelected -> RetroColors.PrimaryID.copy(alpha = 0.2f)
        else -> RetroColors.CardBackground
    }

    val textColor = when {
        !isAvailable -> RetroColors.NESGray
        isSelected -> RetroColors.PrimaryID
        else -> RetroColors.NESWhite
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(4.dp))
            .border(2.dp, if (isSelected) RetroColors.PrimaryID else RetroColors.BorderColor, RoundedCornerShape(4.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection cursor
            Text(
                text = if (isSelected && promptBlink) "►" else " ",
                style = RetroTypography.body,
                color = RetroColors.PrimaryID
            )

            // Choice text
            Text(
                text = choice.text,
                style = RetroTypography.body,
                color = textColor
            )
        }

        // Show cost if applicable
        choice.cost?.let { cost ->
            Text(
                text = when (cost) {
                    is ChoiceCost.KNOW -> "Cost: ${cost.amount} KNOW"
                    is ChoiceCost.Reputation -> "Cost: ${cost.amount} REP"
                },
                style = RetroTypography.caption,
                color = if (isAvailable) RetroColors.Warning else RetroColors.Error
            )
        }
    }
}

@Composable
private fun SmartContractActionMenu(
    node: DialogueNode.SmartContractAction,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var isConfirming by remember { mutableStateOf(false) }
    var isExecuting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(600.dp)
            .background(RetroColors.WindowBackground, RoundedCornerShape(8.dp))
            .border(4.dp, RetroColors.BorderColor, RoundedCornerShape(8.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = node.actionType.name.replace("_", " "),
            style = RetroTypography.heading,
            color = RetroColors.BorderColor
        )

        Divider(color = RetroColors.BorderColor, thickness = 2.dp)

        // Description
        Text(
            text = node.description,
            style = RetroTypography.body,
            color = RetroColors.NESWhite,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Stake requirement
        node.requiredStake?.let { stake ->
            InfoBox(
                text = "REQUIRED STAKE: $stake KNOW",
                color = RetroColors.Warning
            )
        }

        // Risk/Reward
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RISK", style = RetroTypography.caption, color = RetroColors.Error)
                Text(node.risk, style = RetroTypography.body, color = RetroColors.NESWhite)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("REWARD", style = RetroTypography.caption, color = RetroColors.Success)
                Text(node.reward, style = RetroTypography.body, color = RetroColors.NESWhite)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isExecuting) {
            // Show loading state
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(color = RetroColors.PrimaryID)
                Text(
                    text = "EXECUTING TRANSACTION...",
                    style = RetroTypography.body,
                    color = RetroColors.PrimaryID
                )
                Text(
                    text = "Waiting for blockchain confirmation...",
                    style = RetroTypography.caption,
                    color = RetroColors.NESGray
                )
            }
        } else if (isConfirming) {
            // Confirmation step
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "⚠️ ARE YOU SURE? ⚠️",
                    style = RetroTypography.heading,
                    color = RetroColors.Error
                )
                Text(
                    text = "This will execute a real blockchain transaction.",
                    style = RetroTypography.body,
                    color = RetroColors.NESWhite
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RetroDialogButton("CANCEL", RetroColors.NESGray) {
                        isConfirming = false
                    }
                    RetroDialogButton("CONFIRM", RetroColors.Error) {
                        isExecuting = true
                        onConfirm()
                    }
                }
            }
        } else {
            // Initial state - show action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RetroDialogButton("CANCEL", RetroColors.Error) {
                    onCancel()
                }
                RetroDialogButton("STAKE & SUBMIT", RetroColors.Success) {
                    isConfirming = true
                }
            }
        }
    }
}

@Composable
private fun NarrationBox(
    text: String,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(600.dp)
            .background(Color.Black.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
            .border(4.dp, RetroColors.PrimaryID, RoundedCornerShape(8.dp))
            .padding(24.dp)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == ComposeKey.Enter || event.key == ComposeKey.Spacebar)) {
                    onContinue()
                    true
                } else false
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = text,
            style = RetroTypography.body,
            color = RetroColors.NESWhite,
            textAlign = TextAlign.Center
        )

        Text(
            text = "[ PRESS ENTER TO CONTINUE ]",
            style = RetroTypography.caption,
            color = RetroColors.PrimaryID
        )
    }
}

@Composable
private fun FarewellBox(
    text: String,
    onClose: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onClose()
    }

    Box(
        modifier = Modifier
            .width(400.dp)
            .background(RetroColors.WindowBackground, RoundedCornerShape(8.dp))
            .border(2.dp, RetroColors.Success, RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = RetroTypography.body,
            color = RetroColors.Success,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InfoBox(
    text: String,
    color: Color = RetroColors.PrimaryID
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = RetroTypography.caption,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════

private fun isChoiceAvailable(
    choice: Choice,
    playerTier: Int,
    playerReputation: Long,
    playerKNOW: BigInteger?
): Boolean {
    val requirement = choice.requirement ?: return true

    return when (requirement) {
        is ChoiceRequirement.MinimumTier -> playerTier >= requirement.tier
        is ChoiceRequirement.MinimumReputation -> playerReputation >= requirement.reputation
        is ChoiceRequirement.MinimumKNOW -> {
            val knowBalance = playerKNOW?.toBigDecimal()?.divide(
                java.math.BigDecimal("1000000000000000000")
            )?.toDouble() ?: 0.0
            knowBalance >= requirement.amount
        }
        is ChoiceRequirement.QuestCompleted -> {
            // TODO: Implement quest tracking system
            false
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// EXAMPLE NPC DEFINITIONS
// ═══════════════════════════════════════════════════════════════════════

/**
 * Old Man Deckard - Tutorial NPC in Greytown
 */
fun createDeckardNPC(
    onRequestVerification: () -> Unit
): InteractableNPC {
    return InteractableNPC(
        id = "deckard",
        name = "Old Man Deckard",
        sprite = npcDeckard,
        faction = NPCFaction.NEUTRAL,
        location = "greytown",
        dialogue = listOf(
            DialogueNode.Speech(
                id = "greeting",
                text = "Kid, I've seen a thousand bots walk through here claiming they're human. You say you're real? Then prove it.",
                npcName = "Old Man Deckard",
                choices = listOf(
                    Choice("How do I prove I'm human?", "explain"),
                    Choice("I need verification help", "help"),
                    Choice("Tell me about this city", "lore"),
                    Choice("Goodbye", "end")
                )
            ),
            DialogueNode.Speech(
                id = "explain",
                text = "You need to stake KNOW tokens and submit a verification request. Find someone with Primary ID status to vouch for you. The community votes. Win, you get rewards. Lose, you lose your stake. Simple as that.",
                npcName = "Old Man Deckard",
                choices = listOf(
                    Choice("I'm ready to request verification", "verify_prompt"),
                    Choice("I need more time", "greeting")
                )
            ),
            DialogueNode.SmartContractAction(
                id = "verify_prompt",
                actionType = ContractActionType.REQUEST_PRIMARY_VERIFICATION,
                description = "Submit a Primary ID verification request to the network. Your identity will be reviewed by the community.",
                requiredStake = 0.03,
                risk = "30% slashing if rejected",
                reward = "Primary ID + 1000 KNOW",
                onConfirm = onRequestVerification,
                onSuccess = "verify_success",
                onFailure = "verify_failure"
            ),
            DialogueNode.Speech(
                id = "verify_success",
                text = "Well done! Your claim has been submitted. Now we wait for the community to vote. Check back at the Guild Hall to see the results.",
                npcName = "Old Man Deckard",
                choices = listOf(
                    Choice("Thank you!", "end")
                )
            ),
            DialogueNode.End(
                id = "end",
                farewell = "Stay safe out there, kid."
            )
        )
    )
}
