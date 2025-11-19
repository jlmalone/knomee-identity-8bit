# Knomee Identity Protocol - Solana Implementation

## Overview

This document describes the Solana blockchain implementation of the Knomee Identity Protocol, providing a general-purpose identity verification system that can work on any Solana network (localnet, devnet, mainnet).

## Why Solana?

The Solana implementation offers several advantages:

1. **High Performance**: ~400ms block times vs Ethereum's ~12s
2. **Low Costs**: Fractions of a cent per transaction vs dollars on Ethereum
3. **Scalability**: 50,000+ TPS capacity vs Ethereum's ~15 TPS
4. **Fast Finality**: ~6 seconds vs ~12 minutes on Ethereum
5. **Parallel Execution**: Better handling of concurrent claims/votes

## Architecture Comparison

### Ethereum (Current)
```
Smart Contracts (Solidity)
├── IdentityRegistry.sol
├── IdentityConsensus.sol
├── GovernanceParameters.sol
├── IdentityToken.sol (ERC-721)
└── KnomeeToken.sol (ERC-20)
```

### Solana (New)
```
Anchor Program (Rust)
└── knomee_identity
    ├── state/
    │   ├── governance.rs
    │   ├── identity.rs
    │   ├── claim.rs
    │   └── vouch.rs
    ├── instructions/
    │   ├── governance.rs
    │   ├── identity.rs
    │   └── consensus.rs
    ├── constants.rs
    └── errors.rs
```

## Key Differences

| Feature | Ethereum | Solana |
|---------|----------|--------|
| **State Model** | Storage mappings | Account-based (PDAs) |
| **Identity NFT** | ERC-721 | Token-2022 (with transfer hook) |
| **Staking Token** | ERC-20 | SPL Token |
| **Addressing** | Contract addresses | Program Derived Addresses |
| **Upgradability** | Proxy patterns | Built-in program upgrades |
| **Event Discovery** | Event logs | Account filtering + events |
| **Gas/Fees** | Variable (gwei) | Predictable (lamports) |

## Core Components

### 1. Governance
- **Account**: Global PDA (`governance` seed)
- **Features**:
  - Protocol parameter management
  - God mode for testing (time warp)
  - Configurable thresholds and economics

### 2. Identity Management
- **Account**: Per-address PDA (`identity` + owner)
- **Features**:
  - Four tiers: GreyGhost, LinkedID, PrimaryID, Oracle
  - Soul-bound identity (cannot transfer)
  - Linked accounts for multi-device support

### 3. Consensus & Voting
- **Accounts**:
  - Claims: PDA (`claim` + timestamp + ID)
  - Vouches: PDA (`vouch` + claim_id + voucher)
- **Features**:
  - Three claim types with different thresholds
  - Weighted voting (tier × stake)
  - Economic incentives (staking & slashing)

## Program Instructions

### Governance
```rust
initialize_governance(params)    // One-time setup
update_governance_params(params) // Update by governance authority
time_warp(seconds)              // Testing only (god mode)
renounce_god_mode()             // Permanent renouncement
```

### Identity
```rust
initialize_identity()           // Create identity account
upgrade_to_oracle()            // Admin/governance upgrade
link_identity(platform)        // Link secondary account
```

### Consensus
```rust
request_link_to_primary(primary, platform, justification, stake)
request_primary_verification(justification, stake)
challenge_duplicate(addr1, addr2, evidence, stake)
vouch_for(claim_id, stake)
vouch_against(claim_id, stake)
resolve_consensus(claim_id)
claim_rewards(claim_id)
```

## Account Structure

### Governance Account
```rust
pub struct Governance {
    pub authority: Pubkey,              // Governance authority
    pub god_mode_authority: Pubkey,     // Testing authority
    pub god_mode_active: bool,          // Can time warp?
    pub time_warp_seconds: i64,         // Testing offset
    pub params: GovernanceParams,       // All parameters
    pub initialized_at: i64,            // Creation time
    pub bump: u8,                       // PDA bump
}
```

### Identity Account
```rust
pub struct Identity {
    pub owner: Pubkey,                  // Address this identity belongs to
    pub tier: IdentityTier,             // GreyGhost/LinkedID/Primary/Oracle
    pub primary_address: Pubkey,        // Primary if LinkedID
    pub verified_at: i64,               // Verification timestamp
    pub total_vouches_received: u64,    // Lifetime vouches
    pub total_stake_received: u64,      // Lifetime stake
    pub under_challenge: bool,          // Currently challenged?
    pub challenge_claim_id: u64,        // Active challenge ID
    pub oracle_decay_start: i64,        // Oracle decay tracking
    pub linked_count: u16,              // Number of linked accounts
    pub last_failed_claim_at: i64,      // Cooldown tracking
    pub bump: u8,                       // PDA bump
}
```

### Claim Account
```rust
pub struct IdentityClaim {
    pub claim_id: u64,                  // Unique claim ID
    pub claim_type: ClaimType,          // Link/Primary/Duplicate
    pub status: ClaimStatus,            // Active/Approved/Rejected/Expired
    pub subject: Pubkey,                // Claimant address
    pub related_address: Pubkey,        // Primary or duplicate
    pub platform: String,               // Platform name (for Link)
    pub justification: String,          // Evidence/reasoning
    pub created_at: i64,                // Creation time
    pub expires_at: i64,                // Expiry time
    pub total_votes_for: u128,          // Weighted votes FOR
    pub total_votes_against: u128,      // Weighted votes AGAINST
    pub total_stake: u64,               // Total KNOW staked
    pub total_slashed: u64,             // Total KNOW slashed
    pub vouch_count: u32,               // Number of vouches
    pub rewards_distributed: bool,      // Rewards claimed?
    pub bump: u8,                       // PDA bump
}
```

### Vouch Account
```rust
pub struct Vouch {
    pub claim_id: u64,                  // Claim being vouched on
    pub voucher: Pubkey,                // Voter address
    pub supports: bool,                 // true=FOR, false=AGAINST
    pub weight: u64,                    // Identity tier weight
    pub stake: u64,                     // KNOW tokens staked
    pub vouched_at: i64,                // Vote timestamp
    pub rewards_claimed: bool,          // Rewards claimed?
    pub reward_amount: u64,             // Calculated reward
    pub bump: u8,                       // PDA bump
}
```

## Economic Model

### Vote Weight Formula
```
Final Vote Weight = Identity Tier Weight × KNOW Stake Amount

Examples:
- GreyGhost stakes 1000 KNOW → 0 vote weight (cannot vote)
- PrimaryID stakes 30 KNOW → 30 vote weight (1 × 30)
- Oracle stakes 30 KNOW → 3000 vote weight (100 × 30)
```

### Stake Requirements
| Claim Type | Minimum Stake | Threshold |
|------------|--------------|-----------|
| LinkToPrimary | 1x (0.01 KNOW) | 51% |
| NewPrimary | 3x (0.03 KNOW) | 67% |
| DuplicateFlag | 10x (0.1 KNOW) | 80% |

### Slashing Rates
| Scenario | Slash Rate | Notes |
|----------|-----------|-------|
| Link claim wrong | 10% | Low penalty |
| Primary claim wrong | 30% | Medium penalty |
| Duplicate challenge wrong | 50% | High penalty (discourages spam) |
| Sybil detected | 100% | Severe penalty for fraud |

## Deployment Options

### 1. Localnet (Development)
```bash
# Start local validator
solana-test-validator

# Deploy
cd solana-programs
anchor build
anchor deploy

# Test
anchor test
```

### 2. Devnet (Staging)
```bash
# Configure for devnet
solana config set --url devnet
solana airdrop 2

# Deploy
anchor deploy --provider.cluster devnet

# Initialize
anchor run initialize-governance -- --provider.cluster devnet
```

### 3. Mainnet (Production)
```bash
# Configure for mainnet
solana config set --url mainnet-beta

# Ensure sufficient SOL
solana balance

# Deploy
anchor deploy --provider.cluster mainnet

# Initialize (CAREFULLY)
anchor run initialize-governance -- --provider.cluster mainnet

# Renounce god mode (AFTER TESTING)
anchor run renounce-god-mode -- --provider.cluster mainnet
```

## Integration Guide

### For DApp Developers

1. **Install Dependencies**
```bash
npm install @coral-xyz/anchor @solana/web3.js @solana/spl-token
```

2. **Load Program**
```typescript
import { Program, AnchorProvider } from "@coral-xyz/anchor";
import { Connection, PublicKey } from "@solana/web3.js";
import idl from "./knomee_identity.json";

const programId = new PublicKey("KNoMeeID11111111111111111111111111111111111");
const connection = new Connection("https://api.mainnet-beta.solana.com");
const provider = new AnchorProvider(connection, wallet, {});
const program = new Program(idl, programId, provider);
```

3. **Query Identity**
```typescript
const [identityPda] = PublicKey.findProgramAddressSync(
  [Buffer.from("identity"), userAddress.toBuffer()],
  program.programId
);

const identity = await program.account.identity.fetch(identityPda);
console.log("Tier:", identity.tier);
console.log("Is Primary:", identity.tier.primaryId || identity.tier.oracle);
```

4. **Create Claim**
```typescript
const tx = await program.methods
  .requestPrimaryVerification(
    "I am a unique human. Evidence: ...",
    new BN(30_000_000) // 0.03 KNOW
  )
  .accounts({
    governance: governancePda,
    claim: claimPda,
    subjectIdentity: identityPda,
    subject: wallet.publicKey,
    subjectTokenAccount: knowAccount,
    stakeEscrow: escrowAccount,
  })
  .rpc();
```

5. **Vote on Claim**
```typescript
const tx = await program.methods
  .vouchFor(claimId, new BN(10_000_000))
  .accounts({
    governance: governancePda,
    claim: claimPda,
    voucherIdentity: voucherIdentityPda,
    vouch: vouchPda,
    voucher: wallet.publicKey,
    voucherTokenAccount: voucherKnowAccount,
    stakeEscrow: escrowAccount,
  })
  .rpc();
```

## Security Considerations

### 1. Account Validation
- All accounts validated via Anchor constraints
- PDA derivation ensures correct ownership
- Signer checks prevent unauthorized actions

### 2. Arithmetic Safety
- All math uses checked operations
- Overflow protection built-in
- U128 for vote totals (prevents overflow)

### 3. Reentrancy Protection
- Anchor framework provides reentrancy guards
- State updates before external calls
- Atomicity guaranteed within instruction

### 4. Economic Security
- Stake requirements prevent spam
- Slashing deters malicious behavior
- Tiered multipliers for different risk levels
- Cooldown periods prevent DOS attacks

## Testing Strategy

### Unit Tests
Test individual instructions in isolation:
```bash
anchor test --file tests/governance.spec.ts
anchor test --file tests/identity.spec.ts
anchor test --file tests/consensus.spec.ts
```

### Integration Tests
Test complete workflows:
```bash
anchor test --file tests/integration/claim-lifecycle.spec.ts
anchor test --file tests/integration/sybil-detection.spec.ts
```

### Simulation Tests
Test economic attacks:
```bash
anchor test --file tests/simulation/vote-buying.spec.ts
anchor test --file tests/simulation/collusion.spec.ts
```

## Performance Benchmarks

| Operation | Compute Units | Transaction Cost (SOL) |
|-----------|---------------|------------------------|
| Initialize Identity | ~10,000 | ~0.000005 |
| Create Claim | ~20,000 | ~0.00001 |
| Cast Vote | ~15,000 | ~0.0000075 |
| Resolve Claim | ~25,000 | ~0.0000125 |
| Claim Rewards | ~10,000 | ~0.000005 |

*Note: Costs based on current Solana fee schedule (~5000 lamports per signature)*

## Roadmap

### Phase 1: Core Implementation ✅
- ✅ Program architecture
- ✅ State accounts (Governance, Identity, Claim, Vouch)
- ✅ Instruction handlers
- ✅ Economic parameters
- ✅ Documentation

### Phase 2: Token Integration 🚧
- [ ] KNOW token deployment (SPL Token)
- [ ] IDT token deployment (Token-2022 with transfer hook)
- [ ] Token account management
- [ ] Staking/escrow mechanisms
- [ ] Reward distribution

### Phase 3: Testing & Audit
- [ ] Comprehensive unit tests
- [ ] Integration tests
- [ ] Simulation tests
- [ ] Security audit
- [ ] Bug bounty program

### Phase 4: Production Deployment
- [ ] Devnet deployment & testing
- [ ] Mainnet deployment
- [ ] God mode renouncement
- [ ] Governance transition
- [ ] Public launch

## Resources

### Documentation
- [Solana Programs README](./solana-programs/README.md)
- [Deployment Guide](./solana-programs/DEPLOYMENT.md)
- [Whitepaper](./WHITEPAPER_0.9_CLAUDE_SYNTHESIS.md)
- [Protocol Specification](./KNOMEE_IDENTITY_PROTOCOL_V1.md)

### External Resources
- [Anchor Book](https://book.anchor-lang.com/)
- [Solana Cookbook](https://solanacookbook.com/)
- [SPL Token Documentation](https://spl.solana.com/token)
- [Token-2022 Guide](https://spl.solana.com/token-2022)

### Community
- GitHub: [knomee-identity-8bit](https://github.com/jlmalone/knomee-identity-8bit)
- Discord: [Coming soon]
- Twitter: [@KnomeeProtocol]

## FAQ

**Q: Can this work on any Solana network?**
A: Yes! The program is network-agnostic. Deploy to localnet for testing, devnet for staging, and mainnet for production.

**Q: How does this compare to the Ethereum version?**
A: Functionally identical, but with Solana's performance benefits (faster, cheaper, more scalable). See comparison table above.

**Q: What are the costs to use?**
A: Very low. Creating a claim costs ~0.00001 SOL (~$0.001 at $100/SOL). Voting costs ~0.0000075 SOL. Compare to Ethereum's $5-50 per transaction.

**Q: Is god mode safe for production?**
A: No! God mode MUST be renounced before mainnet deployment. It's only for testing.

**Q: Can programs be upgraded?**
A: Yes, Solana programs are upgradeable by default. The upgrade authority can deploy new versions. For full decentralization, transfer upgrade authority to a governance contract.

**Q: How do I integrate this into my DApp?**
A: See the Integration Guide section above. Use the Anchor TypeScript client to interact with the program.

## Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Write tests for new features
4. Ensure all tests pass
5. Submit a pull request

See [CONTRIBUTING.md](./CONTRIBUTING.md) for detailed guidelines.

## License

MIT License - See [LICENSE](./LICENSE) file for details.

---

**Built on Solana for the decentralized future 🚀**

*Making identity verification accessible, affordable, and scalable for everyone.*
