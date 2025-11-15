# Integration Documentation

## Table of Contents

1. [Overview](#overview)
2. [Web3j Integration](#web3j-integration)
3. [Common Query Patterns](#common-query-patterns)
4. [Transaction Lifecycle](#transaction-lifecycle)
5. [Concurrency and Nonce Management](#concurrency-and-nonce-management)
6. [Error Handling](#error-handling)
7. [Real-Time Event Monitoring](#real-time-event-monitoring)
8. [Testing Integration](#testing-integration)

---

## Overview

### Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                 User Interface Layer                     │
│            (Compose Desktop / Web UI)                    │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                  ViewModel Layer                         │
│               IdentityViewModel.kt                       │
│  • State management                                      │
│  • Business logic                                        │
│  • Error handling                                        │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│              Repository/Service Layer                    │
│  ┌──────────────────┐  ┌──────────────────┐            │
│  │ ContractRepository│  │ TransactionService│            │
│  │  (Read ops)       │  │  (Write ops)      │            │
│  └──────────┬────────┘  └────────┬──────────┘            │
│             │                    │                       │
│  ┌──────────▼────────────────────▼──────────┐            │
│  │         EventListener.kt                 │            │
│  │       (Event monitoring)                 │            │
│  └──────────────────┬───────────────────────┘            │
└────────────────────────┼────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                  Web3 Client Layer                       │
│                   Web3Service.kt                         │
│  • Connection management                                 │
│  • ABI encoding/decoding                                 │
│  • Transaction signing                                   │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                 Web3j Library (4.10.3)                   │
│  • HTTP/WebSocket RPC                                    │
│  • Transaction serialization                             │
│  • Cryptography (signing)                                │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│             Ethereum RPC Node                            │
│  • Anvil (local)                                         │
│  • Infura/Alchemy (testnet/mainnet)                     │
│  • Self-hosted Geth/Erigon                              │
└─────────────────────────────────────────────────────────┘
```

---

## Web3j Integration

### Connection Setup

**File**: `desktop-client/src/main/kotlin/com/knomee/identity/blockchain/Web3Service.kt`

```kotlin
class Web3Service(
    private val rpcUrl: String = "http://127.0.0.1:8545",
    private val privateKey: String? = null
) {
    // Initialize Web3j client
    private val web3j: Web3j = Web3j.build(HttpService(rpcUrl))

    // Credentials for signing transactions
    private val credentials: Credentials? = privateKey?.let { Credentials.create(it) }

    // Contract addresses (set after deployment)
    var governanceAddress: String = ""
    var registryAddress: String = ""
    var consensusAddress: String = ""
    var identityTokenAddress: String = ""
    var knomeeTokenAddress: String = ""

    // Gas price strategy
    private val gasProvider = DefaultGasProvider() // Can be customized

    // Check connection
    suspend fun isConnected(): Boolean = withContext(Dispatchers.IO) {
        try {
            web3j.web3ClientVersion().send().web3ClientVersion != null
        } catch (e: Exception) {
            false
        }
    }

    // Get current block number
    suspend fun getCurrentBlockNumber(): BigInteger = withContext(Dispatchers.IO) {
        web3j.ethBlockNumber().send().blockNumber
    }

    // Get account balance
    suspend fun getBalance(address: String): BigInteger = withContext(Dispatchers.IO) {
        web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().balance
    }
}
```

**Usage**:
```kotlin
// Local development
val web3 = Web3Service(
    rpcUrl = "http://127.0.0.1:8545",
    privateKey = "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"
)

// Sepolia testnet
val web3 = Web3Service(
    rpcUrl = "https://sepolia.infura.io/v3/YOUR_INFURA_KEY",
    privateKey = System.getenv("PRIVATE_KEY")
)

// Set contract addresses
web3.registryAddress = "0xCf7Ed3AccA5a467e9e704C703E8D87F634fB0Fc9"
web3.consensusAddress = "0xDc64a140Aa3E981100a9becA4E685f962f0cF6C9"
```

---

### ABI Encoding/Decoding

#### Encoding Function Calls

```kotlin
// Encode a function call
fun encodeFunctionCall(functionName: String, vararg params: Any): String {
    val function = Function(
        functionName,
        params.map { typeFromValue(it) }.toList(),
        emptyList() // No outputs for encoding
    )
    return FunctionEncoder.encode(function)
}

// Helper to convert Kotlin types to Solidity types
fun typeFromValue(value: Any): Type<*> = when (value) {
    is String -> if (value.startsWith("0x")) Address(value) else Utf8String(value)
    is BigInteger -> Uint256(value)
    is Boolean -> Bool(value)
    is Int -> Uint256(BigInteger.valueOf(value.toLong()))
    else -> throw IllegalArgumentException("Unsupported type: ${value::class}")
}
```

**Example**:
```kotlin
// Encode getTier(address) call
val data = encodeFunctionCall("getTier", "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb1")
// Returns: "0x47064d6a000000000000000000000000742d35cc6634c0532925a3b844bc9e7595f0beb1"
```

---

#### Decoding Function Results

```kotlin
// Decode a function result
fun <T> decodeFunctionResult(
    data: String,
    outputType: TypeReference<T>
): T {
    val function = Function(
        "dummy",
        emptyList(),
        listOf(outputType)
    )

    val results = FunctionReturnDecoder.decode(data, function.outputParameters)
    @Suppress("UNCHECKED_CAST")
    return results[0].value as T
}
```

**Example**:
```kotlin
// Decode getTier result
val result = web3j.ethCall(
    Transaction.createEthCallTransaction(
        null,
        registryAddress,
        encodeFunctionCall("getTier", userAddress)
    ),
    DefaultBlockParameterName.LATEST
).send().value

val tier = decodeFunctionResult(result, object : TypeReference<Uint8>() {})
// Returns: Uint8(2) → IdentityTier.PRIMARYID
```

---

### Contract Abstraction

**File**: `ContractRepository.kt`

```kotlin
class ContractRepository(private val web3Service: Web3Service) {

    // Read identity from IdentityRegistry
    suspend fun getIdentity(address: String): IdentityData? = withContext(Dispatchers.IO) {
        try {
            val function = Function(
                "identities",
                listOf(Address(address)),
                listOf(
                    object : TypeReference<DynamicStruct>() {},
                )
            )

            val response = web3Service.web3j.ethCall(
                Transaction.createEthCallTransaction(
                    null,
                    web3Service.registryAddress,
                    FunctionEncoder.encode(function)
                ),
                DefaultBlockParameterName.LATEST
            ).send()

            if (response.hasError()) {
                println("Error: ${response.error.message}")
                return@withContext null
            }

            val results = FunctionReturnDecoder.decode(
                response.value,
                function.outputParameters
            )

            val struct = results[0].value as DynamicStruct
            // Parse struct fields...
            IdentityData(
                address = address,
                tier = parseTier(struct[0].value as BigInteger),
                primaryAddress = (struct[1].value as Address).value,
                verifiedAt = (struct[2].value as Uint256).value,
                // ...
            )
        } catch (e: Exception) {
            println("Exception getting identity: ${e.message}")
            null
        }
    }

    // Get tier (simpler, single return value)
    suspend fun getTier(address: String): IdentityTier? = withContext(Dispatchers.IO) {
        try {
            val function = Function(
                "getTier",
                listOf(Address(address)),
                listOf(object : TypeReference<Uint8>() {})
            )

            val response = web3Service.web3j.ethCall(
                Transaction.createEthCallTransaction(null, web3Service.registryAddress, FunctionEncoder.encode(function)),
                DefaultBlockParameterName.LATEST
            ).send()

            val results = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            val tierValue = (results[0].value as Uint8).value.toInt()

            IdentityTier.fromValue(tierValue)
        } catch (e: Exception) {
            null
        }
    }
}
```

---

## Common Query Patterns

### Pattern 1: Simple Read

**Use Case**: Get current state of a single value

```kotlin
suspend fun getVotingWeight(address: String): BigInteger {
    val function = Function(
        "getVotingWeight",
        listOf(Address(address)),
        listOf(object : TypeReference<Uint256>() {})
    )

    val response = web3j.ethCall(
        Transaction.createEthCallTransaction(
            null,
            registryAddress,
            FunctionEncoder.encode(function)
        ),
        DefaultBlockParameterName.LATEST
    ).send()

    val results = FunctionReturnDecoder.decode(response.value, function.outputParameters)
    return (results[0].value as Uint256).value
}
```

---

### Pattern 2: Struct Read

**Use Case**: Get complex object with multiple fields

```kotlin
data class ClaimData(
    val claimId: BigInteger,
    val claimType: ClaimType,
    val status: ClaimStatus,
    val subject: String,
    val relatedAddress: String,
    val platform: String,
    val justification: String,
    val createdAt: BigInteger,
    val expiresAt: BigInteger,
    val totalWeightFor: BigInteger,
    val totalWeightAgainst: BigInteger,
    val totalStake: BigInteger,
    val resolved: Boolean
)

suspend fun getClaim(claimId: BigInteger): ClaimData? {
    val function = Function(
        "claims",
        listOf(Uint256(claimId)),
        listOf(object : TypeReference<DynamicStruct>() {})
    )

    val response = web3j.ethCall(
        Transaction.createEthCallTransaction(null, consensusAddress, FunctionEncoder.encode(function)),
        DefaultBlockParameterName.LATEST
    ).send()

    if (response.hasError()) return null

    val results = FunctionReturnDecoder.decode(response.value, function.outputParameters)
    val struct = results[0].value as DynamicStruct

    return ClaimData(
        claimId = (struct[0].value as Uint256).value,
        claimType = ClaimType.fromValue((struct[1].value as Uint8).value.toInt()),
        status = ClaimStatus.fromValue((struct[2].value as Uint8).value.toInt()),
        subject = (struct[3].value as Address).value,
        relatedAddress = (struct[4].value as Address).value,
        platform = (struct[5].value as Utf8String).value,
        justification = (struct[6].value as Utf8String).value,
        createdAt = (struct[7].value as Uint256).value,
        expiresAt = (struct[8].value as Uint256).value,
        totalWeightFor = (struct[9].value as Uint256).value,
        totalWeightAgainst = (struct[10].value as Uint256).value,
        totalStake = (struct[11].value as Uint256).value,
        resolved = (struct[12].value as Bool).value
    )
}
```

---

### Pattern 3: Array Read

**Use Case**: Get list of items

```kotlin
suspend fun getLinkedAccounts(primary: String): List<LinkedPlatform> {
    val function = Function(
        "linkedPlatforms",
        listOf(Address(primary)),
        listOf(object : TypeReference<DynamicArray<DynamicStruct>>() {})
    )

    val response = web3j.ethCall(
        Transaction.createEthCallTransaction(null, registryAddress, FunctionEncoder.encode(function)),
        DefaultBlockParameterName.LATEST
    ).send()

    if (response.hasError()) return emptyList()

    val results = FunctionReturnDecoder.decode(response.value, function.outputParameters)
    val array = results[0].value as DynamicArray<DynamicStruct>

    return array.value.map { struct ->
        LinkedPlatform(
            linkedAddress = (struct.value[0] as Address).value,
            platform = (struct.value[1] as Utf8String).value,
            justification = (struct.value[2] as Utf8String).value,
            linkedAt = (struct.value[3] as Uint256).value
        )
    }
}
```

---

### Pattern 4: Multi-Contract Read

**Use Case**: Fetch data from multiple contracts

```kotlin
suspend fun getFullUserProfile(address: String): UserProfile = coroutineScope {
    // Launch parallel reads
    val identityDeferred = async { getIdentity(address) }
    val tokenIdDeferred = async { getIdentityTokenId(address) }
    val knowBalanceDeferred = async { getKnomeeTokenBalance(address) }
    val linkedAccountsDeferred = async { getLinkedAccounts(address) }

    // Await all results
    UserProfile(
        address = address,
        identity = identityDeferred.await(),
        tokenId = tokenIdDeferred.await(),
        knowBalance = knowBalanceDeferred.await(),
        linkedAccounts = linkedAccountsDeferred.await()
    )
}
```

---

## Transaction Lifecycle

### Phase 1: Construction

```kotlin
suspend fun requestPrimaryVerification(
    consensusAddress: String,
    justification: String,
    stakeAmount: BigInteger
): TransactionResult {
    // 1. Validate inputs
    require(justification.isNotBlank()) { "Justification required" }
    require(justification.length <= 1000) { "Justification too long" }
    require(stakeAmount >= minStake) { "Insufficient stake" }

    // 2. Encode function call
    val function = Function(
        "submitPrimaryClaim",
        listOf(Utf8String(justification)),
        emptyList()
    )
    val encodedFunction = FunctionEncoder.encode(function)

    // 3. Get current nonce
    val nonce = web3j.ethGetTransactionCount(
        credentials.address,
        DefaultBlockParameterName.PENDING // Important: use PENDING
    ).send().transactionCount

    // 4. Estimate gas
    val gasEstimate = web3j.ethEstimateGas(
        Transaction.createFunctionCallTransaction(
            credentials.address,
            null, // nonce (null for estimation)
            null, // gasPrice
            null, // gasLimit
            consensusAddress,
            stakeAmount, // value (ETH stake)
            encodedFunction
        )
    ).send().amountUsed

    // Add 20% buffer
    val gasLimit = gasEstimate.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100))

    // 5. Get current gas price
    val gasPrice = web3j.ethGasPrice().send().gasPrice

    return TransactionResult(nonce, gasLimit, gasPrice, encodedFunction)
}
```

---

### Phase 2: Signing

```kotlin
// 6. Create raw transaction
val rawTransaction = RawTransaction.createTransaction(
    nonce,
    gasPrice,
    gasLimit,
    consensusAddress,
    stakeAmount, // value
    encodedFunction
)

// 7. Sign transaction
val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
val hexValue = Numeric.toHexString(signedMessage)
```

---

### Phase 3: Broadcasting

```kotlin
// 8. Send transaction
val txHash = web3j.ethSendRawTransaction(hexValue).send().transactionHash

if (txHash == null) {
    return TransactionResult(
        success = false,
        message = "Failed to broadcast transaction"
    )
}

println("Transaction broadcast: $txHash")
```

---

### Phase 4: Confirmation

```kotlin
// 9. Wait for confirmation
suspend fun waitForConfirmation(
    txHash: String,
    confirmations: Int = 1,
    timeoutSeconds: Long = 120
): TransactionReceipt? = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()

    while (true) {
        // Check timeout
        if ((System.currentTimeMillis() - startTime) / 1000 > timeoutSeconds) {
            println("Transaction timeout: $txHash")
            return@withContext null
        }

        // Get receipt
        val receipt = web3j.ethGetTransactionReceipt(txHash).send().transactionReceipt

        if (receipt.isPresent) {
            val r = receipt.get()

            // Check status (1 = success, 0 = reverted)
            if (r.status == "0x0") {
                println("Transaction reverted: $txHash")
                return@withContext null
            }

            // Check confirmations
            val currentBlock = web3j.ethBlockNumber().send().blockNumber
            val txBlock = r.blockNumber
            val confirmedBlocks = currentBlock - txBlock

            if (confirmedBlocks >= BigInteger.valueOf(confirmations.toLong())) {
                println("Transaction confirmed: $txHash (${confirmedBlocks} blocks)")
                return@withContext r
            }
        }

        // Wait before retry
        delay(2000) // Poll every 2 seconds
    }
}
```

**Usage**:
```kotlin
val txHash = submitTransaction(...)
val receipt = waitForConfirmation(txHash, confirmations = 2)

if (receipt != null) {
    println("Success! Gas used: ${receipt.gasUsed}")
} else {
    println("Transaction failed or timed out")
}
```

---

### Complete Transaction Flow

```kotlin
suspend fun submitAndWait(
    function: Function,
    contractAddress: String,
    value: BigInteger = BigInteger.ZERO
): TransactionReceipt? {
    val encoded = FunctionEncoder.encode(function)

    // Get nonce
    val nonce = web3j.ethGetTransactionCount(
        credentials.address,
        DefaultBlockParameterName.PENDING
    ).send().transactionCount

    // Estimate gas
    val gasEstimate = web3j.ethEstimateGas(
        Transaction.createFunctionCallTransaction(
            credentials.address, null, null, null,
            contractAddress, value, encoded
        )
    ).send().amountUsed

    val gasLimit = gasEstimate.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100))
    val gasPrice = web3j.ethGasPrice().send().gasPrice

    // Sign and send
    val rawTx = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, contractAddress, value, encoded)
    val signedTx = TransactionEncoder.signMessage(rawTx, credentials)
    val txHash = web3j.ethSendRawTransaction(Numeric.toHexString(signedTx)).send().transactionHash

    // Wait for confirmation
    return waitForConfirmation(txHash, confirmations = 1)
}
```

---

## Concurrency and Nonce Management

### Problem: Nonce Conflicts

When sending multiple transactions in parallel, nonce conflicts can occur:

```kotlin
// BAD: Nonce conflicts!
launch { submitClaim(...) }    // Gets nonce=5, sends tx
launch { vouchFor(...) }       // Gets nonce=5, CONFLICT!
```

**Error**: `replacement transaction underpriced` or `nonce too low`

---

### Solution 1: Sequential Transactions

```kotlin
// GOOD: Sequential (but slow)
submitClaim(...)      // nonce=5, wait for confirmation
vouchFor(...)         // nonce=6, wait for confirmation
claimRewards(...)     // nonce=7
```

---

### Solution 2: Manual Nonce Management

```kotlin
class NonceManager(private val web3j: Web3j, private val address: String) {
    private var currentNonce: BigInteger? = null
    private val lock = Mutex()

    suspend fun getNextNonce(): BigInteger = lock.withLock {
        if (currentNonce == null) {
            currentNonce = web3j.ethGetTransactionCount(
                address,
                DefaultBlockParameterName.PENDING
            ).send().transactionCount
        }

        val nonce = currentNonce!!
        currentNonce = nonce + BigInteger.ONE
        return nonce
    }

    suspend fun reset() = lock.withLock {
        currentNonce = null
    }
}
```

**Usage**:
```kotlin
val nonceManager = NonceManager(web3j, credentials.address)

// Send multiple transactions with correct nonces
launch { sendTx(nonceManager.getNextNonce(), ...) } // nonce=5
launch { sendTx(nonceManager.getNextNonce(), ...) } // nonce=6
launch { sendTx(nonceManager.getNextNonce(), ...) } // nonce=7
```

---

### Solution 3: Transaction Queue

```kotlin
class TransactionQueue(private val web3Service: Web3Service) {
    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            for (tx in queue) {
                try {
                    tx.invoke()
                } catch (e: Exception) {
                    println("Transaction failed: ${e.message}")
                }
            }
        }
    }

    suspend fun enqueue(tx: suspend () -> Unit) {
        queue.send(tx)
    }
}
```

**Usage**:
```kotlin
val txQueue = TransactionQueue(web3Service)

// Queue transactions (processed sequentially)
txQueue.enqueue { submitClaim(...) }
txQueue.enqueue { vouchFor(...) }
txQueue.enqueue { claimRewards(...) }
```

---

## Error Handling

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `nonce too low` | Nonce already used | Use PENDING nonce |
| `insufficient funds` | Not enough ETH for gas | Check balance first |
| `gas required exceeds allowance` | Gas estimate too low | Increase gas limit |
| `execution reverted` | Smart contract `require()` failed | Check error message |
| `replacement transaction underpriced` | Nonce conflict | Sequential or queue |
| `timeout` | RPC node slow/down | Retry with backoff |

---

### Error Handling Pattern

```kotlin
suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> T
): T? {
    var currentDelay = initialDelayMs

    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            when {
                attempt == maxRetries - 1 -> {
                    println("Max retries reached: ${e.message}")
                    return null
                }
                e.message?.contains("timeout") == true ||
                e.message?.contains("connection") == true -> {
                    println("Retry ${attempt + 1}/$maxRetries after ${currentDelay}ms: ${e.message}")
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong()
                }
                else -> throw e // Don't retry non-transient errors
            }
        }
    }

    return null
}
```

**Usage**:
```kotlin
val identity = retryWithBackoff {
    repository.getIdentity(address)
}
```

---

### Parsing Revert Reasons

```kotlin
fun parseRevertReason(error: String): String {
    // Revert messages are encoded as:
    // 0x08c379a0 (Error signature)
    // + 32 bytes (offset)
    // + 32 bytes (length)
    // + UTF-8 string

    if (error.startsWith("0x08c379a0")) {
        val reason = error.substring(138) // Skip signature + offset + length
        return String(Numeric.hexStringToByteArray(reason)).trimEnd('\u0000')
    }

    return error
}
```

---

## Real-Time Event Monitoring

### WebSocket Subscription

```kotlin
// Use WebSocket for real-time events
val web3j = Web3j.build(WebSocketService("wss://sepolia.infura.io/ws/v3/YOUR_KEY", false))

// Subscribe to new blocks
web3j.blockFlowable(false).subscribe { block ->
    println("New block: ${block.number}")
}

// Subscribe to contract events
val filter = EthFilter(
    DefaultBlockParameterName.LATEST,
    DefaultBlockParameterName.LATEST,
    consensusAddress
)

web3j.ethLogFlowable(filter).subscribe { log ->
    when (log.topics[0]) {
        EventEncoder.encode(CLAIM_CREATED_EVENT) -> {
            val event = decodeClaimCreatedEvent(log)
            println("New claim: ${event.claimId}")
        }
        EventEncoder.encode(CONSENSUS_REACHED_EVENT) -> {
            val event = decodeConsensusReachedEvent(log)
            println("Claim resolved: ${event.claimId}")
        }
    }
}
```

---

### Polling for Events

```kotlin
suspend fun pollForNewClaims() {
    var lastBlock = web3j.ethBlockNumber().send().blockNumber

    while (true) {
        val currentBlock = web3j.ethBlockNumber().send().blockNumber

        if (currentBlock > lastBlock) {
            val filter = EthFilter(
                DefaultBlockParameter.valueOf(lastBlock + BigInteger.ONE),
                DefaultBlockParameter.valueOf(currentBlock),
                consensusAddress
            ).addSingleTopic(EventEncoder.encode(CLAIM_CREATED_EVENT))

            val logs = web3j.ethGetLogs(filter).send().logs

            logs.forEach { log ->
                val event = decodeClaimCreatedEvent(log)
                handleNewClaim(event)
            }

            lastBlock = currentBlock
        }

        delay(5000) // Poll every 5 seconds
    }
}
```

---

## Testing Integration

### Mock Web3 Service

```kotlin
class MockWeb3Service : Web3Service("http://mock", null) {
    private val mockIdentities = mutableMapOf<String, IdentityData>()

    override suspend fun getIdentity(address: String): IdentityData? {
        return mockIdentities[address]
    }

    fun setMockIdentity(address: String, identity: IdentityData) {
        mockIdentities[address] = identity
    }
}
```

**Usage**:
```kotlin
@Test
fun testGetIdentity() = runBlocking {
    val mockWeb3 = MockWeb3Service()
    mockWeb3.setMockIdentity("0x123", IdentityData(...))

    val repository = ContractRepository(mockWeb3)
    val identity = repository.getIdentity("0x123")

    assertEquals(IdentityTier.PRIMARYID, identity?.tier)
}
```

---

### Integration Tests with Anvil

```kotlin
@Test
fun testRealContractInteraction() = runBlocking {
    // Start Anvil in test setup
    val process = Runtime.getRuntime().exec("anvil")

    // Deploy contracts
    val deployer = DeployScript()
    deployer.run()

    // Test real contract interaction
    val web3 = Web3Service("http://127.0.0.1:8545", ANVIL_PRIVATE_KEY)
    val repository = ContractRepository(web3)

    val identity = repository.getIdentity(ANVIL_ADDRESS_0)
    assertNotNull(identity)

    // Cleanup
    process.destroy()
}
```

---

## Summary

This integration documentation provides comprehensive patterns for:
- Web3j client setup and configuration
- ABI encoding/decoding for contract interaction
- Common query patterns for reads and writes
- Complete transaction lifecycle management
- Concurrency handling and nonce management
- Error handling with retry logic
- Real-time event monitoring
- Testing strategies

Use these patterns as a foundation for building robust blockchain database integrations.
