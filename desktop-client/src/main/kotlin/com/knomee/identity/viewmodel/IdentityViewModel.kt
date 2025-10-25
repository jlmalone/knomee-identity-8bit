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
    private var transactionService: TransactionService? = null
    private var eventListener: EventListener? = null

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

    var transactionStatus by mutableStateOf<String?>(null)
        private set

    var isTransactionPending by mutableStateOf(false)
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

            // Initialize transaction service
            web3Service?.let { service ->
                if (service.getWalletAddress() != null) {
                    val credentials = org.web3j.crypto.Credentials.create(Web3Service.ANVIL_TEST_PRIVATE_KEY)
                    transactionService = TransactionService(web3j, credentials)
                }
            }

            // Initialize event listener
            eventListener = EventListener(web3j, consensus)

            // Load initial data
            loadIdentityData()
            loadGovernanceParams()
            loadActiveClaimsFromEvents()
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
     * Load active claims from blockchain events
     */
    private fun loadActiveClaimsFromEvents() {
        viewModelScope.launch {
            try {
                // Get recent claim IDs from events
                val claimIds = eventListener?.getAllActiveClaims(lookbackBlocks = 10000) ?: emptyList()

                // Fetch full claim data for each ID
                val claims = mutableListOf<ClaimData>()
                claimIds.forEach { claimId ->
                    contractRepository?.getClaim(claimId)?.let { claim ->
                        if (claim.isActive()) {
                            claims.add(claim)
                        }
                    }
                }

                activeClaims = claims
                println("Loaded ${claims.size} active claims")
            } catch (e: Exception) {
                errorMessage = "Failed to load claims: ${e.message}"
                println("Error loading claims: ${e.message}")
            }
        }
    }

    /**
     * Load active claims
     */
    fun loadActiveClaims() {
        loadActiveClaimsFromEvents()
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
     * Submit primary ID verification request
     */
    fun requestPrimaryID(justification: String, stakeEth: Double) {
        val consensusAddress = web3Service?.consensusAddress
        if (consensusAddress.isNullOrEmpty()) {
            errorMessage = "Consensus contract not configured"
            return
        }

        viewModelScope.launch {
            try {
                isTransactionPending = true
                transactionStatus = "Submitting primary ID request..."

                val stakeWei = (stakeEth * 1e18).toBigDecimal().toBigInteger()
                val result = transactionService?.requestPrimaryVerification(
                    consensusAddress,
                    justification,
                    stakeWei
                )

                when (result) {
                    is TransactionResult.Success -> {
                        transactionStatus = "Success! TX: ${result.txHash.take(10)}..."
                        loadIdentityData()
                        loadActiveClaims()
                    }
                    is TransactionResult.Error -> {
                        errorMessage = result.message
                        transactionStatus = null
                    }
                    else -> {
                        transactionStatus = "Transaction pending..."
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Transaction failed: ${e.message}"
                transactionStatus = null
            } finally {
                isTransactionPending = false
            }
        }
    }

    /**
     * Link secondary account to primary
     */
    fun linkSecondaryAccount(
        primaryAddress: String,
        platform: String,
        justification: String,
        stakeEth: Double
    ) {
        val consensusAddress = web3Service?.consensusAddress
        if (consensusAddress.isNullOrEmpty()) {
            errorMessage = "Consensus contract not configured"
            return
        }

        viewModelScope.launch {
            try {
                isTransactionPending = true
                transactionStatus = "Linking secondary account..."

                val stakeWei = (stakeEth * 1e18).toBigDecimal().toBigInteger()
                val result = transactionService?.requestLinkToPrimary(
                    consensusAddress,
                    primaryAddress,
                    platform,
                    justification,
                    stakeWei
                )

                when (result) {
                    is TransactionResult.Success -> {
                        transactionStatus = "Success! TX: ${result.txHash.take(10)}..."
                        loadIdentityData()
                        loadActiveClaims()
                    }
                    is TransactionResult.Error -> {
                        errorMessage = result.message
                        transactionStatus = null
                    }
                    else -> {
                        transactionStatus = "Transaction pending..."
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Transaction failed: ${e.message}"
                transactionStatus = null
            } finally {
                isTransactionPending = false
            }
        }
    }

    /**
     * Vote in support of a claim
     */
    fun vouchFor(claimId: BigInteger, stakeEth: Double) {
        val consensusAddress = web3Service?.consensusAddress
        if (consensusAddress.isNullOrEmpty()) {
            errorMessage = "Consensus contract not configured"
            return
        }

        viewModelScope.launch {
            try {
                isTransactionPending = true
                transactionStatus = "Submitting vote..."

                val stakeWei = (stakeEth * 1e18).toBigDecimal().toBigInteger()
                val result = transactionService?.vouchFor(consensusAddress, claimId, stakeWei)

                when (result) {
                    is TransactionResult.Success -> {
                        transactionStatus = "Vote submitted! TX: ${result.txHash.take(10)}..."
                        loadActiveClaims()
                    }
                    is TransactionResult.Error -> {
                        errorMessage = result.message
                        transactionStatus = null
                    }
                    else -> {
                        transactionStatus = "Transaction pending..."
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Vote failed: ${e.message}"
                transactionStatus = null
            } finally {
                isTransactionPending = false
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
