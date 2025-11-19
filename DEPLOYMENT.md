# Knomee Identity Protocol - Sepolia Deployment Guide

Complete guide for deploying the Knomee Identity Protocol to Sepolia testnet.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Pre-Deployment Checklist](#pre-deployment-checklist)
4. [Deployment Steps](#deployment-steps)
5. [Post-Deployment Configuration](#post-deployment-configuration)
6. [Verification](#verification)
7. [Testing](#testing)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Tools
- **Foundry** (latest version)
  ```bash
  curl -L https://foundry.paradigm.xyz | bash
  foundryup
  ```

- **Git** (for version control)
- **Node.js** (optional, for additional tooling)

### Required Accounts
- **Deployer Wallet**
  - Funded with Sepolia ETH (minimum 0.1 ETH recommended)
  - Get Sepolia ETH from faucets:
    - https://sepoliafaucet.com/
    - https://www.alchemy.com/faucets/ethereum-sepolia
    - https://faucet.quicknode.com/ethereum/sepolia

- **Etherscan API Key**
  - Create account at https://etherscan.io/
  - Generate API key at https://etherscan.io/myapikey

- **Alchemy/Infura RPC**
  - Alchemy: https://www.alchemy.com/ (recommended)
  - Infura: https://infura.io/

---

## Environment Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd knomee-identity-8bit
```

### 2. Install Dependencies
```bash
forge install
```

### 3. Create Environment File
```bash
cp .env.example .env
```

### 4. Configure .env File
Edit `.env` with your credentials:

```bash
# RPC Endpoints
SEPOLIA_RPC_URL=https://eth-sepolia.g.alchemy.com/v2/YOUR_API_KEY

# Deployer Private Key (NEVER commit this!)
PRIVATE_KEY=your_private_key_without_0x_prefix

# Etherscan API Key (for contract verification)
ETHERSCAN_API_KEY=your_etherscan_api_key

# God Mode Address (optional, for testing)
GOD_MODE_ADDRESS=your_deployer_address
```

**⚠️ SECURITY WARNING**
- NEVER commit your `.env` file
- NEVER share your private key
- Use a dedicated deployment wallet, not your main wallet
- `.env` is already in `.gitignore`

---

## Pre-Deployment Checklist

Run these checks before deploying:

### 1. Verify Foundry Installation
```bash
forge --version
# Should show: forge 0.2.0 or higher
```

### 2. Verify Contract Compilation
```bash
forge build
# Should compile without errors
```

### 3. Run Tests
```bash
forge test
# All tests should pass
```

### 4. Check Deployer Balance
```bash
cast balance $GOD_MODE_ADDRESS --rpc-url sepolia
# Should show > 0.1 ETH (100000000000000000 wei)
```

### 5. Verify Environment Variables
```bash
source .env
echo $SEPOLIA_RPC_URL
echo $ETHERSCAN_API_KEY
# Should display your values (not empty)
```

---

## Deployment Steps

### Option 1: Full Deployment (Recommended)

Deploy all contracts in one transaction:

```bash
forge script script/DeploySepolia.s.sol:DeploySepolia \
  --rpc-url sepolia \
  --broadcast \
  --verify \
  -vvvv
```

**Flags explained:**
- `--rpc-url sepolia`: Use Sepolia network
- `--broadcast`: Actually send transactions (omit for dry run)
- `--verify`: Automatically verify on Etherscan
- `-vvvv`: Very verbose output (helpful for debugging)

**Expected output:**
```
========================================
  KNOMEE IDENTITY PROTOCOL DEPLOYMENT
  Network: Sepolia Testnet
========================================

Deployer address: 0x...
Deployer balance: 0.5 ETH

[1/5] Deploying GovernanceParameters...
  >> Deployed at: 0x...
  >> God mode active: true

[2/5] Deploying IdentityToken (IDT)...
  >> Deployed at: 0x...
  >> Name: Knomee Identity Token
  >> Symbol: IDT

...

========================================
  DEPLOYMENT COMPLETE!
========================================
```

### Option 2: Dry Run First

Test deployment without broadcasting:

```bash
forge script script/DeploySepolia.s.sol:DeploySepolia \
  --rpc-url sepolia \
  -vvv
```

Review the output, then add `--broadcast` to execute.

### 3. Save Deployment Addresses

The script will output addresses like:

```
=== ADD TO YOUR .env FILE ===
GOVERNANCE_PARAMS_ADDRESS=0x...
IDENTITY_TOKEN_ADDRESS=0x...
KNOMEE_TOKEN_ADDRESS=0x...
IDENTITY_REGISTRY_ADDRESS=0x...
IDENTITY_CONSENSUS_ADDRESS=0x...
```

**Add these to your `.env` file** for later use.

---

## Post-Deployment Configuration

### 1. Grant Oracle Status

Oracles have 100x voting weight and are trusted verifiers.

**Edit `script/GrantOracle.s.sol`:**
```solidity
// Update contract addresses
address constant IDENTITY_REGISTRY = 0xYOUR_REGISTRY_ADDRESS;
address constant IDENTITY_TOKEN = 0xYOUR_IDENTITY_TOKEN_ADDRESS;

// Add Oracle addresses
address[] ORACLE_ADDRESSES = [
    0x1234..., // Trusted verifier 1
    0x5678..., // Trusted verifier 2
    // Add more as needed
];
```

**Run the script:**
```bash
forge script script/GrantOracle.s.sol:GrantOracle \
  --rpc-url sepolia \
  --broadcast \
  -vvv
```

### 2. Distribute KNOW Tokens

Users need KNOW tokens to stake on claims.

**Edit `script/DistributeTokens.s.sol`:**
```solidity
address constant KNOMEE_TOKEN = 0xYOUR_KNOMEE_TOKEN_ADDRESS;

function setupRecipients() internal {
    recipients.push(Recipient({
        account: 0x1234...,
        amount: 100,  // 100 KNOW
        role: "Regular Tester"
    }));

    recipients.push(Recipient({
        account: 0x5678...,
        amount: 500,  // 500 KNOW
        role: "Oracle"
    }));
}
```

**Run the script:**
```bash
forge script script/DistributeTokens.s.sol:DistributeTokens \
  --rpc-url sepolia \
  --broadcast \
  -vvv
```

---

## Verification

### Automatic Verification

If you used `--verify` flag during deployment, contracts should auto-verify. Check Etherscan:

```
https://sepolia.etherscan.io/address/0xYOUR_CONTRACT_ADDRESS
```

Look for green checkmark ✅ next to contract name.

### Manual Verification

If auto-verification failed:

**Update `script/VerifyContracts.s.sol` with your addresses:**
```solidity
address constant GOVERNANCE_PARAMS = 0x...;
address constant IDENTITY_TOKEN = 0x...;
// ... etc
```

**Get verification commands:**
```bash
forge script script/VerifyContracts.s.sol:VerifyContracts --rpc-url sepolia
```

Copy and run each command individually.

### Verify Contract Linking

Check that contracts are properly linked:

```bash
# Check IdentityRegistry configuration
cast call $IDENTITY_REGISTRY_ADDRESS \
  "consensusContract()(address)" \
  --rpc-url sepolia

# Should return your IdentityConsensus address

# Check KnomeeToken configuration
cast call $KNOMEE_TOKEN_ADDRESS \
  "registryContract()(address)" \
  --rpc-url sepolia

# Should return your IdentityRegistry address
```

---

## Testing

### 1. Verify Protocol Parameters

```bash
# Check governance parameters
cast call $GOVERNANCE_PARAMS_ADDRESS \
  "primaryThreshold()(uint256)" \
  --rpc-url sepolia
# Should return: 6700 (67%)

cast call $GOVERNANCE_PARAMS_ADDRESS \
  "primaryStake()(uint256)" \
  --rpc-url sepolia
# Should return: 30000000000000000000 (30 KNOW)
```

### 2. Check Token Balances

```bash
# Check KNOW token supply
cast call $KNOMEE_TOKEN_ADDRESS \
  "totalSupply()(uint256)" \
  --rpc-url sepolia
# Should return: 1000000000000000000000000000 (1 billion KNOW)

# Check rewards pool
cast call $KNOMEE_TOKEN_ADDRESS \
  "rewardsPoolBalance()(uint256)" \
  --rpc-url sepolia
# Should return: 400000000000000000000000000 (400 million KNOW)
```

### 3. Test with Sample Claim (Optional)

Use the TestProtocol script to simulate real usage:

**Update `script/TestProtocol.s.sol` with your addresses and test accounts.**

```bash
forge script script/TestProtocol.s.sol:TestProtocol \
  --rpc-url sepolia \
  --broadcast \
  -vvv
```

---

## Protocol Usage

### For Regular Users

#### Request Primary Verification (become verified human)

1. **Approve KNOW tokens:**
   ```bash
   cast send $KNOMEE_TOKEN_ADDRESS \
     "approve(address,uint256)" \
     $IDENTITY_CONSENSUS_ADDRESS \
     30000000000000000000 \
     --rpc-url sepolia \
     --private-key $USER_PRIVATE_KEY
   ```

2. **Submit claim:**
   ```bash
   cast send $IDENTITY_CONSENSUS_ADDRESS \
     "requestPrimaryVerification()(uint256)" \
     --rpc-url sepolia \
     --private-key $USER_PRIVATE_KEY
   ```

3. **Get your claim ID from the transaction receipt**

#### Link Secondary Account

1. **Approve 10 KNOW**
2. **Call:**
   ```bash
   cast send $IDENTITY_CONSENSUS_ADDRESS \
     "requestLinkToPrimary(address,string)" \
     $SECONDARY_ADDRESS \
     "instagram" \
     --rpc-url sepolia \
     --private-key $PRIMARY_PRIVATE_KEY
   ```

### For Oracles

#### Vouch FOR a claim

```bash
cast send $IDENTITY_CONSENSUS_ADDRESS \
  "vouchFor(uint256,uint256)" \
  $CLAIM_ID \
  50000000000000000000 \
  --rpc-url sepolia \
  --private-key $ORACLE_PRIVATE_KEY
```

#### Vouch AGAINST a claim

```bash
cast send $IDENTITY_CONSENSUS_ADDRESS \
  "vouchAgainst(uint256,uint256)" \
  $CLAIM_ID \
  50000000000000000000 \
  --rpc-url sepolia \
  --private-key $ORACLE_PRIVATE_KEY
```

#### Claim Rewards

After voting on a resolved claim:

```bash
cast send $IDENTITY_CONSENSUS_ADDRESS \
  "claimRewards(uint256)" \
  $CLAIM_ID \
  --rpc-url sepolia \
  --private-key $ORACLE_PRIVATE_KEY
```

---

## Troubleshooting

### Deployment Failed

**Error: Insufficient funds**
```
Solution: Get more Sepolia ETH from faucet
```

**Error: Nonce too high**
```bash
Solution: Reset nonce
cast nonce $YOUR_ADDRESS --rpc-url sepolia
# Use the returned nonce in next transaction
```

**Error: Contract already deployed**
```
Solution: Check if previous deployment succeeded
Use a fresh deployer account or wait for nonce to sync
```

### Verification Failed

**Error: Contract source code already verified**
```
Solution: Verification successful! Check Etherscan
```

**Error: Unable to verify**
```
Solution: Use manual verification commands from VerifyContracts.s.sol
Wait 1-2 minutes after deployment before verifying
```

### Gas Issues

**Error: Gas price too high**
```bash
Solution: Wait for lower gas, or specify max fee:
--with-gas-price 50gwei
```

**Error: Transaction underpriced**
```bash
Solution: Increase gas price:
--with-gas-price 100gwei
```

### Contract Interaction Issues

**Error: Execution reverted**
```
Check:
1. Contract addresses are correct
2. You have sufficient token balance
3. You've approved token spending
4. Your account has the required permissions
```

**Debug with simulation:**
```bash
cast call $CONTRACT \
  "functionName(args)" \
  --rpc-url sepolia
```

---

## Security Checklist

Before mainnet deployment:

- [ ] All tests pass (`forge test`)
- [ ] Contracts verified on Etherscan
- [ ] Oracle permissions granted only to trusted addresses
- [ ] God mode renounced (`params.renounceGodMode()`)
- [ ] Ownership transferred to multisig
- [ ] Code audited by security firm
- [ ] Bug bounty program launched
- [ ] Emergency pause mechanism tested
- [ ] Upgrade paths documented
- [ ] Deployment addresses backed up securely

---

## Gas Costs (Estimated)

Approximate gas costs on Sepolia:

| Contract | Gas Used | Cost (50 gwei) |
|----------|----------|----------------|
| GovernanceParameters | ~1.2M | ~0.06 ETH |
| IdentityToken | ~2.0M | ~0.10 ETH |
| KnomeeToken | ~2.5M | ~0.13 ETH |
| IdentityRegistry | ~2.8M | ~0.14 ETH |
| IdentityConsensus | ~4.0M | ~0.20 ETH |
| **Total Deployment** | **~12.5M** | **~0.63 ETH** |

*Note: Actual costs vary with gas prices*

---

## Contract Addresses (Template)

After deployment, fill this in:

```
Network: Sepolia Testnet
Deployed: YYYY-MM-DD

GovernanceParameters: 0x...
IdentityToken (IDT):   0x...
KnomeeToken (KNOW):    0x...
IdentityRegistry:      0x...
IdentityConsensus:     0x...

Deployer: 0x...
Initial Oracles:
- 0x... (Name)
- 0x... (Name)
```

---

## Next Steps

1. **Test thoroughly** with multiple users and Oracles
2. **Monitor gas costs** and optimize if needed
3. **Gather feedback** from early testers
4. **Implement Phase 2** (ProductRegistry, ReputationDistribution)
5. **Prepare for mainnet** (audit, security review)
6. **Launch!** 🚀

---

## Support & Resources

- **Documentation**: See `KNOMEE_IDENTITY_PROTOCOL_V1.md`
- **Tokenomics**: See `TOKENOMICS.md`
- **Progress**: See `PROGRESS.md`
- **Issues**: [GitHub Issues]
- **Foundry Docs**: https://book.getfoundry.sh/

---

## License

MIT License - See LICENSE file for details
