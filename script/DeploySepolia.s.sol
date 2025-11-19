// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Script.sol";
import "../contracts/identity/GovernanceParameters.sol";
import "../contracts/identity/IdentityRegistry.sol";
import "../contracts/identity/IdentityConsensus.sol";
import "../contracts/identity/IdentityToken.sol";
import "../contracts/identity/KnomeeToken.sol";

/**
 * @title DeploySepolia - Complete Sepolia Deployment Script
 * @notice Deploys all Phase 1 Knomee Identity Protocol contracts to Sepolia testnet
 * @dev This script handles the complete deployment and configuration of all contracts
 *
 * Prerequisites:
 * - .env file with SEPOLIA_RPC_URL, PRIVATE_KEY, ETHERSCAN_API_KEY
 * - Deployer wallet funded with Sepolia ETH (get from faucet)
 * - Foundry installed and up to date
 *
 * Usage:
 *   forge script script/DeploySepolia.s.sol:DeploySepolia --rpc-url sepolia --broadcast --verify -vvvv
 *
 * Deployment order:
 * 1. GovernanceParameters (protocol configuration)
 * 2. IdentityToken (soul-bound NFT)
 * 3. KnomeeToken (ERC-20 staking token)
 * 4. IdentityRegistry (identity state management)
 * 5. IdentityConsensus (voting and claims)
 * 6. Link all contracts together
 * 7. Verify configuration
 */
contract DeploySepolia is Script {
    // Deployment tracking
    GovernanceParameters public params;
    IdentityToken public identityToken;
    KnomeeToken public knomeeToken;
    IdentityRegistry public registry;
    IdentityConsensus public consensus;

    function run() external {
        // Load environment variables
        uint256 deployerPrivateKey = vm.envUint("PRIVATE_KEY");
        address deployer = vm.addr(deployerPrivateKey);

        console.log("========================================");
        console.log("  KNOMEE IDENTITY PROTOCOL DEPLOYMENT");
        console.log("  Network: Sepolia Testnet");
        console.log("========================================");
        console.log("");
        console.log("Deployer address:", deployer);
        console.log("Deployer balance:", deployer.balance / 1e18, "ETH");

        require(deployer.balance >= 0.1 ether, "Insufficient balance. Need at least 0.1 ETH for deployment");

        // Start broadcasting transactions
        vm.startBroadcast(deployerPrivateKey);

        // Step 1: Deploy GovernanceParameters
        console.log("\n[1/5] Deploying GovernanceParameters...");
        params = new GovernanceParameters();
        console.log("  >> Deployed at:", address(params));
        console.log("  >> God mode active:", params.godModeActive());

        // Step 2: Deploy IdentityToken (Soul-bound NFT)
        console.log("\n[2/5] Deploying IdentityToken (IDT)...");
        identityToken = new IdentityToken();
        console.log("  >> Deployed at:", address(identityToken));
        console.log("  >> Name:", identityToken.name());
        console.log("  >> Symbol:", identityToken.symbol());

        // Step 3: Deploy KnomeeToken (Staking token)
        console.log("\n[3/5] Deploying KnomeeToken (KNOW)...");
        knomeeToken = new KnomeeToken();
        console.log("  >> Deployed at:", address(knomeeToken));
        console.log("  >> Name:", knomeeToken.name());
        console.log("  >> Symbol:", knomeeToken.symbol());
        console.log("  >> Total Supply:", knomeeToken.totalSupply() / 1e18, "KNOW");
        console.log("  >> Rewards Pool:", knomeeToken.rewardsPoolBalance() / 1e18, "KNOW");

        // Step 4: Deploy IdentityRegistry
        console.log("\n[4/5] Deploying IdentityRegistry...");
        registry = new IdentityRegistry();
        console.log("  >> Deployed at:", address(registry));
        console.log("  >> Owner:", registry.owner());

        // Step 5: Deploy IdentityConsensus
        console.log("\n[5/5] Deploying IdentityConsensus...");
        consensus = new IdentityConsensus(
            address(registry),
            address(params),
            address(identityToken),
            address(knomeeToken)
        );
        console.log("  >> Deployed at:", address(consensus));

        // Step 6: Link all contracts
        console.log("\n========================================");
        console.log("  LINKING CONTRACTS");
        console.log("========================================");

        console.log("\nConfiguring IdentityRegistry...");
        registry.setConsensusContract(address(consensus));
        console.log("  >> Consensus contract linked");

        registry.setIdentityToken(address(identityToken));
        console.log("  >> IdentityToken linked");

        console.log("\nConfiguring IdentityToken...");
        identityToken.setIdentityRegistry(address(registry));
        console.log("  >> IdentityRegistry linked");

        console.log("\nConfiguring KnomeeToken...");
        knomeeToken.setRegistryContract(address(registry));
        console.log("  >> IdentityRegistry linked");

        knomeeToken.setConsensusContract(address(consensus));
        console.log("  >> IdentityConsensus linked");

        // Step 7: Verify deployment
        console.log("\n========================================");
        console.log("  DEPLOYMENT VERIFICATION");
        console.log("========================================");

        verifyDeployment();

        vm.stopBroadcast();

        // Print deployment summary
        printDeploymentSummary();
    }

    function verifyDeployment() internal view {
        console.log("\nGovernance Parameters:");
        console.log("  Link threshold:", params.linkThreshold(), "bps (51%)");
        console.log("  Primary threshold:", params.primaryThreshold(), "bps (67%)");
        console.log("  Duplicate threshold:", params.duplicateThreshold(), "bps (80%)");
        console.log("  Min stake (Link):", params.linkStake() / 1e18, "KNOW");
        console.log("  Min stake (Primary):", params.primaryStake() / 1e18, "KNOW");
        console.log("  Min stake (Duplicate):", params.duplicateStake() / 1e18, "KNOW");
        console.log("  Primary vote weight:", params.primaryVoteWeight());
        console.log("  Oracle vote weight:", params.oracleVoteWeight());
        console.log("  God mode active:", params.godModeActive());

        console.log("\nIdentityRegistry Configuration:");
        console.log("  Consensus contract:", registry.consensusContract());
        console.log("  Identity token:", address(registry.identityToken()));
        console.log("  Owner:", registry.owner());

        console.log("\nIdentityToken Configuration:");
        console.log("  Registry:", identityToken.identityRegistry());
        console.log("  Owner:", identityToken.owner());

        console.log("\nKnomeeToken Configuration:");
        console.log("  Registry:", knomeeToken.registryContract());
        console.log("  Consensus:", knomeeToken.consensusContract());
        console.log("  Rewards pool:", knomeeToken.rewardsPoolBalance() / 1e18, "KNOW");
        console.log("  Early adopter period:", knomeeToken.isEarlyAdopterPeriod());

        console.log("\n[OK] All contracts deployed and linked successfully!");
    }

    function printDeploymentSummary() internal view {
        console.log("\n========================================");
        console.log("  DEPLOYMENT COMPLETE!");
        console.log("========================================");
        console.log("\n=== CONTRACT ADDRESSES ===");
        console.log("GovernanceParameters:", address(params));
        console.log("IdentityToken (IDT):", address(identityToken));
        console.log("KnomeeToken (KNOW):", address(knomeeToken));
        console.log("IdentityRegistry:", address(registry));
        console.log("IdentityConsensus:", address(consensus));

        console.log("\n=== ADD TO YOUR .env FILE ===");
        console.log("GOVERNANCE_PARAMS_ADDRESS=", vm.toString(address(params)));
        console.log("IDENTITY_TOKEN_ADDRESS=", vm.toString(address(identityToken)));
        console.log("KNOMEE_TOKEN_ADDRESS=", vm.toString(address(knomeeToken)));
        console.log("IDENTITY_REGISTRY_ADDRESS=", vm.toString(address(registry)));
        console.log("IDENTITY_CONSENSUS_ADDRESS=", vm.toString(address(consensus)));

        console.log("\n=== ETHERSCAN LINKS (Sepolia) ===");
        console.log("GovernanceParameters:", string.concat("https://sepolia.etherscan.io/address/", vm.toString(address(params))));
        console.log("IdentityToken:", string.concat("https://sepolia.etherscan.io/address/", vm.toString(address(identityToken))));
        console.log("KnomeeToken:", string.concat("https://sepolia.etherscan.io/address/", vm.toString(address(knomeeToken))));
        console.log("IdentityRegistry:", string.concat("https://sepolia.etherscan.io/address/", vm.toString(address(registry))));
        console.log("IdentityConsensus:", string.concat("https://sepolia.etherscan.io/address/", vm.toString(address(consensus))));

        console.log("\n=== NEXT STEPS ===");
        console.log("1. Verify contracts on Etherscan (use --verify flag or VerifyContracts.s.sol)");
        console.log("2. Grant Oracle status to trusted addresses:");
        console.log("   forge script script/GrantOracle.s.sol --rpc-url sepolia --broadcast");
        console.log("3. Distribute test KNOW tokens to users:");
        console.log("   forge script script/DistributeTokens.s.sol --rpc-url sepolia --broadcast");
        console.log("4. Test the protocol with sample claims");
        console.log("5. When ready for production, renounce god mode:");
        console.log("   params.renounceGodMode()");

        console.log("\n=== IMPORTANT SECURITY NOTES ===");
        console.log("- God mode is ACTIVE for testing (allows time manipulation)");
        console.log("- This is a TESTNET deployment - do not use real assets");
        console.log("- Deployer owns all contracts - transfer ownership before production");
        console.log("- Always test thoroughly before mainnet deployment");
    }
}
