package com.knomee.identity.blockchain

import com.knomee.identity.utils.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.web3j.abi.EventEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Event
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.Log
import java.math.BigInteger

/**
 * Service for listening to blockchain events
 * Monitors ClaimCreated, VouchSubmitted, ClaimResolved events
 *
 * FIXED: Uses callbackFlow to properly emit events to Flow
 */
class EventListener(
    private val web3j: Web3j,
    private val consensusAddress: String
) {
    private val log = logger()

    /**
     * Listen for ClaimCreated events in real-time
     * Emitted when someone creates a new identity claim
     *
     * FIXED: Now properly emits events to Flow using callbackFlow
     */
    fun listenForClaimCreated(): Flow<ClaimCreatedEvent> = callbackFlow {
        log.info("Starting ClaimCreated event listener")

        try {
            val event = Event(
                "ClaimCreated",
                listOf(
                    TypeReference.create(Uint256::class.java, true),  // indexed claimId
                    TypeReference.create(Address::class.java, true),  // indexed claimant
                    TypeReference.create(Uint8::class.java, false)    // claimType
                )
            )

            val filter = EthFilter(
                DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.LATEST,
                consensusAddress
            ).addSingleTopic(EventEncoder.encode(event))

            val subscription = web3j.ethLogFlowable(filter).subscribe(
                { eventLog ->
                    try {
                        val claimCreated = parseClaimCreatedEvent(eventLog)
                        log.info("Claim created: $claimCreated")

                        // FIXED: Actually emit to flow!
                        trySend(claimCreated)
                    } catch (e: Exception) {
                        log.error("Error parsing ClaimCreated event", e)
                    }
                },
                { error ->
                    log.error("Error in ClaimCreated subscription", error)
                    close(error)
                }
            )

            // Wait for cancellation
            awaitClose {
                log.info("Closing ClaimCreated event listener")
                subscription.dispose()
            }
        } catch (e: Exception) {
            log.error("Error setting up ClaimCreated listener", e)
            close(e)
        }
    }

    /**
     * Get recent ClaimCreated events (last N blocks)
     */
    suspend fun getRecentClaims(fromBlock: BigInteger): List<ClaimCreatedEvent> = withContext(Dispatchers.IO) {
        try {
            val event = Event(
                "ClaimCreated",
                listOf(
                    TypeReference.create(Uint256::class.java, true),
                    TypeReference.create(Address::class.java, true),
                    TypeReference.create(Uint8::class.java, false)
                )
            )

            val filter = EthFilter(
                org.web3j.protocol.core.DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameterName.LATEST,
                consensusAddress
            ).addSingleTopic(EventEncoder.encode(event))

            val logs = web3j.ethGetLogs(filter).send().logs
            logs.mapNotNull { logResult ->
                try {
                    val eventLog = logResult.get() as Log
                    parseClaimCreatedEvent(eventLog)
                } catch (e: Exception) {
                    log.warn("Error parsing event log", e)
                    null
                }
            }
        } catch (e: Exception) {
            log.error("Error getting recent claims", e)
            emptyList()
        }
    }

    /**
     * Parse ClaimCreated event from log
     */
    private fun parseClaimCreatedEvent(log: Log): ClaimCreatedEvent {
        // Topics: [eventSignature, indexed_claimId, indexed_claimant]
        val claimId = BigInteger(log.topics[1].substring(2), 16)
        val claimant = "0x" + log.topics[2].substring(26) // Remove padding

        // Parse non-indexed data (claimType)
        @Suppress("UNCHECKED_CAST")
        val nonIndexedParams = listOf(
            object : TypeReference<Uint8>() {}
        ) as List<TypeReference<Type<*>>>
        val data = FunctionReturnDecoder.decode(log.data, nonIndexedParams)
        val claimType = if (data.isNotEmpty()) {
            (data[0] as org.web3j.abi.datatypes.generated.Uint8).value.toInt()
        } else 0

        return ClaimCreatedEvent(
            claimId = claimId,
            claimant = claimant,
            claimType = ClaimType.fromValue(claimType),
            blockNumber = log.blockNumber,
            transactionHash = log.transactionHash
        )
    }

    /**
     * Get all active claims from events
     * This is more efficient than iterating through claim IDs
     */
    suspend fun getAllActiveClaims(lookbackBlocks: Long = 1000): List<BigInteger> = withContext(Dispatchers.IO) {
        try {
            val currentBlock = web3j.ethBlockNumber().send().blockNumber
            val fromBlock = (currentBlock - BigInteger.valueOf(lookbackBlocks)).max(BigInteger.ZERO)

            log.debug("Fetching active claims from block $fromBlock to $currentBlock")

            val recentClaims = getRecentClaims(fromBlock)
            recentClaims.map { it.claimId }
        } catch (e: Exception) {
            log.error("Error getting all active claims", e)
            emptyList()
        }
    }
}

/**
 * ClaimCreated event data
 */
data class ClaimCreatedEvent(
    val claimId: BigInteger,
    val claimant: String,
    val claimType: ClaimType,
    val blockNumber: BigInteger,
    val transactionHash: String
)

/**
 * VouchSubmitted event data
 */
data class VouchSubmittedEvent(
    val claimId: BigInteger,
    val voucher: String,
    val support: Boolean,
    val weight: BigInteger,
    val stakeAmount: BigInteger
)

/**
 * ClaimResolved event data
 */
data class ClaimResolvedEvent(
    val claimId: BigInteger,
    val approved: Boolean,
    val finalWeightFor: BigInteger,
    val finalWeightAgainst: BigInteger
)
