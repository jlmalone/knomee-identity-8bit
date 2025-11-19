# Knomee Identity Protocol - Solana Deployment Guide

## Pre-Deployment Checklist

### 1. Code Review
- [ ] All smart contract code reviewed
- [ ] Security audit completed
- [ ] Test coverage > 90%
- [ ] No hardcoded addresses or secrets
- [ ] God mode renouncement plan in place

### 2. Environment Setup
- [ ] Solana CLI installed and configured
- [ ] Anchor CLI installed (v0.29.0+)
- [ ] Deployment wallet funded with SOL
- [ ] Program keypair generated and backed up
- [ ] All dependencies installed

### 3. Token Preparation
- [ ] KNOW token deployed and verified
- [ ] IDT token program deployed (Token-2022 with transfer restrictions)
- [ ] Token mint authorities configured
- [ ] Initial token distribution planned

## Deployment Steps

### Localnet (Testing)

1. **Start local validator**
   ```bash
   solana-test-validator
   ```

2. **Deploy program**
   ```bash
   cd solana-programs
   anchor build
   anchor deploy
   ```

3. **Initialize governance**
   ```bash
   anchor run initialize-governance
   ```

4. **Test with sample data**
   ```bash
   anchor test
   ```

### Devnet (Staging)

1. **Configure Solana CLI for devnet**
   ```bash
   solana config set --url devnet
   ```

2. **Create/fund deployment wallet**
   ```bash
   # Generate new keypair (or use existing)
   solana-keygen new --outfile ~/.config/solana/devnet-deploy.json

   # Airdrop SOL for deployment
   solana airdrop 2 $(solana address -k ~/.config/solana/devnet-deploy.json)
   ```

3. **Update Anchor.toml for devnet**
   ```toml
   [provider]
   cluster = "devnet"
   wallet = "~/.config/solana/devnet-deploy.json"
   ```

4. **Build and deploy**
   ```bash
   anchor build
   anchor deploy --provider.cluster devnet
   ```

5. **Verify deployment**
   ```bash
   # Get program ID
   solana address -k target/deploy/knomee_identity-keypair.json

   # Check program account
   solana program show <PROGRAM_ID>
   ```

6. **Initialize governance**
   ```bash
   anchor run initialize-governance -- --provider.cluster devnet
   ```

7. **Deploy KNOW token (if not already deployed)**
   ```bash
   spl-token create-token --decimals 9
   # Save token mint address
   ```

8. **Test on devnet**
   ```bash
   anchor test --provider.cluster devnet --skip-local-validator
   ```

### Mainnet (Production)

⚠️ **CRITICAL**: Complete all testing on devnet before mainnet deployment.

1. **Final security checks**
   ```bash
   # Run full test suite
   anchor test

   # Run security audit tools
   cargo audit
   cargo clippy -- -D warnings
   ```

2. **Prepare mainnet wallet**
   ```bash
   # Use hardware wallet or secure key management
   # Ensure sufficient SOL for deployment (~5-10 SOL recommended)
   solana balance
   ```

3. **Update Anchor.toml for mainnet**
   ```toml
   [provider]
   cluster = "mainnet-beta"
   wallet = "~/.config/solana/mainnet-deploy.json"

   [programs.mainnet]
   knomee_identity = "YOUR_PROGRAM_ID_HERE"
   ```

4. **Build with mainnet configuration**
   ```bash
   anchor build
   ```

5. **Deploy to mainnet**
   ```bash
   # This will cost ~2-5 SOL
   anchor deploy --provider.cluster mainnet

   # Verify deployment
   solana program show <PROGRAM_ID>
   ```

6. **Initialize governance (CAREFULLY)**
   ```bash
   # Double-check all parameters
   anchor run initialize-governance -- --provider.cluster mainnet
   ```

7. **Verify governance initialization**
   ```bash
   # Use explorer to verify governance account
   # https://explorer.solana.com/address/<GOVERNANCE_PDA>?cluster=mainnet
   ```

8. **Deploy and configure tokens**
   ```bash
   # Deploy KNOW token
   spl-token create-token --decimals 9

   # Create initial supply
   spl-token create-account <KNOW_MINT>
   spl-token mint <KNOW_MINT> 1000000000 # 1 billion tokens
   ```

9. **Renounce god mode** (AFTER thorough testing)
   ```bash
   anchor run renounce-god-mode -- --provider.cluster mainnet

   # VERIFY god mode is renounced
   # This is IRREVERSIBLE
   ```

10. **Transfer upgrade authority** (optional, for full decentralization)
    ```bash
    # Transfer to multisig or governance contract
    solana program set-upgrade-authority <PROGRAM_ID> \
      --new-upgrade-authority <GOVERNANCE_ADDRESS>
    ```

## Post-Deployment

### 1. Verification

- [ ] Program deployed successfully
- [ ] Governance initialized with correct parameters
- [ ] God mode renounced
- [ ] Token contracts deployed
- [ ] Initial identities created successfully
- [ ] Claims can be created and resolved
- [ ] Rewards distribution working

### 2. Monitoring

```bash
# Monitor program logs
solana logs <PROGRAM_ID>

# Check program balance
solana balance <PROGRAM_ID>

# View recent transactions
solana transaction-history <PROGRAM_ID>
```

### 3. Documentation

- [ ] Update README with mainnet addresses
- [ ] Publish program IDL
- [ ] Create integration guide for DApps
- [ ] Write migration guide for existing users
- [ ] Update frontend with mainnet configuration

## Rollback Plan

If critical issues are discovered post-deployment:

1. **Immediate actions**
   - Pause new claims (if pause functionality exists)
   - Notify users via official channels
   - Document the issue

2. **Code fix**
   - Fix the bug in new version
   - Test extensively on devnet
   - Security audit the fix

3. **Upgrade**
   - Deploy new version
   - Use upgrade authority to update program
   - Verify fix on mainnet
   - Resume operations

## Upgrade Strategy

Solana programs are upgradeable by default. To upgrade:

```bash
# Build new version
anchor build

# Deploy upgrade (requires upgrade authority)
anchor upgrade <PROGRAM_ID> target/deploy/knomee_identity.so

# Verify upgrade
solana program show <PROGRAM_ID>
```

## Cost Estimates

### Deployment Costs (Mainnet)

| Item | Approximate Cost | Notes |
|------|------------------|-------|
| Program deployment | 2-5 SOL | Depends on program size |
| Governance initialization | 0.001 SOL | One-time |
| Token deployment (KNOW) | 0.01 SOL | SPL token creation |
| Token deployment (IDT) | 0.01 SOL | Token-2022 |
| Buffer account | 1-2 SOL | Required for upgrades |
| **Total** | **~3-8 SOL** | **Plus buffer for operations** |

### Operational Costs

| Operation | Approximate Cost | Notes |
|-----------|------------------|-------|
| Initialize identity | 0.001 SOL | Rent-exempt account |
| Create claim | 0.002 SOL | Larger account |
| Cast vote | 0.001 SOL | Create vouch account |
| Resolve claim | 0.0005 SOL | Update accounts |
| Claim rewards | 0.0005 SOL | Token transfer |

## Security Considerations

### Pre-Deployment
1. **Code audits**
   - Internal review
   - External security audit
   - Formal verification (if applicable)

2. **Testing**
   - Unit tests (100% coverage)
   - Integration tests
   - Stress tests
   - Sybil attack simulations

3. **Economic analysis**
   - Game theory review
   - Economic attack vectors
   - Incentive alignment verification

### Post-Deployment
1. **Monitoring**
   - Set up alerting for unusual activity
   - Monitor program logs
   - Track economic metrics

2. **Bug bounty**
   - Launch bug bounty program
   - Define scope and rewards
   - Establish responsible disclosure process

3. **Incident response**
   - Document emergency procedures
   - Identify key stakeholders
   - Prepare communication templates

## Governance Parameters

Recommended initial values for mainnet:

```rust
GovernanceParams {
    // Consensus thresholds
    link_threshold: 5100,              // 51%
    primary_threshold: 6700,           // 67%
    duplicate_threshold: 8000,         // 80%

    // Staking (assuming 9 decimals)
    min_stake_lamports: 10_000_000,    // 0.01 KNOW
    primary_stake_multiplier: 3,       // 0.03 KNOW for Primary
    duplicate_stake_multiplier: 10,    // 0.1 KNOW for Duplicate

    // Slashing
    link_slash_bps: 1000,              // 10%
    primary_slash_bps: 3000,           // 30%
    duplicate_slash_bps: 5000,         // 50%
    sybil_slash_bps: 10000,            // 100%

    // Voting weights
    primary_vote_weight: 1,
    oracle_vote_weight: 100,

    // Cooldowns
    failed_claim_cooldown: 604_800,    // 7 days
    duplicate_flag_cooldown: 2_592_000, // 30 days
    claim_expiry_duration: 2_592_000,  // 30 days

    // Decay rates
    oracle_decay_rate_bps: 10,         // 0.1%/day
    admin_decay_rate_bps: 50,          // 0.5%/day
}
```

## Contact & Support

- **Emergency Contact**: [emergency@knomee.io]
- **Technical Support**: [GitHub Issues](https://github.com/jlmalone/knomee-identity-8bit/issues)
- **Security Reports**: [security@knomee.io] (PGP key available)

---

**Remember**: Mainnet deployment is irreversible. Triple-check everything before deploying.

**Good luck! 🚀**
