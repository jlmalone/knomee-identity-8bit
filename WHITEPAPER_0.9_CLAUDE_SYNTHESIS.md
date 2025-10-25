# **Knomee Protocol**

## **A Synthesis: Decentralized Identity & Character Reputation**

**Version 0.9 - Claude Synthesis**

*Synthesizing insights from parallel development branches by Rene-Lee Sylvain, Joseph Malone, Bryan Hellard, and Claude (Anthropic)*

---

## **Preface: Two Branches, One Vision**

This document represents a synthesis of two parallel intellectual branches of the Knomee protocol:

1. **The Gemini Branch (v0.9 RC1)**: A comprehensive whitepaper outlining the full vision for a dual-layer reputation system (character + identity)
2. **The Claude Branch (Implementation)**: A working protocol implementation focusing on the identity verification layer with precise game-theoretic mechanics

Both branches emerged from the same core insight: **"Reputation cannot be bought, only earned."** This synthesis preserves the visionary scope of the Gemini branch while incorporating the technical refinements and implementation discoveries of the Claude branch.

---

## **Executive Summary**

Knomee is a decentralized protocol for establishing **verifiable identity** and **measurable character** in digital spaces. The protocol operates on two distinct but interconnected layers:

### **Layer 1: Identity Trust (IMPLEMENTED)**
A soul-bound NFT system where users prove their unique humanity through stake-weighted consensus. Identity cannot be transferred, bought, or sold. The protocol uses a dual-token model:
- **Identity Token (IDT)**: Non-transferable NFT representing verified unique identity
- **Vouch Token (VT/KNOW)**: Stakeable ERC-20 token used to vouch for others' identities

### **Layer 2: Character Reputation (PROPOSED)**
A daily energy system where users build nuanced, multi-faceted reputation by exchanging weighted character tokens. Users receive a daily allotment of "Knomee" energy which they invest in others through positive/negative reputation tokens that naturally decay, requiring ongoing maintenance.

### **The Justice System**
A permissionless, economically-driven system where any participant can challenge suspected identity fraud by staking VT. Asymmetric outcomes (massive rewards for correct accusations, brutal slashing for false ones) create a self-correcting immune system without requiring privileged authority roles.

**Core Innovation**: The protocol separates **who you are** (identity) from **what you're worth** (character), preventing plutocracy while maintaining economic alignment. Vote weight is calculated as: **Identity Tier × Stake Amount**, ensuring wealth amplifies but cannot override identity verification.

---

## **1. The Identity Trust Layer (Layer 1)**

### **1.1 The Fundamental Rule**

**One Human, One Validated Identity.**

Creating multiple validated identities is the protocol's cardinal sin, as it constitutes theft of influence from the entire community. This rule is enforced through cryptographic soul-binding and economic game theory, not centralized authority.

### **1.2 Identity Tiers**

The protocol recognizes four distinct identity states:

| Tier | Voting Weight | Token Type | Capabilities |
|------|---------------|------------|--------------|
| **GreyGhost** | 0 | None | Can receive reputation, cannot vote or vouch |
| **LinkedID** | 0 | Soul-bound NFT | Verified secondary account of a Primary ID |
| **PrimaryID** | 1 | Soul-bound NFT | Full voting rights, can vouch for others |
| **Oracle** | 100 | Soul-bound NFT | Trusted verifier, 100× voting weight |

**Key Innovation (Claude Branch)**: The **LinkedID** tier solves the "multiple devices, one human" problem. A user can link verified secondary accounts (e.g., work laptop, mobile, gaming PC) to their Primary ID without fragmenting their reputation or creating Sybil vulnerabilities.

### **1.3 Dual-Token Economics**

#### **Identity Token (IDT) - The "Vespene Gas"**

- **Type**: ERC-721 Soul-bound NFT
- **Transferability**: None (overrides all transfer functions)
- **Minting**: Only by IdentityRegistry when consensus reached
- **Purpose**: Proves unique human identity
- **Voting Weight**: Encoded in tier (0, 1, or 100)

```solidity
// Soul-bound implementation prevents all transfers
function _update(address to, uint256 tokenId, address auth) internal override {
    address from = _ownerOf(tokenId);
    if (from != address(0) && to != address(0)) {
        require(!transfersDisabled, "Identity tokens are soul-bound");
    }
    return super._update(to, tokenId, auth);
}
```

#### **Vouch Token (VT/KNOW) - The "Minerals"**

- **Type**: ERC-20 Standard Token
- **Total Supply**: 1,000,000,000 KNOW
- **Transferability**: Full (can be bought/sold)
- **Purpose**: Economic stake for vouching and governance
- **Distribution**:
  - 40% - Protocol rewards pool (self-custodied by contract)
  - 30% - Community treasury (DAO governance)
  - 20% - Team/early contributors (vested)
  - 10% - Liquidity provision (DEX)

**The Starcraft Resource Model**: Like Starcraft's economy, you need both resources:
- **Minerals (KNOW)**: Abundant, tradeable, required for all operations
- **Vespene Gas (IDT)**: Scarce, earned, unlocks advanced capabilities
- **Supply Cap (Tier Weight)**: Prevents infinite scaling through money alone

### **1.4 Vote Weight Formula**

The protocol's anti-plutocracy mechanism is mathematically precise:

```
Final Vote Weight = Identity Tier Weight × KNOW Stake Amount
```

**Examples**:
- GreyGhost stakes 10,000 KNOW → **0 vote weight** (no identity token)
- PrimaryID stakes 30 KNOW → **30 vote weight**
- Oracle stakes 30 KNOW → **3,000 vote weight**

**Key Insight (Claude Branch)**: This formula ensures:
1. **Identity Required**: Without IDT, infinite KNOW = 0 power
2. **Stake Matters**: Among verified identities, economic alignment still matters
3. **Oracle Advantage**: Bounded at 100×, not infinite
4. **Plutocracy Resistant**: A billionaire with GreyGhost status has zero power

### **1.5 Pathways to Validation**

#### **Path 1: Oracle Bridge (Centralized → Decentralized)**

A specialized Oracle verifies control over multiple Web2 identities:
- Government-issued ID (passport, driver's license)
- Established LinkedIn profile (500+ connections, 2+ years old)
- Verified social media accounts
- Phone number, email verification

**Oracle Incentive**: 10 KNOW per successful verification (minted from rewards pool)

**Implementation Status**: ✅ Smart contract ready, Oracle verification logic pending

#### **Path 2: Consensus Web of Trust (Fully Decentralized)**

Existing Primary IDs vouch for new users by staking KNOW tokens:

1. **Claim Creation**: GreyGhost requests Primary ID verification, stakes 30 KNOW
2. **Vouching Period**: 7 days for community to vouch FOR or AGAINST
3. **Weighted Voting**: Each vouch carries weight = Voter's Identity Tier × KNOW Staked
4. **Consensus Threshold**: 67% weighted support required
5. **Resolution**:
   - **Approved**: Claimant becomes Primary ID, receives 100 KNOW reward, vouchers get proportional rewards
   - **Rejected**: Claimant loses 30% of stake, enters 30-day cooldown

**Key Refinement (Claude Branch)**: Platform-specific linking allows flexible secondary accounts:

```solidity
function requestLinkToPrimary(
    address primary,
    string calldata platform,      // "GitHub-work", "LinkedIn-personal"
    string calldata justification,  // Evidence of ownership
    uint256 knowStake               // 10 KNOW minimum
)
```

### **1.6 Consensus Mechanics**

#### **Three Claim Types with Graduated Thresholds**

| Claim Type | Threshold | Min Stake | Expiry | Cooldown on Failure |
|------------|-----------|-----------|--------|---------------------|
| **LinkToPrimary** | 51% | 10 KNOW | 7 days | 7 days |
| **NewPrimary** | 67% | 30 KNOW | 7 days | 30 days |
| **DuplicateFlag** | 80% | 50 KNOW | 14 days | 90 days |

**Rationale (Claude Branch)**:
- **LinkToPrimary**: Simple majority (low risk)
- **NewPrimary**: Supermajority (creating new identity power)
- **DuplicateFlag**: High bar (severe accusation)

#### **Time-Based Protections**

```solidity
// Prevents spam attacks
uint256 public constant CLAIM_EXPIRY = 7 days;
uint256 public constant FAILED_CLAIM_COOLDOWN = 30 days;
uint256 public constant DUPLICATE_FLAG_COOLDOWN = 90 days;
```

**Why this matters**: The Gemini whitepaper's permissionless contests could be vulnerable to denial-of-service attacks. Time-based cooldowns prevent spam while maintaining openness.

#### **Event-Driven Claim Discovery**

**Challenge**: How do voters discover active claims without centralized indexing?

**Solution (Claude Branch)**: Ethereum event logs with efficient filtering:

```kotlin
class EventListener {
    suspend fun getAllActiveClaims(lookbackBlocks: Long = 1000): List<BigInteger> {
        val claimEvents = getRecentClaims(fromBlock)
        return claimEvents.map { it.claimId }.filter { isActive(it) }
    }
}
```

Users query the last 1,000 blocks (~3 hours) for `ClaimCreated` events, filter to active claims, display in UI. Fully decentralized, no indexer required.

### **1.7 Slashing & Rewards**

#### **Incorrect Vote Slashing**

| Claim Type | Vote | Outcome | Slash Rate |
|------------|------|---------|------------|
| LinkToPrimary | FOR | REJECTED | 10% |
| LinkToPrimary | AGAINST | APPROVED | 10% |
| NewPrimary | FOR | REJECTED | 30% |
| NewPrimary | AGAINST | APPROVED | 10% |
| DuplicateFlag | FOR | REJECTED | 30% |
| DuplicateFlag | AGAINST | APPROVED | 100% (Sybil detected) |

**Slashing Implementation**:
```solidity
function slash(address account, uint256 amount, string calldata reason) external {
    totalSlashed += amount;
    _burn(account, amount);  // Tokens permanently destroyed
    emit TokensSlashed(account, amount, reason);
}
```

#### **Correct Vote Rewards**

```solidity
function mintVotingReward(address voter, uint256 baseReward) external {
    uint256 reward = baseReward;
    if (block.timestamp < launchTimestamp + EARLY_ADOPTER_PERIOD) {
        reward = reward * 2;  // 2× first 6 months
    }
    _transfer(rewardsPool, voter, reward);
    emit RewardMinted(voter, reward, "Correct vote");
}
```

**Reward Calculation**: Proportional share of slashed stakes from incorrect voters.

### **1.8 Early Adopter Incentives**

**Cold Start Problem**: New protocols need initial participants to reach critical mass.

**Solution (Claude Branch)**:
```solidity
uint256 public constant EARLY_ADOPTER_MULTIPLIER = 2;
uint256 public constant EARLY_ADOPTER_PERIOD = 180 days;
```

First 6 months:
- Primary ID verification reward: **200 KNOW** (normally 100)
- Oracle verification reward: **20 KNOW** (normally 10)
- Voting rewards: **2× proportional share**

After 6 months, multiplier drops to 1×, creating urgency for early participation.

---

## **2. The Character Reputation Layer (Layer 2 - PROPOSED)**

### **2.1 Daily Knomee Energy**

**Philosophical Foundation (Gemini Branch)**: Character reputation should be based on **ongoing commitment**, not past wealth. Daily energy forces users to continually demonstrate good faith.

#### **The Allotment System**

Every validated identity receives at 00:00 UTC:
```
Daily Knomee = 100 (base) + Reputation Weight Bonus + Character Ratio Bonus
```

**Key Properties**:
- **Non-transferable**: Cannot be bought, sold, or gifted
- **Non-accumulating**: Unused Knomee expires at midnight
- **Earned through character**: Bonuses based on received reputation

#### **Reputation Weight Bonus**

Users who have strong positive reputation receive bonus Knomee:
```
Weight Bonus = log₁₀(Total Positive Token Weight) × 10
```

Example: A user with 1,000 points of positive reputation receives +30 Knomee/day.

#### **Character Ratio Bonus**

Users with high positive/negative ratios receive bonus Knomee:
```
Ratio Bonus = log₁₀(Positive Tokens / Negative Tokens) × 20
```

Example: A user with 100 positive, 5 negative receives +26 Knomee/day.

**Combined**: A well-regarded user could earn 100 + 30 + 26 = **156 Knomee/day**.

### **2.2 Reputation Tokens**

#### **The Token Library**

All reputation tokens come from a DAO-controlled library of paired opposites:

| Positive | Negative | Weight Range | Decay Rate |
|----------|----------|--------------|------------|
| Honest | Dishonest | 1-50 Knomee | 1 Knomee/2 days |
| Reliable | Unreliable | 1-50 Knomee | 1 Knomee/2 days |
| Kind | Unkind | 1-50 Knomee | 1 Knomee/2 days |
| Competent | Incompetent | 1-50 Knomee | 1 Knomee/2 days |
| Responsive | Unresponsive | 1-50 Knomee | 1 Knomee/2 days |

**Negative tokens decay faster**:
- Positive: 1 Knomee/2 days
- Negative: 20 Knomee/1 day

**Rationale**: Forgiveness should be easier than condemnation. A negative judgment requires ongoing commitment to maintain.

#### **Core Functions**

```solidity
function giveToken(
    address recipient,
    string calldata tokenType,  // From library
    uint256 knomeeAmount,       // 1-50 Knomee
    string calldata context     // Why you're giving this
) external {
    require(knomeeAmount <= dailyBalance[msg.sender], "Insufficient Knomee");
    require(knomeeAmount <= 50, "Max 50 per token");

    dailyBalance[msg.sender] -= knomeeAmount;

    tokens[recipient][tokenType].push(Token({
        giver: msg.sender,
        weight: knomeeAmount,
        timestamp: block.timestamp,
        context: context
    }));
}

function topUpToken(
    address recipient,
    uint256 tokenIndex,
    uint256 knomeeAmount
) external {
    // Prevents decay by adding fresh Knomee
}

function retractToken(
    address recipient,
    uint256 tokenIndex
) external {
    // Removes token entirely (with cooldown)
}
```

#### **Stack Dynamics**

Tokens of the same type form "stacks" on a user's profile:
- Newest tokens have highest weight
- Older tokens decay naturally
- Total stack weight = sum of all active tokens of that type

**Example Profile**:
```
Alice's Reputation
├─ Honest: 127 points (8 active tokens)
├─ Reliable: 94 points (5 active tokens)
├─ Kind: 63 points (4 active tokens)
└─ Dishonest: 12 points (2 active tokens, decaying rapidly)
```

### **2.3 Integration with Identity Layer**

The two layers work in tandem:

#### **Identity → Character**
- **GreyGhost**: Can receive tokens but cannot give (no daily Knomee)
- **LinkedID**: Cannot give tokens (no daily Knomee)
- **PrimaryID**: Receives 100+ Knomee daily, can give/receive
- **Oracle**: Receives 100+ Knomee daily, can give/receive

#### **Character → Identity**
- High character ratio users earn bonus VT emission
- Strong positive reputation required to become Oracle candidate
- Character reputation displayed on identity verification UI

**Synthesis Point**: A user's **identity proves who they are**, their **character proves how they behave**.

---

## **3. The Justice System: Permissionless Identity Contests**

### **3.1 Core Principle**

**"A Participant is a Participant"**

There is no privileged "Watchdog" class. Any user—from a new Primary ID to the most powerful Oracle—can initiate a contest. Their influence is determined solely by the amount of VT they're willing to risk.

**Evolution (Gemini Branch)**: v0.8 had privileged Watchdog roles. v0.9 eliminates this in favor of emergent behavior from economic incentives.

**Refinement (Claude Branch)**: Implemented as `challengeDuplicate()` function with specific mechanics.

### **3.2 Initiating a Contest**

```solidity
function challengeDuplicate(
    address addr1,                  // First alleged duplicate
    address addr2,                  // Second alleged duplicate
    string calldata evidence,       // IPFS hash of evidence
    uint256 knowStake              // Prosecution stake (min 50 KNOW)
) external returns (uint256 contestId) {
    require(knowStake >= 50 * 10**18, "Minimum 50 KNOW stake");
    require(registry.isPrimary(addr1), "addr1 not primary");
    require(registry.isPrimary(addr2), "addr2 not primary");

    knomeeToken.transferFrom(msg.sender, address(this), knowStake);

    contestId = _createContest(addr1, addr2, evidence);

    // Mark both accounts as "Under Challenge"
    registry.markUnderChallenge(addr1, contestId);
    registry.markUnderChallenge(addr2, contestId);

    emit ContestInitiated(contestId, msg.sender, addr1, addr2, knowStake);
}
```

**Under Challenge Status**:
- Cannot create new claims
- Cannot vote on other claims
- Can still receive character reputation
- Daily Knomee allotment continues

### **3.3 The Contest Period**

**Duration**: 14 days (extended from normal 7-day claims due to severity)

**Voting Mechanics**: Standard weighted consensus
```
Vote Weight = Identity Tier × KNOW Stake
```

**Threshold**: 80% weighted support required to convict

**Evidence Presentation**:
- Accusers post evidence to IPFS
- Defenders post counter-evidence
- Community reviews and votes
- Oracles provide final verdict

### **3.4 Asymmetric Outcomes**

#### **Outcome 1: GUILTY (Sybil Detected)**

**For the Fraudulent Accounts**:
1. **Immediate Downgrade**: Both accounts → GreyGhost status
2. **Identity Revocation**: Soul-bound tokens burned
3. **Permanent Mark**: `fraudHistory[account] = contestId`
4. **Debt Creation**:
   ```
   Debt = 2× (Total Knomee Received + Total VT Received + DAO Governance Tokens)
   ```
5. **Asset Seizure**: All KNOW tokens transferred to bounty pool

**For the Accusers**:
1. **Stake Returned**: Full prosecution stake refunded
2. **Massive Bounty**: Target **50× stake** from:
   - Seized fraudulent assets
   - Protocol treasury
   - Minted VT (if insufficient funds)
3. **Early Bird Multiplier**:
   ```
   Multiplier = 1 / log₁₀(accuser_rank + 1)
   ```
   - First accuser: 50× base bounty
   - Second accuser: ~33× base bounty
   - Third accuser: ~25× base bounty

**Example**:
- Alice stakes 50 KNOW, accuses Bob/Charlie (first accuser)
- Contest resolved GUILTY after 14 days
- Alice receives: 50 KNOW (refund) + 2,500 KNOW (50× bounty) = **2,550 KNOW**
- Bob/Charlie each lose all assets, marked as frauds, owe debt before redemption

#### **Outcome 2: INNOCENT (False Accusation)**

**"Slash, Compensate, and Burn" Mechanism**

This three-step process prevents collusion:

1. **Slash**: Entire prosecution stake destroyed
   ```solidity
   uint256 stake = contests[contestId].prosecutionStake;
   ```

2. **Compensate**: 50% to innocent victims
   ```solidity
   uint256 compensation = stake / 2;
   knomeeToken.transfer(addr1, compensation / 2);
   knomeeToken.transfer(addr2, compensation / 2);
   ```

3. **Burn**: 50% permanently destroyed
   ```solidity
   uint256 burnAmount = stake / 2;
   knomeeToken.burn(burnAmount);
   emit TokensBurned(burnAmount, "False accusation penalty");
   ```

**Why 50/50 split matters (Gemini Branch insight)**:

If 100% went to victims, two colluding accounts could:
1. Alice accuses Bob (false accusation)
2. Bob wins, receives Alice's stake
3. Bob transfers funds back to Alice off-chain
4. **Net result**: Free token transfer

With 50% burn:
1. Alice stakes 100 KNOW
2. Bob receives 50 KNOW compensation
3. 50 KNOW burned forever
4. **Net result**: Alice + Bob collectively lose 50 KNOW

**Collusion is unprofitable.**

### **3.5 Oracle Jury System**

**Question from Gemini Branch**: "What is the exact relationship between community vote and Oracle verdict?"

**Proposed Answer (Synthesis)**:

```solidity
struct ContestVerdict {
    uint256 communityVotePercent;  // 0-10000 basis points
    bool oracleVerdictGuilty;
    address[] oraclePanel;
    uint256 oracleSupermajority;   // Require 2/3
}

function finalizeContest(uint256 contestId) external {
    Contest storage contest = contests[contestId];

    // Step 1: Community vote is advisory
    uint256 communitySupport = calculateConsensus(contestId);

    // Step 2: Randomly select 7 Oracles
    address[] memory panel = selectRandomOracles(7);

    // Step 3: Oracles vote (weighted by their stake)
    uint256 oracleVotesGuilty = 0;
    uint256 totalOracleWeight = 0;

    for (uint i = 0; i < panel.length; i++) {
        uint256 weight = identityToken.getVotingWeight(panel[i]);
        totalOracleWeight += weight;
        if (oracleVotes[contestId][panel[i]] == Verdict.GUILTY) {
            oracleVotesGuilty += weight;
        }
    }

    // Step 4: Require 2/3 Oracle supermajority
    bool oracleVerdictGuilty = (oracleVotesGuilty * 3) >= (totalOracleWeight * 2);

    // Step 5: Oracle verdict is BINDING
    _resolveContest(contestId, oracleVerdictGuilty);
}
```

**Key Properties**:
- **Community vote informs** but does not decide
- **Oracle panel randomly selected** (prevents targeted collusion)
- **2/3 supermajority required** (no single Oracle kingmaker)
- **Oracle verdict is final** (binding resolution)

**Oracle Incentive**: 10 KNOW per contest resolution (win or lose, paid for judgment)

### **3.6 Redemption Arc for Fraudsters**

**Question from Gemini Branch**: "What is the concrete mechanism for debt repayment?"

**Proposed Answer (Synthesis)**:

```solidity
struct FraudDebt {
    uint256 principalOwed;
    uint256 amountPaid;
    uint256 createdAt;
}

mapping(address => FraudDebt) public fraudDebts;

function repayDebt(address debtor) external {
    FraudDebt storage debt = fraudDebts[debtor];
    require(debt.principalOwed > 0, "No debt");

    // Calculate repayment from altruistic tokens received
    uint256 altruisticValue = calculateAltruisticTokenValue(debtor);

    debt.amountPaid += altruisticValue;

    if (debt.amountPaid >= debt.principalOwed) {
        // Debt cleared, mark account as eligible for re-verification
        fraudDebts[debtor].principalOwed = 0;
        emit DebtCleared(debtor);
    }
}
```

**Repayment Sources**:
1. **Graph Gardening Tasks**: 1 KNOW per verified data update
2. **Altruistic Tokens**: Others can give "Second Chance" tokens
3. **Community Service**: DAO-approved tasks that generate value

**Path Back to Primary ID**:
1. GreyGhost with fraud mark receives altruistic tokens
2. Debt fully repaid (takes months/years)
3. Request Primary ID verification with **5× normal stake** (150 KNOW)
4. Requires **90% consensus** (higher bar than normal 67%)
5. If approved, fraud mark remains in history but identity restored

**Philosophy**: Redemption is possible but **extremely difficult**, preserving deterrence while allowing human fallibility.

---

## **4. Graph Gardening: Decentralized Data Maintenance**

### **4.1 The Problem**

Centralized systems employ data janitors. Decentralized systems must incentivize community maintenance.

**Examples of Graph Rot**:
- Phone numbers change
- Email addresses become invalid
- Social media handles updated
- LinkedIn profiles deleted

### **4.2 Micro-Task Rewards**

```solidity
function proposeGraphUpdate(
    address account,
    string calldata updateType,  // "phone", "email", "linkedin"
    string calldata oldValue,
    string calldata newValue,
    string calldata evidence     // IPFS hash
) external returns (uint256 proposalId) {
    proposals[proposalId] = GraphProposal({
        proposer: msg.sender,
        account: account,
        updateType: updateType,
        oldValue: oldValue,
        newValue: newValue,
        evidence: evidence,
        votesFor: 0,
        votesAgainst: 0,
        resolved: false
    });
}

function resolveGraphProposal(uint256 proposalId) external {
    GraphProposal storage prop = proposals[proposalId];
    require(prop.votesFor >= 3, "Need 3 confirmations");

    // Update graph
    registry.updateUserData(prop.account, prop.updateType, prop.newValue);

    // Reward proposer
    knomeeToken.mint(prop.proposer, 1 * 10**18);  // 1 KNOW

    emit GraphUpdated(proposalId, prop.account);
}
```

**Incentive Design**:
- Small reward (1 KNOW) prevents spam
- Requires 3 confirmations (prevents false updates)
- Low stakes keep barrier to entry minimal
- Cumulative rewards for dedicated gardeners

### **4.3 Emergent Curation**

Over time, certain users become known as diligent graph gardeners:
- High success rate on proposals
- Earn reputation tokens: "Diligent", "Detail-Oriented"
- Build trust for higher-stakes roles (Oracle candidate)

**Synthesis Point**: Graph gardening becomes a proving ground for character, feeding into Layer 2 reputation.

---

## **5. Technical Implementation Details**

### **5.1 Smart Contract Architecture**

```
┌─────────────────────────────────────────┐
│         Governance Layer                │
│  ┌─────────────────────────────────┐   │
│  │   GovernanceParameters.sol      │   │
│  │   - Thresholds, cooldowns       │   │
│  │   - God mode (dev only)         │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
               ▼
┌─────────────────────────────────────────┐
│         Token Layer                     │
│  ┌──────────────────┐  ┌─────────────┐ │
│  │ IdentityToken    │  │ KnomeeToken │ │
│  │ (IDT)            │  │ (VT/KNOW)   │ │
│  │ - Soul-bound     │  │ - ERC-20    │ │
│  │ - Tier weights   │  │ - Rewards   │ │
│  └──────────────────┘  └─────────────┘ │
└─────────────────────────────────────────┘
               ▼
┌─────────────────────────────────────────┐
│         Identity Layer                  │
│  ┌─────────────────────────────────┐   │
│  │   IdentityRegistry.sol          │   │
│  │   - Tier management             │   │
│  │   - Oracle upgrades             │   │
│  │   - Graph data storage          │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
               ▼
┌─────────────────────────────────────────┐
│         Consensus Layer                 │
│  ┌─────────────────────────────────┐   │
│  │   IdentityConsensus.sol         │   │
│  │   - Claim creation              │   │
│  │   - Weighted voting             │   │
│  │   - Slashing/rewards            │   │
│  │   - Contest resolution          │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
               ▼
┌─────────────────────────────────────────┐
│         Reputation Layer (Phase 2)      │
│  ┌─────────────────────────────────┐   │
│  │   ReputationEngine.sol          │   │
│  │   - Daily Knomee allotment      │   │
│  │   - Token giving/decay          │   │
│  │   - Bonus calculations          │   │
│  └─────────────────────────────────┘   │
│  ┌─────────────────────────────────┐   │
│  │   ReputationLibrary.sol         │   │
│  │   - Token type registry         │   │
│  │   - DAO governance              │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

### **5.2 Desktop Client Architecture**

**Technology Stack** (Claude Branch):
- **UI Framework**: Jetpack Compose Desktop
- **Language**: Kotlin
- **Blockchain**: Web3j for Ethereum interaction
- **Design Language**: Retro/NES aesthetic (8-bit, CRT effects)

**Why Desktop-First?** (Philosophical stance)
- Trustless infrastructure requires power-user tools first
- Mobile UX comes after protocol is proven
- Retro aesthetic signals "under construction, for builders"

**Key Components**:
```kotlin
MainScreen.kt           // Navigation and screen routing
IdentityStatusScreen    // View identity tier, stats
ClaimVerificationScreen // Create new claims
VouchSystemScreen       // View active claims
ActiveClaimsScreen      // Vote on claims with stake
MyVouchesScreen         // Vote history and outcomes
ClaimRewardsScreen      // Claim rewards from resolved claims
OraclePanelScreen       // Oracle-only functions

EventListener.kt        // Blockchain event monitoring
TransactionService.kt   // Web3j transaction handling
ContractRepository.kt   // Smart contract interaction
```

### **5.3 Development Tools**

**God Mode (Claude Branch)**:
```solidity
bool public godModeActive = true;

function timeWarp(uint256 secondsForward) external onlyOwner {
    require(godModeActive, "God mode disabled");
    timeOffset += secondsForward;
    emit TimeWarped(secondsForward, getCurrentTime());
}

function renounceGodMode() external onlyOwner {
    godModeActive = false;
    emit GodModeRenounced();
}
```

**Critical**: This MUST be disabled before mainnet deployment. Allows testing:
- Token decay (warp 2 days forward)
- Claim expiry (warp 7 days forward)
- Cooldown periods (warp 30 days forward)
- Early adopter period (warp 180 days forward)

### **5.4 Event System for Decentralized Discovery**

**Challenge**: How do clients discover active claims without a centralized indexer?

**Solution** (Claude Branch):
```kotlin
suspend fun getAllActiveClaims(lookbackBlocks: Long = 1000): List<BigInteger> {
    val currentBlock = web3j.ethBlockNumber().send().blockNumber
    val fromBlock = (currentBlock - BigInteger.valueOf(lookbackBlocks)).max(BigInteger.ZERO)

    // Query ClaimCreated events
    val filter = EthFilter(
        DefaultBlockParameterNumber(fromBlock),
        DefaultBlockParameterName.LATEST,
        consensusAddress
    )
    filter.addSingleTopic(EventEncoder.encode(CLAIM_CREATED_EVENT))

    val logs = web3j.ethGetLogs(filter).send().logs
    val claimIds = logs.map { parseClaimId(it) }

    // Filter to only active claims
    return claimIds.filter { isActive(it) }
}
```

**Performance**:
- 1,000 blocks ≈ 3 hours of history (Ethereum ~12s blocks)
- Typical query: <100ms
- No centralized indexer needed
- Fully verifiable (anyone can query events)

---

## **6. Economic Analysis & Game Theory**

### **6.1 Attack Vectors & Defenses**

#### **Attack 1: Sybil Identity Creation**

**Method**: Create many fake Primary IDs to dominate voting

**Defense**:
1. **Economic Cost**: Each Primary ID requires 30 KNOW stake + community approval
2. **Social Graph**: Requires existing Primary IDs to vouch (web of trust)
3. **Oracle Scrutiny**: High-stake claims attract Oracle attention
4. **Bounty Hunting**: 50× reward for detecting duplicates

**Outcome**: **Economically infeasible** beyond small scale

#### **Attack 2: Plutocratic Vote Buying**

**Method**: Wealthy actor buys massive KNOW supply, dominates votes

**Defense**:
```
Vote Weight = Identity Tier × KNOW Stake
```

Without verified identity (IDT), infinite KNOW = 0 power. Must first:
1. Pass consensus verification (community scrutiny)
2. Maintain reputation (ongoing character requirement)
3. Risk slashing on every vote (economic downside)

**Outcome**: **Money cannot override identity verification**

#### **Attack 3: Collusion for Stake Transfer**

**Method**: Alice falsely accuses Bob, Bob compensated, transfers back off-chain

**Defense**: **50% burn on false accusations**

**Math**:
- Alice stakes: 100 KNOW
- Alice loses (false accusation)
- Bob compensated: 50 KNOW
- Burned: 50 KNOW
- Net loss to pair: 50 KNOW

**Outcome**: **Collusion is unprofitable**

#### **Attack 4: Oracle Cartel**

**Method**: Oracles collude to control all contest verdicts

**Defense**:
1. **Random panel selection**: 7 Oracles chosen randomly per contest
2. **Supermajority required**: Need 2/3 agreement (5 of 7)
3. **Economic deterrence**: False verdicts slash Oracle reputation
4. **Permissionless contests**: Anyone can challenge Oracles as duplicates

**Outcome**: **Collusion requires coordinating 5 of 7 randomly selected Oracles**, prohibitively difficult

#### **Attack 5: Spam DoS**

**Method**: Create thousands of fake claims to overwhelm voters

**Defense**:
1. **Stake requirement**: 30 KNOW per claim
2. **Cooldown periods**: 30 days after failed claim
3. **Claim expiry**: Auto-fail after 7 days
4. **Slashing**: Lose 30% of stake on failed claims

**Outcome**: **Spam is expensive and self-limiting**

### **6.2 Virtuous Cycles**

#### **Cycle 1: Reputation Compounds**

```
Good behavior → Positive tokens → Bonus Knomee → More influence → More responsibility → Higher stakes → Greater caution → Better behavior
```

#### **Cycle 2: Oracle Emergence**

```
High character ratio → VT emission → Stake on claims → Successful verdicts → Oracle candidate → Oracle upgrade → 100× weight → Greater scrutiny → Higher standards
```

#### **Cycle 3: Network Effects**

```
More users → More vouching needed → More KNOW staked → Higher security → More trust → More adoption
```

### **6.3 Equilibrium Analysis**

**Long-term equilibrium** (5+ years):

**Identity Layer**:
- **Primary ID distribution**: 60% of active users
- **LinkedID distribution**: 30% of active users
- **Oracle distribution**: 0.1% of active users (1 in 1,000)
- **GreyGhost**: 9.9% (new users, fraudsters)

**KNOW Token**:
- **Circulating supply**: 600M (40% burned through slashing)
- **Rewards pool**: Depleted, new rewards from fees only
- **Median stake**: 50 KNOW per active user
- **Oracle holdings**: 10,000 KNOW median

**Character Reputation**:
- **Median daily Knomee**: 120 (base 100 + bonuses)
- **Active tokens per user**: 15-20
- **Top 1% daily Knomee**: 200+

---

## **7. Roadmap & Implementation Phases**

### **Phase 1: Identity Foundation** ✅ **COMPLETE**

**Deliverables**:
- [x] GovernanceParameters.sol with time controls
- [x] IdentityToken.sol (soul-bound NFT)
- [x] KnomeeToken.sol (VT/KNOW)
- [x] IdentityRegistry.sol (tier management)
- [x] IdentityConsensus.sol (weighted voting)
- [x] Desktop client with NES aesthetic
- [x] Event-based claim discovery
- [x] Slashing & rewards system

**Git Commit**: `851c85c` - "Implement two-token economic model for plutocracy resistance"

### **Phase 2: Character Reputation** (NEXT - Q2 2025)

**Deliverables**:
- [ ] ReputationEngine.sol (daily Knomee allotment)
- [ ] ReputationLibrary.sol (token type registry)
- [ ] Token decay mechanism
- [ ] `giveToken()`, `retractToken()`, `topUpToken()` functions
- [ ] Reputation weight & character ratio bonuses
- [ ] UI for giving/receiving character tokens
- [ ] Decay visualization & top-up reminders

**Estimated Complexity**: 3-4 weeks development

### **Phase 3: Advanced Contest System** (Q3 2025)

**Deliverables**:
- [ ] Oracle panel random selection
- [ ] Contest verdict system (community + Oracle)
- [ ] "Slash, Compensate, Burn" implementation
- [ ] Early Bird Multiplier math
- [ ] 50× bounty reward distribution
- [ ] Fraud debt tracking
- [ ] UI for contest evidence presentation

**Estimated Complexity**: 4-6 weeks development

### **Phase 4: Graph Gardening & Governance** (Q4 2025)

**Deliverables**:
- [ ] GraphProposal.sol (micro-task system)
- [ ] DAO governance for token library
- [ ] Parameter adjustment voting
- [ ] Fraud debt repayment tracking
- [ ] Redemption arc UI
- [ ] Community moderation tools

**Estimated Complexity**: 3-4 weeks development

### **Phase 5: Mobile & Production** (Q1 2026)

**Deliverables**:
- [ ] React Native mobile app
- [ ] Simplified UX (hide crypto complexity)
- [ ] Oracle verification mobile flow
- [ ] Push notifications for claims
- [ ] Mainnet deployment
- [ ] Security audits (Trail of Bits, OpenZeppelin)
- [ ] God mode disabled permanently

**Estimated Complexity**: 8-12 weeks development

### **Phase 6: Killer App Integration** (Q2 2026+)

**Potential Applications**:
- **P2P Lending**: "Trust Score" for Aave/Compound
- **Dating Apps**: Verified human + character proof
- **Remote Work**: Hiring verification for DAOs
- **Social Recovery**: Wallet recovery via trusted identities
- **DAO Governance**: Quadratic voting weighted by reputation

---

## **8. Open Questions & Research Directions**

### **8.1 Technical Questions**

1. **Oracle Selection Algorithm**:
   - Pure random vs. stake-weighted random?
   - On-chain randomness (Chainlink VRF) or commit-reveal?

2. **Cross-Chain Identity**:
   - Should IDT exist on multiple chains?
   - How to prevent double-claiming across chains?

3. **Privacy Preserving Reputation**:
   - Can we use zk-SNARKs for anonymous character tokens?
   - "Prove I have >100 'Honest' tokens without revealing from whom"

4. **Scalability**:
   - Layer 2 deployment (Optimism, Arbitrum)?
   - Vote aggregation for gas efficiency?

### **8.2 Economic Questions**

1. **KNOW Supply Dynamics**:
   - Should total supply be fixed or inflationary?
   - If inflationary, what's the emission curve?

2. **Bounty Sustainability**:
   - 50× bounty requires large treasury
   - What if treasury depleted? Mint new KNOW?

3. **Oracle Compensation**:
   - Is 10 KNOW per verdict sufficient long-term?
   - Should it scale with contest stake size?

4. **Decay Rate Optimization**:
   - Are current rates (1 Knomee/2 days) ideal?
   - Should decay accelerate for very old tokens?

### **8.3 Legal & Social Questions**

1. **GDPR Compliance**:
   - "Right to be forgotten" conflicts with immutable blockchain
   - Can we prove identity deletion while preserving graph integrity?

2. **Defamation Law**:
   - Is "Dishonest" token legally actionable?
   - Jurisdiction issues in decentralized system

3. **Oracle Liability**:
   - Are Oracles legally liable for false verdicts?
   - Insurance pools for Oracle errors?

4. **Cultural Adaptation**:
   - Western vs. Eastern concepts of "character"
   - How to make token library culturally neutral?

---

## **9. Comparison to Existing Systems**

### **9.1 vs. Worldcoin**

| Feature | Worldcoin | Knomee |
|---------|-----------|--------|
| **Identity Proof** | Orb iris scan | Consensus voting |
| **Verification** | Centralized hardware | Decentralized community |
| **Transferability** | Non-transferable | Non-transferable (IDT) |
| **Character Layer** | None | Full reputation system |
| **Plutocracy Risk** | Low | Very Low (dual-token) |
| **Sybil Resistance** | Very High (biometric) | High (stake + consensus) |
| **Privacy** | Biometric data concerns | On-chain transparency |
| **Censorship Resistance** | Medium (hardware dependency) | High (pure software) |

**Synthesis**: Worldcoin has stronger identity assurance (biometrics), Knomee has richer character data and full decentralization.

### **9.2 vs. Gitcoin Passport**

| Feature | Gitcoin Passport | Knomee |
|---------|------------------|--------|
| **Identity Method** | Web2 account linking | Stake-weighted consensus |
| **Score Type** | Binary stamps | Weighted reputation |
| **Transferability** | Non-transferable | Non-transferable (IDT) |
| **Sybil Resistance** | Medium (account age) | High (economic stake) |
| **Character Layer** | None | Full reputation tokens |
| **Use Case** | Quadratic funding | General-purpose identity |

**Synthesis**: Gitcoin focuses on one use case (QF), Knomee is foundational infrastructure.

### **9.3 vs. BrightID**

| Feature | BrightID | Knomee |
|---------|----------|--------|
| **Identity Method** | Video verification parties | Stake-weighted consensus |
| **Social Graph** | Connection graph | Character reputation graph |
| **Transferability** | Non-transferable | Non-transferable (IDT) |
| **Sybil Resistance** | High (social proof) | High (economic stake) |
| **Plutocracy Risk** | Very Low (no economics) | Very Low (dual-token) |
| **Incentive Model** | Altruistic | Economic + Altruistic |

**Synthesis**: BrightID relies on social incentives, Knomee adds economic alignment.

---

## **10. Success Metrics**

### **10.1 Phase 1 Success (Identity Layer)**

**Target by Q2 2025**:
- [ ] 1,000+ Primary IDs verified
- [ ] 100+ active claims per week
- [ ] <1% false positive rate on consensus
- [ ] 10+ Oracles actively participating
- [ ] 50,000+ KNOW tokens staked

### **10.2 Phase 2 Success (Character Layer)**

**Target by Q4 2025**:
- [ ] 5,000+ daily active users giving tokens
- [ ] 100,000+ character tokens exchanged
- [ ] Median 15 tokens per active user
- [ ] Top 10% users have 150+ daily Knomee

### **10.3 Phase 3 Success (Contests)**

**Target by Q1 2026**:
- [ ] 5+ successful fraud detections
- [ ] 0 false accusations (100% accuracy)
- [ ] Median bounty payout: 1,000+ KNOW
- [ ] <0.1% of identities flagged as frauds

### **10.4 Long-Term Success (Adoption)**

**Target by 2027**:
- [ ] 100,000+ verified identities
- [ ] Integration with 3+ major DApps
- [ ] $10M+ total value staked
- [ ] Recognized standard for Web3 identity

---

## **11. Conclusion**

### **The Synthesis**

This document merges two parallel explorations of the same fundamental problem: **How do we build trust in digital spaces without centralized authority?**

**The Gemini Branch** provided visionary scope:
- Daily energy systems
- Character reputation tokens
- Permissionless justice
- Redemption arcs

**The Claude Branch** provided technical precision:
- Soul-bound implementation
- Exact vote weight formula
- Event-based discovery
- Anti-collusion math

Together, they form a **complete protocol** that addresses both **who you are** (identity) and **how you behave** (character).

### **The Path Forward**

We stand at the completion of Phase 1: a working identity verification protocol with:
- ✅ Non-buyable reputation
- ✅ Sybil-resistant consensus
- ✅ Plutocracy-resistant voting
- ✅ Self-correcting slashing

The remaining phases build **character reputation** (Layer 2) and **advanced justice** (Layer 3) on this foundation.

### **The Ultimate Vision**

Knomee aims to be **foundational social infrastructure** for a digital world where trust is scarce. In a future of deepfakes, AI agents, and anonymous accounts, Knomee provides verifiable proof of:

1. **Identity**: "I am a unique human"
2. **Character**: "I am a trustworthy human"
3. **Continuity**: "I have been trustworthy over time"

This is not a social network. This is not a reputation platform. This is **social infrastructure** — the roads and bridges of digital trust.

The protocol's ultimate success will be measured not by user counts, but by **disappearance** — when Knomee verification becomes so ubiquitous that users no longer think about it, just as we no longer marvel at email or HTTPS.

That is the future we're building.

---

## **Appendix A: Key Differences from v0.9 RC1**

| Aspect | Gemini v0.9 RC1 | Claude Synthesis |
|--------|-----------------|------------------|
| **Token Names** | Knomee (daily) + VT (stake) | KNOW (stake) + IDT (identity) |
| **Implementation** | Conceptual | Phase 1 working code |
| **Identity Tiers** | GreyGhost, Blue Check | GreyGhost, LinkedID, Primary, Oracle |
| **Vote Formula** | Implied | Explicit: Tier × Stake |
| **Cooldowns** | Not specified | Precise: 7/30/90 days |
| **Contest Resolution** | Oracle binding vote | Oracle supermajority (2/3) |
| **Early Adopters** | Mentioned | Exact: 2× for 180 days |
| **God Mode** | Not mentioned | Dev tool, must disable |
| **Client** | Not specified | Desktop-first, retro UI |
| **Discovery** | Not specified | Event-driven, no indexer |

---

## **Appendix B: Smart Contract Addresses**

**Anvil Testnet (Local Development)**:
- GovernanceParameters: `0x5FbDB2315678afecb367f032d93F642f64180aa3`
- IdentityToken (IDT): `0xDc64a140Aa3E981100a9becA4E685f962f0cF6C9`
- KnomeeToken (KNOW): `0x5FC8d32690cc91D4c39d9d3abcBD16989F875707`
- IdentityRegistry: `0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512`
- IdentityConsensus: `0x9fE46736679d2D9a65F0992F2272dE9f3c7fa6e0`

**Production Deployment**: TBD (Q1 2026 target)

---

## **Appendix C: References & Acknowledgments**

**Intellectual Lineage**:
- Gemini (Google) - Whitepaper v0.9 RC1 conceptual framework
- Claude (Anthropic) - Implementation refinement and technical precision
- Vitalik Buterin - Quadratic voting, soul-bound tokens concepts
- Glen Weyl - Plural voting mechanisms
- Balaji Srinivasan - Network states, pseudonymous economy

**Team**:
- Rene-Lee Sylvain - Protocol design, game theory
- Joseph Malone - Implementation, smart contracts
- Bryan Hellard - Economics, tokenomics

**Repository**: https://github.com/jlmalone/knomee-identity-8bit

---

**End of Synthesis Document**

*"In a world of infinite identities, trust becomes the scarcest resource."*
