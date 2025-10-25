package com.knomee.identity.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.knomee.identity.blockchain.*
import kotlinx.coroutines.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigInteger

/**
 * ViewModel managing blockchain state and user identity
 * Bridges between UI and Web3 services
 */
class IdentityViewModel {
    // Web3 Service
    private var web3Service: Web3Service? = null
    private var contractRepository: ContractRepository? = null

    // Coroutine scope for async operations
    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // UI State
    var isConnected by mutableStateOf(false)
        private set

    var currentAddress by mutableStateOf<String?>(null)
        private set

    var currentTier by mutableStateOf("GREYGHOST")
        private set

    var identityData by mutableStateOf<IdentityData?>(null)
        private set

    var activeClaims by mutableStateOf<List<ClaimData>>(emptyList())
        private set

    var governanceParams by mutableStateOf<GovernanceParams?>(null)
        private set

    var chainId by mutableStateOf<BigInteger?>(null)
        private set

    var blockNumber by mutableStateOf<BigInteger?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * Connect to Ethereum network (Anvil local testnet)
     */
    fun connect(useTestAccount: Boolean = true) {
        viewModelScope.launch {
            try {
                // Initialize Web3 service
                web3Service = if (useTestAccount) {
                    Web3Service(
                        rpcUrl = Web3Service.ANVIL_RPC,
                        privateKey = Web3Service.ANVIL_TEST_PRIVATE_KEY
                    )
                } else {
                    Web3Service(rpcUrl = Web3Service.ANVIL_RPC)
                }

                // Check connection
                val connected = web3Service?.isConnected() ?: false
                isConnected = connected

                if (connected) {
                    currentAddress = web3Service?.getWalletAddress()
                    chainId = web3Service?.getChainId()
                    blockNumber = web3Service?.getBlockNumber()

                    println("✅ Connected to Ethereum")
                    println("   Chain ID: $chainId")
                    println("   Block: $blockNumber")
                    println("   Address: $currentAddress")
                } else {
                    errorMessage = "Failed to connect to Ethereum network"
                }
            } catch (e: Exception) {
                errorMessage = "Connection error: ${e.message}"
                isConnected = false
            }
        }
    }

    /**
     * Disconnect from Ethereum network
     */
    fun disconnect() {
        web3Service?.shutdown()
        web3Service = null
        contractRepository = null
        isConnected = false
        currentAddress = null
        identityData = null
        chainId = null
        blockNumber = null
    }

    /**
     * Set contract addresses (after deployment)
     */
    fun setContractAddresses(
        governance: String,
        registry: String,
        consensus: String
    ) {
        web3Service?.let { service ->
            service.governanceAddress = governance
            service.registryAddress = registry
            service.consensusAddress = consensus

            // Initialize contract repository
            val web3j = Web3j.build(HttpService(Web3Service.ANVIL_RPC))
            contractRepository = ContractRepository(
                web3j = web3j,
                registryAddress = registry,
                consensusAddress = consensus,
                governanceAddress = governance
            )

            // Load initial data
            loadIdentityData()
            loadGovernanceParams()
        }
    }

    /**
     * Load identity data for current address
     */
    fun loadIdentityData() {
        val address = currentAddress ?: return
        viewModelScope.launch {
            try {
                identityData = contractRepository?.getIdentity(address)
                currentTier = identityData?.tier?.displayName ?: "GREYGHOST"
                println("Loaded identity: $identityData")
            } catch (e: Exception) {
                errorMessage = "Failed to load identity: ${e.message}"
            }
        }
    }

    /**
     * Load governance parameters
     */
    private fun loadGovernanceParams() {
        viewModelScope.launch {
            try {
                governanceParams = contractRepository?.getGovernanceParameters()
                println("Loaded governance: $governanceParams")
            } catch (e: Exception) {
                errorMessage = "Failed to load governance: ${e.message}"
            }
        }
    }

    /**
     * Load active claims
     */
    fun loadActiveClaims() {
        viewModelScope.launch {
            try {
                activeClaims = contractRepository?.getActiveClaims() ?: emptyList()
            } catch (e: Exception) {
                errorMessage = "Failed to load claims: ${e.message}"
            }
        }
    }

    /**
     * Refresh blockchain data (block number, balance, etc.)
     */
    fun refresh() {
        viewModelScope.launch {
            try {
                blockNumber = web3Service?.getBlockNumber()
                loadIdentityData()
                loadActiveClaims()
            } catch (e: Exception) {
                errorMessage = "Refresh failed: ${e.message}"
            }
        }
    }

    /**
     * Cleanup when ViewModel is disposed
     */
    fun onDispose() {
        viewModelScope.cancel()
        web3Service?.shutdown()
    }
}
