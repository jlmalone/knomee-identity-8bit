// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Script.sol";
import "../contracts/identity/KnomeeToken.sol";

/**
 * @title DistributeTokens - KNOW Token Distribution
 * @notice Distribute KNOW tokens to test users
 * @dev Distributes from community treasury or deployer balance
 *
 * Prerequisites:
 * - KnomeeToken deployed
 * - Deployer has KNOW tokens to distribute
 * - Recipients array configured below
 *
 * Usage:
 *   forge script script/DistributeTokens.s.sol:DistributeTokens --rpc-url sepolia --broadcast -vvv
 *
 * Distribution amounts:
 * - Regular testers: 100 KNOW (for 1-3 Primary claims)
 * - Oracles: 500 KNOW (for multiple votes)
 * - Power users: 1000 KNOW (for extensive testing)
 */
contract DistributeTokens is Script {
    // Update this with your deployed KnomeeToken address
    address constant KNOMEE_TOKEN = address(0); // UPDATE THIS

    // Distribution recipients
    struct Recipient {
        address account;
        uint256 amount; // Amount in KNOW (will be multiplied by 1e18)
        string role;
    }

    // Add your test addresses here
    Recipient[] recipients;

    function setupRecipients() internal {
        // Example recipients - UPDATE THESE
        // recipients.push(Recipient({
        //     account: 0x1234567890123456789012345678901234567890,
        //     amount: 100,  // 100 KNOW
        //     role: "Regular Tester"
        // }));

        // recipients.push(Recipient({
        //     account: 0x2234567890123456789012345678901234567890,
        //     amount: 500,  // 500 KNOW
        //     role: "Oracle"
        // }));

        // recipients.push(Recipient({
        //     account: 0x3234567890123456789012345678901234567890,
        //     amount: 1000, // 1000 KNOW
        //     role: "Power User"
        // }));
    }

    function run() external {
        // Setup recipients
        setupRecipients();

        require(recipients.length > 0, "No recipients configured");

        // Load environment
        uint256 deployerPrivateKey = vm.envUint("PRIVATE_KEY");
        address deployer = vm.addr(deployerPrivateKey);

        KnomeeToken knomeeToken = KnomeeToken(KNOMEE_TOKEN);

        console.log("========================================");
        console.log("  KNOW TOKEN DISTRIBUTION");
        console.log("========================================");
        console.log("Distributor:", deployer);
        console.log("KnomeeToken:", address(knomeeToken));
        console.log("Distributor balance:", knomeeToken.balanceOf(deployer) / 1e18, "KNOW");
        console.log("Recipients:", recipients.length);

        // Calculate total distribution
        uint256 totalDistribution = 0;
        for (uint256 i = 0; i < recipients.length; i++) {
            totalDistribution += recipients[i].amount * 1e18;
        }
        console.log("Total to distribute:", totalDistribution / 1e18, "KNOW");

        require(
            knomeeToken.balanceOf(deployer) >= totalDistribution,
            "Insufficient KNOW balance for distribution"
        );

        vm.startBroadcast(deployerPrivateKey);

        for (uint256 i = 0; i < recipients.length; i++) {
            Recipient memory recipient = recipients[i];
            uint256 amount = recipient.amount * 1e18;

            console.log("\n[", i + 1, "/", recipients.length, "] Distributing to:", recipient.account);
            console.log("  Role:", recipient.role);
            console.log("  Amount:", recipient.amount, "KNOW");

            // Check current balance
            uint256 currentBalance = knomeeToken.balanceOf(recipient.account);
            console.log("  Current balance:", currentBalance / 1e18, "KNOW");

            // Transfer tokens
            bool success = knomeeToken.transfer(recipient.account, amount);
            require(success, "Transfer failed");

            // Verify new balance
            uint256 newBalance = knomeeToken.balanceOf(recipient.account);
            console.log("  New balance:", newBalance / 1e18, "KNOW");
            console.log("  [SUCCESS] Distributed");
        }

        vm.stopBroadcast();

        console.log("\n========================================");
        console.log("  DISTRIBUTION COMPLETE");
        console.log("========================================");
        console.log("Total distributed:", totalDistribution / 1e18, "KNOW");
        console.log("Remaining balance:", knomeeToken.balanceOf(deployer) / 1e18, "KNOW");

        // Print distribution summary
        console.log("\n=== DISTRIBUTION SUMMARY ===");
        for (uint256 i = 0; i < recipients.length; i++) {
            Recipient memory recipient = recipients[i];
            uint256 balance = knomeeToken.balanceOf(recipient.account);
            console.log(recipient.account, "->", balance / 1e18, "KNOW (", recipient.role, ")");
        }

        console.log("\n=== MINIMUM STAKES ===");
        console.log("Link to Primary claim: 10 KNOW");
        console.log("New Primary claim: 30 KNOW");
        console.log("Duplicate flag: 50 KNOW");
    }
}
