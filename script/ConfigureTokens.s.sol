// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Script.sol";
import "../contracts/identity/IdentityToken.sol";
import "../contracts/identity/KnomeeToken.sol";

/**
 * @title ConfigureTokens
 * @notice Configure token contracts with registry and consensus addresses
 *
 * Usage:
 * forge script script/ConfigureTokens.s.sol:ConfigureTokens --rpc-url http://localhost:8545 --broadcast
 */
contract ConfigureTokens is Script {
    // Deployed token addresses
    address constant IDENTITY_TOKEN = 0xDc64a140Aa3E981100a9becA4E685f962f0cF6C9;
    address constant KNOMEE_TOKEN = 0x5FC8d32690cc91D4c39d9d3abcBD16989F875707;

    // Existing contract addresses
    address constant GOVERNANCE = 0x5FbDB2315678afecb367f032d93F642f64180aa3;
    address constant REGISTRY = 0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512;
    address constant CONSENSUS = 0x9fE46736679d2D9a65F0992F2272dE9f3c7fa6e0;

    function run() external {
        // Anvil default account #0
        uint256 deployerPrivateKey = 0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80;

        vm.startBroadcast(deployerPrivateKey);

        IdentityToken identityToken = IdentityToken(IDENTITY_TOKEN);
        KnomeeToken knomeeToken = KnomeeToken(KNOMEE_TOKEN);

        console.log("=== CONFIGURING TOKEN CONTRACTS ===\n");

        // Configure IdentityToken
        console.log("1. Setting IdentityRegistry on IdentityToken...");
        identityToken.setIdentityRegistry(REGISTRY);
        console.log("   [OK] IdentityRegistry set to:", REGISTRY);

        // Configure KnomeeToken
        console.log("\n2. Setting IdentityRegistry on KnomeeToken...");
        knomeeToken.setRegistryContract(REGISTRY);
        console.log("   [OK] RegistryContract set to:", REGISTRY);

        console.log("\n3. Setting IdentityConsensus on KnomeeToken...");
        knomeeToken.setConsensusContract(CONSENSUS);
        console.log("   [OK] ConsensusContract set to:", CONSENSUS);

        console.log("\n=== CONFIGURATION COMPLETE ===");
        console.log("\nContract Summary:");
        console.log("IdentityToken (IDT):", IDENTITY_TOKEN);
        console.log("  Registry:", identityToken.identityRegistry());
        console.log("\nKnomeeToken (KNOW):", KNOMEE_TOKEN);
        console.log("  Registry:", knomeeToken.registryContract());
        console.log("  Consensus:", knomeeToken.consensusContract());
        console.log("  Rewards Pool:", knomeeToken.rewardsPoolBalance());

        console.log("\n=== NEXT STEPS ===");
        console.log("1. Update IdentityRegistry to mint IdentityTokens (not just track state)");
        console.log("2. Update IdentityConsensus to:");
        console.log("   - Require IdentityToken for voting (check canVote)");
        console.log("   - Use KNOW token staking instead of ETH");
        console.log("   - Call KNOW.slash() for incorrect votes");
        console.log("   - Call KNOW.mintVotingReward() for correct votes");

        vm.stopBroadcast();
    }
}
