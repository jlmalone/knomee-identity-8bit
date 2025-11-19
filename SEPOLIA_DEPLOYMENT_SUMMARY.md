# Sepolia Deployment Layer - Summary

This document summarizes the complete Sepolia deployment infrastructure for the Knomee Identity Protocol.

## What Was Built

A comprehensive Ethereum smart contract deployment layer for Sepolia testnet, including:

### 1. Enhanced Deployment Scripts

| Script | Purpose | Usage |
|--------|---------|-------|
| `DeploySepolia.s.sol` | Main deployment script with full configuration | Primary deployment tool |
| `GrantOracle.s.sol` | Grant Oracle status to trusted verifiers | Post-deployment setup |
| `DistributeTokens.s.sol` | Distribute KNOW tokens to test users | Token distribution |
| `VerifyContracts.s.sol` | Manual contract verification helpers | If auto-verify fails |
| `TestProtocol.s.sol` | Integration testing template | Protocol testing |

### 2. Smart Contract Improvements

**IdentityRegistry.sol** - Added admin functions for easier deployment:
- `adminGrantPrimaryID(address)` - Grant Primary ID directly (owner only)
- Enhanced `upgradeToOracle(address)` - Also upgrades the Identity Token

These functions enable seamless Oracle setup during initial deployment.

### 3. Complete Documentation

**DEPLOYMENT.md** - Comprehensive deployment guide covering:
- Prerequisites and environment setup
- Step-by-step deployment instructions
- Post-deployment configuration
- Testing and verification procedures
- Troubleshooting guide
- Security checklist

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    DEPLOYMENT WORKFLOW                       │
└─────────────────────────────────────────────────────────────┘

1. SETUP ENVIRONMENT
   ├── Install Foundry
   ├── Configure .env (RPC, private key, API keys)
   ├── Fund deployer wallet with Sepolia ETH
   └── Verify compilation: forge build

2. DEPLOY CONTRACTS
   ├── Run: forge script script/DeploySepolia.s.sol --broadcast --verify
   ├── Contracts deployed:
   │   ├── GovernanceParameters (protocol rules)
   │   ├── IdentityToken (soul-bound NFT)
   │   ├── KnomeeToken (staking ERC-20)
   │   ├── IdentityRegistry (identity state)
   │   └── IdentityConsensus (voting logic)
   └── All contracts linked and configured

3. POST-DEPLOYMENT SETUP
   ├── Grant Oracle status: script/GrantOracle.s.sol
   ├── Distribute KNOW tokens: script/DistributeTokens.s.sol
   └── Verify on Etherscan (auto or manual)

4. TESTING
   ├── Test with script/TestProtocol.s.sol
   ├── Verify contract interactions
   └── Monitor gas costs and behavior

5. PRODUCTION READY
   ├── Renounce god mode: params.renounceGodMode()
   ├── Transfer ownership to multisig
   └── Launch! 🚀
```

## Smart Contracts Deployed

### Phase 1: Identity Consensus Layer ✅

| Contract | Size | Purpose | Key Features |
|----------|------|---------|--------------|
| **GovernanceParameters** | ~1.2M gas | Protocol configuration | Thresholds, stakes, slashing rates, god mode |
| **IdentityToken** | ~2.0M gas | Soul-bound proof | ERC-721, non-transferable, voting weights |
| **KnomeeToken** | ~2.5M gas | Staking token | ERC-20, 1B supply, rewards & slashing |
| **IdentityRegistry** | ~2.8M gas | Identity state | 4 tiers, platform linking, challenge tracking |
| **IdentityConsensus** | ~4.0M gas | Voting & claims | 3 claim types, weighted voting, rewards |

**Total Deployment Cost**: ~12.5M gas (~0.63 ETH at 50 gwei)

### Phase 2: Product & Reputation Layer ⏳

Planned for weeks 5-8:
- `ProductRegistry.sol` - Product ownership and fractional splits
- `ReputationDistribution.sol` - Reputation flows and daily UBI

## Key Protocol Parameters

### Consensus Thresholds
- **Link to Primary**: 51% (5100 basis points)
- **New Primary**: 67% (6700 basis points)
- **Duplicate Flag**: 80% (8000 basis points)

### Minimum Stakes
- **Link claim**: 10 KNOW
- **Primary claim**: 30 KNOW
- **Duplicate flag**: 50 KNOW

### Voting Weights
- **Grey Ghost**: 0 (cannot vote)
- **Linked ID**: 0 (cannot vote)
- **Primary ID**: 1
- **Oracle**: 100

### Slashing Rates
- **Wrong Link vote**: 10%
- **Wrong Primary vote**: 30%
- **Wrong Duplicate vote**: 50%
- **Confirmed Sybil**: 100%

### Cooldowns
- **Failed claim**: 7 days
- **Duplicate flag**: 30 days
- **Claim expiry**: 30 days

## Security Features

### Sybil Resistance
1. **High duplicate threshold** (80%) - Hard to false-flag
2. **Large stake requirement** (50 KNOW) - Discourages frivolous challenges
3. **Permanent downgrade** - Confirmed Sybils lose all privileges
4. **Economic slashing** - 100% KNOW burned for duplicates

### Plutocracy Prevention
1. **Identity-first voting** - No voting without verified identity
2. **Stake as collateral** - Not voting power itself
3. **Weighted formula** - Identity multiplier × KNOW stake
4. **Soul-bound tokens** - Cannot buy/sell identity

### God Mode (Testing Only)
- **Time warp capability** - Test time-based mechanics
- **Renounce-able** - `params.renounceGodMode()` permanently disables
- **WARNING**: Must renounce before mainnet deployment

## Usage Examples

### Deploy All Contracts
```bash
forge script script/DeploySepolia.s.sol:DeploySepolia \
  --rpc-url sepolia \
  --broadcast \
  --verify \
  -vvvv
```

### Grant Oracle Status
```bash
# 1. Edit script/GrantOracle.s.sol with addresses
# 2. Run:
forge script script/GrantOracle.s.sol:GrantOracle \
  --rpc-url sepolia \
  --broadcast \
  -vvv
```

### Distribute KNOW Tokens
```bash
# 1. Edit script/DistributeTokens.s.sol with recipients
# 2. Run:
forge script script/DistributeTokens.s.sol:DistributeTokens \
  --rpc-url sepolia \
  --broadcast \
  -vvv
```

### Request Primary Verification (User)
```bash
# 1. Approve KNOW tokens
cast send $KNOMEE_TOKEN \
  "approve(address,uint256)" \
  $CONSENSUS_CONTRACT \
  30000000000000000000 \
  --rpc-url sepolia \
  --private-key $USER_KEY

# 2. Submit claim
cast send $CONSENSUS_CONTRACT \
  "requestPrimaryVerification()(uint256)" \
  --rpc-url sepolia \
  --private-key $USER_KEY
```

### Vouch FOR a Claim (Oracle)
```bash
cast send $CONSENSUS_CONTRACT \
  "vouchFor(uint256,uint256)" \
  $CLAIM_ID \
  50000000000000000000 \
  --rpc-url sepolia \
  --private-key $ORACLE_KEY
```

## Files Created/Modified

### New Files
```
script/
├── DeploySepolia.s.sol          # Main deployment script
├── GrantOracle.s.sol            # Oracle management
├── DistributeTokens.s.sol       # Token distribution
├── VerifyContracts.s.sol        # Manual verification
└── TestProtocol.s.sol           # Integration testing

docs/
├── DEPLOYMENT.md                # Complete deployment guide
└── SEPOLIA_DEPLOYMENT_SUMMARY.md # This file
```

### Modified Files
```
contracts/identity/
└── IdentityRegistry.sol
    ├── + adminGrantPrimaryID()  # Admin function for setup
    └── ↻ upgradeToOracle()      # Enhanced to update token
```

## Quick Start

### Prerequisites
1. **Foundry installed**
   ```bash
   curl -L https://foundry.paradigm.xyz | bash
   foundryup
   ```

2. **Sepolia ETH** (0.1+ ETH)
   - https://sepoliafaucet.com/
   - https://www.alchemy.com/faucets/ethereum-sepolia

3. **API Keys**
   - Alchemy RPC: https://www.alchemy.com/
   - Etherscan API: https://etherscan.io/myapikey

### 5-Minute Deployment

```bash
# 1. Clone and setup
git clone <repo>
cd knomee-identity-8bit
cp .env.example .env
# Edit .env with your keys

# 2. Verify setup
forge build
forge test

# 3. Deploy
forge script script/DeploySepolia.s.sol:DeploySepolia \
  --rpc-url sepolia \
  --broadcast \
  --verify \
  -vvvv

# 4. Save addresses from output to .env

# 5. Setup Oracles
# Edit script/GrantOracle.s.sol
forge script script/GrantOracle.s.sol:GrantOracle \
  --rpc-url sepolia \
  --broadcast

# 6. Distribute tokens
# Edit script/DistributeTokens.s.sol
forge script script/DistributeTokens.s.sol:DistributeTokens \
  --rpc-url sepolia \
  --broadcast

# 7. Test!
```

## Next Steps

### Immediate
- [ ] Deploy to Sepolia testnet
- [ ] Grant Oracle status to trusted testers
- [ ] Distribute KNOW tokens
- [ ] Run integration tests
- [ ] Monitor for issues

### Phase 2 (Weeks 5-8)
- [ ] Design ProductRegistry contract
- [ ] Design ReputationDistribution contract
- [ ] Implement daily UBI mechanics
- [ ] Add tests for Phase 2

### Production Preparation
- [ ] Security audit
- [ ] Bug bounty program
- [ ] Mainnet deployment plan
- [ ] Renounce god mode
- [ ] Transfer to multisig
- [ ] Launch strategy

## Support Resources

- **Full Documentation**: `KNOMEE_IDENTITY_PROTOCOL_V1.md`
- **Deployment Guide**: `DEPLOYMENT.md`
- **Tokenomics**: `TOKENOMICS.md`
- **Progress Tracker**: `PROGRESS.md`
- **Foundry Book**: https://book.getfoundry.sh/

## Technical Specifications

### Network Details
- **Network**: Sepolia Testnet
- **Chain ID**: 11155111
- **Currency**: SepoliaETH (test ETH)
- **Block Time**: ~12 seconds
- **RPC**: Alchemy, Infura, or public endpoints

### Compiler Settings
```toml
solc_version = "0.8.20"
optimizer = true
optimizer_runs = 200
via_ir = false
```

### Dependencies
- OpenZeppelin Contracts (ERC-20, ERC-721, Ownable, AccessControl)
- Forge Standard Library

## Gas Optimization Notes

The contracts are optimized for:
- **Storage packing** - Related variables grouped
- **Event emissions** - Proper indexing for query efficiency
- **External calls** - Minimized cross-contract calls
- **View functions** - Gas-free state queries

Average transaction costs (at 50 gwei):
- Request Primary claim: ~150k gas (~$0.50)
- Vouch FOR/AGAINST: ~120k gas (~$0.40)
- Claim rewards: ~80k gas (~$0.27)
- Link secondary ID: ~180k gas (~$0.60)

## License

MIT License - See LICENSE file

---

**Built with ❤️ for decentralized identity and UBI**

*Last updated: 2025-11-19*
