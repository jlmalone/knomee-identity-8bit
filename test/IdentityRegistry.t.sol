// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../contracts/identity/IdentityRegistry.sol";

contract IdentityRegistryTest is Test {
    IdentityRegistry public registry;

    address public owner = address(1);
    address public consensus = address(2);
    address public alice = address(3);
    address public bob = address(4);
    address public charlie = address(5);
    address public unauthorized = address(6);

    event IdentityVerified(address indexed addr, IdentityRegistry.IdentityTier tier, uint256 timestamp);
    event IdentityLinked(address indexed secondary, address indexed primary, string platform, string justification);
    event IdentityUpgraded(address indexed addr, IdentityRegistry.IdentityTier from, IdentityRegistry.IdentityTier to);
    event IdentityDowngraded(address indexed addr, IdentityRegistry.IdentityTier from, IdentityRegistry.IdentityTier to);
    event IdentityChallenged(address indexed addr, uint256 challengeId);
    event ChallengeCleared(address indexed addr, uint256 challengeId);
    event ConsensusContractUpdated(address indexed oldContract, address indexed newContract);

    function setUp() public {
        vm.startPrank(owner);
        registry = new IdentityRegistry();
        registry.setConsensusContract(consensus);
        vm.stopPrank();
    }

    // ============ Constructor & Setup Tests ============

    function test_Constructor_SetsOwner() public view {
        assertEq(registry.owner(), owner);
    }

    function test_SetConsensusContract_UpdatesAddress() public {
        vm.startPrank(owner);

        address newConsensus = address(7);

        vm.expectEmit(true, true, true, true);
        emit ConsensusContractUpdated(consensus, newConsensus);
        registry.setConsensusContract(newConsensus);

        assertEq(registry.consensusContract(), newConsensus);

        vm.stopPrank();
    }

    function test_SetConsensusContract_RevertsIfNotOwner() public {
        vm.prank(unauthorized);
        vm.expectRevert();
        registry.setConsensusContract(address(7));
    }

    function test_SetConsensusContract_RevertsIfZeroAddress() public {
        vm.prank(owner);
        vm.expectRevert("Invalid address");
        registry.setConsensusContract(address(0));
    }

    // ============ Upgrade to Primary Tests ============

    function test_UpgradeToPrimary_UpgradesGreyGhost() public {
        vm.prank(consensus);

        vm.expectEmit(true, true, true, true);
        emit IdentityVerified(alice, IdentityRegistry.IdentityTier.PrimaryID, block.timestamp);
        registry.upgradeToPrimary(alice);

        assertEq(uint256(registry.getTier(alice)), uint256(IdentityRegistry.IdentityTier.PrimaryID));
        assertTrue(registry.isPrimary(alice));
    }

    function test_UpgradeToPrimary_SetsVerifiedAt() public {
        vm.prank(consensus);
        registry.upgradeToPrimary(alice);

        IdentityRegistry.Identity memory identity = registry.getIdentity(alice);
        assertEq(identity.verifiedAt, block.timestamp);
    }

    function test_UpgradeToPrimary_RevertsIfNotConsensus() public {
        vm.prank(unauthorized);
        vm.expectRevert("Only consensus contract");
        registry.upgradeToPrimary(alice);
    }

    function test_UpgradeToPrimary_RevertsIfAlreadyVerified() public {
        vm.startPrank(consensus);

        // First upgrade to Primary
        registry.upgradeToPrimary(alice);

        // Try to upgrade again
        vm.expectRevert("Already verified");
        registry.upgradeToPrimary(alice);

        vm.stopPrank();
    }

    // ============ Upgrade to Linked Tests ============

    function test_UpgradeToLinked_CreatesLink() public {
        // First make alice a Primary
        vm.startPrank(consensus);
        registry.upgradeToPrimary(alice);

        // Now link bob to alice
        vm.expectEmit(true, true, true, true);
        emit IdentityVerified(bob, IdentityRegistry.IdentityTier.LinkedID, block.timestamp);
        emit IdentityLinked(bob, alice, "LinkedIn", "This is my work account");

        registry.upgradeToLinked(bob, alice, "LinkedIn", "This is my work account");

        assertEq(uint256(registry.getTier(bob)), uint256(IdentityRegistry.IdentityTier.LinkedID));
        assertEq(registry.getPrimaryAddress(bob), alice);
        assertEq(registry.getLinkedAddress(alice, "LinkedIn"), bob);

        vm.stopPrank();
    }

    function test_UpgradeToLinked_AllowsMultipleAccountsPerPlatform() public {
        // Make alice a Primary
        vm.startPrank(consensus);
        registry.upgradeToPrimary(alice);

        // Link first LinkedIn
        registry.upgradeToLinked(bob, alice, "LinkedIn", "My personal LinkedIn");

        // Link second LinkedIn (different account name)
        registry.upgradeToLinked(charlie, alice, "LinkedIn-business", "My business LinkedIn");

        assertEq(registry.getLinkedAddress(alice, "LinkedIn"), bob);
        assertEq(registry.getLinkedAddress(alice, "LinkedIn-business"), charlie);
        assertEq(registry.getLinkedCount(alice), 2);

        vm.stopPrank();
    }

    function test_UpgradeToLinked_FlexiblePlatformNames() public {
        vm.startPrank(consensus);
        registry.upgradeToPrimary(alice);

        // Test various platform names
        registry.upgradeToLinked(bob, alice, "TikTok", "New platform");
        registry.upgradeToLinked(charlie, alice, "CustomPlatform123", "Custom platform");

        assertEq(registry.getLinkedAddress(alice, "TikTok"), bob);
        assertEq(registry.getLinkedAddress(alice, "CustomPlatform123"), charlie);

        vm.stopPrank();
    }

    function test_UpgradeToLinked_RevertsIfNotConsensus() public {
        vm.prank(consensus);
        registry.upgradeToPrimary(alice);

        vm.prank(unauthorized);
        vm.expectRevert("Only consensus contract");
        registry.upgradeToLinked(bob, alice, "LinkedIn", "Test");
    }

    function test_UpgradeToLinked_RevertsIfAlreadyVerified() public {
        vm.startPrank(consensus);
        registry.upgradeToPrimary(alice);
        registry.upgradeToPrimary(bob);

        // Try to link an already-Primary address
        vm.expectRevert("Already verified");
        registry.upgradeToLinked(bob, alice, "LinkedIn", "Test");

        vm.stopPrank();
    }

    function test_UpgradeToLinked_RevertsIfInvalidPrimary() public {
        vm.prank(consensus);

        // Try to link to a non-Primary address
        vm.expectRevert("Invalid primary");
        registry.upgradeToLinked(bob, alice, "LinkedIn", "Test");
    }

    function test_UpgradeToLinked_RevertsIfNoPlatform() public {
        vm.startPrank(consensus);
        registry.upgradeToPrimary(alice);

        vm.expectRevert("Platform required");
        registry.upgradeToLinked(bob, alice, "", "Test");

        vm.stopPrank();
    }

    function test_UpgradeToLinked_WorksWithOraclePrimary() public {
        vm.startPrank(consensus);
        registry.upgradeToPrimary(alice);
        vm.stopPrank();

        vm.prank(owner);
        registry.upgradeToOracle(alice);

        vm.prank(consensus);
        registry.upgradeToLinked(bob, alice, "LinkedIn", "Test");

        assertEq(registry.getPrimaryAddress(bob), alice);
    }

    // ============ Upgrade to Oracle Tests ============

    function test_UpgradeToOracle_UpgradesPrimary() public {
        vm.prank(consensus);
        registry.upgradeToPrimary(alice);

        vm.prank(owner);
        vm.expectEmit(true, true, true, true);
        emit IdentityUpgraded(alice, IdentityRegistry.IdentityTier.PrimaryID, IdentityRegistry.IdentityTier.Oracle);
        registry.upgradeToOracle(alice);

        assertEq(uint256(registry.getTier(alice)), uint256(IdentityRegistry.IdentityTier.Oracle));
        assertTrue(registry.isOracle(alice));
        assertTrue(registry.isPrimary(alice)); // Oracle is also considered Primary
    }

    function test_UpgradeToOracle_SetsOracleGrantedAt() public {
        vm.prank(consensus);
        registry.upgradeToPrimary(alice);

        vm.prank(owner);
        registry.upgradeToOracle(alice);

        IdentityRegistry.Identity memory identity = registry.getIdentity(alice);
        assertEq(identity.oracleGrantedAt, block.timestamp);
    }

    function test_UpgradeToOracle_RevertsIfNotOwner() public {
        vm.prank(consensus);
        registry.upgradeToPrimary(alice);

        vm.prank(unauthorized);
        vm.expectRevert();
        registry.upgradeToOracle(alice);
    }

    function test_UpgradeToOracle_RevertsIfNotPrimary() public {
        vm.prank(owner);
        vm.expectRevert("Must be Primary first");
        registry.upgradeToOracle(alice);
    }

    // ============ Downgrade Tests ============

    function test_DowngradeIdentity_DowngradesPrimary() public {
        vm.startPrank(consensus);
        registry.upgradeToPrimary(alice);

        vm.expectEmit(true, true, true, true);
        emit IdentityDowngraded(alice, IdentityRegistry.IdentityTier.PrimaryID, IdentityRegistry.IdentityTier.GreyGhost);
        registry.downgradeIdentity(alice, IdentityRegistry.IdentityTier.GreyGhost);

        assertEq(uint256(registry.getTier(alice)), uint256(IdentityRegistry.IdentityTier.GreyGhost));

        vm.stopPrank();
    }

    function test_DowngradeIdentity_ClearsLinkedIds() public {
        vm.startPrank(consensus);

        // Setup: Alice as Primary with linked IDs
        registry.upgradeToPrimary(alice);
        registry.upgradeToLinked(bob, alice, "LinkedIn", "Test");
        registry.upgradeToLinked(charlie, alice, "Instagram", "Test");

        assertEq(registry.getLinkedCount(alice), 2);

        // Downgrade alice
        registry.downgradeIdentity(alice, IdentityRegistry.IdentityTier.GreyGhost);

        // Linked IDs should be cleared
        assertEq(registry.getLinkedCount(alice), 0);
        assertEq(registry.getLinkedAddress(alice, "LinkedIn"), address(0));
        assertEq(registry.getLinkedAddress(alice, "Instagram"), address(0));

        // Linked addresses should be downgraded too
        assertEq(uint256(registry.getTier(bob)), uint256(IdentityRegistry.IdentityTier.GreyGhost));
        assertEq(uint256(registry.getTier(charlie)), uint256(IdentityRegistry.IdentityTier.GreyGhost));

        vm.stopPrank();
    }

    function test_DowngradeIdentity_RevertsIfNotDowngrade() public {
        vm.startPrank(consensus);
        registry.upgradeToPrimary(alice);

        // Try to "downgrade" to same tier
        vm.expectRevert("Not a downgrade");
        registry.downgradeIdentity(alice, IdentityRegistry.IdentityTier.PrimaryID);

        vm.stopPrank();
    }

    function test_DowngradeIdentity_RevertsIfNotConsensus() public {
        vm.prank(consensus);
        registry.upgradeToPrimary(alice);

        vm.prank(unauthorized);
        vm.expectRevert("Only consensus contract");
        registry.downgradeIdentity(alice, IdentityRegistry.IdentityTier.GreyGhost);
    }

    // ============ Challenge Tests ============

    function test_MarkUnderChallenge_SetsChallenge() public {
        vm.prank(consensus);
        registry.upgradeToPrimary(alice);

        vm.prank(consensus);
        vm.expectEmit(true, true, true, true);
        emit IdentityChallenged(alice, 123);
        registry.markUnderChallenge(alice, 123);

        assertTrue(registry.isUnderChallenge(alice));
        assertEq(registry.getChallengeId(alice), 123);
    }

    function test_ClearChallenge_ClearsChallenge() public {
        vm.startPrank(consensus);

        registry.upgradeToPrimary(alice);
        registry.markUnderChallenge(alice, 123);

        vm.expectEmit(true, true, true, true);
        emit ChallengeCleared(alice, 123);
        registry.clearChallenge(alice);

        assertFalse(registry.isUnderChallenge(alice));
        assertEq(registry.getChallengeId(alice), 0);

        vm.stopPrank();
    }

    // ============ Record Vouch Tests ============

    function test_RecordVouch_IncrementsCounts() public {
        vm.startPrank(consensus);

        registry.recordVouch(alice, 0.01 ether);
        registry.recordVouch(alice, 0.02 ether);
        registry.recordVouch(alice, 0.03 ether);

        IdentityRegistry.Identity memory identity = registry.getIdentity(alice);
        assertEq(identity.totalVouchesReceived, 3);
        assertEq(identity.totalStakeReceived, 0.06 ether);

        vm.stopPrank();
    }

    // ============ View Function Tests ============

    function test_GetLinkedPlatforms_ReturnsAllPlatforms() public {
        vm.startPrank(consensus);

        registry.upgradeToPrimary(alice);
        registry.upgradeToLinked(bob, alice, "LinkedIn", "Work");
        registry.upgradeToLinked(charlie, alice, "Instagram", "Personal");

        IdentityRegistry.LinkedPlatform[] memory platforms = registry.getLinkedPlatforms(alice);

        assertEq(platforms.length, 2);
        assertEq(platforms[0].linkedAddress, bob);
        assertEq(platforms[0].platform, "LinkedIn");
        assertEq(platforms[0].justification, "Work");
        assertEq(platforms[1].linkedAddress, charlie);
        assertEq(platforms[1].platform, "Instagram");
        assertEq(platforms[1].justification, "Personal");

        vm.stopPrank();
    }

    function test_IsPrimary_ReturnsTrueForOracle() public {
        vm.prank(consensus);
        registry.upgradeToPrimary(alice);

        vm.prank(owner);
        registry.upgradeToOracle(alice);

        assertTrue(registry.isPrimary(alice));
    }

    function test_IsPrimary_ReturnsFalseForLinked() public {
        vm.startPrank(consensus);
        registry.upgradeToPrimary(alice);
        registry.upgradeToLinked(bob, alice, "LinkedIn", "Test");

        assertFalse(registry.isPrimary(bob));

        vm.stopPrank();
    }

    function test_IsOracle_ReturnsFalseForPrimary() public {
        vm.prank(consensus);
        registry.upgradeToPrimary(alice);

        assertFalse(registry.isOracle(alice));
    }

    // ============ Edge Case Tests ============

    function test_GetIdentity_ReturnsDefaultForUnregistered() public view {
        IdentityRegistry.Identity memory identity = registry.getIdentity(alice);

        assertEq(uint256(identity.tier), uint256(IdentityRegistry.IdentityTier.GreyGhost));
        assertEq(identity.verifiedAt, 0);
        assertEq(identity.totalVouchesReceived, 0);
    }

    function test_GetLinkedCount_ReturnsZeroForNoLinks() public view {
        assertEq(registry.getLinkedCount(alice), 0);
    }

    function test_GetPrimaryAddress_ReturnsZeroForNonLinked() public view {
        assertEq(registry.getPrimaryAddress(alice), address(0));
    }

    function test_MultiplePrimariesWithLinkedIds() public {
        vm.startPrank(consensus);

        // Alice and Bob both become Primaries
        registry.upgradeToPrimary(alice);
        registry.upgradeToPrimary(bob);

        // Each has their own linked IDs
        address alice_linkedin = address(10);
        address bob_instagram = address(11);

        registry.upgradeToLinked(alice_linkedin, alice, "LinkedIn", "Alice work");
        registry.upgradeToLinked(bob_instagram, bob, "Instagram", "Bob personal");

        // Verify separation
        assertEq(registry.getPrimaryAddress(alice_linkedin), alice);
        assertEq(registry.getPrimaryAddress(bob_instagram), bob);
        assertEq(registry.getLinkedCount(alice), 1);
        assertEq(registry.getLinkedCount(bob), 1);

        vm.stopPrank();
    }

    function test_ComplexPlatformNaming() public {
        vm.startPrank(consensus);
        registry.upgradeToPrimary(alice);

        // Test various naming conventions
        registry.upgradeToLinked(address(10), alice, "LinkedIn", "Main");
        registry.upgradeToLinked(address(11), alice, "LinkedIn-business", "Business");
        registry.upgradeToLinked(address(12), alice, "LinkedIn_recruiting", "Recruiting");
        registry.upgradeToLinked(address(13), alice, "LinkedIn.personal", "Personal");

        assertEq(registry.getLinkedCount(alice), 4);

        vm.stopPrank();
    }
}
