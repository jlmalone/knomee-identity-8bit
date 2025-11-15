# Database Schema Documentation

## Table of Contents

1. [Entity-Relationship Diagrams](#entity-relationship-diagrams)
2. [IdentityRegistry Schema](#identityregistry-schema)
3. [IdentityConsensus Schema](#identityconsensus-schema)
4. [IdentityToken Schema](#identitytoken-schema)
5. [KnomeeToken Schema](#knomeetoken-schema)
6. [GovernanceParameters Schema](#governanceparameters-schema)
7. [Relationships and Foreign Keys](#relationships-and-foreign-keys)
8. [Indexes and Access Patterns](#indexes-and-access-patterns)
9. [Constraints and Validation](#constraints-and-validation)

---

## Entity-Relationship Diagrams

### System-Wide ER Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Knomee Identity System                            │
│                      Blockchain Database Schema                          │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────┐
│   GovernanceParameters   │
│  (Protocol Config)       │
│─────────────────────────│
│ • consensusThresholds   │◄──────┐
│ • stakingParameters     │       │ configures
│ • slashingRates         │       │
│ • votingWeights         │       │
│ • cooldownPeriods       │       │
└──────────────────────────┘       │
                                   │
                                   │
┌──────────────────────────────────▼───────────────────────────────────┐
│                       IdentityRegistry                                │
│                    (Core Identity Storage)                            │
│───────────────────────────────────────────────────────────────────────│
│ PK: address                                                           │
│───────────────────────────────────────────────────────────────────────│
│ • identities[address]                                                 │
│   ├─ tier (GreyGhost/LinkedID/PrimaryID/Oracle)                      │
│   ├─ primaryAddress                                                   │
│   ├─ verifiedAt                                                       │
│   ├─ totalVouchesReceived                                             │
│   ├─ totalStakeReceived                                               │
│   ├─ underChallenge                                                   │
│   ├─ challengeId                                                      │
│   └─ oracleGrantedAt                                                  │
│                                                                        │
│ • linkedIds[address][platform] → linkedAddress                        │
│ • linkedPlatforms[address][] (array of LinkedPlatform structs)        │
│ • linkedToPrimary[linkedAddress] → primaryAddress                     │
│                                                                        │
│ References:                                                           │
│ • consensusContract (address)                                         │
│ • identityToken (address)                                             │
└────────────────────────┬──────────────────────────────────────────────┘
                         │ mints/updates
                         │
                         ├─────────────────────┐
                         │                     │
                         ▼                     ▼
         ┌────────────────────────┐  ┌────────────────────────┐
         │   IdentityToken        │  │   IdentityConsensus    │
         │   (Soul-Bound NFT)     │  │   (Claims & Voting)    │
         │────────────────────────│  │────────────────────────│
         │ PK: tokenId            │  │ PK: claimId            │
         │────────────────────────│  │────────────────────────│
         │ • tokenIdToAccount     │  │ • claims[claimId]      │
         │ • accountToTokenId     │  │   ├─ claimType         │
         │ • identityTiers        │  │   ├─ status            │
         │ • transfersDisabled    │  │   ├─ subject           │
         │                        │  │   ├─ relatedAddress    │
         │ Each verified identity │  │   ├─ platform          │
         │ receives ONE NFT that  │  │   ├─ justification     │
         │ cannot be transferred  │  │   ├─ createdAt         │
         │                        │  │   ├─ expiresAt         │
         │ References:            │  │   ├─ totalWeightFor    │
         │ • identityRegistry     │  │   ├─ totalWeightAgainst│
         └────────────────────────┘  │   ├─ totalStake        │
                  ▲                  │   └─ resolved          │
                  │                  │                        │
                  │ uses for voting  │ • vouchesOnClaim[claimId][]│
                  │ weight           │   ├─ voucher           │
                  │                  │   ├─ isSupporting      │
                  │                  │   ├─ weight            │
                  │                  │   ├─ stake             │
                  │                  │   ├─ vouchedAt         │
                  │                  │   └─ rewardClaimed     │
                  │                  │                        │
                  │                  │ • hasVouched[claimId][address]│
                  │                  │ • claimsByAddress[address][]│
                  │                  │ • lastFailedClaim[address]│
                  │                  │ • lastDuplicateFlag[address]│
                  │                  │                        │
                  │                  │ References:            │
                  │                  │ • identityRegistry     │
                  └──────────────────┤ • knomeeToken          │
                                     │ • governanceParams     │
                                     └────────┬───────────────┘
                                              │
                                              │ stakes/rewards
                                              ▼
                                  ┌────────────────────────┐
                                  │    KnomeeToken         │
                                  │    (ERC-20 Utility)    │
                                  │────────────────────────│
                                  │ PK: address            │
                                  │────────────────────────│
                                  │ • balances[address]    │
                                  │ • allowances[owner][spender]│
                                  │ • hasClaimedPrimaryReward│
                                  │ • totalRewardsMinted   │
                                  │ • totalSlashed         │
                                  │                        │
                                  │ Supply: 1 billion max  │
                                  │ Primary reward: 100 KNOW│
                                  │ Oracle reward: 10 KNOW │
                                  │                        │
                                  │ References:            │
                                  │ • consensusContract    │
                                  │ • registryContract     │
                                  └────────────────────────┘
```

### Identity Tier Progression Flow

```
┌────────────────────────────────────────────────────────────────┐
│                    Identity Lifecycle                           │
└────────────────────────────────────────────────────────────────┘

    New User
        │
        ▼
┌───────────────┐
│  GreyGhost    │ ← Default tier, no verification
│  (Tier 0)     │   Can: Submit claims, vote (weight=0)
└───────┬───────┘
        │
        │ Submit PrimaryID claim
        │ with stake & justification
        ▼
┌───────────────┐
│ Claim Pending │ ← Consensus voting in progress
│ (Active Vote) │   Others vouch FOR/AGAINST
└───────┬───────┘   Weighted voting based on tier
        │
        ├─── Approved (67%+ weighted consensus)
        │
        ▼
┌───────────────┐
│  PrimaryID    │ ← Verified unique human
│  (Tier 2)     │   Can: Create LinkedIDs, vote (weight=1)
└───────┬───────┘   Receives: 100 KNOW tokens, Soul-bound NFT
        │
        │ Link secondary accounts
        │ (social platforms)
        ▼
┌───────────────┐
│  LinkedID     │ ← Secondary account of a Primary
│  (Tier 1)     │   linkedToPrimary → PrimaryID address
└───────────────┘   Can: Vote with Primary's weight
        │
        │
        │ Participate in consensus,
        │ prove expertise over time
        ▼
┌───────────────┐
│  Oracle       │ ← Trusted expert verifier
│  (Tier 3)     │   Can: Same as Primary + boosted vote weight (100x)
└───────────────┘   Receives: 10 KNOW per claim resolved


Duplicate Challenge Flow:
┌───────────────┐
│  PrimaryID    │ ─── Someone flags as duplicate
└───────┬───────┘
        │
        ▼
┌───────────────┐
│ Under         │ ← underChallenge = true
│ Challenge     │   challengeId points to duplicate claim
└───────┬───────┘
        │
        ├─── Approved (80%+ weighted consensus) → Slashed, reverts to GreyGhost
        └─── Rejected → Challenge dismissed, identity intact
```

---

## IdentityRegistry Schema

**Contract**: `contracts/identity/IdentityRegistry.sol` (349 lines)
**Purpose**: Core identity state management and tier progression

### Data Structures

#### Identity Struct

```solidity
struct Identity {
    IdentityTier tier;              // Current verification tier
    address primaryAddress;         // For LinkedIDs: parent Primary address
    uint256 verifiedAt;            // Timestamp of verification
    uint256 totalVouchesReceived;  // Lifetime vouches count
    uint256 totalStakeReceived;    // Lifetime stake in wei
    bool underChallenge;           // Currently flagged as duplicate
    uint256 challengeId;           // Active duplicate challenge claim ID
    uint256 oracleGrantedAt;       // Timestamp when Oracle status granted
}
```

**Field Descriptions**:

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| `tier` | IdentityTier (enum) | Current verification level (0-3) | Default: GreyGhost (0) |
| `primaryAddress` | address | Parent Primary if LinkedID, else 0x0 | Must be valid Primary for tier=1 |
| `verifiedAt` | uint256 | Unix timestamp of first verification | Set on Primary approval |
| `totalVouchesReceived` | uint256 | Cumulative vouches (not current) | Monotonically increasing |
| `totalStakeReceived` | uint256 | Cumulative stake in wei | Monotonically increasing |
| `underChallenge` | bool | Duplicate investigation active | Locks tier progression |
| `challengeId` | uint256 | Points to active duplicate claim | Only valid if underChallenge=true |
| `oracleGrantedAt` | uint256 | Timestamp of Oracle promotion | 0 if not Oracle |

#### IdentityTier Enum

```solidity
enum IdentityTier {
    GreyGhost,   // 0 - Unverified, can vote but weight=0
    LinkedID,    // 1 - Verified secondary account, links to Primary
    PrimaryID,   // 2 - Verified unique human, vote weight=1
    Oracle       // 3 - Trusted expert, vote weight=100
}
```

#### LinkedPlatform Struct

```solidity
struct LinkedPlatform {
    address linkedAddress;  // Ethereum address of the LinkedID
    string platform;        // Platform name (e.g., "LinkedIn", "Instagram")
    string justification;   // Reason for linking this account
    uint256 linkedAt;       // Timestamp of linking
}
```

### Storage Mappings

```solidity
// Primary identity registry (address → Identity)
mapping(address => Identity) public identities;

// Platform linking: PrimaryAddress → Platform → LinkedAddress
mapping(address => mapping(string => address)) public linkedIds;

// All linked platforms for a Primary (for enumeration)
mapping(address => LinkedPlatform[]) public linkedPlatforms;

// Reverse lookup: LinkedAddress → PrimaryAddress
mapping(address => address) public linkedToPrimary;

// Contract references
IdentityConsensus public consensusContract;
IdentityToken public identityToken;
```

**Indexing Strategy**:
- **Primary Key**: `address` (Ethereum account address)
- **Secondary Index**: `linkedToPrimary` (reverse lookup from LinkedID → Primary)
- **Composite Index**: `linkedIds[primary][platform]` (lookup specific platform link)

### Access Patterns

1. **Read Identity**: `identities[address]` → `Identity` struct
2. **Check Tier**: `identities[address].tier` → `IdentityTier` enum
3. **Find Primary**: `linkedToPrimary[linkedAddress]` → `primaryAddress`
4. **List Linked Accounts**: `linkedPlatforms[primary]` → `LinkedPlatform[]` array
5. **Check Platform Link**: `linkedIds[primary]["LinkedIn"]` → `linkedAddress`

### State-Changing Functions

| Function | Access Control | Gas Cost | Description |
|----------|---------------|----------|-------------|
| `verifyPrimary(address)` | onlyConsensus | ~80k gas | Upgrade GreyGhost → PrimaryID |
| `grantOracle(address)` | onlyConsensus | ~50k gas | Upgrade PrimaryID → Oracle |
| `linkSecondaryToPrimary(...)` | onlyConsensus | ~120k gas | Link secondary account |
| `recordVouch(...)` | onlyConsensus | ~40k gas | Increment vouch counters |
| `flagDuplicate(address, claimId)` | onlyConsensus | ~30k gas | Set underChallenge=true |
| `clearDuplicate(address)` | onlyConsensus | ~30k gas | Set underChallenge=false |
| `downgradeToGreyGhost(address)` | onlyConsensus | ~50k gas | Revoke verification (slashing) |

### View Functions (Free Reads)

```solidity
function getTier(address addr) public view returns (IdentityTier)
function isVerified(address addr) public view returns (bool)
function isPrimary(address addr) public view returns (bool)
function isOracle(address addr) public view returns (bool)
function getPrimaryAddress(address addr) public view returns (address)
function getLinkedAccounts(address primary) public view returns (LinkedPlatform[] memory)
function getVotingWeight(address addr) public view returns (uint256)
function isUnderChallenge(address addr) public view returns (bool)
```

### Events

```solidity
event IdentityVerified(address indexed addr, IdentityTier tier, uint256 timestamp);
event OracleGranted(address indexed oracle, uint256 timestamp);
event SecondaryLinked(address indexed primary, address indexed secondary, string platform);
event DuplicateFlagged(address indexed addr, uint256 challengeId);
event DuplicateCleared(address indexed addr);
event IdentitySlashed(address indexed addr, IdentityTier previousTier);
```

**Event Indexing**:
- `indexed` parameters create bloom filters for fast log searching
- Off-chain applications can filter events by address or tier
- Events provide historical audit trail (cheaper than storage reads)

---

## IdentityConsensus Schema

**Contract**: `contracts/identity/IdentityConsensus.sol` (400+ lines)
**Purpose**: Decentralized identity verification via weighted voting

### Data Structures

#### IdentityClaim Struct

```solidity
struct IdentityClaim {
    uint256 claimId;              // Auto-incrementing unique ID
    ClaimType claimType;          // Type of claim (LinkToPrimary, NewPrimary, DuplicateFlag)
    ClaimStatus status;           // Current status (Active, Approved, Rejected, Expired)
    address subject;              // Address making claim or being challenged
    address relatedAddress;       // Primary (for Link) or alleged duplicate (for Flag)
    string platform;              // Platform name (for LinkedID claims)
    string justification;         // Claimant's reasoning (max 1000 chars)
    uint256 createdAt;            // Submission timestamp
    uint256 expiresAt;            // Expiry deadline (30 days default)
    uint256 totalWeightFor;       // Cumulative weighted votes FOR
    uint256 totalWeightAgainst;   // Cumulative weighted votes AGAINST
    uint256 totalStake;           // Total KNOW tokens staked
    bool resolved;                // Consensus reached (approved/rejected)
}
```

**Field Descriptions**:

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| `claimId` | uint256 | Unique identifier | Auto-incremented from 1 |
| `claimType` | ClaimType (enum) | Claim category | 0=LinkToPrimary, 1=NewPrimary, 2=DuplicateFlag |
| `status` | ClaimStatus (enum) | Current state | 0=Active, 1=Approved, 2=Rejected, 3=Expired |
| `subject` | address | Main address involved | Cannot be 0x0 |
| `relatedAddress` | address | Context-dependent address | 0x0 for NewPrimary |
| `platform` | string | Social platform name | Required for LinkToPrimary |
| `justification` | string | Claimant's reasoning | Max 1000 characters |
| `createdAt` | uint256 | Submission timestamp | Set to `block.timestamp` |
| `expiresAt` | uint256 | Deadline for voting | createdAt + 30 days |
| `totalWeightFor` | uint256 | Weighted votes in favor | Sum of voucher weights |
| `totalWeightAgainst` | uint256 | Weighted votes against | Sum of voucher weights |
| `totalStake` | uint256 | Total KNOW staked | Sum of all stakes |
| `resolved` | bool | Consensus finalized | Set to true after resolution |

#### ClaimType Enum

```solidity
enum ClaimType {
    LinkToPrimary,    // 0 - Request to link secondary account to Primary
    NewPrimary,       // 1 - Request to become verified Primary (unique human)
    DuplicateFlag     // 2 - Challenge that an address is duplicate/Sybil
}
```

**Consensus Thresholds** (from GovernanceParameters):
- **LinkToPrimary**: 51% weighted approval
- **NewPrimary**: 67% weighted approval
- **DuplicateFlag**: 80% weighted approval (higher bar to slash)

#### ClaimStatus Enum

```solidity
enum ClaimStatus {
    Active,     // 0 - Voting in progress
    Approved,   // 1 - Reached consensus threshold, claim accepted
    Rejected,   // 2 - Did not reach threshold or majority voted against
    Expired     // 3 - Voting period ended without resolution
}
```

#### Vouch Struct

```solidity
struct Vouch {
    address voucher;        // Address of the voter
    bool isSupporting;      // true = vote FOR, false = vote AGAINST
    uint256 weight;         // Voting weight (1 for Primary, 100 for Oracle)
    uint256 stake;          // KNOW tokens staked on this vote
    uint256 vouchedAt;      // Timestamp of vote
    bool rewardClaimed;     // Whether voter claimed reward/refund
}
```

**Voting Weight Calculation**:
```solidity
function getVotingWeight(address voter) internal view returns (uint256) {
    IdentityTier tier = identityRegistry.getTier(voter);
    if (tier == IdentityTier.PrimaryID) return governanceParams.primaryVoteWeight();  // 1
    if (tier == IdentityTier.Oracle) return governanceParams.oracleVoteWeight();      // 100
    return 0;  // GreyGhost and LinkedID cannot vote (weight=0)
}
```

### Storage Mappings

```solidity
// Claim registry (claimId → IdentityClaim)
mapping(uint256 => IdentityClaim) public claims;

// Votes on each claim (claimId → Vouch[])
mapping(uint256 => Vouch[]) public vouchesOnClaim;

// Vote tracking to prevent double-voting (claimId → voter → hasVoted)
mapping(uint256 => mapping(address => bool)) public hasVouched;

// Claims by address for easy lookup (address → claimId[])
mapping(address => uint256[]) public claimsByAddress;

// Cooldown tracking
mapping(address => uint256) public lastFailedClaim;      // Timestamp of last rejection
mapping(address => uint256) public lastDuplicateFlag;    // Timestamp of last duplicate flag

// Auto-incrementing claim ID
uint256 public nextClaimId = 1;

// Contract references
IdentityRegistry public identityRegistry;
KnomeeToken public knomeeToken;
GovernanceParameters public governanceParams;
```

**Indexing Strategy**:
- **Primary Key**: `claimId` (auto-incrementing uint256)
- **Secondary Index**: `claimsByAddress[address]` (find all claims by user)
- **Vote Lookup**: `hasVouched[claimId][voter]` (O(1) double-vote check)
- **Vote Details**: `vouchesOnClaim[claimId]` (array of all votes)

### Access Patterns

1. **Read Claim**: `claims[claimId]` → `IdentityClaim` struct
2. **Check Vote**: `hasVouched[claimId][voter]` → `bool`
3. **Get Votes**: `vouchesOnClaim[claimId]` → `Vouch[]` array
4. **User Claims**: `claimsByAddress[user]` → `uint256[]` claim IDs
5. **Check Cooldown**: `lastFailedClaim[user]` → timestamp

### State-Changing Functions

| Function | Access Control | Gas Cost | Description |
|----------|---------------|----------|-------------|
| `submitPrimaryClaim(justification)` | Public | ~150k gas | Request PrimaryID verification |
| `submitLinkClaim(primary, platform, justification)` | Public | ~160k gas | Request to link secondary account |
| `submitDuplicateFlag(target, duplicate, justification)` | Only Primary/Oracle | ~170k gas | Challenge address as Sybil |
| `vouchFor(claimId)` | Primary/Oracle | ~100k gas | Vote FOR claim (no stake) |
| `vouchAgainst(claimId)` | Primary/Oracle | ~100k gas | Vote AGAINST claim (no stake) |
| `vouchForWithStake(claimId, amount)` | Primary/Oracle | ~130k gas | Vote FOR + stake KNOW |
| `vouchAgainstWithStake(claimId, amount)` | Primary/Oracle | ~130k gas | Vote AGAINST + stake KNOW |
| `resolveConsensus(claimId)` | Public | ~200k gas | Finalize claim if threshold met |
| `expireClaim(claimId)` | Public | ~50k gas | Mark claim as expired |
| `claimRewards(claimId)` | Public | ~80k gas | Withdraw rewards/refunds |

### View Functions (Free Reads)

```solidity
function getClaim(uint256 claimId) public view returns (IdentityClaim memory)
function getClaimStatus(uint256 claimId) public view returns (ClaimStatus)
function getVouches(uint256 claimId) public view returns (Vouch[] memory)
function hasUserVouched(uint256 claimId, address user) public view returns (bool)
function getClaimsByAddress(address user) public view returns (uint256[] memory)
function isClaimResolved(uint256 claimId) public view returns (bool)
function calculateCurrentResult(uint256 claimId) public view returns (bool wouldPass)
function canUserClaim(address user) public view returns (bool, string memory reason)
```

### Events

```solidity
event ClaimCreated(
    uint256 indexed claimId,
    address indexed subject,
    ClaimType claimType,
    uint256 createdAt,
    uint256 expiresAt
);

event VouchCast(
    uint256 indexed claimId,
    address indexed voucher,
    bool isSupporting,
    uint256 weight,
    uint256 stake
);

event ConsensusReached(
    uint256 indexed claimId,
    bool approved,
    uint256 totalWeightFor,
    uint256 totalWeightAgainst,
    uint256 resolvedAt
);

event ClaimExpired(uint256 indexed claimId, uint256 expiredAt);

event RewardsClaimed(
    uint256 indexed claimId,
    address indexed claimer,
    uint256 amount,
    bool wasSlashed
);

event StakeSlashed(
    uint256 indexed claimId,
    address indexed staker,
    uint256 slashedAmount,
    uint256 slashRate
);
```

---

## IdentityToken Schema

**Contract**: `contracts/identity/IdentityToken.sol` (150+ lines)
**Purpose**: Soul-bound NFT representing verified unique identities

### Key Properties

- **ERC-721 Compliant**: Standard NFT interface
- **Non-Transferable**: `transfersDisabled = true` (soul-bound)
- **One Per Identity**: Each verified human receives exactly one token
- **Tier Tracking**: Stores current identity tier for each token

### Storage Mappings

```solidity
// NFT ownership mappings (standard ERC-721)
mapping(uint256 => address) private _owners;             // tokenId → owner
mapping(address => uint256) private _balances;           // owner → count (always 0 or 1)
mapping(uint256 => address) private _tokenApprovals;     // tokenId → approved (unused)
mapping(address => mapping(address => bool)) private _operatorApprovals;  // (unused)

// Identity-specific mappings
mapping(address => IdentityTier) public identityTiers;   // account → current tier
mapping(address => uint256) public accountToTokenId;     // account → tokenId
mapping(uint256 => address) public tokenIdToAccount;     // tokenId → account (reverse)

// Contract references
address public identityRegistry;   // Only registry can mint/update
uint256 private _nextTokenId = 1;  // Auto-incrementing token IDs
bool public transfersDisabled = true;  // Soul-bound flag
```

**Indexing Strategy**:
- **Primary Key**: `tokenId` (uint256)
- **Unique Constraint**: One token per address (enforced in `mint()`)
- **Bidirectional Lookup**: `accountToTokenId` ↔ `tokenIdToAccount`

### Access Patterns

1. **Check Ownership**: `accountToTokenId[address]` → `tokenId` (0 if none)
2. **Get Owner**: `tokenIdToAccount[tokenId]` → `address`
3. **Get Tier**: `identityTiers[address]` → `IdentityTier`
4. **Check Exists**: `accountToTokenId[address] != 0` → `bool`

### State-Changing Functions

| Function | Access Control | Gas Cost | Description |
|----------|---------------|----------|-------------|
| `mint(address, IdentityTier)` | onlyRegistry | ~80k gas | Issue NFT to newly verified identity |
| `updateTier(address, IdentityTier)` | onlyRegistry | ~30k gas | Update tier when upgraded/downgraded |
| `burn(address)` | onlyRegistry | ~40k gas | Destroy NFT (rare, for slashed identities) |

### View Functions

```solidity
function balanceOf(address owner) public view returns (uint256)  // Always 0 or 1
function ownerOf(uint256 tokenId) public view returns (address)
function tokenURI(uint256 tokenId) public view returns (string memory)  // SVG metadata
function getTier(address account) public view returns (IdentityTier)
function hasToken(address account) public view returns (bool)
```

### Token URI and Metadata

Each token generates dynamic on-chain SVG metadata:

```solidity
function tokenURI(uint256 tokenId) public view returns (string memory) {
    // Returns base64-encoded JSON with:
    // - name: "Knomee Identity #123"
    // - description: "Soul-bound identity NFT - PrimaryID tier"
    // - image: SVG with tier badge and visual representation
}
```

**Example SVG Output**:
- **GreyGhost**: Gray badge, "Unverified"
- **LinkedID**: Blue badge, "Linked Account"
- **PrimaryID**: Green badge, "Verified Human"
- **Oracle**: Gold badge, "Trusted Oracle"

### Events

```solidity
event Transfer(address indexed from, address indexed to, uint256 indexed tokenId);
event IdentityMinted(address indexed account, uint256 indexed tokenId, IdentityTier tier);
event TierUpdated(address indexed account, IdentityTier newTier);
event IdentityBurned(address indexed account, uint256 indexed tokenId);
```

---

## KnomeeToken Schema

**Contract**: `contracts/identity/KnomeeToken.sol` (150+ lines)
**Purpose**: ERC-20 utility token for staking, rewards, and governance

### Token Economics

| Parameter | Value |
|-----------|-------|
| Name | Knomee Token |
| Symbol | KNOW |
| Decimals | 18 |
| Max Supply | 1,000,000,000 (1 billion) |
| Primary ID Reward | 100 KNOW |
| Oracle Reward per Claim | 10 KNOW |
| Initial Distribution | Minted on-demand (rewards only) |

### Storage Mappings

```solidity
// Standard ERC-20 mappings
mapping(address => uint256) private _balances;                        // account → balance
mapping(address => mapping(address => uint256)) private _allowances;  // owner → spender → amount

// Identity-specific tracking
mapping(address => bool) public hasClaimedPrimaryReward;  // One-time 100 KNOW reward

// Statistics
uint256 public totalRewardsMinted;  // Total tokens minted as rewards
uint256 public totalSlashed;        // Total tokens burned via slashing

// Contract references
address public consensusContract;   // Can mint rewards for voters
address public registryContract;    // Can mint rewards for verified identities
```

**Supply Management**:
```solidity
function totalSupply() public view returns (uint256) {
    return totalRewardsMinted - totalSlashed;
}

function circulatingSupply() public view returns (uint256) {
    return totalSupply() - balanceOf(consensusContract) - balanceOf(registryContract);
}
```

### Access Patterns

1. **Check Balance**: `balanceOf(address)` → `uint256`
2. **Check Allowance**: `allowance(owner, spender)` → `uint256`
3. **Check Claimed**: `hasClaimedPrimaryReward[address]` → `bool`
4. **Total Supply**: `totalRewardsMinted - totalSlashed` → `uint256`

### State-Changing Functions

| Function | Access Control | Gas Cost | Description |
|----------|---------------|----------|-------------|
| `transfer(to, amount)` | Public | ~60k gas | Transfer tokens |
| `approve(spender, amount)` | Public | ~45k gas | Grant spending allowance |
| `transferFrom(from, to, amount)` | Public | ~70k gas | Spend allowance |
| `mintPrimaryReward(to)` | onlyRegistry | ~70k gas | Mint 100 KNOW (one-time) |
| `mintOracleReward(to)` | onlyConsensus | ~60k gas | Mint 10 KNOW per resolution |
| `mintVoterReward(to, amount)` | onlyConsensus | ~60k gas | Mint stake refund + bonus |
| `slash(from, amount)` | onlyConsensus | ~50k gas | Burn staked tokens (penalty) |

### View Functions

```solidity
function balanceOf(address account) public view returns (uint256)
function allowance(address owner, address spender) public view returns (uint256)
function totalSupply() public view returns (uint256)
function canClaimPrimaryReward(address account) public view returns (bool)
function rewardStats() public view returns (uint256 minted, uint256 slashed, uint256 circulating)
```

### Events

```solidity
event Transfer(address indexed from, address indexed to, uint256 value);
event Approval(address indexed owner, address indexed spender, uint256 value);
event RewardMinted(address indexed recipient, uint256 amount, string reason);
event TokensSlashed(address indexed account, uint256 amount);
```

---

## GovernanceParameters Schema

**Contract**: `contracts/identity/GovernanceParameters.sol` (337 lines)
**Purpose**: Centralized protocol configuration with governance control

### Parameter Categories

#### 1. Consensus Thresholds (Basis Points)

```solidity
uint256 public linkThreshold = 5100;        // 51% - LinkToPrimary claims
uint256 public primaryThreshold = 6700;     // 67% - NewPrimary claims
uint256 public duplicateThreshold = 8000;   // 80% - DuplicateFlag claims
```

**Basis Points**: `10000 = 100%`, `5100 = 51%`, etc.

#### 2. Staking Parameters

```solidity
uint256 public minStakeWei = 10**16;         // 0.01 ETH minimum stake
uint256 public primaryStakeMultiplier = 3;   // 3x stake for Primary claims
uint256 public duplicateStakeMultiplier = 10; // 10x stake for Duplicate flags
```

**Effective Min Stakes**:
- LinkToPrimary: 0.01 ETH
- NewPrimary: 0.03 ETH (3x)
- DuplicateFlag: 0.1 ETH (10x)

#### 3. Slashing Rates (Basis Points)

```solidity
uint256 public linkSlashBps = 1000;       // 10% - Wrong link vote
uint256 public primarySlashBps = 3000;    // 30% - Wrong Primary vote
uint256 public duplicateSlashBps = 5000;  // 50% - Wrong Duplicate vote
uint256 public sybilSlashBps = 10000;     // 100% - Proven Sybil attacker
```

**Penalty Calculation**:
```solidity
slashedAmount = (stake * slashBps) / 10000
```

#### 4. Voting Weights

```solidity
uint256 public primaryVoteWeight = 1;      // Standard vote
uint256 public oracleVoteWeight = 100;     // Oracle boost (100x)
```

#### 5. Cooldown Periods

```solidity
uint256 public failedClaimCooldown = 7 days;      // After claim rejection
uint256 public duplicateFlagCooldown = 30 days;   // After flagging someone
uint256 public claimExpiryDuration = 30 days;     // Voting window
```

#### 6. Testing Utilities

```solidity
bool public godModeActive = true;          // Admin override for testing
uint256 public timeWarpSeconds = 0;        // Time manipulation for tests
address public godModeAddress;             // Admin address
```

### Storage

All parameters are stored as public state variables (no complex mappings).

### State-Changing Functions

| Function | Access Control | Gas Cost | Description |
|----------|---------------|----------|-------------|
| `setThresholds(link, primary, duplicate)` | onlyOwner | ~30k gas | Update consensus thresholds |
| `setStakingParams(min, primaryMult, dupMult)` | onlyOwner | ~30k gas | Update staking requirements |
| `setSlashingRates(link, primary, dup, sybil)` | onlyOwner | ~30k gas | Update penalty rates |
| `setVotingWeights(primary, oracle)` | onlyOwner | ~20k gas | Update vote weights |
| `setCooldowns(failed, flag, expiry)` | onlyOwner | ~20k gas | Update time periods |
| `setGodMode(active, address)` | onlyOwner | ~20k gas | Toggle testing mode |
| `timeWarp(seconds)` | onlyGodMode | ~10k gas | Advance time in tests |

### View Functions

```solidity
function getConsensusThreshold(ClaimType claimType) public view returns (uint256)
function getMinStake(ClaimType claimType) public view returns (uint256)
function getSlashRate(ClaimType claimType) public view returns (uint256)
function getVotingWeight(IdentityTier tier) public view returns (uint256)
function getCurrentTimestamp() public view returns (uint256)  // With timeWarp support
function getAllParameters() public view returns (...)  // Full config snapshot
```

### Events

```solidity
event ThresholdsUpdated(uint256 link, uint256 primary, uint256 duplicate);
event StakingParamsUpdated(uint256 minStake, uint256 primaryMult, uint256 duplicateMult);
event SlashingRatesUpdated(uint256 link, uint256 primary, uint256 duplicate, uint256 sybil);
event VotingWeightsUpdated(uint256 primary, uint256 oracle);
event CooldownsUpdated(uint256 failed, uint256 flag, uint256 expiry);
event GodModeToggled(bool active, address godAddress);
```

---

## Relationships and Foreign Keys

### Contract References (Foreign Keys)

```
IdentityRegistry
  ├─ consensusContract → IdentityConsensus (address)
  └─ identityToken → IdentityToken (address)

IdentityConsensus
  ├─ identityRegistry → IdentityRegistry (address)
  ├─ knomeeToken → KnomeeToken (address)
  └─ governanceParams → GovernanceParameters (address)

IdentityToken
  └─ identityRegistry → IdentityRegistry (address)

KnomeeToken
  ├─ consensusContract → IdentityConsensus (address)
  └─ registryContract → IdentityRegistry (address)

GovernanceParameters
  └─ (standalone, no references)
```

### Data Relationships

#### 1. Identity → LinkedPlatforms (One-to-Many)

```solidity
// IdentityRegistry.sol
mapping(address => LinkedPlatform[]) public linkedPlatforms;

// One Primary can have multiple LinkedIDs
// Example: Primary 0xAAA has LinkedIDs on ["LinkedIn", "Instagram", "TikTok"]
```

#### 2. Identity → Claims (One-to-Many)

```solidity
// IdentityConsensus.sol
mapping(address => uint256[]) public claimsByAddress;

// One address can submit multiple claims over time
// Example: Address 0xBBB submitted claims [1, 5, 12]
```

#### 3. Claim → Vouches (One-to-Many)

```solidity
// IdentityConsensus.sol
mapping(uint256 => Vouch[]) public vouchesOnClaim;

// One claim receives multiple votes
// Example: Claim #10 has 50 vouches (voters)
```

#### 4. Identity → Token (One-to-One)

```solidity
// IdentityToken.sol
mapping(address => uint256) public accountToTokenId;
mapping(uint256 => address) public tokenIdToAccount;

// Bidirectional mapping, enforced uniqueness
// Example: Address 0xCCC owns token #42
```

#### 5. Address → KNOW Balance (One-to-One)

```solidity
// KnomeeToken.sol
mapping(address => uint256) private _balances;

// Standard ERC-20 balance tracking
```

### Referential Integrity

Since there are no database-level foreign key constraints, referential integrity is enforced via:

1. **Access Modifiers**: Only authorized contracts can modify state
   ```solidity
   modifier onlyConsensus() {
       require(msg.sender == address(consensusContract));
       _;
   }
   ```

2. **Address Validation**: Checks for zero addresses
   ```solidity
   require(primaryAddress != address(0), "Invalid primary address");
   ```

3. **Existence Checks**: Verify records exist before operations
   ```solidity
   require(claims[claimId].createdAt != 0, "Claim does not exist");
   ```

4. **State Consistency**: Atomic updates prevent orphaned records
   ```solidity
   // Link secondary to primary atomically
   linkedIds[primary][platform] = secondary;
   linkedToPrimary[secondary] = primary;
   linkedPlatforms[primary].push(LinkedPlatform(...));
   ```

---

## Indexes and Access Patterns

### Mapping-Based Indexes

Ethereum mappings provide O(1) key-value lookups. The schema uses strategic mapping structures for common queries:

#### 1. Primary Identity Lookup
```solidity
// O(1) lookup by address
mapping(address => Identity) public identities;

// Usage: Direct read of user's identity
Identity memory id = identities[userAddress];
```

#### 2. Reverse Lookups
```solidity
// Find Primary from LinkedID (O(1))
mapping(address => address) public linkedToPrimary;

// Usage: Given a LinkedID, find parent Primary
address primary = linkedToPrimary[linkedAddress];
```

#### 3. Composite Lookups
```solidity
// Nested mapping: Primary → Platform → LinkedAddress
mapping(address => mapping(string => address)) public linkedIds;

// Usage: Check if Primary has LinkedIn account
address linkedLinkedIn = linkedIds[primary]["LinkedIn"];
```

#### 4. Claims by User
```solidity
// Find all claims submitted by a user
mapping(address => uint256[]) public claimsByAddress;

// Usage: Get user's claim history
uint256[] memory userClaims = claimsByAddress[user];
```

#### 5. Double-Vote Prevention
```solidity
// Fast check if user already voted
mapping(uint256 => mapping(address => bool)) public hasVouched;

// Usage: Prevent duplicate votes (O(1))
require(!hasVouched[claimId][voter], "Already voted");
```

### Event-Based Indexing

For historical queries and filtering, events provide indexed logs:

```solidity
event ClaimCreated(
    uint256 indexed claimId,    // Indexed: can filter by claim ID
    address indexed subject,     // Indexed: can filter by user
    ClaimType claimType,         // Not indexed but included
    uint256 createdAt,
    uint256 expiresAt
);
```

**Query Pattern**:
```kotlin
// Find all claims by a specific user
val filter = EthFilter(
    DefaultBlockParameterName.EARLIEST,
    DefaultBlockParameterName.LATEST,
    consensusAddress
).addSingleTopic(EventEncoder.encode(CLAIM_CREATED_EVENT))
 .addSingleTopic(TypeEncoder.encode(Address(userAddress)))

val logs = web3j.ethGetLogs(filter).send()
```

**Index Limitations**:
- Maximum 3 indexed parameters per event
- Indexed strings/bytes are hashed (can't search by substring)
- Non-indexed parameters can't be filtered (must scan all events)

### Array-Based Storage (O(N) Scans)

Some data uses arrays for enumeration (less efficient):

```solidity
// All linked platforms for a Primary (must iterate)
mapping(address => LinkedPlatform[]) public linkedPlatforms;

// All vouches on a claim (must iterate)
mapping(uint256 => Vouch[]) public vouchesOnClaim;
```

**Access Pattern**:
```solidity
// Iterate over all linked platforms
LinkedPlatform[] memory platforms = linkedPlatforms[primary];
for (uint i = 0; i < platforms.length; i++) {
    // Process platforms[i]
}
```

**Gas Costs**:
- Reading array: ~3k gas + 2k per element
- Writing array: ~20k gas base + 20k per element
- Appending: ~20k gas per element

### Optimization Strategies

1. **Use Mappings for Lookups**: O(1) vs O(N) arrays
2. **Index Event Parameters**: Enable off-chain filtering
3. **Avoid Large Arrays**: Gas costs grow linearly
4. **Paginate Reads**: Batch large data fetches
5. **Cache Off-Chain**: Store frequently accessed data locally

---

## Constraints and Validation

### Identity Tier Constraints

```solidity
// Can only upgrade tier, not downgrade (except via slashing)
function verifyPrimary(address addr) external onlyConsensus {
    require(identities[addr].tier == IdentityTier.GreyGhost, "Must be GreyGhost");
    identities[addr].tier = IdentityTier.PrimaryID;
    identities[addr].verifiedAt = block.timestamp;
}

// Oracle requires Primary first
function grantOracle(address addr) external onlyConsensus {
    require(identities[addr].tier == IdentityTier.PrimaryID, "Must be PrimaryID");
    identities[addr].tier = IdentityTier.Oracle;
    identities[addr].oracleGrantedAt = block.timestamp;
}
```

### Claim Constraints

```solidity
// No duplicate claims while one is active
require(
    claimsByAddress[msg.sender].length == 0 ||
    claims[claimsByAddress[msg.sender][lastIndex]].resolved,
    "Already have active claim"
);

// Cooldown after failed claim
require(
    block.timestamp >= lastFailedClaim[msg.sender] + governanceParams.failedClaimCooldown(),
    "Cooldown period not expired"
);

// Justification length limit
require(bytes(justification).length <= 1000, "Justification too long");
```

### Staking Constraints

```solidity
// Minimum stake requirement
uint256 minStake = governanceParams.getMinStake(claimType);
require(msg.value >= minStake, "Insufficient stake");

// Sufficient KNOW balance for token stakes
require(
    knomeeToken.balanceOf(msg.sender) >= stakeAmount,
    "Insufficient KNOW balance"
);
```

### Voting Constraints

```solidity
// Only Primary and Oracle can vote
uint256 weight = identityRegistry.getVotingWeight(msg.sender);
require(weight > 0, "Only Primary/Oracle can vote");

// Cannot vote twice on same claim
require(!hasVouched[claimId][msg.sender], "Already vouched");

// Cannot vote on expired claim
require(block.timestamp <= claim.expiresAt, "Claim expired");
```

### Linking Constraints

```solidity
// Can only link to verified Primary
require(
    identityRegistry.getTier(primaryAddress) == IdentityTier.PrimaryID,
    "Must link to verified Primary"
);

// Cannot link same platform twice
require(
    linkedIds[primaryAddress][platform] == address(0),
    "Platform already linked"
);

// Cannot link to self
require(subject != primaryAddress, "Cannot link to self");
```

### Token Constraints

```solidity
// Soul-bound NFT cannot be transferred
function _beforeTokenTransfer(address from, address to, uint256 tokenId) internal override {
    if (transfersDisabled) {
        require(from == address(0) || to == address(0), "Soul-bound: transfers disabled");
    }
}

// One token per address
function mint(address to, IdentityTier tier) external onlyRegistry {
    require(accountToTokenId[to] == 0, "Already has identity token");
    // ...
}
```

### Economic Constraints

```solidity
// Max supply cap
uint256 public constant MAX_SUPPLY = 1_000_000_000 * 10**18;  // 1 billion KNOW

function _mint(address account, uint256 amount) internal {
    require(totalSupply() + amount <= MAX_SUPPLY, "Exceeds max supply");
    // ...
}
```

### Governance Constraints

```solidity
// Threshold bounds (must be between 0-100%)
function setThresholds(uint256 link, uint256 primary, uint256 duplicate) external onlyOwner {
    require(link <= 10000 && primary <= 10000 && duplicate <= 10000, "Invalid threshold");
    require(link < primary && primary < duplicate, "Thresholds must increase");
    // ...
}

// Slash rate bounds (0-100%)
function setSlashingRates(...) external onlyOwner {
    require(linkSlashBps <= 10000 && ..., "Invalid slash rate");
    // ...
}
```

---

## Summary Table

| Contract | Primary Key | Secondary Indexes | Storage Size | Avg Gas Cost |
|----------|-------------|-------------------|--------------|--------------|
| IdentityRegistry | address | linkedToPrimary, linkedIds | ~500 bytes/identity | ~80k (verify) |
| IdentityConsensus | claimId | claimsByAddress, hasVouched | ~800 bytes/claim | ~150k (submit) |
| IdentityToken | tokenId | accountToTokenId | ~200 bytes/token | ~80k (mint) |
| KnomeeToken | address (ERC-20) | N/A (standard) | ~64 bytes/balance | ~60k (transfer) |
| GovernanceParameters | N/A (single instance) | N/A | ~1.5 KB total | ~20k (update) |

**Total Schema Complexity**:
- 5 smart contracts
- ~15 primary storage mappings
- ~10 secondary indexes
- 7 enum types
- 6 struct definitions
- 30+ view functions
- 25+ state-changing functions
- 20+ events

---

## File Locations

- **Schema Definitions**: `contracts/identity/*.sol`
- **Client Data Models**: `desktop-client/src/main/kotlin/com/knomee/identity/blockchain/IdentityData.kt`
- **Repository Layer**: `desktop-client/src/main/kotlin/com/knomee/identity/blockchain/ContractRepository.kt`
- **Transaction Layer**: `desktop-client/src/main/kotlin/com/knomee/identity/blockchain/TransactionService.kt`
- **Event Monitoring**: `desktop-client/src/main/kotlin/com/knomee/identity/blockchain/EventListener.kt`

---

This schema provides a decentralized, immutable identity verification system with economic incentives for honest participation and cryptographic guarantees of data integrity.
