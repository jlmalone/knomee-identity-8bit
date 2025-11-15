# Database Query Reference

## Table of Contents

1. [Query Overview](#query-overview)
2. [Identity Registry Queries](#identity-registry-queries)
3. [Identity Consensus Queries](#identity-consensus-queries)
4. [Identity Token Queries](#identity-token-queries)
5. [Knomee Token Queries](#knomee-token-queries)
6. [Governance Parameters Queries](#governance-parameters-queries)
7. [Event Queries](#event-queries)
8. [Complex Multi-Contract Queries](#complex-multi-contract-queries)
9. [Transaction Examples](#transaction-examples)
10. [Performance Optimization](#performance-optimization)

---

## Query Overview

### Query Types

In blockchain databases, there are three primary query types:

| Type | Cost | Speed | Mutability | Use Case |
|------|------|-------|------------|----------|
| **View Functions** | Free | ~100-500ms | Read-only | Fetch current state |
| **Transactions** | Gas fees ($1-50) | ~12-15 sec | State-changing | Modify data |
| **Event Queries** | Free | ~100ms-5s | Read-only | Historical data |

### Connection Setup

```kotlin
// Initialize Web3 connection
val web3Service = Web3Service(
    rpcUrl = "http://127.0.0.1:8545",
    privateKey = "0x..."
)

// Set contract addresses
web3Service.governanceAddress = "0x..."
web3Service.registryAddress = "0x..."
web3Service.consensusAddress = "0x..."

// Create repository for queries
val repository = ContractRepository(web3Service)
val txService = TransactionService(web3Service)
```

---

## Identity Registry Queries

### 1. Get Identity by Address

**Solidity**:
```solidity
function identities(address addr) public view returns (Identity memory);
```

**Kotlin**:
```kotlin
val identity = repository.getIdentity("0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb1")

// Returns:
// IdentityData(
//   address = "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb1",
//   tier = IdentityTier.PRIMARYID,
//   primaryAccount = "0x0000000000000000000000000000000000000000",
//   verifiedAt = 1699564800,
//   vouchesReceived = 15,
//   stakeReceived = 1500000000000000000, // 1.5 ETH
//   underChallenge = false,
//   challengeId = 0
// )
```

**Direct Web3j**:
```kotlin
val function = Function(
    "identities",
    listOf(Address(userAddress)),
    listOf(object : TypeReference<DynamicStruct>() {})
)

val response = web3j.ethCall(
    Transaction.createEthCallTransaction(null, registryAddress, FunctionEncoder.encode(function)),
    DefaultBlockParameterName.LATEST
).send()

val results = FunctionReturnDecoder.decode(response.value, function.outputParameters)
```

### 2. Get Identity Tier

**Solidity**:
```solidity
function getTier(address addr) public view returns (IdentityTier);
```

**Kotlin**:
```kotlin
val tier = repository.getTier("0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb1")
// Returns: IdentityTier.PRIMARYID
```

### 3. Check if Address is Verified

**Solidity**:
```solidity
function isVerified(address addr) public view returns (bool);
```

**Kotlin**:
```kotlin
val isVerified = repository.isVerified("0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb1")
// Returns: true
```

### 4. Check if Address is Primary

**Solidity**:
```solidity
function isPrimary(address addr) public view returns (bool);
```

**Kotlin**:
```kotlin
val isPrimary = repository.isPrimary("0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb1")
// Returns: true
```

### 5. Check if Address is Oracle

**Solidity**:
```solidity
function isOracle(address addr) public view returns (bool);
```

**Kotlin**:
```kotlin
val isOracle = repository.isOracle("0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb1")
// Returns: false
```

### 6. Get Voting Weight

**Solidity**:
```solidity
function getVotingWeight(address addr) public view returns (uint256);
```

**Kotlin**:
```kotlin
val weight = repository.getVotingWeight("0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb1")
// Returns: 1 (Primary), 100 (Oracle), or 0 (GreyGhost/LinkedID)
```

### 7. Get Primary Address (for LinkedIDs)

**Solidity**:
```solidity
function getPrimaryAddress(address addr) public view returns (address);
```

**Kotlin**:
```kotlin
val primary = repository.getPrimaryAddress("0x123...") // LinkedID address
// Returns: "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb1" (parent Primary)
```

### 8. Get Linked Accounts for a Primary

**Solidity**:
```solidity
function linkedPlatforms(address primary) public view returns (LinkedPlatform[] memory);
```

**Kotlin**:
```kotlin
val linkedAccounts = repository.getLinkedAccounts("0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb1")

// Returns:
// [
//   LinkedPlatform(linkedAddress="0xAAA...", platform="LinkedIn", justification="...", linkedAt=1699564800),
//   LinkedPlatform(linkedAddress="0xBBB...", platform="Instagram", justification="...", linkedAt=1699651200)
// ]
```

### 9. Check Specific Platform Link

**Solidity**:
```solidity
function linkedIds(address primary, string platform) public view returns (address);
```

**Kotlin**:
```kotlin
val linkedInAddress = repository.getLinkedPlatform("0x742d35Cc...", "LinkedIn")
// Returns: "0xAAA..." or "0x0000..." if not linked
```

### 10. Reverse Lookup: LinkedID → Primary

**Solidity**:
```solidity
function linkedToPrimary(address linked) public view returns (address);
```

**Kotlin**:
```kotlin
val primary = repository.linkedToPrimary("0xAAA...") // LinkedID
// Returns: "0x742d35Cc..." (parent Primary)
```

### 11. Check if Under Challenge

**Solidity**:
```solidity
function isUnderChallenge(address addr) public view returns (bool);
```

**Kotlin**:
```kotlin
val underChallenge = repository.isUnderChallenge("0x742d35Cc...")
// Returns: false
```

### 12. Get Challenge ID

**Kotlin**:
```kotlin
val identity = repository.getIdentity("0x742d35Cc...")
val challengeId = identity.challengeId
// Returns: 0 (no challenge) or claim ID if challenged
```

---

## Identity Consensus Queries

### 13. Get Claim by ID

**Solidity**:
```solidity
function claims(uint256 claimId) public view returns (IdentityClaim memory);
```

**Kotlin**:
```kotlin
val claim = repository.getClaim(BigInteger.valueOf(42))

// Returns:
// ClaimData(
//   claimId = 42,
//   claimType = ClaimType.NEW_PRIMARY,
//   status = ClaimStatus.ACTIVE,
//   subject = "0x123...",
//   relatedAddress = "0x0000...",
//   platform = "",
//   justification = "I am a unique human because...",
//   createdAt = 1699564800,
//   expiresAt = 1702156800,
//   totalWeightFor = 150,
//   totalWeightAgainst = 50,
//   totalStake = 5000000000000000000, // 5 ETH
//   resolved = false
// )
```

### 14. Get Claim Status

**Solidity**:
```solidity
function getClaimStatus(uint256 claimId) public view returns (ClaimStatus);
```

**Kotlin**:
```kotlin
val status = repository.getClaimStatus(BigInteger.valueOf(42))
// Returns: ClaimStatus.ACTIVE, APPROVED, REJECTED, or EXPIRED
```

### 15. Get All Vouches on a Claim

**Solidity**:
```solidity
function vouchesOnClaim(uint256 claimId) public view returns (Vouch[] memory);
```

**Kotlin**:
```kotlin
val vouches = repository.getVouches(BigInteger.valueOf(42))

// Returns:
// [
//   Vouch(voucher="0xAAA...", isSupporting=true, weight=1, stake=0.01 ETH, vouchedAt=1699564900),
//   Vouch(voucher="0xBBB...", isSupporting=true, weight=100, stake=0.05 ETH, vouchedAt=1699565000),
//   Vouch(voucher="0xCCC...", isSupporting=false, weight=1, stake=0.01 ETH, vouchedAt=1699565100)
// ]
```

### 16. Check if User Vouched

**Solidity**:
```solidity
function hasVouched(uint256 claimId, address user) public view returns (bool);
```

**Kotlin**:
```kotlin
val hasVouched = repository.hasUserVouched(BigInteger.valueOf(42), "0xAAA...")
// Returns: true
```

### 17. Get Claims by Address

**Solidity**:
```solidity
function claimsByAddress(address user) public view returns (uint256[] memory);
```

**Kotlin**:
```kotlin
val claimIds = repository.getClaimsByAddress("0x123...")
// Returns: [1, 5, 12, 42] (claim IDs)
```

### 18. Calculate Current Consensus Result

**Solidity**:
```solidity
function calculateCurrentResult(uint256 claimId) public view returns (bool wouldPass);
```

**Kotlin**:
```kotlin
val wouldPass = repository.calculateCurrentResult(BigInteger.valueOf(42))
// Returns: true (claim would be approved) or false (would be rejected)
```

### 19. Check if Claim is Resolved

**Solidity**:
```solidity
function isClaimResolved(uint256 claimId) public view returns (bool);
```

**Kotlin**:
```kotlin
val isResolved = repository.isClaimResolved(BigInteger.valueOf(42))
// Returns: false (still voting) or true (finalized)
```

### 20. Check if User Can Submit Claim

**Solidity**:
```solidity
function canUserClaim(address user) public view returns (bool, string memory reason);
```

**Kotlin**:
```kotlin
val (canClaim, reason) = repository.canUserClaim("0x123...")
// Returns: (false, "Cooldown period not expired") or (true, "")
```

### 21. Get Last Failed Claim Timestamp

**Solidity**:
```solidity
function lastFailedClaim(address user) public view returns (uint256);
```

**Kotlin**:
```kotlin
val lastFailed = repository.getLastFailedClaim("0x123...")
// Returns: 1699564800 (Unix timestamp)
```

### 22. Get Next Claim ID

**Solidity**:
```solidity
function nextClaimId() public view returns (uint256);
```

**Kotlin**:
```kotlin
val nextId = repository.getNextClaimId()
// Returns: 43 (next claim will be ID 43)
```

### 23. Get Active Claims

**Kotlin** (via events):
```kotlin
val activeClaims = repository.getActiveClaims()
// Returns: List of ClaimData for all Active status claims
```

### 24. Get Claims Expiring Soon

**Kotlin**:
```kotlin
val expiringClaims = repository.getClaimsExpiringSoon(days = 3)
// Returns: List of claims expiring in next 3 days
```

### 25. Get User's Active Claim

**Kotlin**:
```kotlin
val activeClaim = repository.getUserActiveClaim("0x123...")
// Returns: ClaimData or null if no active claim
```

---

## Identity Token Queries

### 26. Check if Address Has Token

**Solidity**:
```solidity
function accountToTokenId(address account) public view returns (uint256);
```

**Kotlin**:
```kotlin
val tokenId = repository.getIdentityTokenId("0x742d35Cc...")
// Returns: 42 (token ID) or 0 (no token)

val hasToken = repository.hasIdentityToken("0x742d35Cc...")
// Returns: true
```

### 27. Get Token Owner

**Solidity**:
```solidity
function tokenIdToAccount(uint256 tokenId) public view returns (address);
```

**Kotlin**:
```kotlin
val owner = repository.getTokenOwner(BigInteger.valueOf(42))
// Returns: "0x742d35Cc..."
```

### 28. Get Tier from Token

**Solidity**:
```solidity
function identityTiers(address account) public view returns (IdentityTier);
```

**Kotlin**:
```kotlin
val tier = repository.getTierFromToken("0x742d35Cc...")
// Returns: IdentityTier.PRIMARYID
```

### 29. Get Token Balance

**Solidity**:
```solidity
function balanceOf(address owner) public view returns (uint256);
```

**Kotlin**:
```kotlin
val balance = repository.getIdentityTokenBalance("0x742d35Cc...")
// Returns: 1 (always 0 or 1 due to soul-bound constraint)
```

### 30. Get Token URI (Metadata)

**Solidity**:
```solidity
function tokenURI(uint256 tokenId) public view returns (string memory);
```

**Kotlin**:
```kotlin
val metadata = repository.getTokenURI(BigInteger.valueOf(42))
// Returns: base64-encoded JSON with SVG image
```

---

## Knomee Token Queries

### 31. Get KNOW Token Balance

**Solidity**:
```solidity
function balanceOf(address account) public view returns (uint256);
```

**Kotlin**:
```kotlin
val balance = repository.getKnomeeTokenBalance("0x742d35Cc...")
// Returns: 100000000000000000000 (100 KNOW in wei)
```

### 32. Get KNOW Token Allowance

**Solidity**:
```solidity
function allowance(address owner, address spender) public view returns (uint256);
```

**Kotlin**:
```kotlin
val allowance = repository.getKnomeeAllowance(
    owner = "0x742d35Cc...",
    spender = consensusAddress
)
// Returns: 1000000000000000000000 (1000 KNOW approved)
```

### 33. Get Total Supply

**Solidity**:
```solidity
function totalSupply() public view returns (uint256);
```

**Kotlin**:
```kotlin
val totalSupply = repository.getKnomeeTotalSupply()
// Returns: 5000000000000000000000 (5000 KNOW minted)
```

### 34. Check if Claimed Primary Reward

**Solidity**:
```solidity
function hasClaimedPrimaryReward(address account) public view returns (bool);
```

**Kotlin**:
```kotlin
val hasClaimed = repository.hasClaimedPrimaryReward("0x742d35Cc...")
// Returns: true
```

### 35. Get Reward Statistics

**Solidity**:
```solidity
function rewardStats() public view returns (uint256 minted, uint256 slashed, uint256 circulating);
```

**Kotlin**:
```kotlin
val (minted, slashed, circulating) = repository.getRewardStats()
// Returns: (5000 KNOW, 100 KNOW, 4900 KNOW)
```

---

## Governance Parameters Queries

### 36. Get Consensus Threshold

**Solidity**:
```solidity
function getConsensusThreshold(ClaimType claimType) public view returns (uint256);
```

**Kotlin**:
```kotlin
val threshold = repository.getConsensusThreshold(ClaimType.NEW_PRIMARY)
// Returns: 6700 (67% in basis points)
```

### 37. Get Minimum Stake

**Solidity**:
```solidity
function getMinStake(ClaimType claimType) public view returns (uint256);
```

**Kotlin**:
```kotlin
val minStake = repository.getMinStake(ClaimType.NEW_PRIMARY)
// Returns: 30000000000000000 (0.03 ETH in wei)
```

### 38. Get Slash Rate

**Solidity**:
```solidity
function getSlashRate(ClaimType claimType) public view returns (uint256);
```

**Kotlin**:
```kotlin
val slashRate = repository.getSlashRate(ClaimType.NEW_PRIMARY)
// Returns: 3000 (30% in basis points)
```

### 39. Get Voting Weight by Tier

**Solidity**:
```solidity
function getVotingWeight(IdentityTier tier) public view returns (uint256);
```

**Kotlin**:
```kotlin
val weight = repository.getVotingWeightByTier(IdentityTier.ORACLE)
// Returns: 100
```

### 40. Get All Parameters

**Solidity**:
```solidity
function getAllParameters() public view returns (...);
```

**Kotlin**:
```kotlin
val params = repository.getGovernanceParameters()

// Returns:
// GovernanceParams(
//   linkThreshold = 5100,
//   primaryThreshold = 6700,
//   duplicateThreshold = 8000,
//   minStakeWei = 10000000000000000,
//   primaryStakeMultiplier = 3,
//   duplicateStakeMultiplier = 10,
//   linkSlashBps = 1000,
//   primarySlashBps = 3000,
//   duplicateSlashBps = 5000,
//   sybilSlashBps = 10000,
//   primaryVoteWeight = 1,
//   oracleVoteWeight = 100,
//   failedClaimCooldown = 604800, // 7 days in seconds
//   duplicateFlagCooldown = 2592000, // 30 days
//   claimExpiryDuration = 2592000 // 30 days
// )
```

### 41. Get Current Timestamp (with TimeWarp)

**Solidity**:
```solidity
function getCurrentTimestamp() public view returns (uint256);
```

**Kotlin**:
```kotlin
val timestamp = repository.getCurrentTimestamp()
// Returns: block.timestamp + timeWarpSeconds
```

---

## Event Queries

### 42. Get All IdentityVerified Events

**Kotlin**:
```kotlin
val filter = EthFilter(
    DefaultBlockParameterName.EARLIEST,
    DefaultBlockParameterName.LATEST,
    registryAddress
).addSingleTopic(EventEncoder.encode(IDENTITY_VERIFIED_EVENT))

val logs = web3j.ethGetLogs(filter).send().logs

logs.forEach { log ->
    val event = decodeIdentityVerifiedEvent(log)
    println("${event.address} verified as ${event.tier} at ${event.timestamp}")
}
```

### 43. Get IdentityVerified Events for Specific Address

**Kotlin**:
```kotlin
val filter = EthFilter(
    DefaultBlockParameterName.EARLIEST,
    DefaultBlockParameterName.LATEST,
    registryAddress
).addSingleTopic(EventEncoder.encode(IDENTITY_VERIFIED_EVENT))
 .addSingleTopic(TypeEncoder.encode(Address("0x742d35Cc...")))

val logs = web3j.ethGetLogs(filter).send().logs
// Returns: All verification events for this address
```

### 44. Get All ClaimCreated Events

**Kotlin**:
```kotlin
val claims = eventListener.getRecentClaims(fromBlock = BigInteger.ZERO)

// Returns:
// [
//   ClaimCreatedEvent(claimId=1, subject="0x123...", claimType=NEW_PRIMARY, createdAt=...),
//   ClaimCreatedEvent(claimId=2, subject="0x456...", claimType=LINK_TO_PRIMARY, createdAt=...),
//   ...
// ]
```

### 45. Get ClaimCreated Events for Specific User

**Kotlin**:
```kotlin
val filter = EthFilter(
    DefaultBlockParameterName.EARLIEST,
    DefaultBlockParameterName.LATEST,
    consensusAddress
).addSingleTopic(EventEncoder.encode(CLAIM_CREATED_EVENT))
 .addSingleTopic(TypeEncoder.encode(Address("0x123...")))

val logs = web3j.ethGetLogs(filter).send().logs
// Returns: All claims submitted by this user
```

### 46. Get Recent VouchCast Events

**Kotlin**:
```kotlin
val fromBlock = BigInteger.valueOf(currentBlock - 1000) // Last 1000 blocks
val filter = EthFilter(fromBlock, DefaultBlockParameterName.LATEST, consensusAddress)
    .addSingleTopic(EventEncoder.encode(VOUCH_CAST_EVENT))

val logs = web3j.ethGetLogs(filter).send().logs

logs.forEach { log ->
    val event = decodeVouchCastEvent(log)
    println("Claim ${event.claimId}: ${event.voucher} voted ${if (event.isSupporting) "FOR" else "AGAINST"}")
}
```

### 47. Get ConsensusReached Events

**Kotlin**:
```kotlin
val filter = EthFilter(
    DefaultBlockParameterName.EARLIEST,
    DefaultBlockParameterName.LATEST,
    consensusAddress
).addSingleTopic(EventEncoder.encode(CONSENSUS_REACHED_EVENT))

val logs = web3j.ethGetLogs(filter).send().logs

logs.forEach { log ->
    val event = decodeConsensusReachedEvent(log)
    println("Claim ${event.claimId}: ${if (event.approved) "APPROVED" else "REJECTED"}")
}
```

### 48. Monitor Events in Real-Time

**Kotlin**:
```kotlin
// Subscribe to new claim events
web3j.ethLogFlowable(filter).subscribe { log ->
    when (log.topics[0]) {
        EventEncoder.encode(CLAIM_CREATED_EVENT) -> {
            val event = decodeClaimCreatedEvent(log)
            println("New claim ${event.claimId} from ${event.subject}")
        }
        EventEncoder.encode(VOUCH_CAST_EVENT) -> {
            val event = decodeVouchCastEvent(log)
            println("New vote on claim ${event.claimId}")
        }
        EventEncoder.encode(CONSENSUS_REACHED_EVENT) -> {
            val event = decodeConsensusReachedEvent(log)
            println("Claim ${event.claimId} resolved: ${event.approved}")
        }
    }
}
```

---

## Complex Multi-Contract Queries

### 49. Get Full User Profile

**Kotlin**:
```kotlin
data class UserProfile(
    val address: String,
    val identity: IdentityData?,
    val tokenId: BigInteger?,
    val knowBalance: BigInteger,
    val votingWeight: BigInteger,
    val linkedAccounts: List<LinkedPlatform>,
    val activeClaim: ClaimData?,
    val claimHistory: List<BigInteger>
)

suspend fun getFullUserProfile(address: String): UserProfile {
    return UserProfile(
        address = address,
        identity = repository.getIdentity(address),
        tokenId = repository.getIdentityTokenId(address).takeIf { it > BigInteger.ZERO },
        knowBalance = repository.getKnomeeTokenBalance(address),
        votingWeight = repository.getVotingWeight(address),
        linkedAccounts = repository.getLinkedAccounts(address),
        activeClaim = repository.getUserActiveClaim(address),
        claimHistory = repository.getClaimsByAddress(address)
    )
}
```

### 50. Get Claim with Full Details

**Kotlin**:
```kotlin
data class ClaimDetails(
    val claim: ClaimData,
    val vouches: List<Vouch>,
    val subjectProfile: IdentityData?,
    val wouldPass: Boolean,
    val percentageFor: Double,
    val percentageAgainst: Double,
    val timeRemaining: Long // seconds
)

suspend fun getClaimDetails(claimId: BigInteger): ClaimDetails {
    val claim = repository.getClaim(claimId)
    val vouches = repository.getVouches(claimId)
    val subjectProfile = repository.getIdentity(claim.subject)
    val wouldPass = repository.calculateCurrentResult(claimId)

    val totalWeight = claim.totalWeightFor + claim.totalWeightAgainst
    val percentageFor = if (totalWeight > BigInteger.ZERO) {
        (claim.totalWeightFor.toDouble() / totalWeight.toDouble()) * 100.0
    } else 0.0

    val now = System.currentTimeMillis() / 1000
    val timeRemaining = maxOf(0L, claim.expiresAt.toLong() - now)

    return ClaimDetails(
        claim = claim,
        vouches = vouches,
        subjectProfile = subjectProfile,
        wouldPass = wouldPass,
        percentageFor = percentageFor,
        percentageAgainst = 100.0 - percentageFor,
        timeRemaining = timeRemaining
    )
}
```

### 51. Get All Active Claims with Progress

**Kotlin**:
```kotlin
data class ClaimProgress(
    val claimId: BigInteger,
    val subject: String,
    val claimType: ClaimType,
    val percentComplete: Double,
    val votesFor: Int,
    val votesAgainst: Int,
    val wouldPass: Boolean,
    val expiresIn: Long // seconds
)

suspend fun getAllActiveClaimsWithProgress(): List<ClaimProgress> {
    val activeClaims = repository.getActiveClaims()
    val params = repository.getGovernanceParameters()

    return activeClaims.map { claim ->
        val threshold = when (claim.claimType) {
            ClaimType.LINK_TO_PRIMARY -> params.linkThreshold
            ClaimType.NEW_PRIMARY -> params.primaryThreshold
            ClaimType.DUPLICATE_FLAG -> params.duplicateThreshold
        }

        val totalWeight = claim.totalWeightFor + claim.totalWeightAgainst
        val percentComplete = if (totalWeight > BigInteger.ZERO) {
            (claim.totalWeightFor.toDouble() / totalWeight.toDouble()) * 10000.0
        } else 0.0

        val vouches = repository.getVouches(claim.claimId)
        val votesFor = vouches.count { it.isSupporting }
        val votesAgainst = vouches.count { !it.isSupporting }

        val now = System.currentTimeMillis() / 1000
        val expiresIn = maxOf(0L, claim.expiresAt.toLong() - now)

        ClaimProgress(
            claimId = claim.claimId,
            subject = claim.subject,
            claimType = claim.claimType,
            percentComplete = percentComplete,
            votesFor = votesFor,
            votesAgainst = votesAgainst,
            wouldPass = percentComplete >= threshold.toDouble(),
            expiresIn = expiresIn
        )
    }
}
```

### 52. Get User's Voting History

**Kotlin**:
```kotlin
data class VoteRecord(
    val claimId: BigInteger,
    val claimType: ClaimType,
    val votedFor: Boolean,
    val weight: BigInteger,
    val stake: BigInteger,
    val claimApproved: Boolean?,
    val votedCorrectly: Boolean?,
    val rewardClaimed: Boolean
)

suspend fun getUserVotingHistory(userAddress: String): List<VoteRecord> {
    // Get all VouchCast events for this user
    val filter = EthFilter(
        DefaultBlockParameterName.EARLIEST,
        DefaultBlockParameterName.LATEST,
        consensusAddress
    ).addSingleTopic(EventEncoder.encode(VOUCH_CAST_EVENT))
     .addNullTopic() // claimId (not filtering)
     .addSingleTopic(TypeEncoder.encode(Address(userAddress)))

    val logs = web3j.ethGetLogs(filter).send().logs

    return logs.map { log ->
        val event = decodeVouchCastEvent(log)
        val claim = repository.getClaim(event.claimId)

        val claimApproved = if (claim.resolved) {
            claim.status == ClaimStatus.APPROVED
        } else null

        val votedCorrectly = if (claimApproved != null) {
            (event.isSupporting && claimApproved) || (!event.isSupporting && !claimApproved)
        } else null

        val vouches = repository.getVouches(event.claimId)
        val userVouch = vouches.find { it.voucher == userAddress }

        VoteRecord(
            claimId = event.claimId,
            claimType = claim.claimType,
            votedFor = event.isSupporting,
            weight = event.weight,
            stake = event.stake,
            claimApproved = claimApproved,
            votedCorrectly = votedCorrectly,
            rewardClaimed = userVouch?.rewardClaimed ?: false
        )
    }
}
```

### 53. Get Network Statistics

**Kotlin**:
```kotlin
data class NetworkStats(
    val totalIdentities: Int,
    val greyGhosts: Int,
    val linkedIds: Int,
    val primaryIds: Int,
    val oracles: Int,
    val totalClaims: Int,
    val activeClaims: Int,
    val approvedClaims: Int,
    val rejectedClaims: Int,
    val totalKnowMinted: BigInteger,
    val totalKnowSlashed: BigInteger,
    val circulatingSupply: BigInteger
)

suspend fun getNetworkStatistics(): NetworkStats {
    // This requires iterating through events or maintaining off-chain index
    val verifiedEvents = getAllIdentityVerifiedEvents()
    val claimEvents = getAllClaimCreatedEvents()
    val consensusEvents = getAllConsensusReachedEvents()
    val (minted, slashed, circulating) = repository.getRewardStats()

    val tierCounts = verifiedEvents.groupingBy { it.tier }.eachCount()

    val activeClaims = claimEvents.filter { claimId ->
        val claim = repository.getClaim(claimId)
        claim.status == ClaimStatus.ACTIVE
    }.count()

    return NetworkStats(
        totalIdentities = verifiedEvents.size,
        greyGhosts = tierCounts[IdentityTier.GREYGHOST] ?: 0,
        linkedIds = tierCounts[IdentityTier.LINKEDID] ?: 0,
        primaryIds = tierCounts[IdentityTier.PRIMARYID] ?: 0,
        oracles = tierCounts[IdentityTier.ORACLE] ?: 0,
        totalClaims = claimEvents.size,
        activeClaims = activeClaims,
        approvedClaims = consensusEvents.count { it.approved },
        rejectedClaims = consensusEvents.count { !it.approved },
        totalKnowMinted = minted,
        totalKnowSlashed = slashed,
        circulatingSupply = circulating
    )
}
```

---

## Transaction Examples

### 54. Submit Primary Verification Claim

**Kotlin**:
```kotlin
val result = txService.requestPrimaryVerification(
    consensusAddress = consensusAddress,
    justification = "I am a unique human with verified social media presence on LinkedIn and Twitter.",
    stakeAmount = BigInteger.valueOf(30).multiply(BigInteger.TEN.pow(15)) // 0.03 ETH
)

// Returns:
// TransactionResult(
//   success = true,
//   txHash = "0xabc123...",
//   claimId = 43,
//   gasUsed = 150000,
//   message = "Primary verification claim submitted successfully"
// )
```

### 55. Submit Link to Primary Claim

**Kotlin**:
```kotlin
val result = txService.requestLinkToPrimary(
    consensusAddress = consensusAddress,
    primaryAddress = "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb1",
    platform = "LinkedIn",
    justification = "This LinkedIn account belongs to the same person as the Primary address.",
    stakeAmount = BigInteger.valueOf(10).multiply(BigInteger.TEN.pow(15)) // 0.01 ETH
)
```

### 56. Submit Duplicate Flag

**Kotlin**:
```kotlin
val result = txService.submitDuplicateFlag(
    consensusAddress = consensusAddress,
    targetAddress = "0x123...",
    allegedDuplicate = "0x456...",
    justification = "These two addresses show identical behavior patterns and linked accounts.",
    stakeAmount = BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(15)) // 0.1 ETH
)
```

### 57. Vote FOR a Claim (No Stake)

**Kotlin**:
```kotlin
val result = txService.vouchFor(
    consensusAddress = consensusAddress,
    claimId = BigInteger.valueOf(42)
)

// Gas cost: ~100k gas
```

### 58. Vote FOR a Claim (With Stake)

**Kotlin**:
```kotlin
val result = txService.vouchForWithStake(
    consensusAddress = consensusAddress,
    claimId = BigInteger.valueOf(42),
    stakeAmount = BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)) // 100 KNOW
)

// Higher stake = higher potential reward
```

### 59. Vote AGAINST a Claim

**Kotlin**:
```kotlin
val result = txService.vouchAgainst(
    consensusAddress = consensusAddress,
    claimId = BigInteger.valueOf(42)
)
```

### 60. Resolve Consensus

**Kotlin**:
```kotlin
val result = txService.resolveConsensus(
    consensusAddress = consensusAddress,
    claimId = BigInteger.valueOf(42)
)

// Anyone can call this if threshold is met
// Triggers identity state update in IdentityRegistry
// Distributes rewards to correct voters
```

### 61. Claim Rewards

**Kotlin**:
```kotlin
val result = txService.claimRewards(
    consensusAddress = consensusAddress,
    claimId = BigInteger.valueOf(42)
)

// If voted correctly: Refund stake + bonus KNOW
// If voted incorrectly: Stake slashed, no refund
```

### 62. Approve KNOW Spending

**Kotlin**:
```kotlin
val result = txService.approveKnomeeSpending(
    tokenAddress = knomeeTokenAddress,
    spender = consensusAddress,
    amount = BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(18)) // 1000 KNOW
)

// Required before staking KNOW tokens
```

### 63. Transfer KNOW Tokens

**Kotlin**:
```kotlin
val result = txService.transferKnomee(
    tokenAddress = knomeeTokenAddress,
    to = "0x123...",
    amount = BigInteger.valueOf(50).multiply(BigInteger.TEN.pow(18)) // 50 KNOW
)
```

---

## Performance Optimization

### 64. Batch Read Multiple Identities

**Kotlin**:
```kotlin
// Instead of sequential reads
val identities = addresses.map { repository.getIdentity(it) } // Slow!

// Use batch RPC call
val batch = BatchRequest()
addresses.forEach { addr ->
    batch.add(
        Request("eth_call", listOf(
            mapOf(
                "to" to registryAddress,
                "data" to encodeGetIdentityCall(addr)
            ),
            "latest"
        ))
    )
}

val results = web3j.sendBatch(batch).send()
val identities = results.map { decodeIdentityResponse(it) }
```

### 65. Cache Frequently Accessed Data

**Kotlin**:
```kotlin
class CachedRepository(val repository: ContractRepository) {
    private val governanceCache = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String, GovernanceParams>()

    suspend fun getGovernanceParameters(): GovernanceParams {
        return governanceCache.get("params") {
            repository.getGovernanceParameters()
        }
    }
}
```

### 66. Use Pagination for Large Results

**Kotlin**:
```kotlin
suspend fun getClaimsPaginated(
    startBlock: BigInteger,
    endBlock: BigInteger,
    pageSize: Long = 1000
): List<ClaimCreatedEvent> {
    val allClaims = mutableListOf<ClaimCreatedEvent>()
    var currentBlock = startBlock

    while (currentBlock < endBlock) {
        val nextBlock = minOf(currentBlock + BigInteger.valueOf(pageSize), endBlock)

        val filter = EthFilter(currentBlock, nextBlock, consensusAddress)
            .addSingleTopic(EventEncoder.encode(CLAIM_CREATED_EVENT))

        val logs = web3j.ethGetLogs(filter).send().logs
        allClaims.addAll(logs.map { decodeClaimCreatedEvent(it) })

        currentBlock = nextBlock + BigInteger.ONE
    }

    return allClaims
}
```

### 67. Subscribe to Events Instead of Polling

**Kotlin**:
```kotlin
// Instead of polling every second
while (true) {
    val claims = repository.getActiveClaims() // Inefficient!
    delay(1000)
}

// Use event subscription
web3j.ethLogFlowable(filter).subscribe { log ->
    when (log.topics[0]) {
        EventEncoder.encode(CLAIM_CREATED_EVENT) -> handleNewClaim(log)
        EventEncoder.encode(CONSENSUS_REACHED_EVENT) -> handleResolution(log)
    }
}
```

---

## Summary Table

| Query Category | Count | Avg Gas Cost | Avg Response Time |
|----------------|-------|--------------|-------------------|
| Identity Registry Queries | 12 | Free (view) | ~200ms |
| Consensus Queries | 13 | Free (view) | ~300ms |
| Token Queries | 5 | Free (view) | ~150ms |
| Governance Queries | 6 | Free (view) | ~100ms |
| Event Queries | 6 | Free | ~500ms-3s |
| Complex Queries | 5 | Free | ~1-5s |
| Write Transactions | 10 | ~50k-200k gas | ~12-15s |

**Total Queries Documented**: 67+ examples

---

## Best Practices

1. **Always use view functions for reads** - They're free and fast
2. **Batch RPC calls** when fetching multiple data points
3. **Cache governance parameters** - They change infrequently
4. **Index events off-chain** for complex queries
5. **Use pagination** for large datasets
6. **Subscribe to events** instead of polling
7. **Handle RPC errors** with retries and fallbacks
8. **Validate inputs** before sending transactions
9. **Estimate gas** before submitting transactions
10. **Monitor transaction status** until confirmation

---

This query reference provides comprehensive examples for all common operations in the Knomee Identity system. Use these patterns as templates for building robust blockchain data access layers.
