package com.knomee.identity.blockchain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger

/**
 * Web3j service for Ethereum blockchain connectivity
 * Manages connection to local Anvil testnet or Sepolia
 */
class Web3Service(
    private val rpcUrl: String = "http://127.0.0.1:8545", // Local Anvil by default
    private val privateKey: String? = null // Optional: for transaction signing
) {
    private val web3j: Web3j = Web3j.build(HttpService(rpcUrl))
    private val credentials: Credentials? = privateKey?.let { Credentials.create(it) }
    private val gasProvider = DefaultGasProvider()

    // Contract addresses (will be set after deployment)
    var governanceAddress: String = ""
    var registryAddress: String = ""
    var consensusAddress: String = ""

    /**
     * Check if connected to Ethereum network
     */
    suspend fun isConnected(): Boolean = withContext(Dispatchers.IO) {
        try {
            web3j.web3ClientVersion().send().web3ClientVersion != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get current network chain ID
     */
    suspend fun getChainId(): BigInteger? = withContext(Dispatchers.IO) {
        try {
            web3j.ethChainId().send().chainId
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get current block number
     */
    suspend fun getBlockNumber(): BigInteger? = withContext(Dispatchers.IO) {
        try {
            web3j.ethBlockNumber().send().blockNumber
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get ETH balance for an address
     */
    suspend fun getBalance(address: String): BigInteger? = withContext(Dispatchers.IO) {
        try {
            web3j.ethGetBalance(address, org.web3j.protocol.core.DefaultBlockParameterName.LATEST)
                .send().balance
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get user's wallet address
     */
    fun getWalletAddress(): String? = credentials?.address

    /**
     * Close the connection
     */
    fun shutdown() {
        web3j.shutdown()
    }

    companion object {
        // Anvil default test account (publicly known, DO NOT use in production!)
        const val ANVIL_TEST_PRIVATE_KEY = "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"

        // Network configurations
        const val ANVIL_RPC = "http://127.0.0.1:8545"
        const val SEPOLIA_RPC = "https://sepolia.infura.io/v3/YOUR_INFURA_KEY"
    }
}
