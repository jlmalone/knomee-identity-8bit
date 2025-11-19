// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Script.sol";
import "../contracts/identity/IdentityRegistry.sol";
import "../contracts/identity/IdentityConsensus.sol";
import "../contracts/identity/IdentityToken.sol";
import "../contracts/identity/KnomeeToken.sol";

/**
 * @title TestProtocol - Integration Testing Script
 * @notice Test the deployed Knomee Identity Protocol with sample claims
 * @dev This script simulates real protocol usage for testing
 *
 * Prerequisites:
 * - All contracts deployed
 * - Oracles have been granted
 * - Test users have KNOW tokens
 *
 * Usage:
 *   forge script script/TestProtocol.s.sol:TestProtocol --rpc-url sepolia --broadcast -vvv
 *
 * Test scenarios:
 * 1. Request Primary verification
 * 2. Oracle vouches FOR
 * 3. Consensus reached -> PrimaryID granted
 * 4. Link secondary account
 * 5. Challenge duplicate
 */
contract TestProtocol is Script {
    // UPDATE THESE with your deployed addresses
    address constant IDENTITY_REGISTRY = address(0);
    address constant IDENTITY_CONSENSUS = address(0);
    address constant IDENTITY_TOKEN = address(0);
    address constant KNOMEE_TOKEN = address(0);

    // UPDATE THESE with your test addresses
    address constant TEST_USER = address(0);      // User requesting Primary
    address constant ORACLE = address(0);          // Oracle who will vouch

    function run() external {
        uint256 deployerPrivateKey = vm.envUint("PRIVATE_KEY");

        IdentityRegistry registry = IdentityRegistry(IDENTITY_REGISTRY);
        IdentityConsensus consensus = IdentityConsensus(IDENTITY_CONSENSUS);
        IdentityToken identityToken = IdentityToken(IDENTITY_TOKEN);
        KnomeeToken knomeeToken = KnomeeToken(KNOMEE_TOKEN);

        console.log("========================================");
        console.log("  PROTOCOL INTEGRATION TEST");
        console.log("========================================");
        console.log("Registry:", address(registry));
        console.log("Consensus:", address(consensus));
        console.log("IdentityToken:", address(identityToken));
        console.log("KnomeeToken:", address(knomeeToken));
        console.log("\nTest user:", TEST_USER);
        console.log("Oracle:", ORACLE);

        // Check initial state
        console.log("\n=== INITIAL STATE ===");
        console.log("User tier:", uint8(registry.getIdentityTier(TEST_USER)));
        console.log("User KNOW balance:", knomeeToken.balanceOf(TEST_USER) / 1e18, "KNOW");
        console.log("Oracle tier:", uint8(registry.getIdentityTier(ORACLE)));
        console.log("Oracle KNOW balance:", knomeeToken.balanceOf(ORACLE) / 1e18, "KNOW");

        vm.startBroadcast(deployerPrivateKey);

        // Test 1: Request Primary verification
        console.log("\n=== TEST 1: REQUEST PRIMARY VERIFICATION ===");

        // Approve KNOW tokens for staking
        console.log("Approving 30 KNOW for Primary claim...");
        // Note: This would need to be done by TEST_USER, not deployer
        // knomeeToken.approve(address(consensus), 30 * 1e18);

        // Request Primary verification
        console.log("Requesting Primary verification...");
        // Note: This would need to be done by TEST_USER
        // uint256 claimId = consensus.requestPrimaryVerification();
        // console.log("Claim ID:", claimId);

        // Test 2: Oracle vouches FOR
        console.log("\n=== TEST 2: ORACLE VOUCHES FOR ===");
        // Note: This would need to be done by ORACLE
        // consensus.vouchFor(claimId, 50 * 1e18);
        // console.log("Oracle vouched with 50 KNOW");

        // Check claim status
        // console.log("\n=== CLAIM STATUS ===");
        // (address account, IdentityConsensus.ClaimType claimType, uint256 totalFor, uint256 totalAgainst, bool resolved) = consensus.getClaim(claimId);
        // console.log("Total FOR:", totalFor / 1e18, "weighted KNOW");
        // console.log("Total AGAINST:", totalAgainst / 1e18, "weighted KNOW");
        // console.log("Resolved:", resolved);

        // Check if consensus reached
        // console.log("\n=== CONSENSUS CHECK ===");
        // bool consensusReached = consensus.isConsensusReached(claimId);
        // console.log("Consensus reached:", consensusReached);

        // If resolved, check final tier
        // console.log("\n=== FINAL STATE ===");
        // console.log("User tier:", uint8(registry.getIdentityTier(TEST_USER)));
        // console.log("User has IdentityToken:", identityToken.balanceOf(TEST_USER) > 0);

        vm.stopBroadcast();

        console.log("\n========================================");
        console.log("  TEST COMPLETE");
        console.log("========================================");
        console.log("\nNote: This script template shows the test flow.");
        console.log("Uncomment and modify the sections to run actual tests.");
        console.log("Remember to use the correct private keys for each role.");
    }
}
