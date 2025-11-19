// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Script.sol";

/**
 * @title VerifyContracts - Manual Contract Verification
 * @notice Verify deployed contracts on Etherscan if auto-verification failed
 * @dev Uses forge verify-contract command
 *
 * Prerequisites:
 * - Contracts already deployed
 * - ETHERSCAN_API_KEY in .env
 * - Contract addresses from deployment
 *
 * Usage:
 *   forge script script/VerifyContracts.s.sol:VerifyContracts --rpc-url sepolia
 *
 * This script prints the verification commands.
 * Copy and run them manually if auto-verification failed.
 */
contract VerifyContracts is Script {
    // UPDATE THESE with your deployed addresses
    address constant GOVERNANCE_PARAMS = address(0);
    address constant IDENTITY_TOKEN = address(0);
    address constant KNOMEE_TOKEN = address(0);
    address constant IDENTITY_REGISTRY = address(0);
    address constant IDENTITY_CONSENSUS = address(0);

    function run() external view {
        console.log("========================================");
        console.log("  CONTRACT VERIFICATION COMMANDS");
        console.log("========================================");
        console.log("\nIf auto-verification failed during deployment, run these commands:\n");

        console.log("# 1. Verify GovernanceParameters");
        console.log("forge verify-contract \\");
        console.log("  ", vm.toString(GOVERNANCE_PARAMS), "\\");
        console.log("  contracts/identity/GovernanceParameters.sol:GovernanceParameters \\");
        console.log("  --chain sepolia \\");
        console.log("  --watch");
        console.log("");

        console.log("# 2. Verify IdentityToken");
        console.log("forge verify-contract \\");
        console.log("  ", vm.toString(IDENTITY_TOKEN), "\\");
        console.log("  contracts/identity/IdentityToken.sol:IdentityToken \\");
        console.log("  --chain sepolia \\");
        console.log("  --watch");
        console.log("");

        console.log("# 3. Verify KnomeeToken");
        console.log("forge verify-contract \\");
        console.log("  ", vm.toString(KNOMEE_TOKEN), "\\");
        console.log("  contracts/identity/KnomeeToken.sol:KnomeeToken \\");
        console.log("  --chain sepolia \\");
        console.log("  --watch");
        console.log("");

        console.log("# 4. Verify IdentityRegistry");
        console.log("forge verify-contract \\");
        console.log("  ", vm.toString(IDENTITY_REGISTRY), "\\");
        console.log("  contracts/identity/IdentityRegistry.sol:IdentityRegistry \\");
        console.log("  --chain sepolia \\");
        console.log("  --watch");
        console.log("");

        console.log("# 5. Verify IdentityConsensus (with constructor args)");
        console.log("forge verify-contract \\");
        console.log("  ", vm.toString(IDENTITY_CONSENSUS), "\\");
        console.log("  contracts/identity/IdentityConsensus.sol:IdentityConsensus \\");
        console.log("  --chain sepolia \\");
        console.log("  --constructor-args $(cast abi-encode \"constructor(address,address,address,address)\" \\");
        console.log("    ", vm.toString(IDENTITY_REGISTRY), "\\");
        console.log("    ", vm.toString(GOVERNANCE_PARAMS), "\\");
        console.log("    ", vm.toString(IDENTITY_TOKEN), "\\");
        console.log("    ", vm.toString(KNOMEE_TOKEN), ") \\");
        console.log("  --watch");
        console.log("");

        console.log("========================================");
        console.log("  ETHERSCAN LINKS");
        console.log("========================================");
        console.log("GovernanceParameters:", string.concat("https://sepolia.etherscan.io/address/", vm.toString(GOVERNANCE_PARAMS)));
        console.log("IdentityToken:", string.concat("https://sepolia.etherscan.io/address/", vm.toString(IDENTITY_TOKEN)));
        console.log("KnomeeToken:", string.concat("https://sepolia.etherscan.io/address/", vm.toString(KNOMEE_TOKEN)));
        console.log("IdentityRegistry:", string.concat("https://sepolia.etherscan.io/address/", vm.toString(IDENTITY_REGISTRY)));
        console.log("IdentityConsensus:", string.concat("https://sepolia.etherscan.io/address/", vm.toString(IDENTITY_CONSENSUS)));
        console.log("");
    }
}
