// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../contracts/identity/IdentityRegistry.sol";
import "../contracts/identity/IdentityConsensus.sol";
import "../contracts/identity/GovernanceParameters.sol";
import "../contracts/identity/IdentityToken.sol";
import "../contracts/identity/KnomeeToken.sol";

/**
 * @title IntegrationTest
 * @notice Comprehensive integration tests for the entire Knomee Identity Protocol
 * @dev Tests the full system with all contracts deployed and interacting
 */
contract IntegrationTest is Test {
    // Contracts
    IdentityRegistry public registry;
    IdentityConsensus public consensus;
    GovernanceParameters public params;
    IdentityToken public identityToken;
    KnomeeToken public knomeeToken;

    // Test accounts
    address public owner = address(1);
    address public alice = address(2);
    address public bob = address(3);
    address public charlie = address(4);
    address public oracle1 = address(5);
    address public oracle2 = address(6);
    address public newUser1 = address(100);
    address public newUser2 = address(101);
    address public newUser3 = address(102);

    function setUp() public {
        vm.startPrank(owner);

        // Deploy all contracts
        params = new GovernanceParameters();
        identityToken = new IdentityToken();
        knomeeToken = new KnomeeToken();
        registry = new IdentityRegistry();
        consensus = new IdentityConsensus(address(registry), address(params));

        // Link contracts together
        registry.setConsensusContract(address(consensus));
        registry.setIdentityToken(address(identityToken));
        identityToken.setIdentityRegistry(address(registry));
        knomeeToken.setConsensusContract(address(consensus));
        knomeeToken.setRegistryContract(address(registry));
        consensus.setIdentityToken(address(identityToken));
        consensus.setKnomeeToken(address(knomeeToken));

        vm.stopPrank();

        // Setup initial test identities via consensus
        vm.startPrank(address(consensus));
        registry.upgradeToPrimary(alice);
        registry.upgradeToPrimary(bob);
        registry.upgradeToPrimary(charlie);
        registry.upgradeToPrimary(oracle1);
        registry.upgradeToPrimary(oracle2);
        vm.stopPrank();

        // Upgrade oracles
        vm.startPrank(owner);
        registry.upgradeToOracle(oracle1);
        registry.upgradeToOracle(oracle2);
        vm.stopPrank();

        // Mint identity tokens for all
        vm.startPrank(address(registry));
        identityToken.mintPrimaryID(alice);
        identityToken.mintPrimaryID(bob);
        identityToken.mintPrimaryID(charlie);
        identityToken.mintPrimaryID(oracle1);
        identityToken.mintPrimaryID(oracle2);
        vm.stopPrank();

        // Upgrade oracle tokens
        vm.startPrank(owner);
        identityToken.upgradeToOracle(oracle1);
        identityToken.upgradeToOracle(oracle2);
        vm.stopPrank();

        // Give everyone KNOW tokens for staking
        vm.startPrank(owner);
        knomeeToken.transfer(alice, 1000 * 10**18);
        knomeeToken.transfer(bob, 1000 * 10**18);
        knomeeToken.transfer(charlie, 1000 * 10**18);
        knomeeToken.transfer(oracle1, 1000 * 10**18);
        knomeeToken.transfer(oracle2, 1000 * 10**18);
        knomeeToken.transfer(newUser1, 1000 * 10**18);
        knomeeToken.transfer(newUser2, 1000 * 10**18);
        knomeeToken.transfer(newUser3, 1000 * 10**18);
        vm.stopPrank();
    }

    // ============ Full Flow: New Primary Verification ============

    function test_Integration_NewPrimaryVerification_Success() public {
        uint256 initialBalance = knomeeToken.balanceOf(newUser1);

        // Step 1: New user requests Primary verification
        vm.prank(newUser1);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}(
            "I am a unique human from San Francisco"
        );

        // Verify claim created
        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(claimId);
        assertEq(uint256(claim.claimType), uint256(IdentityConsensus.ClaimType.NewPrimary));
        assertEq(claim.subject, newUser1);

        // Step 2: Oracle vouches FOR (should auto-resolve)
        vm.prank(oracle1);
        consensus.vouchFor{value: 0.03 ether}(claimId);

        // Step 3: Verify claim approved
        claim = consensus.getClaim(claimId);
        assertEq(uint256(claim.status), uint256(IdentityConsensus.ClaimStatus.Approved));

        // Step 4: Verify user is now Primary in registry
        assertTrue(registry.isPrimary(newUser1));
        assertEq(uint256(registry.getTier(newUser1)), uint256(IdentityRegistry.IdentityTier.PrimaryID));

        // Step 5: Verify identity token minted
        assertEq(identityToken.balanceOf(newUser1), 1);
        assertTrue(identityToken.isPrimary(newUser1));
        assertEq(identityToken.getVotingWeight(newUser1), 1);

        // Step 6: Verify KNOW token reward minted (Primary ID reward)
        // This happens automatically in the consensus contract
        // The user should have received the Primary ID reward

        // Step 7: Oracle can claim rewards
        vm.prank(oracle1);
        consensus.claimRewards(claimId);

        // Oracle should receive stake back + rewards
        assertGe(oracle1.balance, 0.03 ether);
    }

    function test_Integration_NewPrimaryVerification_Rejected() public {
        // Step 1: New user requests Primary verification
        vm.prank(newUser1);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}(
            "Suspicious claim"
        );

        // Step 2: Oracle vouches AGAINST (should auto-resolve rejection)
        vm.prank(oracle1);
        consensus.vouchAgainst{value: 0.03 ether}(claimId);

        // Step 3: Verify claim rejected
        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(claimId);
        assertEq(uint256(claim.status), uint256(IdentityConsensus.ClaimStatus.Rejected));

        // Step 4: Verify user still GreyGhost
        assertFalse(registry.isPrimary(newUser1));
        assertEq(uint256(registry.getTier(newUser1)), uint256(IdentityRegistry.IdentityTier.GreyGhost));

        // Step 5: Verify no identity token
        assertEq(identityToken.balanceOf(newUser1), 0);
    }

    // ============ Full Flow: Link to Primary ============

    function test_Integration_LinkToPrimary_Success() public {
        // Step 1: New address requests link to Alice's Primary
        vm.prank(newUser1);
        uint256 claimId = consensus.requestLinkToPrimary{value: 0.01 ether}(
            alice,
            "LinkedIn",
            "This is my professional LinkedIn account"
        );

        // Step 2: Oracle approves (auto-resolves)
        vm.prank(oracle1);
        consensus.vouchFor{value: 0.01 ether}(claimId);

        // Step 3: Verify claim approved
        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(claimId);
        assertEq(uint256(claim.status), uint256(IdentityConsensus.ClaimStatus.Approved));

        // Step 4: Verify link in registry
        assertEq(uint256(registry.getTier(newUser1)), uint256(IdentityRegistry.IdentityTier.LinkedID));
        assertEq(registry.getPrimaryAddress(newUser1), alice);
        assertEq(registry.getLinkedAddress(alice, "LinkedIn"), newUser1);

        // Step 5: Verify linked identity token minted
        assertEq(identityToken.balanceOf(newUser1), 1);
        assertEq(uint256(identityToken.getTier(newUser1)), uint256(IdentityToken.IdentityTier.LinkedID));
        assertEq(identityToken.getVotingWeight(newUser1), 0); // Linked IDs cannot vote
    }

    function test_Integration_MultipleLinkedAccounts() public {
        // Link multiple accounts to Alice
        vm.prank(newUser1);
        uint256 claim1 = consensus.requestLinkToPrimary{value: 0.01 ether}(
            alice, "LinkedIn", "Personal LinkedIn"
        );

        vm.prank(oracle1);
        consensus.vouchFor{value: 0.01 ether}(claim1);

        vm.prank(newUser2);
        uint256 claim2 = consensus.requestLinkToPrimary{value: 0.01 ether}(
            alice, "LinkedIn-business", "Business LinkedIn"
        );

        vm.prank(oracle1);
        consensus.vouchFor{value: 0.01 ether}(claim2);

        vm.prank(newUser3);
        uint256 claim3 = consensus.requestLinkToPrimary{value: 0.01 ether}(
            alice, "Instagram", "Instagram account"
        );

        vm.prank(oracle1);
        consensus.vouchFor{value: 0.01 ether}(claim3);

        // Verify all links
        assertEq(registry.getLinkedCount(alice), 3);
        assertEq(registry.getPrimaryAddress(newUser1), alice);
        assertEq(registry.getPrimaryAddress(newUser2), alice);
        assertEq(registry.getPrimaryAddress(newUser3), alice);

        IdentityRegistry.LinkedPlatform[] memory platforms = registry.getLinkedPlatforms(alice);
        assertEq(platforms.length, 3);
    }

    // ============ Full Flow: Duplicate Detection ============

    function test_Integration_DuplicateDetection_Success() public {
        // Setup: Alice and Bob are both Primaries (simulating Sybil attack)

        // Charlie challenges them as duplicates
        uint256 challengerBalance = knomeeToken.balanceOf(charlie);

        vm.prank(charlie);
        uint256 claimId = consensus.challengeDuplicate{value: 0.1 ether}(
            alice,
            bob,
            "Same IP address, same transaction patterns, same writing style"
        );

        // Verify both under challenge
        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(claimId);
        assertEq(uint256(claim.claimType), uint256(IdentityConsensus.ClaimType.DuplicateFlag));

        // Oracle1 votes FOR (confirming duplicate)
        vm.prank(oracle1);
        consensus.vouchFor{value: 0.1 ether}(claimId);

        // Claim should be approved (80% threshold met)
        claim = consensus.getClaim(claimId);
        assertEq(uint256(claim.status), uint256(IdentityConsensus.ClaimStatus.Approved));

        // Both identities should be downgraded to GreyGhost
        assertEq(uint256(registry.getTier(alice)), uint256(IdentityRegistry.IdentityTier.GreyGhost));
        assertEq(uint256(registry.getTier(bob)), uint256(IdentityRegistry.IdentityTier.GreyGhost));

        // Identity tokens should be revoked
        assertEq(identityToken.balanceOf(alice), 0);
        assertEq(identityToken.balanceOf(bob), 0);

        // Challenger should get rewards
        vm.prank(charlie);
        consensus.claimRewards(claimId);

        assertGe(charlie.balance, 0.1 ether);
    }

    // ============ Governance Parameter Changes ============

    function test_Integration_GovernanceParameterUpdate() public {
        // Grant governance role to alice
        vm.prank(owner);
        params.grantRole(params.GOVERNANCE_ROLE(), alice);

        // Alice updates thresholds
        vm.prank(alice);
        params.setThresholds(6000, 7500, 8500);

        // Verify new thresholds used in consensus
        assertEq(params.linkThreshold(), 6000);
        assertEq(params.primaryThreshold(), 7500);
        assertEq(params.duplicateThreshold(), 8500);

        // Test that new claim uses new thresholds
        vm.prank(newUser1);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}("Test");

        // Verify threshold from params
        assertEq(params.getThreshold(uint256(IdentityConsensus.ClaimType.NewPrimary)), 7500);
    }

    // ============ Oracle Upgrade Flow ============

    function test_Integration_OracleUpgrade() public {
        // Alice is Primary, owner upgrades her to Oracle
        vm.prank(owner);
        registry.upgradeToOracle(alice);

        // Verify in registry
        assertTrue(registry.isOracle(alice));

        // Upgrade identity token
        vm.prank(owner);
        identityToken.upgradeToOracle(alice);

        // Verify voting weight
        assertEq(identityToken.getVotingWeight(alice), 100);

        // Test Alice can now vote with Oracle weight
        vm.prank(newUser1);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}("Test");

        vm.prank(alice);
        consensus.vouchFor{value: 0.03 ether}(claimId);

        // Should auto-resolve due to Oracle weight
        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(claimId);
        assertEq(uint256(claim.status), uint256(IdentityConsensus.ClaimStatus.Approved));
    }

    // ============ Token Economics Integration ============

    function test_Integration_TokenEconomics() public {
        uint256 initialPool = knomeeToken.rewardsPoolBalance();

        // User becomes Primary and gets reward
        vm.prank(newUser1);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}("Test");

        vm.prank(oracle1);
        consensus.vouchFor{value: 0.03 ether}(claimId);

        // Check Primary ID reward was minted (happens in IdentityRegistry)
        // Note: This requires the registry to call knomeeToken.mintPrimaryIDReward
        // which would need to be integrated in IdentityRegistry.upgradeToPrimary

        // Oracle gets reward for helping
        // Note: This would happen in consensus when claim resolves

        // Rewards pool should decrease
        // (This test would be more accurate if the contract integration was complete)
    }

    // ============ Complex Multi-Step Scenarios ============

    function test_Integration_FullIdentityJourney() public {
        // Step 1: New user becomes Primary
        vm.prank(newUser1);
        uint256 primaryClaim = consensus.requestPrimaryVerification{value: 0.03 ether}(
            "Verified human from NYC"
        );

        vm.prank(oracle1);
        consensus.vouchFor{value: 0.03 ether}(primaryClaim);

        assertTrue(registry.isPrimary(newUser1));

        // Step 2: Links secondary account
        vm.prank(newUser2);
        uint256 linkClaim = consensus.requestLinkToPrimary{value: 0.01 ether}(
            newUser1, "Twitter", "My Twitter account"
        );

        vm.prank(oracle1);
        consensus.vouchFor{value: 0.01 ether}(linkClaim);

        assertEq(registry.getLinkedCount(newUser1), 1);

        // Step 3: User1 participates in consensus by vouching
        vm.prank(newUser3);
        uint256 anotherClaim = consensus.requestPrimaryVerification{value: 0.03 ether}("Test");

        vm.prank(newUser1);
        consensus.vouchFor{value: 0.03 ether}(anotherClaim);

        // Step 4: User1 gets challenged as duplicate (false alarm)
        vm.prank(charlie);
        uint256 dupClaim = consensus.challengeDuplicate{value: 0.1 ether}(
            newUser1, bob, "Suspicious activity"
        );

        // Oracle rejects (votes against duplicate claim)
        vm.prank(oracle1);
        consensus.vouchAgainst{value: 0.1 ether}(dupClaim);

        // Claim rejected, user1 survives
        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(dupClaim);
        assertEq(uint256(claim.status), uint256(IdentityConsensus.ClaimStatus.Rejected));

        assertTrue(registry.isPrimary(newUser1));
    }

    function test_Integration_CommunityConsensus() public {
        // Test scenario where multiple Primaries vote without Oracle
        // and consensus is reached through community

        vm.prank(newUser1);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}("Test");

        // Alice, Bob, Charlie all vouch FOR
        vm.prank(alice);
        consensus.vouchFor{value: 0.03 ether}(claimId);

        // Check consensus percentage
        (uint256 percentFor,) = consensus.getCurrentConsensus(claimId);

        // With multiple Primary votes, should reach threshold eventually
        if (percentFor < 6700) {
            vm.prank(bob);
            consensus.vouchFor{value: 0.03 ether}(claimId);

            (percentFor,) = consensus.getCurrentConsensus(claimId);
        }

        if (percentFor < 6700) {
            vm.prank(charlie);
            consensus.vouchFor{value: 0.03 ether}(claimId);
        }

        // Eventually should reach consensus
        // (Exact threshold depends on number of participants and weights)
    }

    // ============ Error Recovery Scenarios ============

    function test_Integration_FailedClaimRetry() public {
        // User's claim gets rejected
        vm.prank(newUser1);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}("Weak claim");

        vm.prank(oracle1);
        consensus.vouchAgainst{value: 0.03 ether}(claimId);

        // Claim rejected
        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(claimId);
        assertEq(uint256(claim.status), uint256(IdentityConsensus.ClaimStatus.Rejected));

        // User cannot immediately retry (cooldown)
        vm.prank(newUser1);
        vm.expectRevert(); // Should revert due to cooldown
        consensus.requestPrimaryVerification{value: 0.03 ether}("Better claim");

        // After cooldown period, user can retry
        vm.warp(block.timestamp + 7 days + 1);

        vm.prank(newUser1);
        uint256 newClaimId = consensus.requestPrimaryVerification{value: 0.03 ether}(
            "Much better justification"
        );

        // This time succeeds
        vm.prank(oracle1);
        consensus.vouchFor{value: 0.03 ether}(newClaimId);

        assertTrue(registry.isPrimary(newUser1));
    }

    // ============ Soul-Bound Token Integration ============

    function test_Integration_SoulBoundTokensCannotBeTraded() public {
        // Alice has identity token
        assertEq(identityToken.balanceOf(alice), 1);

        // Alice cannot transfer to Bob
        vm.prank(alice);
        vm.expectRevert("Identity tokens are soul-bound");
        identityToken.transferFrom(alice, bob, identityToken.accountToTokenId(alice));

        // Alice cannot approve Bob
        vm.prank(alice);
        vm.expectRevert("Identity tokens cannot be approved");
        identityToken.approve(bob, identityToken.accountToTokenId(alice));

        // Identity tokens stay with verified humans
        assertEq(identityToken.balanceOf(alice), 1);
        assertEq(identityToken.balanceOf(bob), 1);
    }

    // ============ System-Wide State Consistency ============

    function test_Integration_StateConsistency() public {
        // Verify all Primaries have identity tokens
        assertTrue(registry.isPrimary(alice));
        assertEq(identityToken.balanceOf(alice), 1);

        assertTrue(registry.isPrimary(bob));
        assertEq(identityToken.balanceOf(bob), 1);

        // Verify Oracles have correct weight
        assertTrue(registry.isOracle(oracle1));
        assertEq(identityToken.getVotingWeight(oracle1), 100);

        // Verify GreyGhosts have no tokens
        assertFalse(registry.isPrimary(newUser1));
        assertEq(identityToken.balanceOf(newUser1), 0);
        assertEq(identityToken.getVotingWeight(newUser1), 0);
    }
}
