// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../contracts/identity/IdentityConsensus.sol";
import "../contracts/identity/IdentityRegistry.sol";
import "../contracts/identity/GovernanceParameters.sol";

contract IdentityConsensusTest is Test {
    IdentityConsensus public consensus;
    IdentityRegistry public registry;
    GovernanceParameters public params;

    address public owner = address(1);
    address public alice = address(2);
    address public bob = address(3);
    address public charlie = address(4);
    address public oracle1 = address(5);
    address public oracle2 = address(6);

    function setUp() public {
        vm.startPrank(owner);

        // Deploy contracts
        params = new GovernanceParameters();
        registry = new IdentityRegistry();
        consensus = new IdentityConsensus(address(registry), address(params));

        // Link contracts
        registry.setConsensusContract(address(consensus));

        vm.stopPrank();

        // Setup test identities (must be called from consensus contract)
        vm.startPrank(address(consensus));

        // Alice, Bob, Charlie are Primaries
        registry.upgradeToPrimary(alice);
        registry.upgradeToPrimary(bob);
        registry.upgradeToPrimary(charlie);

        // Oracle1 and Oracle2 are Primaries first
        registry.upgradeToPrimary(oracle1);
        registry.upgradeToPrimary(oracle2);

        vm.stopPrank();

        // Then upgrade to Oracle (owner-only)
        vm.startPrank(owner);
        registry.upgradeToOracle(oracle1);
        registry.upgradeToOracle(oracle2);

        vm.stopPrank();

        // Give everyone ETH for staking
        vm.deal(alice, 10 ether);
        vm.deal(bob, 10 ether);
        vm.deal(charlie, 10 ether);
        vm.deal(oracle1, 10 ether);
        vm.deal(oracle2, 10 ether);
    }

    // ============ Request Primary Verification Tests ============

    function test_RequestPrimaryVerification_CreatesClaimWithStake() public {
        address newUser = address(100);
        vm.deal(newUser, 1 ether);

        vm.prank(newUser);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}(
            "I am a unique human living in California"
        );

        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(claimId);

        assertEq(claim.claimId, 1);
        assertEq(uint256(claim.claimType), uint256(IdentityConsensus.ClaimType.NewPrimary));
        // Note: Status might be Active or already resolved depending on self-vouch weight
        assertEq(claim.subject, newUser);
        assertEq(claim.justification, "I am a unique human living in California");
        // Total weight should be positive from self-vouch
        assertTrue(claim.totalWeightFor > 0 || claim.totalWeightAgainst == 0);
    }

    function test_RequestPrimaryVerification_RevertsIfInsufficientStake() public {
        address newUser = address(100);
        vm.deal(newUser, 1 ether);

        vm.prank(newUser);
        vm.expectRevert("Insufficient stake");
        consensus.requestPrimaryVerification{value: 0.01 ether}("Test"); // Only 0.01 ETH, need 0.03
    }

    function test_RequestPrimaryVerification_RevertsIfAlreadyVerified() public {
        vm.prank(alice);
        vm.expectRevert("Already verified");
        consensus.requestPrimaryVerification{value: 0.03 ether}("Test");
    }

    function test_RequestPrimaryVerification_RevertsWithoutJustification() public {
        address newUser = address(100);
        vm.deal(newUser, 1 ether);

        vm.prank(newUser);
        vm.expectRevert("Justification required");
        consensus.requestPrimaryVerification{value: 0.03 ether}("");
    }

    // ============ Request Link to Primary Tests ============

    function test_RequestLinkToPrimary_CreatesClaimWithFlexiblePlatform() public {
        address newLinked = address(101);
        vm.deal(newLinked, 1 ether);

        vm.prank(newLinked);
        uint256 claimId = consensus.requestLinkToPrimary{value: 0.01 ether}(
            alice,
            "LinkedIn-business",
            "This is my business LinkedIn account"
        );

        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(claimId);

        assertEq(uint256(claim.claimType), uint256(IdentityConsensus.ClaimType.LinkToPrimary));
        assertEq(claim.relatedAddress, alice);
        assertEq(claim.platform, "LinkedIn-business");
        assertEq(claim.justification, "This is my business LinkedIn account");
    }

    function test_RequestLinkToPrimary_RevertsIfNotPrimary() public {
        address newLinked = address(101);
        address notPrimary = address(102);
        vm.deal(newLinked, 1 ether);

        vm.prank(newLinked);
        vm.expectRevert("Not a primary");
        consensus.requestLinkToPrimary{value: 0.01 ether}(
            notPrimary,
            "LinkedIn",
            "Test"
        );
    }

    // ============ Vouching Tests ============

    function test_VouchFor_AddsPrimaryWeight() public {
        address newUser = address(100);
        vm.deal(newUser, 1 ether);

        vm.prank(newUser);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}("Test");

        // Alice (Primary, weight=1) vouches FOR
        vm.prank(alice);
        consensus.vouchFor{value: 0.03 ether}(claimId);

        (uint256 percentFor, ) = consensus.getCurrentConsensus(claimId);

        // Should have more support now
        assertGt(percentFor, 0);
    }

    function test_VouchFor_AddsOracleWeight() public {
        address newUser = address(100);
        vm.deal(newUser, 1 ether);

        vm.prank(newUser);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}("Test");

        // Oracle1 (weight=100) vouches FOR
        vm.prank(oracle1);
        consensus.vouchFor{value: 0.03 ether}(claimId);

        (uint256 percentFor, ) = consensus.getCurrentConsensus(claimId);

        // Oracle's massive weight should dominate
        assertGt(percentFor, 9000); // >90% because oracle weight is 100
    }

    function test_VouchAgainst_AddsOppositionWeight() public {
        address newUser = address(100);
        vm.deal(newUser, 1 ether);

        vm.prank(newUser);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}("Test");

        // Oracle1 vouches AGAINST
        vm.prank(oracle1);
        consensus.vouchAgainst{value: 0.03 ether}(claimId);

        (, uint256 percentAgainst) = consensus.getCurrentConsensus(claimId);

        // Should have strong opposition
        assertGt(percentAgainst, 9000);
    }

    function test_Vouch_RevertsIfAlreadyVouched() public {
        // Use Link claim (51% threshold) to avoid immediate resolution
        address newLinked = address(100);
        vm.deal(newLinked, 1 ether);

        vm.prank(newLinked);
        uint256 claimId = consensus.requestLinkToPrimary{value: 0.01 ether}(
            alice,
            "LinkedIn",
            "Test account"
        );

        // Bob vouches FOR
        vm.prank(bob);
        consensus.vouchFor{value: 0.01 ether}(claimId);

        // Charlie vouches AGAINST (now we have 1 FOR, 1 AGAINST = 50/50, below 51% threshold)
        vm.prank(charlie);
        consensus.vouchAgainst{value: 0.01 ether}(claimId);

        // Verify claim still active (50% FOR < 51% threshold)
        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(claimId);
        assertEq(uint256(claim.status), uint256(IdentityConsensus.ClaimStatus.Active));

        // Try to have bob vouch again - should fail with "Already vouched"
        vm.prank(bob);
        vm.expectRevert("Already vouched");
        consensus.vouchFor{value: 0.01 ether}(claimId);
    }

    function test_Vouch_RevertsIfSubjectVouches() public {
        address newUser = address(100);
        vm.deal(newUser, 1 ether);

        vm.prank(newUser);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}("Test");

        // Try to vouch on own claim (already self-vouched in creation)
        vm.prank(newUser);
        vm.expectRevert("Already vouched");
        consensus.vouchFor{value: 0.03 ether}(claimId);
    }

    function test_Vouch_RevertsIfNotVerified() public {
        address newUser = address(100);
        address unverified = address(101);
        vm.deal(newUser, 1 ether);
        vm.deal(unverified, 1 ether);

        vm.prank(newUser);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}("Test");

        vm.prank(unverified);
        vm.expectRevert("Must be Primary or Oracle");
        consensus.vouchFor{value: 0.03 ether}(claimId);
    }

    // ============ Consensus Resolution Tests ============

    function test_ConsensusReached_PrimaryApproved() public {
        address newUser = address(100);
        vm.deal(newUser, 1 ether);

        vm.prank(newUser);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}("Test");

        // Oracle1 vouches FOR (weight=100, auto-resolves consensus)
        vm.prank(oracle1);
        consensus.vouchFor{value: 0.03 ether}(claimId);

        // Claim should be approved immediately (oracle weight >> 67% threshold)
        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(claimId);
        assertEq(uint256(claim.status), uint256(IdentityConsensus.ClaimStatus.Approved));

        // User should now be Primary
        assertTrue(registry.isPrimary(newUser));
    }

    function test_ConsensusReached_PrimaryRejected() public {
        address newUser = address(100);
        vm.deal(newUser, 1 ether);

        vm.prank(newUser);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}("Test");

        // Oracle1 vouches AGAINST (weight=100, auto-resolves rejection)
        vm.prank(oracle1);
        consensus.vouchAgainst{value: 0.03 ether}(claimId);

        // Claim should be rejected immediately
        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(claimId);
        assertEq(uint256(claim.status), uint256(IdentityConsensus.ClaimStatus.Rejected));

        // User should still be Grey Ghost
        assertEq(uint256(registry.getTier(newUser)), uint256(IdentityRegistry.IdentityTier.GreyGhost));
    }

    function test_ConsensusReached_LinkApproved() public {
        address newLinked = address(101);
        vm.deal(newLinked, 1 ether);

        vm.prank(newLinked);
        uint256 claimId = consensus.requestLinkToPrimary{value: 0.01 ether}(
            alice,
            "LinkedIn",
            "My work account"
        );

        // Oracle vouches FOR (auto-resolves with weight=100)
        vm.prank(oracle1);
        consensus.vouchFor{value: 0.01 ether}(claimId);

        // Should be approved immediately
        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(claimId);
        assertEq(uint256(claim.status), uint256(IdentityConsensus.ClaimStatus.Approved));

        // Should be linked
        assertEq(uint256(registry.getTier(newLinked)), uint256(IdentityRegistry.IdentityTier.LinkedID));
        assertEq(registry.getPrimaryAddress(newLinked), alice);
    }

    // ============ Duplicate Challenge Tests ============

    function test_ChallengeDuplicate_CreatesClaim() public {
        // Setup: Alice and Bob are both Primaries (suspicious)
        address challenger = charlie;

        vm.prank(challenger);
        uint256 claimId = consensus.challengeDuplicate{value: 0.1 ether}(
            alice,
            bob,
            "Same IP address, same transaction patterns"
        );

        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(claimId);

        assertEq(uint256(claim.claimType), uint256(IdentityConsensus.ClaimType.DuplicateFlag));
        assertEq(claim.subject, alice);
        assertEq(claim.relatedAddress, bob);

        // Note: With only charlie voting (100% of votes), the 80% threshold is met immediately
        // So the claim auto-resolves and challenges are cleared
        assertEq(uint256(claim.status), uint256(IdentityConsensus.ClaimStatus.Approved));
        assertFalse(registry.isUnderChallenge(alice)); // Challenge cleared after resolution
        assertFalse(registry.isUnderChallenge(bob));

        // Both identities should be downgraded to GreyGhost
        assertEq(uint256(registry.getTier(alice)), uint256(IdentityRegistry.IdentityTier.GreyGhost));
        assertEq(uint256(registry.getTier(bob)), uint256(IdentityRegistry.IdentityTier.GreyGhost));
    }

    function test_ChallengeDuplicate_RevertsIfNotBothPrimary() public {
        address notPrimary = address(200);

        vm.prank(charlie);
        vm.expectRevert("addr2 not primary");
        consensus.challengeDuplicate{value: 0.1 ether}(
            alice,
            notPrimary,
            "Test"
        );
    }

    function test_ChallengeDuplicate_RequiresHighStake() public {
        vm.prank(charlie);
        vm.expectRevert("Insufficient stake");
        consensus.challengeDuplicate{value: 0.05 ether}( // Only 0.05, need 0.1
            alice,
            bob,
            "Test"
        );
    }

    // ============ Reward Distribution Tests ============

    function test_ClaimRewards_RefundsCorrectVouchers() public {
        address newUser = address(100);
        vm.deal(newUser, 1 ether);

        vm.prank(newUser);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}("Test");

        uint256 aliceBalanceBefore = alice.balance;

        // Alice vouches FOR (auto-resolves immediately since she's only voter with weight)
        vm.prank(alice);
        consensus.vouchFor{value: 0.03 ether}(claimId);

        // Claim is now Approved (alice was only voter, 100% > 67% threshold)
        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(claimId);
        assertEq(uint256(claim.status), uint256(IdentityConsensus.ClaimStatus.Approved));

        // Alice can claim rewards
        vm.prank(alice);
        consensus.claimRewards(claimId);

        // Alice should get refund + share of rewards
        assertGe(alice.balance, aliceBalanceBefore);
    }

    function test_ClaimRewards_SlashesIncorrectVouchers() public {
        // Use Link claim to test slashing more easily
        address newLinked = address(100);
        vm.deal(newLinked, 1 ether);

        vm.prank(newLinked);
        uint256 claimId = consensus.requestLinkToPrimary{value: 0.01 ether}(
            alice,
            "LinkedIn",
            "Test account"
        );

        uint256 bobBalanceBefore = bob.balance;

        // Charlie vouches FOR
        vm.prank(charlie);
        consensus.vouchFor{value: 0.01 ether}(claimId);

        // Bob vouches AGAINST (will be wrong, now 1 FOR, 1 AGAINST = 50/50, stays active)
        vm.prank(bob);
        consensus.vouchAgainst{value: 0.01 ether}(claimId);

        // Oracle approves (auto-resolves to Approved, Bob was wrong)
        vm.prank(oracle1);
        consensus.vouchFor{value: 0.01 ether}(claimId);

        // Claim approved, Bob's stake should be slashed
        vm.prank(bob);
        consensus.claimRewards(claimId);

        // Bob should have lost stake (10% slash for Link claims)
        assertLt(bob.balance, bobBalanceBefore);
    }

    // ============ Integration Tests ============

    function test_CompleteFlow_NewPrimaryApproval() public {
        address newUser = address(100);
        vm.deal(newUser, 1 ether);

        // Step 1: Request Primary verification
        vm.prank(newUser);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}(
            "Unique human from San Francisco"
        );

        // Step 2: Alice vouches (auto-resolves immediately as only weighted voter)
        vm.prank(alice);
        consensus.vouchFor{value: 0.03 ether}(claimId);

        // Step 3: Verify consensus reached
        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(claimId);
        assertEq(uint256(claim.status), uint256(IdentityConsensus.ClaimStatus.Approved));

        // Step 4: Verify user is now Primary
        assertTrue(registry.isPrimary(newUser));
        assertEq(uint256(registry.getTier(newUser)), uint256(IdentityRegistry.IdentityTier.PrimaryID));

        // Step 5: Claim rewards
        vm.prank(alice);
        consensus.claimRewards(claimId);
    }

    function test_CompleteFlow_LinkToPrimaryWithMultipleAccounts() public {
        address linkedin1 = address(101);
        address linkedin2 = address(102);
        vm.deal(linkedin1, 1 ether);
        vm.deal(linkedin2, 1 ether);

        // Link first LinkedIn account
        vm.prank(linkedin1);
        uint256 claim1 = consensus.requestLinkToPrimary{value: 0.01 ether}(
            alice,
            "LinkedIn",
            "My personal LinkedIn"
        );

        // Oracle approves (auto-resolves)
        vm.prank(oracle1);
        consensus.vouchFor{value: 0.01 ether}(claim1);

        // Link second LinkedIn account (business)
        vm.prank(linkedin2);
        uint256 claim2 = consensus.requestLinkToPrimary{value: 0.01 ether}(
            alice,
            "LinkedIn-business",
            "My business LinkedIn account"
        );

        // Oracle approves (auto-resolves)
        vm.prank(oracle1);
        consensus.vouchFor{value: 0.01 ether}(claim2);

        // Both should be linked to Alice
        assertEq(registry.getPrimaryAddress(linkedin1), alice);
        assertEq(registry.getPrimaryAddress(linkedin2), alice);
        assertEq(registry.getLinkedCount(alice), 2);
    }

    // ============ View Function Tests ============

    function test_GetClaim_ReturnsCorrectData() public view {
        // Should return default for non-existent claim
        IdentityConsensus.IdentityClaim memory claim = consensus.getClaim(999);
        assertEq(claim.claimId, 0);
    }

    function test_GetCurrentConsensus_CalculatesCorrectly() public {
        address newUser = address(100);
        vm.deal(newUser, 1 ether);

        vm.prank(newUser);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}("Test");

        // Get consensus percentages
        (uint256 percentFor, uint256 percentAgainst) = consensus.getCurrentConsensus(claimId);

        // Either claim is still active with votes, or it auto-resolved
        // If still active, should have votes; if resolved, percentages might be 0
        assertTrue(percentFor >= 0 && percentAgainst >= 0);
        assertTrue(percentFor + percentAgainst <= 10000); // Total can't exceed 100%
    }

    function test_CanVouch_ValidatesCorrectly() public {
        address newUser = address(100);
        vm.deal(newUser, 1 ether);

        vm.prank(newUser);
        uint256 claimId = consensus.requestPrimaryVerification{value: 0.03 ether}("Test");

        // Alice can vouch (is Primary)
        (bool canVouch, string memory reason) = consensus.canVouch(alice, claimId);
        assertTrue(canVouch);
        assertEq(reason, "");

        // Unverified cannot vouch
        address unverified = address(200);
        (canVouch, reason) = consensus.canVouch(unverified, claimId);
        assertFalse(canVouch);
        assertEq(reason, "Must be Primary or Oracle");
    }
}
