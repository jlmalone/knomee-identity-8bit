// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Script.sol";
import "../contracts/identity/IdentityToken.sol";
import "../contracts/identity/KnomeeToken.sol";

/**
 * @title DeployTokens
 * @notice Deploy IdentityToken (IDT) and KnomeeToken (KNOW)
 *
 * Usage:
 * forge script script/DeployTokens.s.sol:DeployTokens --rpc-url http://localhost:8545 --broadcast
 */
contract DeployTokens is Script {
    function run() external {
        // Anvil default account #0
        uint256 deployerPrivateKey = 0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80;

        vm.startBroadcast(deployerPrivateKey);

        // Deploy IdentityToken (soul-bound NFT)
        console.log("Deploying IdentityToken...");
        IdentityToken identityToken = new IdentityToken();
        console.log("IdentityToken deployed at:", address(identityToken));

        // Deploy KnomeeToken (staking token)
        console.log("Deploying KnomeeToken...");
        KnomeeToken knomeeToken = new KnomeeToken();
        console.log("KnomeeToken deployed at:", address(knomeeToken));

        // Log token details
        console.log("\n=== TOKEN DEPLOYMENT SUMMARY ===");
        console.log("IdentityToken (IDT):", address(identityToken));
        console.log("  Name:", identityToken.name());
        console.log("  Symbol:", identityToken.symbol());
        console.log("  Owner:", identityToken.owner());

        console.log("\nKnomeeToken (KNOW):", address(knomeeToken));
        console.log("  Name:", knomeeToken.name());
        console.log("  Symbol:", knomeeToken.symbol());
        console.log("  Total Supply:", knomeeToken.totalSupply());
        console.log("  Max Supply:", knomeeToken.MAX_SUPPLY());
        console.log("  Owner:", knomeeToken.owner());
        console.log("  Rewards Pool:", knomeeToken.rewardsPoolBalance());

        console.log("\n=== CONFIGURATION NEEDED ===");
        console.log("1. Set IdentityRegistry address on IdentityToken:");
        console.log("   identityToken.setIdentityRegistry(<registry_address>)");
        console.log("\n2. Set contract addresses on KnomeeToken:");
        console.log("   knomeeToken.setRegistryContract(<registry_address>)");
        console.log("   knomeeToken.setConsensusContract(<consensus_address>)");

        vm.stopBroadcast();
    }
}
