# Knomee Identity Protocol - Tokenomics Design

## Core Principle
**Reputation cannot be bought, only earned.**

## The Problem with ETH-based Staking
- ❌ Plutocratic: Rich wallets dominate voting
- ❌ Sybil vulnerable: Anyone can buy influence
- ❌ No identity coupling: Money ≠ Verification
- ❌ Misaligned incentives: Economic power over truth

## Two-Token Architecture

### 1. Identity Token (IDT) - Soul-Bound NFT
**Purpose**: Proof of verified unique identity

**Properties**:
- **ERC-721** soul-bound (non-transferable)
- **Minted** only when Primary ID claim approved
- **Unique** per verified human
- **Voting weight** encoded in metadata:
  - GreyGhost: 0 weight (no token)
  - LinkedID: 0 weight (secondary account)
  - PrimaryID: 1 weight
  - Oracle: 100 weight

**Key Functions**:
```solidity
function mint(address account, IdentityTier tier) // Only IdentityRegistry
function getVotingWeight(address account) returns (uint256)
function upgradeToOracle(address account) // Only governance
function revoke(address account) // Only for duplicate detection
```

**Cannot**:
- Be transferred
- Be sold
- Be bought
- Be borrowed

### 2. Knomee Token (KNOW) - Staking & Governance
**Purpose**: Economic alignment and reputation incentive

**Properties**:
- **ERC-20** standard token
- **Required** to stake on claims
- **Slashed** for incorrect votes
- **Rewarded** for correct votes
- **Earned** through protocol participation
- **Transferable** (can be traded)

**Initial Distribution**:
- 40% - Protocol rewards pool
- 30% - Community treasury (governance)
- 20% - Early contributors / team (vested)
- 10% - Liquidity provision

**Earning Mechanisms**:
- ✅ Successful identity verification: 100 KNOW
- ✅ Correct vouching: Proportional rewards
- ✅ Oracle duties: 10 KNOW per resolved claim
- ✅ Early adoption bonus: 2x rewards first 6 months

**Slashing Mechanisms**:
- ❌ Wrong vote on approved claim: -30% of stake
- ❌ Wrong vote on rejected claim: -10% of stake
- ❌ Duplicate identity detected: -100% of all KNOW

## Voting Requirements

### To Vote on Claims:
1. **Must have** Identity Token (IDT) with weight > 0
2. **Must stake** minimum KNOW tokens:
   - Link claims: 10 KNOW
   - Primary claims: 30 KNOW
   - Duplicate challenges: 50 KNOW
3. **Pay** gas fees in ETH (blockchain requirement)

### Vote Weight Calculation:
```
Final Vote Weight = Identity Weight × KNOW Stake
```

**Examples**:
- Primary ID (weight=1) stakes 30 KNOW → 30 vote weight
- Oracle (weight=100) stakes 30 KNOW → 3,000 vote weight
- GreyGhost (weight=0) stakes 1000 KNOW → 0 vote weight (can't vote)

## Economic Security

### Sybil Resistance:
- ✅ Identity tokens can't be bought
- ✅ Must verify through consensus to get IDT
- ✅ Each human gets exactly 1 Primary ID token
- ✅ KNOW stake requirement adds economic cost

### Plutocracy Resistance:
- ✅ Voting weight capped by identity tier
- ✅ Oracle = max 100x multiplier (not infinite)
- ✅ Rich wallets must still verify identity
- ✅ Money alone cannot dominate

### Game Theory:
**For Honest Actors**:
- Low KNOW stake → Low reward potential
- High KNOW stake → High reward potential
- Correct votes → Earn more KNOW → Compound returns

**For Malicious Actors**:
- Need verified identity first (hard)
- Risk losing KNOW stake (expensive)
- Slashed tokens burned (permanent loss)
- Cannot buy way to Oracle status

## Comparison to Worldcoin

| Feature | Worldcoin | Knomee |
|---------|-----------|--------|
| Identity Proof | Orb scan | Consensus voting |
| Identity Token | World ID (non-transferable) | IDT (soul-bound NFT) |
| Utility Token | WLD | KNOW |
| Can buy identity? | ❌ No | ❌ No |
| Can buy voting power? | ❌ No | ❌ No |
| Staking uses ETH? | ❌ No | ❌ No |
| Decentralized verification? | ❌ No (Orb) | ✅ Yes (consensus) |

## Migration from Current System

### Phase 1: Deploy Tokens
1. Deploy IdentityToken.sol
2. Deploy KnomeeToken.sol
3. Mint initial KNOW supply

### Phase 2: Airdrop to Existing Users
1. All verified Primary IDs get:
   - 1 Identity Token (IDT)
   - 1000 KNOW tokens (early adopter bonus)
2. All Oracles get:
   - 1 Oracle-tier Identity Token
   - 5000 KNOW tokens

### Phase 3: Update Consensus Contract
1. Require IDT to vote
2. Replace ETH stakes with KNOW stakes
3. Implement KNOW slashing/rewards

### Phase 4: Frontend Update
1. Display IDT and KNOW balances
2. Show "Acquire KNOW" if insufficient
3. Update staking UI to use KNOW
4. Add token swap interface (KNOW ↔ ETH)

## Security Considerations

### Smart Contract Risks:
- ✅ IDT cannot be transferred (prevent token theft)
- ✅ KNOW slashing is time-delayed (prevent flash attacks)
- ✅ Oracle upgrades require governance vote
- ✅ Token minting controlled by Registry contract only

### Economic Attack Vectors:
1. **KNOW price manipulation**: Mitigated by deep liquidity
2. **Whale accumulation**: Mitigated by identity weight caps
3. **Collusion**: Detected through voting pattern analysis
4. **Bribery**: Cannot transfer IDT, so limited leverage

## Conclusion

This two-token system ensures:
- **Identity** is earned through verification (IDT)
- **Reputation** is proven through correct behavior (KNOW)
- **Money** cannot buy verification or voting power
- **Alignment** between identity, reputation, and economics

The protocol becomes **Sybil-resistant**, **plutocracy-resistant**, and **incentive-aligned**.
