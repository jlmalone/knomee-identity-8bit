# Knomee Identity Protocol - 8-Bit Edition

Decentralized Sybil resistance through weighted social consensus with an 8-bit Nintendo aesthetic.

## Overview

This is not a game. This is a **protocol** for establishing unique human identity on-chain through weighted social consensus, with game-like aesthetics to make it usable.

The protocol enables:
- **Unique Human Verification**: Blue Checkmark Primary IDs for UBI distribution
- **Linked Identity Management**: Connect social accounts to your Primary ID
- **Sybil Attack Prevention**: Community consensus with economic incentives
- **Flexible Platform Support**: Link any platform (LinkedIn, Instagram, custom platforms)
- **Weighted Voting**: Oracles have 100x voting power, Primaries have 1x
- **Stake-Based Security**: Economic alignment through staking and slashing

## Project Structure

```
knomee-identity-8bit/
├── contracts/
│   ├── identity/          # Phase 1: Identity consensus contracts
│   │   ├── IdentityRegistry.sol
│   │   ├── IdentityConsensus.sol
│   │   └── GovernanceParameters.sol
│   └── products/          # Phase 2: Product & reputation layer
│       ├── ProductRegistry.sol
│       └── ReputationDistribution.sol
├── test/                  # Foundry tests
├── scripts/               # Deployment and utility scripts
├── desktop-client/        # Kotlin Compose Desktop (8-bit UI)
├── docs/                  # Additional documentation
├── KNOMEE_IDENTITY_PROTOCOL_V1.md  # Complete protocol specification
├── PROGRESS.md           # Implementation progress tracking
├── CLAUDE.md             # AI assistant context
└── README.md             # This file
```

## Quick Start

### Prerequisites

- [Foundry](https://book.getfoundry.sh/getting-started/installation) for smart contracts
- [JDK 17+](https://adoptium.net/) for desktop client
- Node.js 18+ (optional, for deployment scripts)

### Phase 1: Identity Layer (Current)

**1. Compile contracts:**
```bash
forge build
```

**2. Run tests:**
```bash
forge test
```

**3. Deploy to Sepolia testnet:**
```bash
forge script scripts/Deploy.s.sol --rpc-url sepolia --broadcast
```

### Phase 2: Product & Reputation Layer (Future)

Coming in weeks 5-8. See [KNOMEE_IDENTITY_PROTOCOL_V1.md](./KNOMEE_IDENTITY_PROTOCOL_V1.md) for complete architecture.

## Key Features

### Identity Tiers

- **Grey Ghost** (Tier 0): Unverified addresses
- **Linked ID** (Tier 1): Secondary identities linked to Primary (multiple per platform allowed)
- **Primary ID** (Tier 2): Blue Checkmark, gets daily Knomee UBI
- **Oracle** (Tier 3): High-weight verifiers (100x voting power)

### Claim Types

1. **Link to Primary** (51% threshold): Prove address belongs to existing Primary
2. **New Primary** (67% threshold): Claim unique human status
3. **Duplicate Detection** (80% threshold): Challenge suspected Sybil attacks

### Economic Mechanics

- **Staking**: 0.01 ETH minimum, 3x for Primary claims, 10x for duplicate challenges
- **Slashing**: 10%-100% depending on claim type and outcome
- **Rewards**: Correct vouchers split slashed stakes

## Desktop Client (8-Bit UI)

A native macOS app built with Kotlin Compose Multiplatform featuring:
- 2D identity arena with pixel-art avatars
- Real blockchain transactions (no simulation)
- NPC simulation for testing protocol dynamics
- God mode for time warp and testing

Every action in the UI is a real blockchain transaction.

## Design Decisions

All critical design questions have been answered (see protocol doc):

1. **Phase Scope**: Design both layers, build identity first
2. **Linked IDs**: Flexible string labels (future-proof)
3. **Multi-Account**: Allow multiple per platform with justification
4. **Ownership**: Mutable via 67% governance
5. **Gas Budget**: No owner limit (user pays)
6. **URL Claiming**: Oracle-minted + self-claim
7. **Reputation Flow**: Smart default (auto-split) with override

## Roadmap

### Phase 1 (Weeks 1-4): Identity Consensus ← **WE ARE HERE**
- ✅ Design complete
- ✅ Project initialized
- ✅ Foundry setup
- 🚧 Smart contract development
- 🚧 Desktop client foundation
- 🚧 NPC simulation
- 🚧 Testing and polish

### Phase 2 (Weeks 5-8): Product & Reputation Layer
- Product registry with fractional ownership
- Reputation distribution (auto-split + directed)
- Oracle-minting and self-claim mechanisms
- Daily Knomee UBI distribution

### Phase 3 (Future): Meritocratic Transition
- KNO token governance
- Earned oracle status
- God mode renouncement
- Full decentralization

## Documentation

- **[KNOMEE_IDENTITY_PROTOCOL_V1.md](./KNOMEE_IDENTITY_PROTOCOL_V1.md)**: Complete protocol specification
- **[PROGRESS.md](./PROGRESS.md)**: Implementation progress tracking
- **[CLAUDE.md](./CLAUDE.md)**: AI assistant context
- **Inline comments**: Extensive Solidity documentation

## Testing

```bash
# Run all tests
forge test

# Run specific test file
forge test --match-path test/IdentityConsensus.t.sol

# Run with gas reporting
forge test --gas-report

# Run with coverage
forge coverage
```

## Security

- Economic security through staking and slashing
- Consensus thresholds prevent Sybil attacks
- Oracle weight amplifies trusted signals
- Governance-controlled parameters
- Time-tested OpenZeppelin contracts

**Status**: Testnet only. Full security audit required before mainnet.

## Contributing

This is a focused 4-week sprint. Contributions welcome after Phase 1 completion.

## License

MIT License - See LICENSE file for details.

## Contact

- Project Lead: Joseph Malone
- Protocol Spec: [KNOMEE_IDENTITY_PROTOCOL_V1.md](./KNOMEE_IDENTITY_PROTOCOL_V1.md)
- Progress Tracking: [PROGRESS.md](./PROGRESS.md)
- Main Knomee Project: `~/IdeaProjects/knomee`

---

**Built with ❤️ for the decentralized future**

*This is the identity layer for Knomee's Universal Basic Income distribution.*
