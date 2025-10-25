package com.knomee.identity.blockchain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.*
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Numeric
import java.math.BigInteger

/**
 * Service for creating and submitting blockchain transactions
 * Handles transaction signing, gas estimation, and receipt polling
 */
class TransactionService(
    private val web3j: Web3j,
    private val credentials: Credentials
) {
    private val gasProvider = DefaultGasProvider()

    /**
     * Request primary identity verification
     * Creates a new identity claim requiring 67% consensus
     */
    suspend fun requestPrimaryVerification(
        consensusAddress: String,
        justification: String,
        stakeAmount: BigInteger
    ): TransactionResult = withContext(Dispatchers.IO) {
        try {
            // Encode function: requestPrimaryVerification(string justification)
            val function = Function(
                "requestPrimaryVerification",
                listOf(Utf8String(justification)),
                emptyList()
            )

            submitTransaction(consensusAddress, function, stakeAmount)
        } catch (e: Exception) {
            TransactionResult.Error("Failed to request primary verification: ${e.message}")
        }
    }

    /**
     * Request to link secondary account to primary
     * Creates a link claim requiring 51% consensus
     */
    suspend fun requestLinkToPrimary(
        consensusAddress: String,
        primaryAddress: String,
        platform: String,
        justification: String,
        stakeAmount: BigInteger
    ): TransactionResult = withContext(Dispatchers.IO) {
        try {
            val function = Function(
                "requestLinkToPrimary",
                listOf(
                    Address(160, primaryAddress),
                    Utf8String(platform),
                    Utf8String(justification)
                ),
                emptyList()
            )

            submitTransaction(consensusAddress, function, stakeAmount)
        } catch (e: Exception) {
            TransactionResult.Error("Failed to request link: ${e.message}")
        }
    }

    /**
     * Flag a duplicate identity
     * Creates a duplicate challenge requiring 80% consensus
     */
    suspend fun challengeDuplicate(
        consensusAddress: String,
        suspectedAddress: String,
        justification: String,
        stakeAmount: BigInteger
    ): TransactionResult = withContext(Dispatchers.IO) {
        try {
            val function = Function(
                "challengeDuplicate",
                listOf(
                    Address(160, suspectedAddress),
                    Utf8String(justification)
                ),
                emptyList()
            )

            submitTransaction(consensusAddress, function, stakeAmount)
        } catch (e: Exception) {
            TransactionResult.Error("Failed to challenge duplicate: ${e.message}")
        }
    }

    /**
     * Vote in support of a claim
     */
    suspend fun vouchFor(
        consensusAddress: String,
        claimId: BigInteger,
        stakeAmount: BigInteger
    ): TransactionResult = withContext(Dispatchers.IO) {
        try {
            val function = Function(
                "vouchFor",
                listOf(Uint256(claimId)),
                emptyList()
            )

            submitTransaction(consensusAddress, function, stakeAmount)
        } catch (e: Exception) {
            TransactionResult.Error("Failed to vouch for: ${e.message}")
        }
    }

    /**
     * Vote against a claim
     */
    suspend fun vouchAgainst(
        consensusAddress: String,
        claimId: BigInteger,
        stakeAmount: BigInteger
    ): TransactionResult = withContext(Dispatchers.IO) {
        try {
            val function = Function(
                "vouchAgainst",
                listOf(Uint256(claimId)),
                emptyList()
            )

            submitTransaction(consensusAddress, function, stakeAmount)
        } catch (e: Exception) {
            TransactionResult.Error("Failed to vouch against: ${e.message}")
        }
    }

    /**
     * Claim rewards from a resolved claim
     */
    suspend fun claimRewards(
        consensusAddress: String,
        claimId: BigInteger
    ): TransactionResult = withContext(Dispatchers.IO) {
        try {
            val function = Function(
                "claimRewards",
                listOf(Uint256(claimId)),
                emptyList()
            )

            submitTransaction(consensusAddress, function, BigInteger.ZERO)
        } catch (e: Exception) {
            TransactionResult.Error("Failed to claim rewards: ${e.message}")
        }
    }

    /**
     * Core transaction submission logic
     */
    private suspend fun submitTransaction(
        contractAddress: String,
        function: Function,
        value: BigInteger
    ): TransactionResult {
        try {
            // Encode function call
            val encodedFunction = FunctionEncoder.encode(function)

            // Get nonce
            val nonce = web3j.ethGetTransactionCount(
                credentials.address,
                DefaultBlockParameterName.LATEST
            ).send().transactionCount

            // Get gas price
            val gasPrice = web3j.ethGasPrice().send().gasPrice

            // Estimate gas (or use default)
            val gasLimit = BigInteger.valueOf(500_000) // Conservative estimate

            // Create raw transaction
            val rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                gasLimit,
                contractAddress,
                value,
                encodedFunction
            )

            // Sign transaction
            val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
            val hexValue = Numeric.toHexString(signedMessage)

            // Send transaction
            val response: EthSendTransaction = web3j.ethSendRawTransaction(hexValue).send()

            if (response.hasError()) {
                return TransactionResult.Error("Transaction failed: ${response.error.message}")
            }

            val txHash = response.transactionHash
            println("Transaction sent: $txHash")

            // Poll for receipt
            val receipt = pollForReceipt(txHash)

            return if (receipt != null && receipt.isStatusOK) {
                TransactionResult.Success(txHash, receipt.gasUsed)
            } else {
                TransactionResult.Error("Transaction reverted: $txHash")
            }
        } catch (e: Exception) {
            return TransactionResult.Error("Transaction error: ${e.message}")
        }
    }

    /**
     * Poll for transaction receipt
     */
    private suspend fun pollForReceipt(txHash: String): TransactionReceipt? {
        repeat(30) { // Poll for 30 seconds
            try {
                val receiptResponse = web3j.ethGetTransactionReceipt(txHash).send()
                if (receiptResponse.transactionReceipt.isPresent) {
                    return receiptResponse.transactionReceipt.get()
                }
                kotlinx.coroutines.delay(1000) // Wait 1 second between polls
            } catch (e: Exception) {
                println("Error polling receipt: ${e.message}")
            }
        }
        return null
    }
}

/**
 * Transaction result sealed class
 */
sealed class TransactionResult {
    data class Success(val txHash: String, val gasUsed: BigInteger) : TransactionResult()
    data class Error(val message: String) : TransactionResult()
    object Pending : TransactionResult()
}
