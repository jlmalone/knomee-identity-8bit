# Knomee Identity Consensus Protocol v1
## Decentralized Sybil Resistance Through Weighted Social Consensus

**Version:** 1.0
**Date:** October 24, 2025
**Status:** Design Complete - Ready for Implementation
**Timeline:** Phase 1 (4 weeks) + Phase 2 (4 weeks)
**Platform:** Ethereum Sepolia Testnet → Polygon Mainnet

---

## EXECUTIVE SUMMARY

This is not a game. This is a **protocol** for establishing unique human identity on-chain through weighted social consensus, with game-like aesthetics to make it usable.

### The Core Problem: Sybil Attacks on Universal Basic Income

If Knomee distributes daily tokens (UBI) to verified humans, attackers will try to claim multiple identities to collect multiple allocations. Traditional approaches have limitations:

- **Worldcoin:** Scan eyeballs (centralized hardware, privacy concerns)
- **Proof of Humanity:** Video verification (centralized reviewers)
- **BrightID:** Social graphs (complex, gameable)

**Knomee's approach:** Weighted social consensus where community + oracles vote on identity uniqueness, with economic incentives (staking) to align behavior.

### What We're Building

**Phase 1 - Identity Consensus (Weeks 1-4):**
1. **IdentityRegistry.sol** - Track identity states and tier progression
2. **IdentityConsensus.sol** - Weighted voting and staking on identity claims
3. **GovernanceParameters.sol** - Configurable protocol parameters

**Phase 2 - Product & Reputation Layer (Weeks 5-8):**
4. **ProductRegistry.sol** - Products with fractional ownership
5. **ReputationDistribution.sol** - Reputation flow to products and owners

**Desktop Client (Kotlin Compose, 8-bit aesthetic):**
- 2D space where addresses are represented as pixel-art avatars
- Menu system for identity claims, vouching, staking
- Visual representation of consensus building
- NPC simulation for testing protocol dynamics

**The "game" is the protocol. Smart contracts are game theory. The UI just makes it beautiful.**

---

## DESIGN DECISIONS (FINALIZED)

All critical design questions have been answered and are locked in for implementation:

### Decision 1: Phase Scope ✅
**CHOSEN: Design Both, Build Identity First**

- Design complete 3-layer architecture NOW
- Implement only identity in Phase 1 (4 weeks)
- Products in Phase 2 already designed, faster build
- Best of both worlds: focused execution + complete vision

### Decision 2: Linked ID Type System ✅
**CHOSEN: Flexible String Labels**

```solidity
mapping(address => mapping(string => address)) public linkedIds;
// linkedIds[primary]["LinkedIn"] = 0xabc
// linkedIds[primary]["TikTok"] = 0xdef
```

**Rationale:**
- Future-proof (no contract upgrades for new platforms)
- Flexible (supports any platform: LinkedIn, Instagram, custom platforms)
- Enforcement through consensus (community validates legitimacy)

**Trade-off accepted:** No hardcoded "one per type" rule, rely on consensus to prevent abuse

### Decision 3: Multiple Accounts Per Platform ✅
**CHOSEN: Allow Multiple with Justification**

- Users CAN link multiple Instagram/LinkedIn accounts
- Each link requires consensus approval with justification
- Example: "This is my business Instagram" vs. "This is my personal Instagram"
- Flexible, reflects real-world usage patterns

**Rationale:** Real people have multiple accounts legitimately, consensus can distinguish honest multi-account from Sybil attack

### Decision 4: Product Ownership Mutability ✅
**CHOSEN: Mutable via Governance (Phase 2)**

- Existing owners can propose ownership changes
- Requires 67% consensus from current owners
- Enables fair attribution for ongoing contributors
- Example: Alice (100%) can add Bob (30%) after he edits the blog post

**Rationale:** Collaboration should be rewarded, governance prevents disputes

### Decision 5: Gas Budget for Multi-Owner Products ✅
**CHOSEN: No Limit**

- Allow unlimited owners per product
- User pays whatever gas cost results
- Maximum flexibility for large collaborations

**Gas estimates:**
- 1 owner: ~30k gas (~$0.60)
- 5 owners: ~60k gas (~$1.20)
- 10 owners: ~100k gas (~$2.00)
- 20 owners: ~180k gas (~$3.60)

**Rationale:** Let the market decide, don't artificially constrain collaboration

### Decision 6: URL Claiming Mechanism ✅
**CHOSEN: Both (Oracle + Self-Claim) (Phase 2)**

- **Oracle-Minted:** High trust, verified ownership, higher reputation weight
- **Self-Claim:** Decentralized, requires proof (DNS TXT, HTML meta tag)
- Oracle-minted products have "verified" status

**Rationale:** Centralized quality tier + decentralized permissionless access = best of both

### Decision 7: Reputation Flow Defaults ✅
**CHOSEN: Smart Default with Override (Phase 2)**

- **Default:** Auto-split by ownership percentage
- **Override:** Advanced option to direct to specific owner
- Simple for 95% of users, flexible for power users

**Rationale:** Good UX defaults, power user flexibility when needed

---

## IDENTITY MODEL

### Identity Types

```
┌─────────────────────────────────────────────────┐
│  ONE HUMAN                                      │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌──────────────────┐                          │
│  │  PRIMARY ID      │  ← Blue Checkmark        │
│  │  (0x123...)      │  ← Gets daily Knomee     │
│  │                  │  ← Only ONE per human    │
│  └──────────────────┘                          │
│         │                                       │
│         ├─→ Linked ID ("Instagram" → 0xabc...) │
│         ├─→ Linked ID ("Instagram-biz" → 0xd…) │
│         ├─→ Linked ID ("Twitter" → 0xdef...)   │
│         ├─→ Linked ID ("GitHub" → 0x789...)    │
│         └─→ Linked ID ("Discord" → 0x456...)   │
│                                                 │
│  All Linked IDs:                                │
│  - Can receive reputation tokens (Phase 2)     │
│  - Cannot collect daily Knomee                 │
│  - Proven to belong to same human as Primary   │
│  - Multiple per platform allowed with proof    │
└─────────────────────────────────────────────────┘
```

### Tier Progression

**Tier 0: Grey Ghost** (Unverified)
- New address, no consensus
- Cannot vouch on others
- Cannot collect daily Knomee
- Can request verification

**Tier 1: Linked ID** (Secondary Identity)
- Verified as belonging to a Primary ID
- Easier consensus threshold (51%)
- Can receive reputation (Phase 2)
- No daily Knomee
- **Multiple allowed per platform with justification**

**Tier 2: Primary ID** (Blue Checkmark - THE KEY ONE)
- Verified as unique human
- Strict consensus threshold (67%)
- Gets daily Knomee (UBI)
- Can vouch on others (weight = 1)
- **ONLY ONE per human**

**Tier 3: Oracle** (High-Weight Verifier)
- Elevated Primary ID with proven track record
- Massive voting weight (100x)
- Admin-granted in v1 (meritocratic later)
- Power fades over time (governance-controlled decay)

---

## CLAIM TYPES & CONSENSUS THRESHOLDS

### 1. Link to Primary (51% threshold)

**Claim:** "Address B is a secondary identity of Primary A"

**Example:**
- Alice has Primary ID (0x123)
- Alice controls Instagram account → linked to ETH address (0xabc)
- Alice claims: `linkToPrimary(0x123, "Instagram")`
- Community votes (easier threshold, low risk)
- Approved → 0xabc becomes Linked ID under 0x123

**Why easier?** Not claiming new UBI allocation, just linking existing human's addresses.

**Multiple accounts:** Alice can also link `linkToPrimary(0x123, "Instagram-business")` if justified

---

### 2. New Primary (67% threshold - THE CRITICAL ONE)

**Claim:** "This address is a NEW unique human (Blue Checkmark)"

**Example:**
- Bob (new user) requests Primary verification
- Community/oracles investigate:
  - Does behavior match existing Primary?
  - Are there duplicate signals (same IP, same wallet patterns)?
  - Do trusted oracles vouch?
- Vote reaches 67% → Bob gets Blue Checkmark → daily Knomee enabled

**Why harder?** This is the Sybil resistance gatekeeper. Every Primary = new UBI recipient. Must be strict.

---

### 3. Duplicate Detection (80% threshold - THE ENFORCEMENT)

**Claim:** "Primary A and Primary B are the same human (Sybil attack)"

**Example:**
- Charlie has Primary (0x111)
- Charlie creates new address, gets ANOTHER Primary (0x222) ← FRAUD
- Community member notices patterns, challenges: `challengeDuplicate(0x111, 0x222)`
- High-stakes investigation, 80% consensus required
- If proven → 0x222 demoted, Charlie slashed, challengers rewarded
- If false accusation → Challenger slashed

**Why hardest?** Severe penalty, requires strong evidence. Prevents frivolous challenges.

---

## WEIGHTED VOTING MECHANISM

### Vote Weights by Tier

| Tier | Weight | Rationale |
|------|--------|-----------|
| Grey Ghost | 0 | Cannot vouch (unverified) |
| Linked ID | 0.5 (Phase 2) | Maybe light vouching weight later |
| Primary ID | 1 | Base weight for verified humans |
| Oracle | 100 | Massively amplified (simulates external oracles like Worldcoin) |

**External Oracles (Phase 2):**
- Worldcoin API → adapter contract → vouch as Oracle-weight
- PoH, BrightID, etc. → same pattern
- They participate through same consensus interface

**Key insight:** Oracles aren't a separate system. They're just accounts with huge voting weight. This keeps the protocol unified.

---

## STAKING ECONOMICS

### Why Staking?

Without skin in the game, vouching is free → spam attacks. With staking:
- Correct vouches → get stake back + share of slashed stakes
- Incorrect vouches → lose stake
- Aligns incentives with honesty

### Stake Requirements

**Minimum stake to vouch:** 0.01 ETH (configurable via governance)

**Stake multiplier by claim type:**
- Link to Primary: 1x (0.01 ETH minimum)
- New Primary: 3x (0.03 ETH minimum)
- Duplicate Challenge: 10x (0.1 ETH minimum) ← expensive to prevent spam

### Reward/Slash Distribution

**When consensus resolves:**

```solidity
If claim APPROVED:
  - Vouchers who voted YES: Get stake back + (slashed NO stakes / YES count)
  - Vouchers who voted NO: Lose stake (slashed)

If claim REJECTED:
  - Vouchers who voted NO: Get stake back + (slashed YES stakes / NO count)
  - Vouchers who voted YES: Lose stake (slashed)
```

**Slash percentages (governance-controlled):**
- Link to Primary failure: 10% slash (low penalty)
- New Primary failure: 30% slash (medium penalty)
- Duplicate Challenge wrong: 50% slash (high penalty, discourages frivolous challenges)
- Duplicate Challenge correct (defender): 100% slash (severe penalty for Sybil attackers)

---

## PROTOCOL PARAMETERS (Governance-Controlled)

All critical values are **configurable** via KNO token holder voting:

```solidity
struct GovernanceParams {
    // Consensus thresholds (basis points, 10000 = 100%)
    uint256 linkThreshold;           // Default: 51%
    uint256 primaryThreshold;        // Default: 67%
    uint256 duplicateThreshold;      // Default: 80%

    // Staking
    uint256 minStakeWei;             // Default: 0.01 ETH
    uint256 primaryStakeMultiplier;  // Default: 3x
    uint256 duplicateStakeMultiplier; // Default: 10x

    // Slashing (basis points)
    uint256 linkSlashBps;            // Default: 1000 (10%)
    uint256 primarySlashBps;         // Default: 3000 (30%)
    uint256 duplicateSlashBps;       // Default: 5000 (50%)
    uint256 sybilSlashBps;           // Default: 10000 (100%)

    // Voting weights
    uint256 primaryVoteWeight;       // Default: 1
    uint256 oracleVoteWeight;        // Default: 100

    // Cooldowns (in seconds)
    uint256 failedClaimCooldown;     // Default: 7 days
    uint256 duplicateFlagCooldown;   // Default: 30 days

    // Oracle decay (meritocracy over time)
    uint256 oracleDecayRateBps;      // Default: 10 bps/day (0.1%/day)
    uint256 adminDecayRateBps;       // Default: 50 bps/day (0.5%/day)
}
```

**God Mode (Testing Only):**
```solidity
address public godMode;              // Can warp time, grant Oracle status
bool public godModeActive;           // True in v1, false when renounced
uint256 public timeWarpSeconds;      // Fast-forward time for testing

function renounceGodMode() external onlyGod {
    godModeActive = false;
    godMode = address(0);            // Permanent, irreversible
}
```

---

## SMART CONTRACT ARCHITECTURE

### Contract 1: IdentityRegistry.sol

**Purpose:** State management for identities

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "@openzeppelin/contracts/access/Ownable.sol";

contract IdentityRegistry is Ownable {
    enum IdentityTier {
        GreyGhost,      // 0: Unverified
        LinkedID,       // 1: Secondary identity
        PrimaryID,      // 2: Blue Checkmark (UBI recipient)
        Oracle          // 3: High-weight verifier
    }

    struct Identity {
        IdentityTier tier;
        address primaryAddress;              // If Linked, points to Primary
        mapping(string => address) linkedIds; // Flexible platform mapping
        string[] linkedPlatforms;            // Track platform names
        uint256 verifiedAt;
        uint256 totalVouchesReceived;
        uint256 totalStakeReceived;
        bool underChallenge;                 // Currently being investigated
        uint256 challengeId;                 // Active duplicate challenge
        uint256 oracleDecayStart;            // When Oracle decay began
    }

    mapping(address => Identity) public identities;

    // Events
    event IdentityVerified(address indexed addr, IdentityTier tier, uint256 timestamp);
    event IdentityLinked(address indexed secondary, address indexed primary, string platform);
    event IdentityUpgraded(address indexed addr, IdentityTier from, IdentityTier to);
    event IdentityDowngraded(address indexed addr, IdentityTier from, IdentityTier to);
    event IdentityChallenged(address indexed addr, uint256 challengeId);

    // Core functions
    function getIdentity(address addr) external view returns (Identity memory);
    function getTier(address addr) external view returns (IdentityTier);
    function isPrimary(address addr) external view returns (bool);
    function isOracle(address addr) external view returns (bool);
    function getLinkedAddresses(address primary) external view returns (string[] memory platforms, address[] memory addresses);
    function getPrimaryAddress(address linked) external view returns (address);

    // State changes (called by IdentityConsensus only)
    function upgradeToLinked(address addr, address primary, string calldata platform) external onlyConsensus;
    function upgradeToPrimary(address addr) external onlyConsensus;
    function upgradeToOracle(address addr) external onlyAdmin;
    function downgradeIdentity(address addr, IdentityTier newTier) external onlyConsensus;
    function markUnderChallenge(address addr, uint256 challengeId) external onlyConsensus;
    function clearChallenge(address addr) external onlyConsensus;
}
```

---

### Contract 2: IdentityConsensus.sol

**Purpose:** Voting, staking, consensus resolution

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "@openzeppelin/contracts/security/ReentrancyGuard.sol";
import "@openzeppelin/contracts/security/Pausable.sol";
import "./IdentityRegistry.sol";
import "./GovernanceParameters.sol";

contract IdentityConsensus is ReentrancyGuard, Pausable {
    IdentityRegistry public registry;
    GovernanceParameters public params;

    enum ClaimType {
        LinkToPrimary,      // Claim secondary ID
        NewPrimary,         // Claim unique human
        DuplicateFlag       // Challenge existing Primary as duplicate
    }

    enum ClaimStatus {
        Active,
        Approved,
        Rejected,
        Expired
    }

    struct IdentityClaim {
        uint256 claimId;
        ClaimType claimType;
        ClaimStatus status;
        address subject;            // Address claiming/being challenged
        address relatedAddress;     // Primary (for Link) or duplicate (for Flag)
        string platform;            // Platform name for LinkedID (flexible string)
        string justification;       // Justification for claim (e.g., "business account")
        uint256 createdAt;
        uint256 expiresAt;
        uint256 totalVotesFor;      // Weighted votes
        uint256 totalVotesAgainst;  // Weighted votes
        uint256 totalStake;
        bool resolved;
    }

    struct Vouch {
        address voucher;
        bool supports;              // true = FOR, false = AGAINST
        uint256 weight;             // Calculated at vouch time
        uint256 stake;
        uint256 vouchedAt;
    }

    mapping(uint256 => IdentityClaim) public claims;
    mapping(uint256 => Vouch[]) public vouchesOnClaim;
    mapping(address => uint256[]) public claimsByAddress;
    mapping(address => uint256) public lastFailedClaim; // Cooldown tracking

    uint256 public nextClaimId = 1;

    // Events
    event ClaimCreated(uint256 indexed claimId, ClaimType claimType, address indexed subject, string justification);
    event VouchCast(uint256 indexed claimId, address indexed voucher, bool supports, uint256 weight, uint256 stake);
    event ConsensusReached(uint256 indexed claimId, bool approved);
    event StakeSlashed(uint256 indexed claimId, address indexed voucher, uint256 amount);
    event RewardClaimed(uint256 indexed claimId, address indexed voucher, uint256 amount);

    // Core functions
    function requestLinkToPrimary(address primary, string calldata platform, string calldata justification)
        external payable returns (uint256 claimId);

    function requestPrimaryVerification(string calldata justification)
        external payable returns (uint256 claimId);

    function challengeDuplicate(address addr1, address addr2, string calldata evidence)
        external payable returns (uint256 claimId);

    function vouchFor(uint256 claimId) external payable nonReentrant whenNotPaused;
    function vouchAgainst(uint256 claimId) external payable nonReentrant whenNotPaused;

    function resolveConsensus(uint256 claimId) external nonReentrant returns (bool approved);
    function claimRewards(uint256 claimId) external nonReentrant;

    // View functions
    function getClaim(uint256 claimId) external view returns (IdentityClaim memory);
    function getVouches(uint256 claimId) external view returns (Vouch[] memory);
    function getCurrentConsensus(uint256 claimId) external view returns (uint256 percentFor, uint256 percentAgainst);
    function canVouch(address voucher, uint256 claimId) external view returns (bool, string memory reason);

    // Internal logic
    function _calculateVoteWeight(address voucher) internal view returns (uint256);
    function _validateStake(ClaimType claimType, uint256 stake) internal view returns (bool);
    function _checkConsensusThreshold(IdentityClaim memory claim) internal view returns (bool reached, bool approved);
    function _distributeRewards(uint256 claimId, bool approved) internal;
    function _slashStakes(uint256 claimId, bool approved) internal;
}
```

---

### Contract 3: GovernanceParameters.sol

**Purpose:** Configurable protocol parameters

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "@openzeppelin/contracts/access/AccessControl.sol";

contract GovernanceParameters is AccessControl {
    bytes32 public constant GOVERNANCE_ROLE = keccak256("GOVERNANCE_ROLE");
    bytes32 public constant GOD_MODE_ROLE = keccak256("GOD_MODE_ROLE");

    // Consensus thresholds (basis points, 10000 = 100%)
    uint256 public linkThreshold = 5100;           // 51%
    uint256 public primaryThreshold = 6700;        // 67%
    uint256 public duplicateThreshold = 8000;      // 80%

    // Staking
    uint256 public minStakeWei = 0.01 ether;
    uint256 public primaryStakeMultiplier = 3;
    uint256 public duplicateStakeMultiplier = 10;

    // Slashing (basis points)
    uint256 public linkSlashBps = 1000;            // 10%
    uint256 public primarySlashBps = 3000;         // 30%
    uint256 public duplicateSlashBps = 5000;       // 50%
    uint256 public sybilSlashBps = 10000;          // 100%

    // Voting weights
    uint256 public primaryVoteWeight = 1;
    uint256 public oracleVoteWeight = 100;

    // Cooldowns (seconds)
    uint256 public failedClaimCooldown = 7 days;
    uint256 public duplicateFlagCooldown = 30 days;
    uint256 public claimExpiryDuration = 30 days;

    // Decay rates (basis points per day)
    uint256 public oracleDecayRateBps = 10;        // 0.1%/day
    uint256 public adminDecayRateBps = 50;         // 0.5%/day

    // God mode (testing)
    bool public godModeActive = true;
    uint256 public timeWarpSeconds = 0;

    constructor() {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _grantRole(GOVERNANCE_ROLE, msg.sender);
        _grantRole(GOD_MODE_ROLE, msg.sender);
    }

    function getCurrentTime() external view returns (uint256) {
        return block.timestamp + timeWarpSeconds;
    }

    function warpTime(uint256 seconds_) external onlyRole(GOD_MODE_ROLE) {
        require(godModeActive, "God mode disabled");
        timeWarpSeconds += seconds_;
    }

    function renounceGodMode() external onlyRole(GOD_MODE_ROLE) {
        godModeActive = false;
        revokeRole(GOD_MODE_ROLE, msg.sender);
    }

    // Governance setters (only callable by KNO token holders via voting)
    function setThresholds(uint256 link, uint256 primary, uint256 duplicate)
        external onlyRole(GOVERNANCE_ROLE) {
        require(link >= 5100 && link <= 10000, "Invalid link threshold");
        require(primary >= 5100 && primary <= 10000, "Invalid primary threshold");
        require(duplicate >= 5100 && duplicate <= 10000, "Invalid duplicate threshold");
        linkThreshold = link;
        primaryThreshold = primary;
        duplicateThreshold = duplicate;
    }

    // Additional setters for all other parameters...
}
```

---

## PHASE 2: PRODUCT & REPUTATION LAYER (ARCHITECTURE)

### Contract 4: ProductRegistry.sol (Phase 2)

**Purpose:** Products with fractional ownership

```solidity
struct Product {
    address productAddress;          // Derived from URL hash or ENS
    address[] owners;                 // Fractional ownership (NO LIMIT)
    uint256[] ownershipBps;           // Basis points (must sum to 10000)
    uint256 totalReputationReceived;
    address mintingOracle;            // Who verified ownership (if oracle-minted)
    bool selfClaimed;                 // True if self-claimed, false if oracle-minted
    uint256 mintedAt;
    string metadata;                  // URL, IPFS hash, description
    ProductType productType;
}

enum ProductType {
    CreativeWork,    // Blog posts, videos, art, music
    PhysicalProduct, // Amazon listings, Etsy items
    Service,         // Consulting, businesses, SaaS
    Burner          // Temporary URLs, disposable entities
}
```

**Key Features:**
- **No owner limit** (user pays gas for multi-owner distributions)
- **Mutable ownership** via 67% owner consensus
- **Oracle-minted OR self-claimed** mechanisms

### Contract 5: ReputationDistribution.sol (Phase 2)

**Purpose:** Reputation flow to products and owners

**Mode 1: Auto-Split by Ownership (Default)**
```solidity
function giveReputationToProduct(address product, uint256 amount) external {
    Product storage p = products[product];

    for (uint i = 0; i < p.owners.length; i++) {
        uint256 ownerShare = (amount * p.ownershipBps[i]) / 10000;
        _transferReputation(p.owners[i], ownerShare);
    }

    emit ReputationGivenToProduct(product, msg.sender, amount);
}
```

**Mode 2: Custom Directed to Specific Owner (Override)**
```solidity
function giveReputationToProductOwner(
    address product,
    address targetOwner,
    uint256 amount
) external {
    Product storage p = products[product];
    require(_isOwner(p, targetOwner), "Not an owner");

    _transferReputation(targetOwner, amount);

    emit DirectedReputationGiven(product, targetOwner, msg.sender, amount);
}
```

---

## DESKTOP CLIENT: "NINTENDO" AESTHETIC

### What It Is

A **native macOS desktop app** (Kotlin Compose Multiplatform) that makes interacting with the identity protocol feel like playing a retro 8-bit game.

### What It's NOT

- Not an educational simulation
- Not separate from the real protocol
- Not a toy

**Every action in the UI is a real blockchain transaction.**

---

### UI Architecture

**Main Screen: 2D Identity Arena**

```
┌─────────────────────────────────────────────────────────────┐
│  IDENTITY CITY - Sepolia Testnet                  [⚙️ Menu] │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│     🟦 Alice (Primary)                  🟩 Oracle_1         │
│       ├─ 💼 LinkedIn                       Weight: 100      │
│       ├─ 💼 LinkedIn-biz                                    │
│       ├─ 📷 Instagram                                       │
│       └─ 🐦 Twitter                                         │
│                                                              │
│           ⬜ Bob (Grey Ghost)                                │
│              [Requesting Primary...]                        │
│                   67% ████████░░                            │
│                                                              │
│     🟦 Charlie (Primary)                🟩 Oracle_2         │
│       └─ 🎮 Discord                        Weight: 100      │
│                                                              │
│                                                              │
│  [Your Avatar: 0x742d...] - Primary ID                      │
│  Daily Knomee: 100 | Reputation: 1,247                      │
└─────────────────────────────────────────────────────────────┘
```

**Visual Language:**
- **Grey squares (⬜):** Grey Ghosts (unverified)
- **Blue squares (🟦):** Primary IDs (Blue Checkmarks)
- **Green squares (🟩):** Oracles (high-weight)
- **Small icons:** Linked IDs (social accounts) connected to Primaries
- **Progress bars:** Live consensus status on active claims
- **Lines/beams:** When you vouch, a line draws from your avatar to target

---

### Menu System (8-Bit Style)

```
┌─────────────────────────────────────┐
│   IDENTITY CONSENSUS PROTOCOL       │
├─────────────────────────────────────┤
│  ▶ REQUEST VERIFICATION             │
│    VIEW ACTIVE CLAIMS                │
│    VOUCH ON CLAIM                    │
│    CHALLENGE DUPLICATE               │
│    MY LINKED IDS                     │
│    ORACLE DASHBOARD (if Oracle)     │
│    SETTINGS                          │
│    EXIT                              │
└─────────────────────────────────────┘
```

---

### NPC Simulation (Testing Protocol Dynamics)

**Purpose:** Test the protocol without real users

**NPC Behaviors:**
1. **Honest Primaries:** Vouch accurately, never Sybil
2. **Honest Oracles:** High-weight, accurate voting
3. **Sybil Attackers:** Try to claim multiple Primaries
4. **Vigilant Challengers:** Detect and flag duplicates
5. **Random Voters:** Noisy signal, test threshold robustness

**Simulation Scenarios:**
- 10 NPCs request Primary verification
- 2 NPCs are secretly the same entity (Sybil test)
- Oracles + community vote
- Does consensus correctly identify the duplicate?
- Are stakes distributed fairly?

**God Mode Controls:**
```
[Simulation]
  Speed: [1x] [10x] [100x]
  Spawn NPC: [Primary] [Oracle] [Sybil]
  Warp Time: +7 days

[Results]
  Sybil Detection Rate: 95%
  False Positive Rate: 2%
  Average Time to Consensus: 3.2 days
```

---

## PHASE ROADMAP

### Phase 1 (Weeks 1-4): Identity Consensus Core

**Week 1: Smart Contracts**
- Day 1-2: Write IdentityRegistry.sol
- Day 3-4: Write IdentityConsensus.sol
- Day 5: Write GovernanceParameters.sol
- Day 6-7: Unit tests + deployment scripts

**Week 2: Desktop Client Foundation**
- Day 8-9: Kotlin Compose Desktop scaffold
- Day 10-11: Web3 integration (web3j, Sepolia)
- Day 12-13: Basic 2D canvas rendering
- Day 14: Menu system framework

**Week 3: NPC Simulation + Testing**
- Day 15-16: NPC behavior scripts
- Day 17-18: God mode controls
- Day 19-20: Simulation analytics dashboard
- Day 21: Integration testing

**Week 4: Polish + Documentation**
- Day 22-23: 8-bit pixel art assets
- Day 24-25: Sound effects, visual polish
- Day 26-27: Documentation, video demo
- Day 28: Final testing, prepare for launch

**Success Criteria:**
- ✅ Can simulate 100 identity claims
- ✅ Correctly detects Sybil attacks >90%
- ✅ False positive rate <5%
- ✅ Consensus resolves in reasonable time

---

### Phase 2 (Weeks 5-8): Reputation Layer

**Builds on Phase 1:**
- ✅ ProductRegistry.sol (fractional ownership, mutable governance)
- ✅ ReputationDistribution.sol (auto-split + override)
- ✅ Desktop client updates (products as visual nodes)
- ✅ Oracle-minting AND self-claim mechanisms
- ✅ Integrate with Phase 1 identity system
- ✅ Daily Knomee distribution for Primaries

---

### Phase 3 (Future): Meritocratic Transition

**Governance takeover:**
- KNO token holders vote on parameters
- Oracle status becomes earn-able (not admin-granted)
- God mode fully renounced
- Admin powers decay to zero

---

## TESTING STRATEGY

### Unit Tests (Foundry)
- ✅ IdentityRegistry state transitions
- ✅ Consensus threshold calculations
- ✅ Stake slashing and reward distribution
- ✅ Governance parameter changes
- ✅ God mode time warp
- ✅ Cooldown enforcement
- ✅ Flexible platform linking (string-based)
- ✅ Multiple accounts per platform handling

### Integration Tests
- ✅ Full claim lifecycle (create → vouch → resolve)
- ✅ Multi-claim scenarios (competing claims)
- ✅ Sybil attack detection
- ✅ Oracle weight amplification
- ✅ Parameter changes mid-claim

### Simulation Tests (Desktop Client)
- ✅ 100 NPC primary requests
- ✅ 10% Sybil attacker rate
- ✅ Oracle vs. community voting dynamics
- ✅ Consensus convergence time
- ✅ Economic incentive alignment

---

## SECURITY CONSIDERATIONS

### Attack Vectors

**1. Sybil Attack (Primary Duplication)**
- **Attack:** One person claims multiple Primaries
- **Defense:** 67% consensus threshold + duplicate challenges + stake slashing
- **Residual Risk:** Low if oracles are honest, medium if oracles collude

**2. Collusion Attack**
- **Attack:** Group of users vouch for each other's fake Primaries
- **Defense:** Oracle high-weight votes can override, stake slashing
- **Residual Risk:** Medium, depends on oracle integrity

**3. Griefing Attack**
- **Attack:** Spam false duplicate challenges to harass users
- **Defense:** 10x stake requirement, 80% consensus, 50% slash for false challenges
- **Residual Risk:** Low, expensive to grief

**4. Oracle Corruption**
- **Attack:** Oracle abuses high voting weight
- **Defense:** Oracle decay over time, governance can remove oracles, multiple oracles required
- **Residual Risk:** Medium in early phase, low as system matures

**5. Front-Running**
- **Attack:** Bot watches mempool, copies vouches
- **Defense:** Not profitable (stake required), doesn't affect consensus
- **Residual Risk:** Low

---

## SUCCESS METRICS

**Protocol Viability:**
- ✅ Sybil detection rate >90%
- ✅ False positive rate <5%
- ✅ Average consensus time <7 days (real time)
- ✅ Stake economics aligned (honest vouchers profit)

**Technical Performance:**
- ✅ Desktop client runs 60 FPS
- ✅ Sepolia transactions confirm in <30 seconds
- ✅ God mode time warp functions correctly
- ✅ 100+ NPCs simulated simultaneously

**User Experience:**
- ✅ Can request Primary verification in <2 clicks
- ✅ Can vouch on claim in <3 clicks
- ✅ Visual consensus progress updates live
- ✅ 8-bit aesthetic feels cohesive

---

## DEPLOYMENT PLAN

### Sepolia Testnet (Phase 1)
- Deploy IdentityRegistry.sol
- Deploy GovernanceParameters.sol
- Deploy IdentityConsensus.sol
- Verify contracts on Etherscan
- Initialize with admin oracles

### Polygon Mainnet (After Testing)
- Full security audit
- Renounce god mode
- Community governance transition
- KNO token distribution

---

## CONCLUSION

This is **not** an educational game about identity. This is a **real protocol** for decentralized Sybil resistance, with a beautiful UI.

The innovation: **Weighted social consensus** where oracles are just heavily-weighted votes, not a separate system. Economic incentives (staking) align behavior. Governance makes it adaptable.

The "Nintendo" aesthetic is sugar. The smart contracts are the game. Game theory is the design.

**We're building the identity layer for Knomee's UBI distribution.**

Let's ship it.

---

**Document Version:** 1.0 (Complete Architecture)
**Authors:** Joseph Malone + Claude Code
**Date:** October 24, 2025
**Status:** Ready for Implementation
**Next Action:** Begin Phase 1 Week 1 - Smart Contract Development

---
