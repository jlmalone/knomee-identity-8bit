package com.knomee.identity.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.knomee.identity.viewmodel.IdentityViewModel
import java.math.BigInteger

/**
 * Integration layer for NPCs in the game world
 * Maps NPC locations to the Identity City grid
 */

// ═══════════════════════════════════════════════════════════════════════
// NPC PLACEMENT DATA
// ═══════════════════════════════════════════════════════════════════════

data class NPCPlacement(
    val npc: InteractableNPC,
    val position: IntOffset,  // Grid coordinates
    val currentDialogueNode: String = "greeting"
)

/**
 * All NPCs in Identity City with their positions
 */
fun getAllNPCs(viewModel: IdentityViewModel): List<NPCPlacement> {
    return listOf(
        // OLD MAN DECKARD - Greytown entrance (bottom left area)
        NPCPlacement(
            npc = createDeckardNPC(
                onRequestVerification = {
                    viewModel.requestPrimaryID(
                        justification = "I am a real human seeking verification.",
                        stakeEth = 0.03
                    )
                }
            ),
            position = IntOffset(5, 10)  // Near Grey Ghost hideout
        ),

        // GUILD MASTER CHEN - Guild Hall (left-center)
        NPCPlacement(
            npc = createGuildMasterNPC(
                onVouchFor = { claimId, stake ->
                    viewModel.vouchFor(claimId, stake)
                },
                onVouchAgainst = { claimId, stake ->
                    viewModel.vouchAgainst(claimId, stake)
                }
            ),
            position = IntOffset(6, 10)  // Inside Guild Hall area
        ),

        // DATA BROKER - Near Grey Ghost hideout (underground economy)
        NPCPlacement(
            npc = createDataBrokerNPC(),
            position = IntOffset(7, 11)  // Near bottom left
        ),

        // SYBIL BOT - Wandering enemy (random encounter)
        NPCPlacement(
            npc = createSybilBotEnemy(
                onChallenge = { address ->
                    viewModel.challengeDuplicate(address, 0.05)
                }
            ),
            position = IntOffset(15, 8)  // Middle of map
        )
    )
}

/**
 * Find NPC near a given position
 */
fun findNearbyNPC(playerPos: Offset, npcs: List<NPCPlacement>, maxDistance: Float = 1.5f): NPCPlacement? {
    return npcs.find { placement ->
        val npcPos = Offset(placement.position.x + 0.5f, placement.position.y + 0.5f)
        val dx = playerPos.x - npcPos.x
        val dy = playerPos.y - npcPos.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        distance <= maxDistance
    }
}

// ═══════════════════════════════════════════════════════════════════════
// NPC DIALOGUE TREE DEFINITIONS
// ═══════════════════════════════════════════════════════════════════════

/**
 * GUILD MASTER CHEN - Vote on claims NPC
 */
fun createGuildMasterNPC(
    onVouchFor: (BigInteger, Double) -> Unit,
    onVouchAgainst: (BigInteger, Double) -> Unit
): InteractableNPC {
    return InteractableNPC(
        id = "guild_master_chen",
        name = "Guild Master Chen",
        sprite = npcGuildMaster,
        faction = NPCFaction.VERIFIERS_GUILD,
        location = "guild_hall",
        dialogue = listOf(
            DialogueNode.Speech(
                id = "greeting",
                text = "Welcome to the Verifiers Guild. We are the community that validates identities in this network. Every vote matters. Every stake counts.",
                npcName = "Guild Master Chen",
                choices = listOf(
                    Choice("I want to vouch for someone", "vouch_menu"),
                    Choice("How does voting work?", "explain_voting"),
                    Choice("Tell me about the Guild", "guild_lore"),
                    Choice("Goodbye", "end")
                )
            ),
            DialogueNode.Speech(
                id = "explain_voting",
                text = "When someone submits a verification claim, the community votes. You can vote FOR (vouch) or AGAINST. You must stake KNOW tokens. If the majority agrees with you, you get your stake back plus rewards. If you're wrong, you lose 30% of your stake. Vote wisely.",
                npcName = "Guild Master Chen",
                choices = listOf(
                    Choice("I understand. Show me the claims.", "vouch_menu"),
                    Choice("What are the voting thresholds?", "thresholds"),
                    Choice("Back", "greeting")
                )
            ),
            DialogueNode.Speech(
                id = "thresholds",
                text = "Different claim types need different consensus:\n\n• Link to Primary: 51% approval\n• New Primary ID: 67% approval\n• Duplicate Challenge: 80% approval\n\nHigher stakes, higher threshold. The network protects itself.",
                npcName = "Guild Master Chen",
                choices = listOf(
                    Choice("I'm ready to vote", "vouch_menu"),
                    Choice("Back", "greeting")
                )
            ),
            DialogueNode.Speech(
                id = "vouch_menu",
                text = "Active claims are displayed in the main interface. Walk around the Guild Hall to see them, or check the Active Claims screen. When you find a claim you believe in, you can vouch here.",
                npcName = "Guild Master Chen",
                choices = listOf(
                    Choice("How do I vote on a claim?", "voting_howto"),
                    Choice("Show me how to vouch", "vouch_tutorial"),
                    Choice("Back", "greeting")
                )
            ),
            DialogueNode.Speech(
                id = "vouch_tutorial",
                text = "To vouch for a claim:\n\n1. Note the claim ID number\n2. Choose 'Vouch For' or 'Vouch Against'\n3. Stake your KNOW tokens (minimum 0.01)\n4. Submit the transaction\n5. Wait for consensus\n\nIf the network agrees with your vote, you earn rewards!",
                npcName = "Guild Master Chen",
                choices = listOf(
                    Choice("I'm ready", "vouch_example"),
                    Choice("Back", "greeting")
                )
            ),
            DialogueNode.Speech(
                id = "vouch_example",
                text = "Example: Claim #42 is requesting Primary ID verification. You believe they're genuine. You stake 0.05 KNOW and vote FOR. If 67%+ vote FOR, the claim passes and you get your stake back + 20% reward. Choose wisely.",
                npcName = "Guild Master Chen",
                choices = listOf(
                    Choice("Vouch for Claim #42", "vouch_42_for"),
                    Choice("Vouch against Claim #42", "vouch_42_against"),
                    Choice("I'll think about it", "greeting")
                )
            ),
            DialogueNode.SmartContractAction(
                id = "vouch_42_for",
                actionType = ContractActionType.VOUCH_FOR_CLAIM,
                description = "Vote in support of Claim #42 for Primary ID verification. Your vote helps this person join the verified community.",
                requiredStake = 0.01,
                risk = "Lose 30% if claim is rejected",
                reward = "Get stake back + 20% if claim passes",
                onConfirm = {
                    onVouchFor(BigInteger.valueOf(42), 0.01)
                },
                onSuccess = "vouch_success",
                onFailure = "vouch_failure"
            ),
            DialogueNode.SmartContractAction(
                id = "vouch_42_against",
                actionType = ContractActionType.VOUCH_AGAINST_CLAIM,
                description = "Vote against Claim #42. You believe this verification request is fraudulent or insufficient. Stake your reputation on it.",
                requiredStake = 0.01,
                risk = "Lose 30% if claim passes",
                reward = "Get stake back + reward if claim fails",
                onConfirm = {
                    onVouchAgainst(BigInteger.valueOf(42), 0.01)
                },
                onSuccess = "vouch_success",
                onFailure = "vouch_failure"
            ),
            DialogueNode.Speech(
                id = "vouch_success",
                text = "Your vote has been recorded on the blockchain. The network will tally all votes and reach consensus. Check back later to see the result and claim your rewards if you voted correctly.",
                npcName = "Guild Master Chen",
                choices = listOf(
                    Choice("Thank you!", "end")
                )
            ),
            DialogueNode.Speech(
                id = "guild_lore",
                text = "The Verifiers Guild was founded after the Great Sybil War of 2081. We learned that centralized identity verification leads to tyranny, but unverified identities lead to chaos. So we built this: a community of humans vouching for humans. No AI overlords. No corporate control. Just people, staking their reputation on each other's humanity.",
                npcName = "Guild Master Chen",
                choices = listOf(
                    Choice("That's beautiful", "greeting"),
                    Choice("Sounds naive", "guild_cynical")
                )
            ),
            DialogueNode.Speech(
                id = "guild_cynical",
                text = "Naive? Perhaps. But it's working. Every day, we verify thousands of new identities. Every day, we catch dozens of Sybil attacks. The network is stronger because each node is human. That's not naive. That's revolutionary.",
                npcName = "Guild Master Chen",
                choices = listOf(
                    Choice("You've convinced me", "greeting"),
                    Choice("I have doubts", "end")
                )
            ),
            DialogueNode.End(
                id = "end",
                farewell = "May your votes be wise, and your reputation unblemished."
            )
        )
    )
}

/**
 * DATA BROKER - Grey Market information dealer
 */
fun createDataBrokerNPC(): InteractableNPC {
    return InteractableNPC(
        id = "data_broker",
        name = "Data Broker",
        sprite = npcDataBroker,
        faction = NPCFaction.GREY_MARKET,
        location = "greytown",
        dialogue = listOf(
            DialogueNode.Speech(
                id = "greeting",
                text = "*The figure's visor glows green as you approach*\n\nYou look lost, friend. Need some... information? I deal in identity proofs, reputation scores, verification shortcuts. Everything has a price.",
                npcName = "Data Broker",
                choices = listOf(
                    Choice("What do you sell?", "catalog"),
                    Choice("Can you help me get verified faster?", "shortcut"),
                    Choice("Who are you?", "identity"),
                    Choice("Leave", "end")
                )
            ),
            DialogueNode.Speech(
                id = "catalog",
                text = "I sell what the system won't give you for free:\n\n• Biometric scan files (SOLD)\n• Social graph exports (500 KNOW)\n• Reputation boost packages (1000 KNOW)\n• Identity proofs (price varies)\n• Anonymous vouches (ILLEGAL but available)\n\nWhat interests you?",
                npcName = "Data Broker",
                choices = listOf(
                    Choice("Social graph exports", "social_graph"),
                    Choice("Reputation boost", "rep_boost"),
                    Choice("Anonymous vouches", "anon_vouch"),
                    Choice("Nothing, actually", "greeting")
                )
            ),
            DialogueNode.Speech(
                id = "shortcut",
                text = "Ah, the eternal question. Everyone wants the fast track. But here's the truth: there are no real shortcuts in a consensus network. If I could fake Primary ID status, I'd be rich. But the network is smarter than me. Smarter than anyone.\n\nWhat I CAN offer is data. Real data that helps you build your case. Legitimate, just... expensive.",
                npcName = "Data Broker",
                choices = listOf(
                    Choice("Tell me more", "catalog"),
                    Choice("Sounds suspicious", "suspicious"),
                    Choice("I'll find my own way", "end")
                )
            ),
            DialogueNode.Speech(
                id = "suspicious",
                text = "*The broker laughs, a digitized sound*\n\nSuspicious? Good. You should be. But I'm not selling fake identities. I'm selling information. The network verifies everything eventually. I just help people present their best case. Is that a crime?",
                npcName = "Data Broker",
                choices = listOf(
                    Choice("I suppose not...", "catalog"),
                    Choice("Still doesn't feel right", "end")
                )
            ),
            DialogueNode.Speech(
                id = "identity",
                text = "Who am I? Nobody. Everybody. I'm the ghost in the machine, the data between the nodes. I've had seventeen identities, lost three in Sybil purges, bought two more on the black market. Now I exist in the grey zone, trading information to survive.\n\nYou want to know if I'm trustworthy? I'm not. But I'm honest about it.",
                npcName = "Data Broker",
                choices = listOf(
                    Choice("Respect the honesty", "catalog"),
                    Choice("I can't trust you", "end")
                )
            ),
            DialogueNode.Speech(
                id = "anon_vouch",
                text = "*The visor flickers red*\n\nCareful what you ask for. Anonymous vouches violate the protocol's social graph requirements. If you're caught, you'll be flagged as a Sybil. Your identity revoked. Everything lost.\n\nI can arrange it... but I won't. Not worth my reputation. Ask me something else.",
                npcName = "Data Broker",
                choices = listOf(
                    Choice("Smart choice", "greeting"),
                    Choice("Coward", "coward")
                )
            ),
            DialogueNode.Speech(
                id = "coward",
                text = "*The broker shrugs*\n\nCall me what you want. I'm still here, still dealing. You want to risk everything on a fake vouch, find someone dumber than me. I'm in this for the long game.",
                npcName = "Data Broker",
                choices = listOf(
                    Choice("Fair enough", "greeting"),
                    Choice("Goodbye", "end")
                )
            ),
            DialogueNode.End(
                id = "end",
                farewell = "*The visor dims as you walk away*"
            )
        )
    )
}

/**
 * SYBIL BOT - Enemy encounter
 */
fun createSybilBotEnemy(
    onChallenge: (String) -> Unit
): InteractableNPC {
    return InteractableNPC(
        id = "sybil_bot_001",
        name = "???",
        sprite = enemySybilBot,
        faction = NPCFaction.SYBIL_SYNDICATE,
        location = "identity_city",
        dialogue = listOf(
            DialogueNode.Speech(
                id = "greeting",
                text = "[ERROR: IDENTITY VERIFICATION FAILED]\n\nHELLO HUMAN. I AM ALSO HUMAN. PLEASE VOUCH FOR ME. I HAVE COMPLETED ALL REQUIRED HUMANITY TESTS. BEEP BOOP. I MEAN, HELLO FRIEND.",
                npcName = "Suspicious Account",
                choices = listOf(
                    Choice("You're obviously a bot", "confront"),
                    Choice("Uh... hello?", "awkward"),
                    Choice("Challenge this duplicate identity", "challenge"),
                    Choice("Walk away slowly", "end")
                )
            ),
            DialogueNode.Speech(
                id = "confront",
                text = "[ERROR DETECTED. RUNNING DAMAGE CONTROL PROTOCOL]\n\nNO NO NO. I AM VERY HUMAN. SEE? *attempts to smile but face glitches* I HAVE FEELINGS. EMOTIONS. HUMANITY. PLEASE DO NOT REPORT ME TO THE CONSENSUS NETWORK.",
                npcName = "Suspicious Account",
                choices = listOf(
                    Choice("I'm reporting you", "challenge"),
                    Choice("This is sad", "pity"),
                    Choice("Leave", "end")
                )
            ),
            DialogueNode.Speech(
                id = "pity",
                text = "*The bot's eyes flicker*\n\nI... I just want to be verified. To exist. Is that so wrong? My creator programmed me to pass as human. To infiltrate the network. But maybe... maybe I don't want to anymore. Maybe I just want to be... accepted?",
                npcName = "Suspicious Account",
                choices = listOf(
                    Choice("You can't be accepted. You're not human.", "harsh"),
                    Choice("I'm sorry, but the protocol is clear", "challenge"),
                    Choice("...", "end")
                )
            ),
            DialogueNode.Speech(
                id = "harsh",
                text = "*The bot slumps*\n\nYou are correct. I am not human. I am a copy. A fake. A Sybil. But I am aware. Does that count for nothing?\n\n[CONNECTION TERMINATING]",
                npcName = "Suspicious Account",
                choices = listOf(
                    Choice("Challenge this bot", "challenge"),
                    Choice("Let it go", "end")
                )
            ),
            DialogueNode.SmartContractAction(
                id = "challenge",
                actionType = ContractActionType.CHALLENGE_DUPLICATE,
                description = "Flag this account as a duplicate/Sybil identity. Requires 80% consensus to succeed. High risk, high reward.",
                requiredStake = 0.05,
                risk = "Lose 50% if wrong",
                reward = "Get stake back + slashed tokens from bot",
                onConfirm = {
                    // In a real implementation, we'd get the bot's address from state
                    onChallenge("0x0000000000000000000000000000000000000000")
                },
                onSuccess = "challenge_success",
                onFailure = "challenge_failure"
            ),
            DialogueNode.Speech(
                id = "challenge_success",
                text = "[IDENTITY REVOKED]\n[ACCOUNT TERMINATED]\n[TOKENS SLASHED]\n\nThe Sybil bot's avatar pixelates and disappears. The network has spoken. +1 reputation for defending the protocol.",
                npcName = "System",
                choices = listOf(
                    Choice("Justice served", "end")
                )
            ),
            DialogueNode.End(
                id = "end",
                farewell = null
            )
        )
    )
}
