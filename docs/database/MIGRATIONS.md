# Migration and Deployment Documentation

## Table of Contents

1. [Overview](#overview)
2. [Migration Strategy](#migration-strategy)
3. [Deployment Procedures](#deployment-procedures)
4. [Version History](#version-history)
5. [Rollback Procedures](#rollback-procedures)
6. [Data Migration Scripts](#data-migration-scripts)
7. [Upgrade Patterns](#upgrade-patterns)
8. [Testing Deployments](#testing-deployments)

---

## Overview

### Blockchain vs Traditional Migration

Traditional databases use migrations to modify schema over time. Blockchain databases are fundamentally different:

| Aspect | Traditional DB | Blockchain |
|--------|---------------|------------|
| Schema Changes | ALTER TABLE statements | Deploy new contract version |
| Data Migration | UPDATE/INSERT statements | State transfer or proxy upgrade |
| Rollback | Revert migration | Deploy previous version (data may be lost) |
| Downtime | Maintenance window | No downtime (immutable old contract) |
| Versioning | Migration files (001, 002...) | Contract addresses |

### Immutability Implications

**Key Principle**: Once deployed, smart contract code is immutable. You cannot modify existing contracts.

**Strategies**:
1. **Redeployment**: Deploy new contract, migrate data, update references
2. **Proxy Pattern**: Use upgradeable proxies (not currently implemented)
3. **Governance Parameters**: Make values configurable rather than hardcoded

---

## Migration Strategy

### Current Approach: Contract Redeployment

The Knomee Identity system uses **direct deployment** with **configurable parameters** to minimize the need for migrations.

#### Phase 1: Initial Deployment (Current)

```
┌─────────────────────────────────────────────────────────┐
│                 Initial Deployment                       │
└─────────────────────────────────────────────────────────┘

Step 1: Deploy Governance Parameters
  ↓
Step 2: Deploy Token Contracts (IDT, KNOW)
  ↓
Step 3: Deploy IdentityRegistry
  ↓
Step 4: Deploy IdentityConsensus
  ↓
Step 5: Link Contracts
  ├─ registry.setConsensusContract(consensus)
  ├─ registry.setIdentityToken(identityToken)
  ├─ identityToken.setIdentityRegistry(registry)
  ├─ knomeeToken.setRegistryContract(registry)
  └─ knomeeToken.setConsensusContract(consensus)
```

**Advantages**:
- Simple, straightforward deployment
- No proxy complexity
- Full transparency (code = bytecode)

**Disadvantages**:
- Cannot upgrade contracts without redeployment
- Data migration required for breaking changes
- User addresses change on redeployment

#### Phase 2: Upgradeable Proxies (Future)

For future versions, consider implementing upgradeable proxies:

```solidity
// Example: TransparentUpgradeableProxy pattern
IdentityRegistryProxy (immutable)
  ├─ Implementation V1 (initial)
  ├─ Implementation V2 (upgraded)
  └─ Implementation V3 (upgraded)
```

**Advantages**:
- Contract address remains constant
- Seamless upgrades without data migration
- Backward compatibility

**Disadvantages**:
- More complex codebase
- Storage layout constraints
- Potential security risks if not properly secured

---

## Deployment Procedures

### Local Development (Anvil)

#### 1. Start Local Testnet

```bash
# Terminal 1: Start Anvil
anvil

# Expected output:
# Available Accounts
# ==================
# (0) 0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266 (10000 ETH)
# (1) 0x70997970C51812dc3A010C7d01b50e0d17dc79C8 (10000 ETH)
# ...
#
# Private Keys
# ==================
# (0) 0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80
```

#### 2. Deploy All Contracts

```bash
# Terminal 2: Deploy contracts
forge script script/Deploy.s.sol:DeployScript \
  --rpc-url http://127.0.0.1:8545 \
  --private-key 0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80 \
  --broadcast

# Expected output:
# == Logs ==
#   Deploying contracts with address: 0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266
#   Deployer balance: 10000000000000000000000
#
#   1. Deploying GovernanceParameters...
#   GovernanceParameters deployed to: 0x5FbDB2315678afecb367f032d93F642f64180aa3
#
#   2. Deploying IdentityToken (IDT)...
#   IdentityToken deployed to: 0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512
#   ...
```

#### 3. Save Deployment Addresses

Create `.env.local` with deployed addresses:

```bash
# .env.local
RPC_URL=http://127.0.0.1:8545
GOVERNANCE_PARAMS_ADDRESS=0x5FbDB2315678afecb367f032d93F642f64180aa3
IDENTITY_TOKEN_ADDRESS=0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512
KNOMEE_TOKEN_ADDRESS=0x9fE46736679d2D9a65F0992F2272dE9f3c7fa6e0
IDENTITY_REGISTRY_ADDRESS=0xCf7Ed3AccA5a467e9e704C703E8D87F634fB0Fc9
IDENTITY_CONSENSUS_ADDRESS=0xDc64a140Aa3E981100a9becA4E685f962f0cF6C9
```

#### 4. Verify Deployment

```bash
# Check deployment status
forge script script/Deploy.s.sol:DeployScript \
  --rpc-url http://127.0.0.1:8545 \
  --private-key 0xac0974... \
  --verify-only

# Or use cast to query contracts
cast call $GOVERNANCE_PARAMS_ADDRESS "linkThreshold()(uint256)" --rpc-url http://127.0.0.1:8545
# Expected: 5100 (51%)
```

---

### Sepolia Testnet Deployment

#### 1. Setup Environment Variables

```bash
# .env
SEPOLIA_RPC_URL=https://sepolia.infura.io/v3/YOUR_INFURA_KEY
PRIVATE_KEY=0x... # DO NOT COMMIT THIS!
ETHERSCAN_API_KEY=YOUR_ETHERSCAN_API_KEY
```

#### 2. Fund Deployer Account

```bash
# Get your deployer address
cast wallet address --private-key $PRIVATE_KEY

# Visit Sepolia faucet:
# https://sepoliafaucet.com/
# Request 0.5 ETH to deployer address
```

#### 3. Deploy Contracts

```bash
forge script script/Deploy.s.sol:DeployScript \
  --rpc-url $SEPOLIA_RPC_URL \
  --private-key $PRIVATE_KEY \
  --broadcast \
  --verify \
  --etherscan-api-key $ETHERSCAN_API_KEY

# --broadcast: Actually send transactions
# --verify: Verify source code on Etherscan
```

#### 4. Verify on Etherscan

After deployment, contracts should be automatically verified. Check at:
- `https://sepolia.etherscan.io/address/<contract_address>`

If auto-verification fails:

```bash
forge verify-contract \
  --chain-id 11155111 \
  --compiler-version v0.8.20 \
  --optimizer-runs 200 \
  <contract_address> \
  contracts/identity/IdentityRegistry.sol:IdentityRegistry \
  --etherscan-api-key $ETHERSCAN_API_KEY
```

---

### Polygon Mumbai Testnet Deployment

```bash
# .env
MUMBAI_RPC_URL=https://polygon-mumbai.g.alchemy.com/v2/YOUR_ALCHEMY_KEY
POLYGONSCAN_API_KEY=YOUR_POLYGONSCAN_API_KEY

# Deploy
forge script script/Deploy.s.sol:DeployScript \
  --rpc-url $MUMBAI_RPC_URL \
  --private-key $PRIVATE_KEY \
  --broadcast \
  --verify \
  --etherscan-api-key $POLYGONSCAN_API_KEY

# Verify on PolygonScan Mumbai
# https://mumbai.polygonscan.com/address/<contract_address>
```

---

### Mainnet Deployment (Production)

⚠️ **CRITICAL**: Only deploy to mainnet after thorough testing on testnets!

#### Pre-Deployment Checklist

- [ ] All contracts tested on Anvil
- [ ] All contracts tested on Sepolia for 1+ week
- [ ] Security audit completed
- [ ] Bug bounty program active
- [ ] God mode disabled (`params.renounceGodMode()`)
- [ ] Deployer account secured (hardware wallet)
- [ ] Sufficient ETH for gas fees (~0.5 ETH)
- [ ] Multi-sig wallet ready for ownership transfer
- [ ] Documentation updated
- [ ] Frontend tested against testnet contracts
- [ ] Monitoring and alerting configured

#### Deployment Steps

```bash
# .env
MAINNET_RPC_URL=https://eth-mainnet.g.alchemy.com/v2/YOUR_ALCHEMY_KEY
PRIVATE_KEY=0x... # USE HARDWARE WALLET!
ETHERSCAN_API_KEY=YOUR_ETHERSCAN_API_KEY

# Dry run (simulate without broadcasting)
forge script script/Deploy.s.sol:DeployScript \
  --rpc-url $MAINNET_RPC_URL \
  --private-key $PRIVATE_KEY

# Actual deployment
forge script script/Deploy.s.sol:DeployScript \
  --rpc-url $MAINNET_RPC_URL \
  --private-key $PRIVATE_KEY \
  --broadcast \
  --verify \
  --etherscan-api-key $ETHERSCAN_API_KEY

# IMMEDIATELY after deployment:
# 1. Transfer ownership to multi-sig
cast send $GOVERNANCE_PARAMS_ADDRESS "transferOwnership(address)" $MULTISIG_ADDRESS \
  --rpc-url $MAINNET_RPC_URL \
  --private-key $PRIVATE_KEY

# 2. Renounce god mode
cast send $GOVERNANCE_PARAMS_ADDRESS "renounceGodMode()" \
  --rpc-url $MAINNET_RPC_URL \
  --private-key $PRIVATE_KEY

# 3. Verify configuration
cast call $GOVERNANCE_PARAMS_ADDRESS "godModeActive()(bool)" --rpc-url $MAINNET_RPC_URL
# Expected: false

cast call $GOVERNANCE_PARAMS_ADDRESS "owner()(address)" --rpc-url $MAINNET_RPC_URL
# Expected: <multisig_address>
```

---

## Version History

### Version 1.0.0 - Initial Release (Current)

**Deployed**: TBD
**Contracts**:
- `GovernanceParameters.sol` (337 lines)
- `IdentityRegistry.sol` (349 lines)
- `IdentityConsensus.sol` (400+ lines)
- `IdentityToken.sol` (150+ lines)
- `KnomeeToken.sol` (150+ lines)

**Features**:
- Four-tier identity system (GreyGhost, LinkedID, PrimaryID, Oracle)
- Weighted consensus voting
- Economic incentives (staking, slashing, rewards)
- Soul-bound identity NFTs
- Platform linking (LinkedIn, Instagram, etc.)
- Duplicate detection via challenges

**Parameters**:
- Link threshold: 51%
- Primary threshold: 67%
- Duplicate threshold: 80%
- Min stake: 0.01 ETH
- Primary vote weight: 1
- Oracle vote weight: 100

**Known Issues**:
- None (initial release)

**Migration From**: N/A (genesis deployment)

---

### Version 1.1.0 - Future (Planned)

**Features** (example):
- Upgradeable proxy pattern
- Multi-chain support (Polygon, Arbitrum)
- Enhanced Oracle selection algorithm
- Reputation scoring system
- Appeal mechanism for rejected claims

**Migration Path**:
1. Deploy new contracts with proxy pattern
2. Pause old contracts
3. Export existing identity data
4. Import data to new contracts
5. Redirect users to new addresses
6. Deprecate old contracts

---

## Rollback Procedures

### Immutability Constraints

⚠️ **Important**: Smart contracts are immutable. Rollback is NOT possible in the traditional sense.

### "Rollback" Options

#### Option 1: Deploy Previous Version

If a bug is discovered in a new deployment:

```bash
# 1. Deploy old version again (same bytecode)
forge script script/DeployV1.0.0.s.sol:DeployScript \
  --rpc-url $RPC_URL \
  --broadcast

# 2. Update client to use old contract addresses
# Edit .env or config file:
IDENTITY_REGISTRY_ADDRESS=0x... # Old version
```

**Data Loss**: Any state changes to the new contract are lost.

#### Option 2: Pause Contracts

If contracts have pause functionality:

```solidity
// In future version with Pausable
function pause() external onlyOwner {
    _pause();
}

function unpause() external onlyOwner {
    _unpause();
}
```

```bash
# Pause contracts
cast send $REGISTRY_ADDRESS "pause()" \
  --rpc-url $RPC_URL \
  --private-key $OWNER_KEY

# Users cannot interact until unpaused
```

#### Option 3: Emergency State Export

If critical bug found:

1. **Export current state**:
   ```bash
   # Script to query all identities and claims
   node scripts/exportState.js > state_backup.json
   ```

2. **Deploy fixed contract**:
   ```bash
   forge script script/DeployFixed.s.sol:DeployScript --broadcast
   ```

3. **Import state** (if new contract supports it):
   ```bash
   node scripts/importState.js state_backup.json
   ```

#### Option 4: Governance Override (God Mode)

In testnet deployments with god mode enabled:

```solidity
// GovernanceParameters.sol
modifier onlyGodMode() {
    require(godModeActive && msg.sender == godModeAddress, "God mode only");
    _;
}

function emergencyOverride(...) external onlyGodMode {
    // Force state changes
}
```

⚠️ **Mainnet**: God mode MUST be disabled before production use!

---

## Data Migration Scripts

### Export Identity State

**Script**: `scripts/exportIdentities.js`

```javascript
const Web3 = require('web3');
const fs = require('fs');

const web3 = new Web3('http://127.0.0.1:8545');
const registryABI = require('../abi/IdentityRegistry.json');
const registryAddress = process.env.IDENTITY_REGISTRY_ADDRESS;

const registry = new web3.eth.Contract(registryABI, registryAddress);

async function exportIdentities() {
    // Get all IdentityVerified events
    const events = await registry.getPastEvents('IdentityVerified', {
        fromBlock: 0,
        toBlock: 'latest'
    });

    const identities = [];

    for (const event of events) {
        const address = event.returnValues.addr;
        const identity = await registry.methods.identities(address).call();

        identities.push({
            address: address,
            tier: identity.tier,
            primaryAddress: identity.primaryAddress,
            verifiedAt: identity.verifiedAt,
            totalVouchesReceived: identity.totalVouchesReceived,
            totalStakeReceived: identity.totalStakeReceived,
            oracleGrantedAt: identity.oracleGrantedAt
        });

        // Get linked accounts
        const linkedAccounts = await registry.methods.getLinkedAccounts(address).call();
        identities[identities.length - 1].linkedAccounts = linkedAccounts;
    }

    fs.writeFileSync('identities_export.json', JSON.stringify(identities, null, 2));
    console.log(`Exported ${identities.length} identities to identities_export.json`);
}

exportIdentities().catch(console.error);
```

**Usage**:
```bash
node scripts/exportIdentities.js
# Output: identities_export.json
```

---

### Export Claims State

**Script**: `scripts/exportClaims.js`

```javascript
async function exportClaims() {
    const consensus = new web3.eth.Contract(consensusABI, consensusAddress);

    const events = await consensus.getPastEvents('ClaimCreated', {
        fromBlock: 0,
        toBlock: 'latest'
    });

    const claims = [];

    for (const event of events) {
        const claimId = event.returnValues.claimId;
        const claim = await consensus.methods.claims(claimId).call();
        const vouches = await consensus.methods.vouchesOnClaim(claimId).call();

        claims.push({
            claimId: claimId,
            claimType: claim.claimType,
            status: claim.status,
            subject: claim.subject,
            relatedAddress: claim.relatedAddress,
            platform: claim.platform,
            justification: claim.justification,
            createdAt: claim.createdAt,
            expiresAt: claim.expiresAt,
            totalWeightFor: claim.totalWeightFor,
            totalWeightAgainst: claim.totalWeightAgainst,
            totalStake: claim.totalStake,
            resolved: claim.resolved,
            vouches: vouches
        });
    }

    fs.writeFileSync('claims_export.json', JSON.stringify(claims, null, 2));
    console.log(`Exported ${claims.length} claims to claims_export.json`);
}
```

---

### Import State to New Contract

**Script**: `scripts/importIdentities.js`

```javascript
async function importIdentities() {
    const identities = JSON.parse(fs.readFileSync('identities_export.json'));
    const newRegistry = new web3.eth.Contract(registryABI, newRegistryAddress);

    // Batch import (if new contract supports it)
    for (const identity of identities) {
        if (identity.tier === '2') { // PrimaryID
            await newRegistry.methods.importPrimaryIdentity(
                identity.address,
                identity.verifiedAt,
                identity.totalVouchesReceived,
                identity.totalStakeReceived
            ).send({ from: adminAddress, gas: 500000 });

            console.log(`Imported PrimaryID: ${identity.address}`);
        }

        // Import linked accounts
        for (const link of identity.linkedAccounts) {
            await newRegistry.methods.importLinkedAccount(
                identity.address,
                link.linkedAddress,
                link.platform,
                link.linkedAt
            ).send({ from: adminAddress, gas: 500000 });

            console.log(`  Linked ${link.platform}: ${link.linkedAddress}`);
        }
    }

    console.log('Import complete!');
}
```

⚠️ **Note**: Import functions must be implemented in new contract version.

---

### Verify Migration Integrity

**Script**: `scripts/verifyMigration.js`

```javascript
async function verifyMigration() {
    const oldIdentities = JSON.parse(fs.readFileSync('identities_export.json'));
    const newRegistry = new web3.eth.Contract(registryABI, newRegistryAddress);

    let errors = 0;

    for (const oldIdentity of oldIdentities) {
        const newIdentity = await newRegistry.methods.identities(oldIdentity.address).call();

        // Compare fields
        if (newIdentity.tier !== oldIdentity.tier) {
            console.error(`Tier mismatch for ${oldIdentity.address}`);
            errors++;
        }

        if (newIdentity.verifiedAt !== oldIdentity.verifiedAt) {
            console.error(`VerifiedAt mismatch for ${oldIdentity.address}`);
            errors++;
        }

        // Verify linked accounts
        const newLinked = await newRegistry.methods.getLinkedAccounts(oldIdentity.address).call();
        if (newLinked.length !== oldIdentity.linkedAccounts.length) {
            console.error(`Linked accounts count mismatch for ${oldIdentity.address}`);
            errors++;
        }
    }

    if (errors === 0) {
        console.log('✅ Migration verified successfully! No errors found.');
    } else {
        console.error(`❌ Migration verification failed with ${errors} errors.`);
    }
}
```

---

## Upgrade Patterns

### Pattern 1: Transparent Proxy (Future)

```solidity
// Proxy contract (immutable)
contract IdentityRegistryProxy {
    address public implementation;
    address public admin;

    function upgradeTo(address newImplementation) external {
        require(msg.sender == admin);
        implementation = newImplementation;
    }

    fallback() external payable {
        address impl = implementation;
        assembly {
            calldatacopy(0, 0, calldatasize())
            let result := delegatecall(gas(), impl, 0, calldatasize(), 0, 0)
            returndatacopy(0, 0, returndatasize())
            switch result
            case 0 { revert(0, returndatasize()) }
            default { return(0, returndatasize()) }
        }
    }
}

// Implementation V1
contract IdentityRegistryV1 {
    mapping(address => Identity) public identities;
    // ... existing logic
}

// Implementation V2 (upgraded)
contract IdentityRegistryV2 {
    mapping(address => Identity) public identities; // Same storage layout!
    mapping(address => uint256) public reputationScores; // New feature
    // ... new logic
}
```

**Upgrade Process**:
```bash
# Deploy new implementation
forge create src/IdentityRegistryV2.sol:IdentityRegistryV2

# Upgrade proxy
cast send $PROXY_ADDRESS "upgradeTo(address)" $NEW_IMPL_ADDRESS \
  --rpc-url $RPC_URL \
  --private-key $ADMIN_KEY
```

---

### Pattern 2: Multi-Version Support

Deploy multiple versions simultaneously:

```
IdentityRegistry V1 (0xAAA...)
  ├─ Used by: Web app v1.0
  └─ Status: Deprecated (read-only)

IdentityRegistry V2 (0xBBB...)
  ├─ Used by: Web app v2.0, Desktop app v1.5
  └─ Status: Current

IdentityRegistry V3 (0xCCC...)
  ├─ Used by: Beta users
  └─ Status: Testing
```

Users can migrate at their own pace.

---

## Testing Deployments

### Automated Deployment Tests

**Script**: `test/Deploy.t.sol`

```solidity
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../script/Deploy.s.sol";

contract DeployTest is Test {
    function testFullDeployment() public {
        DeployScript deployer = new DeployScript();
        deployer.run();

        // Verify all contracts deployed
        assertTrue(address(deployer.params()) != address(0));
        assertTrue(address(deployer.registry()) != address(0));
        assertTrue(address(deployer.consensus()) != address(0));

        // Verify linkages
        assertEq(
            address(deployer.registry().consensusContract()),
            address(deployer.consensus())
        );

        // Verify parameters
        assertEq(deployer.params().linkThreshold(), 5100);
        assertEq(deployer.params().primaryThreshold(), 6700);
    }
}
```

**Run**:
```bash
forge test --match-contract DeployTest -vvv
```

---

### Integration Tests Post-Deployment

```bash
# After deployment, run integration tests
forge test --fork-url $SEPOLIA_RPC_URL --match-contract IntegrationTest -vvv
```

---

## Summary

| Deployment Type | Network | Cost | Time | Verification |
|-----------------|---------|------|------|--------------|
| Local (Anvil) | Localhost | Free | ~5 sec | N/A |
| Sepolia Testnet | Ethereum L1 | ~$2-5 | ~3 min | Etherscan |
| Mumbai Testnet | Polygon L2 | ~$0.10 | ~30 sec | PolygonScan |
| Mainnet | Ethereum L1 | ~$50-500 | ~3 min | Etherscan |

**Best Practice**: Always deploy to Anvil → Sepolia → Mainnet in that order, testing thoroughly at each stage.

---

This migration documentation provides a complete guide for deploying, upgrading, and maintaining the Knomee Identity blockchain database system.
