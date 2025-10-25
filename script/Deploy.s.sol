// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Script.sol";
import "../contracts/identity/GovernanceParameters.sol";
import "../contracts/identity/IdentityRegistry.sol";
import "../contracts/identity/IdentityConsensus.sol";

/**
 * @title Deploy Script for Knomee Identity Protocol
 * @notice Deploys all Phase 1 contracts to Sepolia testnet
 *
 * Usage:
 *   forge script script/Deploy.s.sol:DeployScript --rpc-url sepolia --broadcast --verify
 *
 * Environment variables needed:
 *   - SEPOLIA_RPC_URL
 *   - PRIVATE_KEY
 *   - ETHERSCAN_API_KEY (for verification)
 */
contract DeployScript is Script {
    function run() external {
        uint256 deployerPrivateKey = vm.envUint("PRIVATE_KEY");
        address deployer = vm.addr(deployerPrivateKey);

        console.log("Deploying contracts with address:", deployer);
        console.log("Deployer balance:", deployer.balance);

        vm.startBroadcast(deployerPrivateKey);

        // 1. Deploy GovernanceParameters
        console.log("\n1. Deploying GovernanceParameters...");
        GovernanceParameters params = new GovernanceParameters();
        console.log("GovernanceParameters deployed to:", address(params));

        // 2. Deploy IdentityRegistry
        console.log("\n2. Deploying IdentityRegistry...");
        IdentityRegistry registry = new IdentityRegistry();
        console.log("IdentityRegistry deployed to:", address(registry));

        // 3. Deploy IdentityConsensus
        console.log("\n3. Deploying IdentityConsensus...");
        IdentityConsensus consensus = new IdentityConsensus(
            address(registry),
            address(params)
        );
        console.log("IdentityConsensus deployed to:", address(consensus));

        // 4. Link contracts
        console.log("\n4. Linking contracts...");
        registry.setConsensusContract(address(consensus));
        console.log("Consensus contract linked to registry");

        // 5. Verify deployment
        console.log("\n=== DEPLOYMENT COMPLETE ===");
        console.log("GovernanceParameters:", address(params));
        console.log("IdentityRegistry:", address(registry));
        console.log("IdentityConsensus:", address(consensus));

        // 6. Verify configurations
        console.log("\n=== CONFIGURATION CHECK ===");
        console.log("Link threshold:", params.linkThreshold(), "bps (51%)");
        console.log("Primary threshold:", params.primaryThreshold(), "bps (67%)");
        console.log("Duplicate threshold:", params.duplicateThreshold(), "bps (80%)");
        console.log("Min stake:", params.minStakeWei(), "wei");
        console.log("God mode active:", params.godModeActive());

        console.log("\nConsensus contract in registry:", registry.consensusContract());
        console.log("Registry owner:", registry.owner());

        vm.stopBroadcast();

        // 7. Save deployment addresses
        console.log("\n=== SAVE THESE ADDRESSES ===");
        console.log("Add to your .env file:");
        console.log("GOVERNANCE_PARAMS_ADDRESS=", address(params));
        console.log("IDENTITY_REGISTRY_ADDRESS=", address(registry));
        console.log("IDENTITY_CONSENSUS_ADDRESS=", address(consensus));

        console.log("\n=== NEXT STEPS ===");
        console.log("1. Verify contracts on Etherscan");
        console.log("2. Grant Oracle status to trusted addresses:");
        console.log("   registry.upgradeToOracle(address)");
        console.log("3. Test with sample claims");
        console.log("4. When ready for mainnet, renounce god mode:");
        console.log("   params.renounceGodMode()");
    }
}
