package com.knomee.identity.blockchain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.*
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import java.math.BigInteger

/**
 * Repository for interacting with deployed smart contracts
 * Uses Web3j to call contract functions and parse responses
 */
class ContractRepository(
    private val web3j: Web3j,
    private val registryAddress: String,
    private val consensusAddress: String,
    private val governanceAddress: String
) {

    /**
     * Get identity data for an address from IdentityRegistry
     */
    suspend fun getIdentity(address: String): IdentityData? = withContext(Dispatchers.IO) {
        try {
            // Call getIdentity(address) function
            val function = Function(
                "getIdentity",
                listOf(Address(160, address)),
                listOf(
                    object : TypeReference<Uint8>() {},      // tier
                    object : TypeReference<Address>() {},    // primaryAccount
                    object : TypeReference<Bool>() {},       // underChallenge
                    object : TypeReference<Uint256>() {},    // vouchesGiven
                    object : TypeReference<Uint256>() {},    // vouchesReceived
                    object : TypeReference<Uint256>() {}     // reputationScore
                )
            )

            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Transaction.createEthCallTransaction(address, registryAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send()

            if (response.hasError()) {
                println("Error calling getIdentity: ${response.error.message}")
                return@withContext null
            }

            val result = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (result.size < 6) return@withContext null

            val tier = IdentityTier.fromBigInteger((result[0] as Uint).value)
            val primaryAccount = (result[1] as Address).value
            val underChallenge = (result[2] as Bool).value
            val vouchesGiven = (result[3] as Uint).value.toInt()
            val vouchesReceived = (result[4] as Uint).value.toInt()
            val reputationScore = (result[5] as Uint).value

            IdentityData(
                address = address,
                tier = tier,
                primaryAccount = primaryAccount,
                linkedAccounts = emptyMap(), // TODO: Fetch linked accounts separately
                underChallenge = underChallenge,
                vouchesGiven = vouchesGiven,
                vouchesReceived = vouchesReceived,
                reputationScore = reputationScore
            )
        } catch (e: Exception) {
            println("Exception getting identity: ${e.message}")
            null
        }
    }

    /**
     * Get identity tier for an address
     */
    suspend fun getTier(address: String): IdentityTier? = withContext(Dispatchers.IO) {
        try {
            val function = Function(
                "getTier",
                listOf(Address(160, address)),
                listOf(object : TypeReference<Uint8>() {})
            )

            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Transaction.createEthCallTransaction(address, registryAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send()

            if (response.hasError()) return@withContext null

            val result = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (result.isEmpty()) return@withContext null

            IdentityTier.fromBigInteger((result[0] as Uint).value)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get active claims from IdentityConsensus
     * Note: This requires iterating through claim IDs or using events
     */
    suspend fun getActiveClaims(): List<ClaimData> = withContext(Dispatchers.IO) {
        // TODO: Implement by querying recent ClaimCreated events
        // For now, return empty list
        emptyList()
    }

    /**
     * Get governance parameters
     */
    suspend fun getGovernanceParameters(): GovernanceParams? = withContext(Dispatchers.IO) {
        try {
            // Get link threshold (51%)
            val linkThreshold = callUintFunction(governanceAddress, "linkThreshold")
            val primaryThreshold = callUintFunction(governanceAddress, "primaryThreshold")
            val duplicateThreshold = callUintFunction(governanceAddress, "duplicateThreshold")
            val minStake = callUintFunction(governanceAddress, "minStakeWei")

            if (linkThreshold != null && primaryThreshold != null && duplicateThreshold != null && minStake != null) {
                GovernanceParams(
                    linkThreshold = linkThreshold,
                    primaryThreshold = primaryThreshold,
                    duplicateThreshold = duplicateThreshold,
                    minStakeWei = minStake
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Helper: Call a uint-returning function
     */
    private suspend fun callUintFunction(contractAddress: String, functionName: String): BigInteger? {
        try {
            val function = Function(
                functionName,
                emptyList(),
                listOf(object : TypeReference<Uint256>() {})
            )

            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send()

            if (response.hasError()) return null

            val result = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            return if (result.isNotEmpty()) (result[0] as Uint).value else null
        } catch (e: Exception) {
            return null
        }
    }
}

/**
 * Governance parameters data class
 */
data class GovernanceParams(
    val linkThreshold: BigInteger,      // 5100 = 51%
    val primaryThreshold: BigInteger,   // 6700 = 67%
    val duplicateThreshold: BigInteger, // 8000 = 80%
    val minStakeWei: BigInteger         // 0.01 ETH in wei
)
