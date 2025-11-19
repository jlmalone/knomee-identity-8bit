# Knomee Identity Protocol - Solana Smart Contracts

## Overview

This directory contains the Solana blockchain implementation of the Knomee Identity Protocol - a decentralized identity verification system using weighted social consensus. The smart contracts are built using the Anchor framework for Solana.

## Architecture

The protocol consists of a single Anchor program (`knomee_identity`) with multiple modules:

### Core Modules

1. **Governance Module** (`state/governance.rs`, `instructions/governance.rs`)
   - Global protocol parameters
   - God mode for testing (time warp capabilities)
   - Configurable consensus thresholds and economic parameters

2. **Identity Module** (`state/identity.rs`, `instructions/identity.rs`)
   - Identity tier management (GreyGhost, LinkedID, PrimaryID, Oracle)
   - Identity state tracking
   - Oracle upgrades

3. **Consensus Module** (`state/claim.rs`, `state/vouch.rs`, `instructions/consensus.rs`)
   - Claim creation (LinkToPrimary, NewPrimary, DuplicateFlag)
   - Weighted voting with KNOW token staking
   - Consensus resolution and rewards distribution

## Identity Tiers

| Tier | Voting Weight | Description |
|------|---------------|-------------|
| **GreyGhost** | 0 | Unverified address, cannot vote |
| **LinkedID** | 0 | Secondary account linked to Primary |
| **PrimaryID** | 1 | Verified unique human (Blue Checkmark) |
| **Oracle** | 100 | High-trust verifier, 100x voting power |

## Claim Types & Thresholds

| Claim Type | Threshold | Min Stake | Description |
|------------|-----------|-----------|-------------|
| **LinkToPrimary** | 51% | 1x | Link secondary account to Primary |
| **NewPrimary** | 67% | 3x | Claim unique human status |
| **DuplicateFlag** | 80% | 10x | Challenge Sybil attack |

## Key Features

### 1. Soul-Bound Identity
- Identity NFTs cannot be transferred (implemented via account restrictions)
- One Primary ID per unique human
- LinkedIDs for multiple devices/accounts

### 2. Weighted Voting
```
Final Vote Weight = Identity Tier Weight × KNOW Stake Amount
```
- Prevents plutocracy (money alone cannot dominate)
- Amplifies trusted signals (Oracles have 100x weight)

### 3. Economic Security
- **Staking**: KNOW tokens required to vote
- **Slashing**: Incorrect votes lose 10-100% of stake
- **Rewards**: Correct votes share slashed stakes

### 4. Flexible Platform Support
- String-based platform names (future-proof)
- Multiple accounts per platform allowed with justification
- No hardcoded platform types

## Program Accounts

### Governance Account (PDA)
```rust
Seeds: [b"governance"]
```
Stores global protocol parameters and god mode settings.

### Identity Account (PDA)
```rust
Seeds: [b"identity", owner_pubkey]
```
Stores identity tier, stats, and challenge status for each address.

### IdentityClaim Account (PDA)
```rust
Seeds: [b"claim", governance_timestamp, claim_id]
```
Stores claim details, voting totals, and resolution status.

### Vouch Account (PDA)
```rust
Seeds: [b"vouch", claim_id, voucher_pubkey]
```
Stores individual vote with stake, weight, and rewards.

### LinkedIdentity Account (PDA)
```rust
Seeds: [b"linked_identity", primary_pubkey, platform]
```
Records link between Primary and secondary accounts.

## Development Setup

### Prerequisites

- [Rust](https://rustup.rs/) (latest stable)
- [Solana CLI](https://docs.solana.com/cli/install-solana-cli-tools) (v1.17+)
- [Anchor](https://www.anchor-lang.com/docs/installation) (v0.29.0)
- [Node.js](https://nodejs.org/) (v18+)

### Installation

```bash
# Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Install Solana CLI
sh -c "$(curl -sSfL https://release.solana.com/stable/install)"

# Install Anchor
cargo install --git https://github.com/coral-xyz/anchor avm --locked --force
avm install latest
avm use latest

# Clone repository
cd solana-programs
```

### Build

```bash
# Build the program
anchor build

# Get program ID
solana address -k target/deploy/knomee_identity-keypair.json
```

### Test

```bash
# Run tests (to be implemented)
anchor test

# Run tests with detailed logs
anchor test -- --nocapture
```

### Deploy

#### Localnet (Anchor)
```bash
# Start local validator
anchor localnet

# In another terminal, deploy
anchor deploy
```

#### Devnet
```bash
# Configure Solana CLI for devnet
solana config set --url devnet

# Airdrop SOL for deployment (if needed)
solana airdrop 2

# Deploy to devnet
anchor deploy --provider.cluster devnet
```

#### Mainnet
```bash
# IMPORTANT: Renounce god mode before mainnet deployment
# Configure for mainnet
solana config set --url mainnet-beta

# Deploy to mainnet
anchor deploy --provider.cluster mainnet
```

## Usage Examples

### 1. Initialize Governance

```typescript
const params = {
  linkThreshold: 5100,        // 51%
  primaryThreshold: 6700,     // 67%
  duplicateThreshold: 8000,   // 80%
  minStakeLamports: new BN(10_000_000), // 0.01 KNOW
  // ... other params
};

await program.methods
  .initializeGovernance(params)
  .accounts({
    governance: governancePda,
    authority: authority.publicKey,
  })
  .rpc();
```

### 2. Initialize Identity

```typescript
const [identityPda] = PublicKey.findProgramAddressSync(
  [Buffer.from("identity"), owner.toBuffer()],
  program.programId
);

await program.methods
  .initializeIdentity()
  .accounts({
    identity: identityPda,
    owner: owner,
    payer: payer.publicKey,
  })
  .rpc();
```

### 3. Request Primary Verification

```typescript
await program.methods
  .requestPrimaryVerification(
    "I am a unique human. Evidence: ...",
    new BN(30_000_000) // 0.03 KNOW (3x minimum)
  )
  .accounts({
    governance: governancePda,
    claim: claimPda,
    subjectIdentity: identityPda,
    subject: subject.publicKey,
    subjectTokenAccount: subjectKnowAccount,
    stakeEscrow: escrowAccount,
  })
  .rpc();
```

### 4. Vote on Claim

```typescript
// Vote FOR
await program.methods
  .vouchFor(claimId, new BN(10_000_000))
  .accounts({
    governance: governancePda,
    claim: claimPda,
    voucherIdentity: voucherIdentityPda,
    vouch: vouchPda,
    voucher: voucher.publicKey,
    voucherTokenAccount: voucherKnowAccount,
    stakeEscrow: escrowAccount,
  })
  .rpc();

// Vote AGAINST
await program.methods
  .vouchAgainst(claimId, new BN(10_000_000))
  .accounts({
    // same accounts as vouchFor
  })
  .rpc();
```

### 5. Resolve Claim

```typescript
await program.methods
  .resolveConsensus(claimId)
  .accounts({
    governance: governancePda,
    claim: claimPda,
    subjectIdentity: subjectIdentityPda,
  })
  .rpc();
```

### 6. Claim Rewards

```typescript
await program.methods
  .claimRewards(claimId)
  .accounts({
    claim: claimPda,
    vouch: vouchPda,
    voucher: voucher.publicKey,
    voucherTokenAccount: voucherKnowAccount,
    stakeEscrow: escrowAccount,
  })
  .rpc();
```

## Economic Parameters

All parameters are governance-controlled and can be updated via on-chain voting:

```rust
pub struct GovernanceParams {
    // Consensus thresholds (basis points, 10000 = 100%)
    pub link_threshold: u16,           // Default: 5100 (51%)
    pub primary_threshold: u16,        // Default: 6700 (67%)
    pub duplicate_threshold: u16,      // Default: 8000 (80%)

    // Staking
    pub min_stake_lamports: u64,       // Default: 10M lamports (0.01 KNOW)
    pub primary_stake_multiplier: u8,  // Default: 3x
    pub duplicate_stake_multiplier: u8, // Default: 10x

    // Slashing (basis points)
    pub link_slash_bps: u16,           // Default: 1000 (10%)
    pub primary_slash_bps: u16,        // Default: 3000 (30%)
    pub duplicate_slash_bps: u16,      // Default: 5000 (50%)
    pub sybil_slash_bps: u16,          // Default: 10000 (100%)

    // Voting weights
    pub primary_vote_weight: u64,      // Default: 1
    pub oracle_vote_weight: u64,       // Default: 100

    // Cooldowns (in seconds)
    pub failed_claim_cooldown: i64,    // Default: 7 days
    pub duplicate_flag_cooldown: i64,  // Default: 30 days
    pub claim_expiry_duration: i64,    // Default: 30 days

    // Oracle decay rates
    pub oracle_decay_rate_bps: u16,    // Default: 10 bps/day
    pub admin_decay_rate_bps: u16,     // Default: 50 bps/day
}
```

## Security Considerations

### 1. Sybil Resistance
- ✅ Identity tokens cannot be bought (achieved through consensus)
- ✅ Each human limited to one Primary ID
- ✅ KNOW staking adds economic cost to attacks
- ✅ High threshold (80%) for duplicate detection

### 2. Plutocracy Resistance
- ✅ Voting weight capped by identity tier
- ✅ Oracle = max 100x (not infinite)
- ✅ Money alone cannot override identity verification

### 3. Economic Security
- ✅ Stake slashing deters malicious behavior
- ✅ Tiered stake requirements (1x, 3x, 10x)
- ✅ Cooldown periods prevent spam
- ✅ Time-locked claims prevent front-running

### 4. Smart Contract Security
- ✅ Program Derived Addresses (PDAs) for deterministic addressing
- ✅ Account validation via Anchor constraints
- ✅ Overflow protection with checked arithmetic
- ✅ Reentrancy protection via Anchor
- ✅ Authorization checks on all state changes

## God Mode (Testing Only)

⚠️ **IMPORTANT**: God mode MUST be renounced before mainnet deployment.

God mode allows:
- Time warping for testing claim expiry
- Direct Oracle upgrades
- Parameter changes without governance vote

```typescript
// Time warp (testing)
await program.methods
  .timeWarp(new BN(7 * 24 * 60 * 60)) // 7 days
  .accounts({
    governance: governancePda,
    godModeAuthority: authority.publicKey,
  })
  .rpc();

// Renounce god mode (PERMANENT)
await program.methods
  .renounceGodMode()
  .accounts({
    governance: governancePda,
    godModeAuthority: authority.publicKey,
  })
  .rpc();
```

## Integration with KNOW Token

The protocol requires integration with an SPL token (KNOW) for staking. The token should:

1. Implement SPL Token standard (token-2022 recommended)
2. Have sufficient liquidity for staking
3. Support transfer restrictions for soul-bound NFTs (optional)

### Token Integration Example

```typescript
import { TOKEN_PROGRAM_ID } from "@solana/spl-token";

const knowMint = new PublicKey("KNOW_MINT_ADDRESS");
const escrowAccount = await getAssociatedTokenAddress(
  knowMint,
  stakingEscrowPda,
  true // allowOwnerOffCurve for PDA
);
```

## Comparison to Ethereum Version

| Feature | Ethereum (Solidity) | Solana (Anchor) |
|---------|---------------------|-----------------|
| Language | Solidity | Rust (Anchor) |
| State Model | Storage mappings | Account-based (PDAs) |
| Token Standard | ERC-721 (IDT), ERC-20 (KNOW) | Token-2022, SPL Token |
| Gas Model | Per-instruction | Per-account + compute units |
| Consensus Speed | ~12s blocks | ~400ms blocks |
| Finality | Probabilistic (~12 mins) | Optimistic (~6s) |
| Account Model | Address-based | Public key + PDA |
| Upgradability | Proxy patterns | Built-in (Anchor) |

## Roadmap

### Phase 1: Core Identity (Current)
- ✅ Governance and parameters
- ✅ Identity tiers and management
- ✅ Claim creation and voting
- ✅ Consensus resolution
- 🚧 Token integration (KNOW & IDT)
- 🚧 Comprehensive tests
- 🚧 Security audit

### Phase 2: Advanced Features
- [ ] Oracle jury system (random selection)
- [ ] Reputation layer integration
- [ ] Cross-program invocations (CPI) for composability
- [ ] Event compression for cheaper history queries
- [ ] Governance voting (KNO token)

### Phase 3: Production
- [ ] Mainnet deployment
- [ ] God mode renouncement
- [ ] UI/SDK libraries
- [ ] Mobile integration
- [ ] Cross-chain bridges

## Testing Strategy

### Unit Tests
```bash
# Test individual instructions
anchor test --file tests/governance.spec.ts
anchor test --file tests/identity.spec.ts
anchor test --file tests/consensus.spec.ts
```

### Integration Tests
```bash
# Test full claim lifecycle
anchor test --file tests/integration/claim-lifecycle.spec.ts
```

### Simulation Tests
```bash
# Test with multiple actors
anchor test --file tests/simulation/sybil-attack.spec.ts
```

## Common Patterns

### Finding PDAs

```typescript
const [governancePda, governanceBump] = PublicKey.findProgramAddressSync(
  [Buffer.from("governance")],
  program.programId
);

const [identityPda, identityBump] = PublicKey.findProgramAddressSync(
  [Buffer.from("identity"), owner.toBuffer()],
  program.programId
);

const [claimPda, claimBump] = PublicKey.findProgramAddressSync(
  [
    Buffer.from("claim"),
    governance.initializedAt.toBuffer('le', 8),
    claimId.toBuffer('le', 8)
  ],
  program.programId
);
```

### Error Handling

```typescript
try {
  await program.methods.requestPrimaryVerification(...).rpc();
} catch (error) {
  if (error.code === 6010) { // InsufficientStake
    console.log("Need more KNOW tokens to stake");
  } else if (error.code === 6016) { // CooldownNotElapsed
    console.log("Must wait before making another claim");
  }
}
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests for new features
4. Ensure all tests pass
5. Submit a pull request

## License

MIT License - See LICENSE file for details

## Resources

- [Anchor Framework Docs](https://www.anchor-lang.com/)
- [Solana Cookbook](https://solanacookbook.com/)
- [SPL Token Program](https://spl.solana.com/token)
- [Knomee Whitepaper](../WHITEPAPER_0.9_CLAUDE_SYNTHESIS.md)
- [Protocol Specification](../KNOMEE_IDENTITY_PROTOCOL_V1.md)

## Support

For questions and support:
- GitHub Issues: [knomee-identity-8bit/issues](https://github.com/jlmalone/knomee-identity-8bit/issues)
- Documentation: See `/docs` directory
- Whitepaper: `WHITEPAPER_0.9_CLAUDE_SYNTHESIS.md`

---

**Built with ❤️ for the decentralized future on Solana**

*This is the Solana implementation of the identity layer for Knomee's Universal Basic Income distribution.*
