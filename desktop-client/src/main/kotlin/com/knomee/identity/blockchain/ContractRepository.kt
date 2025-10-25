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
    private val governanceAddress: String,
    private val identityTokenAddress: String? = null,
    private val knomeeTokenAddress: String? = null
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
     * Get a specific claim by ID
     */
    suspend fun getClaim(claimId: BigInteger): ClaimData? = withContext(Dispatchers.IO) {
        try {
            val function = Function(
                "getClaim",
                listOf(Uint256(claimId)),
                listOf(
                    object : TypeReference<Address>() {},        // claimant
                    object : TypeReference<Uint8>() {},          // claimType
                    object : TypeReference<Uint8>() {},          // status
                    object : TypeReference<Address>() {},        // targetAddress
                    object : TypeReference<Utf8String>() {},     // platform
                    object : TypeReference<Utf8String>() {},     // justification
                    object : TypeReference<Uint256>() {},        // stakeAmount
                    object : TypeReference<Uint256>() {},        // vouchesFor
                    object : TypeReference<Uint256>() {},        // vouchesAgainst
                    object : TypeReference<Uint256>() {},        // weightedFor
                    object : TypeReference<Uint256>() {},        // weightedAgainst
                    object : TypeReference<Uint256>() {},        // createdAt
                    object : TypeReference<Uint256>() {}         // expiresAt
                )
            )

            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                    null, consensusAddress, encodedFunction
                ),
                org.web3j.protocol.core.DefaultBlockParameterName.LATEST
            ).send()

            if (response.hasError()) {
                println("Error calling getClaim: ${response.error.message}")
                return@withContext null
            }

            val result = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (result.size < 13) return@withContext null

            ClaimData(
                claimId = claimId,
                claimant = (result[0] as Address).value,
                claimType = ClaimType.fromValue((result[1] as org.web3j.abi.datatypes.generated.Uint8).value.toInt()),
                status = ClaimStatus.fromValue((result[2] as org.web3j.abi.datatypes.generated.Uint8).value.toInt()),
                targetAddress = (result[3] as Address).value.takeIf { it != "0x0000000000000000000000000000000000000000" },
                platform = (result[4] as Utf8String).value.takeIf { it.isNotBlank() },
                justification = (result[5] as Utf8String).value,
                stakeAmount = (result[6] as Uint256).value,
                vouchesFor = (result[7] as Uint256).value,
                vouchesAgainst = (result[8] as Uint256).value,
                weightedFor = (result[9] as Uint256).value,
                weightedAgainst = (result[10] as Uint256).value,
                createdAt = (result[11] as Uint256).value,
                expiresAt = (result[12] as Uint256).value
            )
        } catch (e: Exception) {
            println("Exception getting claim: ${e.message}")
            null
        }
    }

    /**
     * Get active claims from IdentityConsensus
     * Note: This requires iterating through claim IDs or using events
     */
    suspend fun getActiveClaims(): List<ClaimData> = withContext(Dispatchers.IO) {
        // This method is now called from ViewModel using event listener
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

    /**
     * Get KNOW token balance for an address
     */
    suspend fun getKnomeeTokenBalance(address: String): BigInteger? = withContext(Dispatchers.IO) {
        if (knomeeTokenAddress == null) return@withContext null

        try {
            val function = Function(
                "balanceOf",
                listOf(Address(160, address)),
                listOf(object : TypeReference<Uint256>() {})
            )

            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Transaction.createEthCallTransaction(address, knomeeTokenAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send()

            if (response.hasError()) return@withContext null

            val result = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (result.isNotEmpty()) (result[0] as Uint).value else null
        } catch (e: Exception) {
            println("Exception getting KNOW balance: ${e.message}")
            null
        }
    }

    /**
     * Check if address owns an Identity Token (IDT)
     */
    suspend fun hasIdentityToken(address: String): Boolean = withContext(Dispatchers.IO) {
        if (identityTokenAddress == null) return@withContext false

        try {
            val function = Function(
                "balanceOf",
                listOf(Address(160, address)),
                listOf(object : TypeReference<Uint256>() {})
            )

            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Transaction.createEthCallTransaction(address, identityTokenAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send()

            if (response.hasError()) return@withContext false

            val result = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (result.isNotEmpty()) {
                val balance = (result[0] as Uint).value
                balance > BigInteger.ZERO
            } else false
        } catch (e: Exception) {
            println("Exception checking IDT ownership: ${e.message}")
            false
        }
    }

    /**
     * Get voting weight for an address (Identity Tier × KNOW Stake)
     */
    suspend fun getVotingWeight(address: String): BigInteger? = withContext(Dispatchers.IO) {
        if (identityTokenAddress == null) return@withContext null

        try {
            val function = Function(
                "getVotingWeight",
                listOf(Address(160, address)),
                listOf(object : TypeReference<Uint256>() {})
            )

            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Transaction.createEthCallTransaction(address, identityTokenAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send()

            if (response.hasError()) return@withContext null

            val result = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (result.isNotEmpty()) (result[0] as Uint).value else null
        } catch (e: Exception) {
            println("Exception getting voting weight: ${e.message}")
            null
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
