// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Script.sol";
import "../contracts/identity/IdentityRegistry.sol";
import "../contracts/identity/IdentityToken.sol";

/**
 * @title GrantOracle - Oracle Status Management
 * @notice Grant Oracle status to trusted verifiers
 * @dev Oracles have 100x voting weight compared to Primary IDs
 *
 * Prerequisites:
 * - Contracts already deployed
 * - Deployer is owner of IdentityRegistry
 * - Addresses in ORACLE_ADDRESSES array are funded with ETH
 *
 * Usage:
 *   forge script script/GrantOracle.s.sol:GrantOracle --rpc-url sepolia --broadcast -vvv
 *
 * To grant Oracle to a specific address:
 * 1. Update ORACLE_ADDRESSES array below
 * 2. Run the script
 */
contract GrantOracle is Script {
    // Update these addresses with your deployed contracts
    address constant IDENTITY_REGISTRY = address(0); // UPDATE THIS
    address constant IDENTITY_TOKEN = address(0);    // UPDATE THIS

    // Add addresses that should become Oracles
    address[] ORACLE_ADDRESSES = [
        // Add trusted verifier addresses here
        // Example: 0x1234567890123456789012345678901234567890
    ];

    function run() external {
        // Load environment
        uint256 deployerPrivateKey = vm.envUint("PRIVATE_KEY");
        address deployer = vm.addr(deployerPrivateKey);

        // Load contracts
        IdentityRegistry registry = IdentityRegistry(IDENTITY_REGISTRY);
        IdentityToken identityToken = IdentityToken(IDENTITY_TOKEN);

        console.log("========================================");
        console.log("  GRANTING ORACLE STATUS");
        console.log("========================================");
        console.log("Deployer:", deployer);
        console.log("Registry:", address(registry));
        console.log("IdentityToken:", address(identityToken));
        console.log("Oracles to grant:", ORACLE_ADDRESSES.length);

        require(ORACLE_ADDRESSES.length > 0, "No oracle addresses specified");

        vm.startBroadcast(deployerPrivateKey);

        for (uint256 i = 0; i < ORACLE_ADDRESSES.length; i++) {
            address oracleAddress = ORACLE_ADDRESSES[i];

            console.log("\n[", i + 1, "/", ORACLE_ADDRESSES.length, "] Processing:", oracleAddress);

            // Check current tier
            IdentityRegistry.IdentityTier currentTier = registry.getIdentityTier(oracleAddress);
            console.log("  Current tier:", uint8(currentTier));

            // If already Oracle, skip
            if (currentTier == IdentityRegistry.IdentityTier.Oracle) {
                console.log("  [SKIP] Already an Oracle");
                continue;
            }

            // If GreyGhost or LinkedID, need to upgrade to Primary first
            if (currentTier == IdentityRegistry.IdentityTier.GreyGhost ||
                currentTier == IdentityRegistry.IdentityTier.LinkedID) {
                console.log("  [WARNING] Address is not a PrimaryID, upgrading first...");

                // Create Primary ID directly (admin privilege)
                registry.adminGrantPrimaryID(oracleAddress);
                console.log("  >> Upgraded to PrimaryID");
            }

            // Upgrade to Oracle
            registry.upgradeToOracle(oracleAddress);
            console.log("  >> Upgraded to Oracle");

            // Verify Oracle status
            IdentityRegistry.IdentityTier newTier = registry.getIdentityTier(oracleAddress);
            require(newTier == IdentityRegistry.IdentityTier.Oracle, "Oracle upgrade failed");

            // Check voting weight
            uint256 weight = identityToken.getVotingWeight(oracleAddress);
            console.log("  >> Voting weight:", weight);
            require(weight == 100, "Oracle voting weight incorrect");

            console.log("  [SUCCESS] Oracle status granted");
        }

        vm.stopBroadcast();

        console.log("\n========================================");
        console.log("  ORACLE GRANT COMPLETE");
        console.log("========================================");
        console.log("Total Oracles granted:", ORACLE_ADDRESSES.length);

        // Print verification info
        console.log("\n=== ORACLE ADDRESSES ===");
        for (uint256 i = 0; i < ORACLE_ADDRESSES.length; i++) {
            console.log(ORACLE_ADDRESSES[i], "-> Oracle (weight: 100)");
        }
    }
}

/**
 * @dev If IdentityRegistry doesn't have adminGrantPrimaryID, you can add this function:
 *
 * function adminGrantPrimaryID(address account) external onlyOwner {
 *     require(identities[account].tier == IdentityTier.GreyGhost, "Already has identity");
 *     identities[account].tier = IdentityTier.PrimaryID;
 *     identityToken.mintPrimaryID(account);
 *     emit IdentityUpgraded(account, IdentityTier.PrimaryID);
 * }
 */
