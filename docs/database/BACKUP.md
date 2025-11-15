# Backup and Recovery Documentation

## Table of Contents

1. [Overview](#overview)
2. [Blockchain State Backup](#blockchain-state-backup)
3. [Node Synchronization](#node-synchronization)
4. [Contract State Export](#contract-state-export)
5. [Disaster Recovery Procedures](#disaster-recovery-procedures)
6. [Archive Node Requirements](#archive-node-requirements)
7. [Key Management and Security](#key-management-and-security)
8. [Monitoring and Alerts](#monitoring-and-alerts)

---

## Overview

### Blockchain Backup Philosophy

Traditional databases require regular backups to prevent data loss. Blockchain databases are fundamentally different:

| Aspect | Traditional DB | Blockchain DB |
|--------|---------------|---------------|
| Data Storage | Centralized server | Distributed across thousands of nodes |
| Backup Responsibility | Application owner | Network consensus |
| Data Persistence | Requires backup strategy | Immutable and permanent |
| Recovery Point | Last backup time | Any historical block |
| Single Point of Failure | Yes (if no backup) | No (decentralized) |

**Key Principle**: The blockchain itself IS the backup. Data is replicated across thousands of nodes globally and cannot be lost unless the entire network fails (virtually impossible for Ethereum).

### What Needs Backing Up?

1. **Private Keys** ⚠️ CRITICAL
   - Cannot be recovered if lost
   - Grants access to accounts and funds
   - Must be backed up securely

2. **Contract Addresses** 📝 Important
   - Needed to interact with deployed contracts
   - Can be recovered from deployment transaction logs

3. **Application State** 💾 Optional
   - Cached data for performance
   - Can be re-synced from blockchain

4. **ABI Files** 📄 Important
   - Required for encoding/decoding contract calls
   - Usually version-controlled in git

---

## Blockchain State Backup

### Option 1: Rely on Public Infrastructure

**Strategy**: Use public RPC nodes (Infura, Alchemy) and don't maintain your own node.

**Pros**:
- No maintenance required
- Always synchronized
- High availability (99.9%+ uptime)
- Multiple geographic regions

**Cons**:
- Dependency on third party
- Rate limits on free tier
- Potential privacy concerns

**Recommended For**: Most applications, especially during development and early production.

**Implementation**:
```kotlin
// .env
SEPOLIA_RPC_URL=https://sepolia.infura.io/v3/YOUR_INFURA_KEY
MAINNET_RPC_URL=https://eth-mainnet.g.alchemy.com/v2/YOUR_ALCHEMY_KEY

// Use multiple providers for redundancy
val primaryWeb3 = Web3j.build(HttpService(System.getenv("MAINNET_RPC_URL")))
val backupWeb3 = Web3j.build(HttpService(System.getenv("BACKUP_RPC_URL")))

suspend fun getIdentityWithFallback(address: String): IdentityData? {
    return try {
        repository.getIdentity(address) // Try primary
    } catch (e: Exception) {
        backupRepository.getIdentity(address) // Fallback to backup
    }
}
```

---

### Option 2: Run Your Own Archive Node

**Strategy**: Operate a full archive node that stores complete historical state.

**Pros**:
- Complete independence
- Full historical data access
- No rate limits
- Enhanced privacy

**Cons**:
- High resource requirements (~14 TB for Ethereum archive)
- Ongoing maintenance and monitoring
- Significant bandwidth usage

**Recommended For**: Large-scale applications, enterprises, or data-intensive use cases.

---

## Node Synchronization

### Running a Full Node

#### Hardware Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| CPU | 4 cores | 8+ cores |
| RAM | 16 GB | 32 GB |
| Storage (Full Node) | 2 TB SSD | 4 TB NVMe SSD |
| Storage (Archive) | 14 TB SSD | 16 TB NVMe SSD |
| Bandwidth | 10 Mbps | 25+ Mbps |

---

#### Option A: Geth (Go Ethereum)

**Installation**:
```bash
# Ubuntu/Debian
sudo add-apt-repository -y ppa:ethereum/ethereum
sudo apt-get update
sudo apt-get install ethereum

# Or via snap
sudo snap install geth --classic
```

**Sync Full Node**:
```bash
# Start Geth with snap sync (fastest)
geth --syncmode snap \
     --http \
     --http.api eth,net,web3 \
     --http.addr 0.0.0.0 \
     --http.port 8545 \
     --http.corsdomain "*" \
     --cache 4096 \
     --maxpeers 50

# Expected sync time: ~6-12 hours (full node)
```

**Sync Archive Node**:
```bash
# Archive mode (stores all historical state)
geth --syncmode full \
     --gcmode archive \
     --http \
     --http.api eth,net,web3,debug,trace \
     --http.addr 0.0.0.0 \
     --http.port 8545 \
     --cache 8192 \
     --maxpeers 50

# Expected sync time: ~2-4 weeks
# Storage: ~14 TB
```

**Monitor Sync Progress**:
```bash
# Attach to console
geth attach

# Check sync status
> eth.syncing
{
  currentBlock: 18500000,
  highestBlock: 18600000,
  knownStates: 1200000000,
  pulledStates: 1150000000,
  startingBlock: 0
}

# Check if synced
> eth.syncing
false  # ← Fully synced
```

---

#### Option B: Erigon (High-Performance Client)

**Installation**:
```bash
git clone https://github.com/ledgerwatch/erigon.git
cd erigon
make erigon

# Or via Docker
docker run -d \
  --name erigon \
  -v /data/erigon:/home/erigon/.local/share/erigon \
  -p 8545:8545 \
  thorax/erigon:latest \
  --chain mainnet \
  --http.api=eth,debug,net,trace,web3 \
  --http.vhosts=*
```

**Benefits**:
- 2-3x faster sync than Geth
- 50% less storage (~7 TB for archive)
- Better performance for historical queries

---

### Backup Node Data

#### Snapshot Backup

```bash
# Stop Geth
sudo systemctl stop geth

# Create snapshot
sudo tar -czf geth_backup_$(date +%Y%m%d).tar.gz /var/lib/geth/chaindata

# Or use rsync for incremental backups
rsync -av --delete /var/lib/geth/chaindata/ /backup/geth/

# Restart Geth
sudo systemctl start geth
```

**Storage Requirements**: Same as node size (~2 TB full, ~14 TB archive)

---

#### Database Export (Specific Contracts)

Export only relevant contract state instead of full blockchain:

```javascript
// scripts/exportContractState.js
const Web3 = require('web3');
const fs = require('fs');

const web3 = new Web3('http://localhost:8545');
const registryAddress = '0x...';

async function exportContractStorage() {
    const latestBlock = await web3.eth.getBlockNumber();
    const storage = {};

    // Export storage slots (brute force approach)
    for (let slot = 0; slot < 1000; slot++) {
        const value = await web3.eth.getStorageAt(
            registryAddress,
            slot,
            latestBlock
        );

        if (value !== '0x0000000000000000000000000000000000000000000000000000000000000000') {
            storage[slot] = value;
        }
    }

    fs.writeFileSync('contract_storage.json', JSON.stringify({
        contract: registryAddress,
        block: latestBlock,
        storage: storage
    }, null, 2));

    console.log(`Exported storage for block ${latestBlock}`);
}

exportContractStorage();
```

---

## Contract State Export

### Export All Identities

**Script**: `scripts/exportAllIdentities.js`

```javascript
const Web3 = require('web3');
const fs = require('fs');

const web3 = new Web3('http://localhost:8545');
const registryABI = require('../abi/IdentityRegistry.json');
const registryAddress = process.env.IDENTITY_REGISTRY_ADDRESS;

const registry = new web3.eth.Contract(registryABI, registryAddress);

async function exportAllIdentities() {
    console.log('Fetching all IdentityVerified events...');

    // Get all verification events from genesis
    const events = await registry.getPastEvents('IdentityVerified', {
        fromBlock: 0,
        toBlock: 'latest'
    });

    console.log(`Found ${events.length} verified identities`);

    const identities = [];

    for (let i = 0; i < events.length; i++) {
        const event = events[i];
        const address = event.returnValues.addr;

        console.log(`[${i+1}/${events.length}] Exporting ${address}...`);

        // Get full identity data
        const identity = await registry.methods.identities(address).call();

        // Get linked accounts
        const linkedAccounts = await registry.methods.linkedPlatforms(address).call();

        identities.push({
            address: address,
            tier: identity.tier,
            primaryAddress: identity.primaryAddress,
            verifiedAt: identity.verifiedAt,
            totalVouchesReceived: identity.totalVouchesReceived,
            totalStakeReceived: identity.totalStakeReceived,
            underChallenge: identity.underChallenge,
            challengeId: identity.challengeId,
            oracleGrantedAt: identity.oracleGrantedAt,
            linkedAccounts: linkedAccounts.map(acc => ({
                linkedAddress: acc.linkedAddress,
                platform: acc.platform,
                justification: acc.justification,
                linkedAt: acc.linkedAt
            })),
            exportedAt: Date.now(),
            exportedBlock: await web3.eth.getBlockNumber()
        });
    }

    const exportData = {
        timestamp: new Date().toISOString(),
        blockNumber: await web3.eth.getBlockNumber(),
        contractAddress: registryAddress,
        totalIdentities: identities.length,
        identities: identities
    };

    const filename = `identities_backup_${Date.now()}.json`;
    fs.writeFileSync(filename, JSON.stringify(exportData, null, 2));

    console.log(`\n✅ Export complete: ${filename}`);
    console.log(`   Total identities: ${identities.length}`);
    console.log(`   File size: ${(fs.statSync(filename).size / 1024 / 1024).toFixed(2)} MB`);
}

exportAllIdentities().catch(console.error);
```

**Usage**:
```bash
node scripts/exportAllIdentities.js
# Output: identities_backup_1699564800000.json
```

---

### Export All Claims

**Script**: `scripts/exportAllClaims.js`

```javascript
async function exportAllClaims() {
    const consensus = new web3.eth.Contract(consensusABI, consensusAddress);

    // Get all ClaimCreated events
    const events = await consensus.getPastEvents('ClaimCreated', {
        fromBlock: 0,
        toBlock: 'latest'
    });

    console.log(`Found ${events.length} claims`);

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
            vouches: vouches.map(v => ({
                voucher: v.voucher,
                isSupporting: v.isSupporting,
                weight: v.weight,
                stake: v.stake,
                vouchedAt: v.vouchedAt,
                rewardClaimed: v.rewardClaimed
            }))
        });
    }

    const filename = `claims_backup_${Date.now()}.json`;
    fs.writeFileSync(filename, JSON.stringify({
        timestamp: new Date().toISOString(),
        blockNumber: await web3.eth.getBlockNumber(),
        contractAddress: consensusAddress,
        totalClaims: claims.length,
        claims: claims
    }, null, 2));

    console.log(`✅ Export complete: ${filename}`);
}
```

---

### Automated Backup Schedule

**Cron Job** (daily backups):

```bash
# /etc/cron.d/knomee-backup
# Run backup every day at 2 AM
0 2 * * * /home/user/knomee-identity-8bit/scripts/backup.sh

# backup.sh
#!/bin/bash
cd /home/user/knomee-identity-8bit

# Export contract state
node scripts/exportAllIdentities.js
node scripts/exportAllClaims.js

# Compress and archive
tar -czf backups/backup_$(date +%Y%m%d).tar.gz *_backup_*.json

# Upload to S3 (optional)
aws s3 cp backups/backup_$(date +%Y%m%d).tar.gz s3://knomee-backups/

# Clean up old backups (keep last 30 days)
find backups/ -name "*.tar.gz" -mtime +30 -delete

echo "Backup complete: $(date)"
```

---

## Disaster Recovery Procedures

### Scenario 1: Lost Private Key

**Problem**: Private key for deployer account is lost.

**Impact**:
- ❌ Cannot call `onlyOwner` functions (e.g., governance updates)
- ✅ Contracts continue to function normally
- ✅ Users can still interact with system

**Recovery**:

1. **Transfer Ownership** (if still have access):
   ```solidity
   // From old owner account
   governanceParams.transferOwnership(newOwnerAddress);
   registry.transferOwnership(newOwnerAddress);
   ```

2. **If No Access**:
   - Contracts are immutable and will continue to function
   - Cannot update governance parameters
   - Must deploy new version and migrate users

**Prevention**:
- ✅ Use multi-sig wallet for ownership
- ✅ Store private key in hardware wallet
- ✅ Backup encrypted private key in multiple locations
- ✅ Use Shamir's Secret Sharing for key backup

---

### Scenario 2: Contract Bug Discovered

**Problem**: Critical bug found in deployed contract.

**Impact**:
- Depends on severity
- Potential loss of funds or data corruption

**Recovery**:

1. **Pause Contracts** (if pause functionality exists):
   ```solidity
   registry.pause(); // Stop new interactions
   ```

2. **Export Current State**:
   ```bash
   node scripts/exportAllIdentities.js
   node scripts/exportAllClaims.js
   ```

3. **Deploy Fixed Contract**:
   ```bash
   forge script script/DeployFixed.s.sol:DeployScript --broadcast
   ```

4. **Import State** (if new contract supports it):
   ```bash
   node scripts/importState.js identities_backup.json
   ```

5. **Update Client Configuration**:
   ```kotlin
   // Update contract addresses in .env
   IDENTITY_REGISTRY_ADDRESS=0x... # New address
   ```

6. **Notify Users**:
   - Publish announcement
   - Update documentation
   - Guide users to new contract

---

### Scenario 3: RPC Provider Outage

**Problem**: Primary RPC provider (Infura/Alchemy) is down.

**Impact**:
- ❌ Cannot read blockchain data
- ❌ Cannot submit transactions

**Recovery**:

1. **Automatic Failover**:
   ```kotlin
   class ResilientWeb3Service {
       private val providers = listOf(
           "https://eth-mainnet.g.alchemy.com/v2/KEY1",
           "https://mainnet.infura.io/v3/KEY2",
           "https://eth.public-rpc.com",
           "https://cloudflare-eth.com"
       )

       suspend fun call(block: suspend (Web3j) -> Any): Any? {
           for (rpc in providers) {
               try {
                   val web3 = Web3j.build(HttpService(rpc))
                   return block(web3)
               } catch (e: Exception) {
                   println("RPC $rpc failed: ${e.message}")
                   continue
               }
           }
           throw Exception("All RPC providers failed")
       }
   }
   ```

2. **Fallback to Local Node**:
   - Keep a synced local node as backup
   - Automatically switch if public RPCs fail

---

### Scenario 4: Blockchain Network Fork

**Problem**: Ethereum network experiences a contentious hard fork.

**Impact**:
- Network splits into two chains
- Contracts exist on both chains
- Users must choose which chain to support

**Recovery**:

1. **Monitor Situation**:
   - Watch block explorers for longest chain
   - Check community consensus

2. **Decide on Canonical Chain**:
   - Usually the chain with most consensus and hashpower

3. **Update RPC Endpoints**:
   - Point to canonical chain RPC nodes

4. **If Supporting Both Chains**:
   - Deploy separate instances for each chain
   - Make clear distinction in UI

---

## Archive Node Requirements

### Why Archive Nodes?

**Use Cases**:
1. **Historical State Queries**: Query state at any past block
2. **Analytics**: Analyze trends over time
3. **Auditing**: Verify historical transactions
4. **Development**: Test against mainnet historical data

**Example Query**:
```javascript
// Get identity tier at specific block in the past
const tier = await registry.methods.getTier(address).call({}, 15000000);
// Block 15000000 was in July 2022
```

---

### Archive Node Setup

**Geth Archive**:
```bash
geth --syncmode full \
     --gcmode archive \
     --http \
     --http.api eth,net,web3,debug,trace \
     --cache 8192 \
     --maxpeers 50 \
     --datadir /data/geth-archive

# Sync time: ~2-4 weeks
# Storage: ~14 TB (grows over time)
```

**Erigon Archive** (recommended for lower storage):
```bash
erigon --chain mainnet \
       --datadir /data/erigon \
       --http.api=eth,debug,net,trace,web3

# Sync time: ~1-2 weeks
# Storage: ~7 TB (50% less than Geth)
```

---

### Pruned vs Archive Comparison

| Feature | Pruned (Snap Sync) | Archive |
|---------|-------------------|---------|
| Storage | ~2 TB | ~14 TB (Geth) / ~7 TB (Erigon) |
| Sync Time | 6-12 hours | 2-4 weeks |
| Current State | ✅ Yes | ✅ Yes |
| Historical State | ❌ No | ✅ Yes (all blocks) |
| Tracing | ❌ Limited | ✅ Full |
| Debug APIs | ❌ No | ✅ Yes |

---

## Key Management and Security

### Private Key Backup

#### Method 1: Encrypted Backup

```bash
# Encrypt private key with GPG
echo "0xYOUR_PRIVATE_KEY" | gpg --symmetric --cipher-algo AES256 > private_key.gpg

# Store encrypted file in multiple locations:
# - USB drive (offline)
# - Password manager (1Password, LastPass)
# - Encrypted cloud storage (with different password)

# Decrypt when needed
gpg --decrypt private_key.gpg
```

---

#### Method 2: Shamir's Secret Sharing

Split private key into N shares, require M shares to reconstruct:

```python
# pip install secretsharing
from secretsharing import PlaintextToHexSecretSharer

# Split into 5 shares, need 3 to recover
shares = PlaintextToHexSecretSharer.split_secret("0xYOUR_PRIVATE_KEY", 3, 5)

# Distribute shares to:
# Share 1: Person A
# Share 2: Person B
# Share 3: Person C
# Share 4: Safety deposit box
# Share 5: Lawyer

# Recover with any 3 shares
recovered = PlaintextToHexSecretSharer.recover_secret(shares[0:3])
```

---

#### Method 3: Hardware Wallet

**Best Practice**: Use hardware wallet for production contracts.

**Ledger Integration**:
```kotlin
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j

// Connect to Ledger
val credentials = LedgerCredentials(
    chainId = 1, // Mainnet
    derivationPath = "m/44'/60'/0'/0/0"
)

// Sign transaction with Ledger (requires physical confirmation)
val signed = credentials.sign(rawTransaction)
```

**Benefits**:
- Private key never leaves device
- Physical confirmation required for transactions
- Protected against malware

---

### Mnemonic Seed Phrase Backup

If using HD wallets:

```
1. Write down 12/24 word seed phrase
2. Store in fireproof safe
3. NEVER store digitally (photos, cloud, etc.)
4. Consider metal backup (cryptosteel)
5. Test recovery before funding
```

**Example**:
```
witch collapse practice feed shame open despair creek road again ice least
```

⚠️ **WARNING**: Anyone with seed phrase has full access to all derived accounts!

---

## Monitoring and Alerts

### Uptime Monitoring

**Script**: `scripts/monitor.sh`

```bash
#!/bin/bash

RPC_URL="https://eth-mainnet.alchemyapi.io/v2/YOUR_KEY"
REGISTRY_ADDRESS="0x..."

# Check if RPC is responsive
check_rpc() {
    response=$(curl -s -X POST $RPC_URL \
        -H "Content-Type: application/json" \
        -d '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}')

    if [[ $response == *"result"* ]]; then
        echo "✅ RPC is healthy"
        return 0
    else
        echo "❌ RPC is down!"
        return 1
    fi
}

# Check if contract is accessible
check_contract() {
    # Try to call a view function
    response=$(curl -s -X POST $RPC_URL \
        -H "Content-Type: application/json" \
        -d "{\"jsonrpc\":\"2.0\",\"method\":\"eth_call\",\"params\":[{\"to\":\"$REGISTRY_ADDRESS\",\"data\":\"0x...\"},\"latest\"],\"id\":1}")

    if [[ $response == *"result"* ]]; then
        echo "✅ Contract is accessible"
        return 0
    else
        echo "❌ Contract call failed!"
        return 1
    fi
}

# Send alert via webhook
send_alert() {
    curl -X POST "https://hooks.slack.com/YOUR_WEBHOOK" \
        -H 'Content-Type: application/json' \
        -d "{\"text\":\"🚨 Knomee Identity System Alert: $1\"}"
}

# Run checks
if ! check_rpc; then
    send_alert "RPC provider is down!"
fi

if ! check_contract; then
    send_alert "Contract is not accessible!"
fi
```

**Cron** (run every 5 minutes):
```cron
*/5 * * * * /home/user/knomee-identity-8bit/scripts/monitor.sh
```

---

### Metrics to Monitor

| Metric | Alert Threshold | Check Interval |
|--------|----------------|----------------|
| RPC Response Time | > 5 seconds | 1 minute |
| RPC Error Rate | > 5% | 1 minute |
| Node Sync Status | Behind by > 10 blocks | 5 minutes |
| Gas Price | > 200 gwei | 10 minutes |
| Contract Event Rate | Anomaly detection | 1 hour |
| Disk Usage (node) | > 90% | 1 hour |

---

### Grafana Dashboard

**Prometheus Metrics**:
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'knomee-identity'
    static_configs:
      - targets: ['localhost:9090']

# Metrics to collect:
# - rpc_response_time_seconds
# - rpc_errors_total
# - contract_calls_total
# - transaction_gas_used
# - node_sync_block_number
```

**Grafana Panels**:
1. RPC Latency (95th percentile)
2. Transaction Success Rate
3. Active Claims Count
4. New Identity Verifications (per day)
5. Gas Price Trends

---

## Summary

### Backup Checklist

- [x] **Private Keys**: Encrypted, multi-location backup
- [x] **Contract Addresses**: Documented in .env and README
- [x] **ABI Files**: Version controlled in git
- [x] **Deployment Scripts**: Version controlled
- [x] **State Exports**: Automated daily backups
- [x] **RPC Redundancy**: Multiple providers configured
- [x] **Monitoring**: Uptime checks and alerts
- [x] **Documentation**: Disaster recovery procedures written

### Recovery Time Objectives

| Scenario | RTO | RPO |
|----------|-----|-----|
| RPC Provider Outage | < 1 minute (auto-failover) | 0 (no data loss) |
| Contract Bug | 1-7 days (deploy + migrate) | Depends on last export |
| Lost Private Key | N/A (immutable) | N/A |
| Node Failure | < 24 hours (restore from snapshot) | 0 (blockchain consensus) |

---

This backup and recovery documentation ensures robust data protection and disaster recovery capabilities for the Knomee Identity blockchain database system.
