# Database Schema Documentation

## Overview

The Knomee Identity system uses a **blockchain-based data architecture** rather than traditional SQL databases. All data is stored on-chain in Ethereum smart contracts, providing:

- **Immutability**: Permanent, tamper-proof records
- **Transparency**: All state changes are publicly auditable
- **Decentralization**: No central database server required
- **Economic Security**: Staking and slashing mechanisms enforce data integrity
- **Cryptographic Guarantees**: Address-based identity and digital signatures

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Data Storage | Ethereum Smart Contracts | Solidity ^0.8.20 |
| Client Library | Web3j | 4.10.3 |
| Development Framework | Foundry | Latest |
| Local Testnet | Anvil | Foundry |
| Public Testnets | Sepolia, Polygon Mumbai | - |
| Application Layer | Kotlin + Compose Multiplatform | - |

## Smart Contract "Databases"

The system consists of 5 primary smart contracts that act as distributed databases:

### 1. IdentityRegistry.sol
**Purpose**: Core identity state management
**Location**: `contracts/identity/IdentityRegistry.sol`
**Size**: 349 lines

Stores user identities with 4 tier levels (GreyGhost, LinkedID, PrimaryID, Oracle) and manages platform linking.

### 2. IdentityConsensus.sol
**Purpose**: Claims submission and consensus voting
**Location**: `contracts/identity/IdentityConsensus.sol`
**Size**: 400+ lines

Manages identity verification claims, weighted voting, and consensus resolution with economic incentives.

### 3. IdentityToken.sol
**Purpose**: Soul-bound NFT representing verified identities
**Location**: `contracts/identity/IdentityToken.sol`
**Size**: 150+ lines

Issues non-transferable NFTs to verified users as proof of unique humanity.

### 4. KnomeeToken.sol
**Purpose**: ERC-20 utility token for staking and rewards
**Location**: `contracts/identity/KnomeeToken.sol`
**Size**: 150+ lines

Governance token (KNOW) used for staking on identity claims and rewarding honest participation.

### 5. GovernanceParameters.sol
**Purpose**: Protocol configuration and governance
**Location**: `contracts/identity/GovernanceParameters.sol`
**Size**: 337 lines

Stores 20+ protocol parameters including consensus thresholds, staking requirements, slashing rates, and voting weights.

## Documentation Structure

This database documentation is organized into the following sections:

### 📊 [Schema Documentation](./SCHEMA.md)
- Entity-Relationship diagrams for all contracts
- Complete data structure definitions
- Mapping structures and access patterns
- Relationships between contracts
- Constraints and validation rules

### 🔍 [Query Reference](./QUERIES.md)
- 50+ example queries for common operations
- Read operations (view functions)
- Write operations (state-changing transactions)
- Event queries for historical data
- Complex multi-contract queries

### 🚀 [Migration & Deployment](./MIGRATIONS.md)
- Contract deployment strategy
- Version history and changelog
- Upgrade procedures and considerations
- Rollback limitations (blockchain immutability)
- Data migration patterns

### ⚡ [Performance Optimization](./PERFORMANCE.md)
- Gas optimization strategies
- Storage access patterns (mapping vs array)
- Query optimization techniques
- Indexing via events
- Scaling considerations
- State pruning strategies

### 🔌 [Integration Guide](./INTEGRATION.md)
- Web3j client integration patterns
- Common usage patterns
- Transaction lifecycle management
- Concurrency and nonce handling
- Error handling strategies
- Real-time event monitoring

### 💾 [Backup & Recovery](./BACKUP.md)
- Blockchain state backup strategies
- Node synchronization
- Contract state export
- Disaster recovery procedures
- Archive node requirements

## Quick Start

### Connecting to the Blockchain Database

```kotlin
// Initialize Web3 connection
val web3Service = Web3Service(
    rpcUrl = "http://127.0.0.1:8545", // Anvil local testnet
    privateKey = "0x..."
)

// Configure contract addresses (deployed instances)
web3Service.governanceAddress = "0x..."
web3Service.registryAddress = "0x..."
web3Service.consensusAddress = "0x..."

// Read data
val repository = ContractRepository(web3Service)
val identity = repository.getIdentity("0x...")
```

### Basic Query Example

```kotlin
// Read identity tier
val tier = repository.getTier(userAddress)

// Read active claims
val claims = repository.getActiveClaims()

// Read governance parameters
val params = repository.getGovernanceParameters()
```

### Basic Write Example

```kotlin
// Submit a new identity claim
val txService = TransactionService(web3Service)
val result = txService.requestPrimaryVerification(
    consensusAddress = consensusAddress,
    justification = "I am a unique human because...",
    stakeAmount = BigInteger.valueOf(10).pow(16) // 0.01 ETH
)
```

## Key Concepts

### Blockchain as Database

Traditional databases use tables, rows, and SQL queries. This system uses:

| Traditional Database | Blockchain Database |
|---------------------|---------------------|
| Tables | Smart Contract Mappings |
| Rows | Mapping Entries (address → data) |
| Primary Keys | Ethereum Addresses |
| Foreign Keys | Address References |
| Indexes | Event Logs |
| Queries | Contract Function Calls |
| Transactions | Ethereum Transactions |
| Locks | Ethereum Nonce Ordering |
| Rollback | Not Possible (Immutable) |
| Migrations | Contract Redeployment |

### Data Persistence

All data is stored in:
1. **Contract Storage**: Persistent state variables and mappings
2. **Event Logs**: Indexed historical data (cheaper than storage)
3. **Transaction History**: Complete audit trail of all state changes

### Access Control

Instead of database user permissions, access control uses:
- **Address-based authentication**: Cryptographic signature verification
- **Solidity modifiers**: `onlyOracle`, `onlyRegistry`, `onlyConsensus`
- **Role-based access**: Tier-based permissions (GreyGhost < LinkedID < PrimaryID < Oracle)

## Data Integrity Guarantees

### Blockchain Provides:
- ✅ Immutability: Data cannot be altered after commitment
- ✅ Transparency: All reads/writes are publicly visible
- ✅ Consensus: Network agrees on state
- ✅ Cryptographic proof: Digital signatures prevent impersonation

### Smart Contract Provides:
- ✅ Validation: Input checking via `require()` statements
- ✅ State machine logic: Tier progression rules
- ✅ Economic incentives: Staking/slashing for honest behavior
- ✅ Access control: Function-level permissions

## Performance Characteristics

| Operation | Cost | Speed |
|-----------|------|-------|
| Read (view function) | Free | ~100-500ms |
| Write (state change) | Gas fees (~$1-50) | ~12-15 seconds |
| Event query | Free | ~100ms-5s |
| Historical query | Free | ~1-30s |

## Network Configuration

### Local Development (Anvil)
- RPC: `http://127.0.0.1:8545`
- Chain ID: 31337
- Block time: Instant
- Cost: Free

### Sepolia Testnet
- RPC: `https://sepolia.infura.io/v3/YOUR_KEY`
- Chain ID: 11155111
- Block time: ~12 seconds
- Cost: Free (testnet ETH)

### Polygon Mumbai Testnet
- RPC: `https://polygon-mumbai.g.alchemy.com/v2/YOUR_KEY`
- Chain ID: 80001
- Block time: ~2 seconds
- Cost: Free (testnet MATIC)

## Environment Variables

```bash
# RPC Endpoints
SEPOLIA_RPC_URL=https://sepolia.infura.io/v3/YOUR_INFURA_KEY
MUMBAI_RPC_URL=https://polygon-mumbai.g.alchemy.com/v2/YOUR_ALCHEMY_KEY
POLYGON_RPC_URL=https://polygon-mainnet.g.alchemy.com/v2/YOUR_ALCHEMY_KEY

# Private Key (for signing transactions)
PRIVATE_KEY=0x...

# Contract Verification
ETHERSCAN_API_KEY=...
POLYGONSCAN_API_KEY=...

# Testing
GOD_MODE_ADDRESS=0x... # Admin address for testing
```

## Security Considerations

### Smart Contract Security
- ✅ Audited for reentrancy vulnerabilities
- ✅ Integer overflow protection (Solidity ^0.8.0)
- ✅ Access control on sensitive functions
- ✅ Economic disincentives for attacks (slashing)

### Private Key Management
- ⚠️ Never commit private keys to version control
- ⚠️ Use environment variables or secure key management
- ⚠️ Consider hardware wallets for mainnet deployments

### Data Privacy
- ⚠️ All blockchain data is publicly visible
- ⚠️ Do not store sensitive personal information on-chain
- ⚠️ Use justification fields carefully (visible to all)

## Contributing

When modifying the schema:

1. **Smart Contract Changes**:
   - Modify Solidity contracts in `contracts/identity/`
   - Update tests in `test/`
   - Run full test suite: `forge test -vvv`
   - Deploy new contract version
   - Update documentation

2. **Client Changes**:
   - Modify Kotlin data classes in `desktop-client/.../blockchain/`
   - Update ViewModel integration
   - Test against local Anvil node
   - Update integration documentation

3. **Documentation**:
   - Update relevant sections in `docs/database/`
   - Add query examples for new functionality
   - Document migration path from old version

## License

This documentation and associated code is part of the Knomee Identity project.

## Additional Resources

- [Solidity Documentation](https://docs.soliditylang.org/)
- [Web3j Documentation](https://docs.web3j.io/)
- [Foundry Book](https://book.getfoundry.sh/)
- [Ethereum Development Documentation](https://ethereum.org/developers)
