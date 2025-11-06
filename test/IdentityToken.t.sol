// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../contracts/identity/IdentityToken.sol";

contract IdentityTokenTest is Test {
    IdentityToken public token;

    address public owner = address(1);
    address public registry = address(2);
    address public alice = address(3);
    address public bob = address(4);
    address public charlie = address(5);
    address public unauthorized = address(6);

    event IdentityMinted(address indexed account, uint256 indexed tokenId, IdentityToken.IdentityTier tier);
    event TierUpgraded(address indexed account, IdentityToken.IdentityTier newTier);
    event TierDowngraded(address indexed account, IdentityToken.IdentityTier newTier);
    event IdentityRevoked(address indexed account, uint256 indexed tokenId);
    event RegistryUpdated(address indexed oldRegistry, address indexed newRegistry);

    function setUp() public {
        vm.startPrank(owner);
        token = new IdentityToken();
        token.setIdentityRegistry(registry);
        vm.stopPrank();
    }

    // ============ Constructor & Setup Tests ============

    function test_Constructor_SetsOwner() public view {
        assertEq(token.owner(), owner);
    }

    function test_Constructor_SetsName() public view {
        assertEq(token.name(), "Knomee Identity Token");
    }

    function test_Constructor_SetsSymbol() public view {
        assertEq(token.symbol(), "IDT");
    }

    function test_Constructor_TransfersDisabled() public view {
        assertTrue(token.transfersDisabled());
    }

    function test_SetIdentityRegistry_UpdatesAddress() public {
        vm.startPrank(owner);

        address newRegistry = address(7);

        vm.expectEmit(true, true, true, true);
        emit RegistryUpdated(registry, newRegistry);
        token.setIdentityRegistry(newRegistry);

        assertEq(token.identityRegistry(), newRegistry);

        vm.stopPrank();
    }

    function test_SetIdentityRegistry_RevertsIfNotOwner() public {
        vm.prank(unauthorized);
        vm.expectRevert();
        token.setIdentityRegistry(address(7));
    }

    function test_SetIdentityRegistry_RevertsIfZeroAddress() public {
        vm.prank(owner);
        vm.expectRevert("Invalid registry address");
        token.setIdentityRegistry(address(0));
    }

    // ============ Minting Tests ============

    function test_Mint_CreatesPrimaryID() public {
        vm.prank(registry);

        vm.expectEmit(true, true, true, true);
        emit IdentityMinted(alice, 1, IdentityToken.IdentityTier.PrimaryID);
        uint256 tokenId = token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        assertEq(tokenId, 1);
        assertEq(token.balanceOf(alice), 1);
        assertEq(token.ownerOf(1), alice);
        assertEq(uint256(token.getTier(alice)), uint256(IdentityToken.IdentityTier.PrimaryID));
        assertEq(token.accountToTokenId(alice), 1);
        assertEq(token.tokenIdToAccount(1), alice);
    }

    function test_Mint_CreatesLinkedID() public {
        vm.prank(registry);

        uint256 tokenId = token.mint(alice, IdentityToken.IdentityTier.LinkedID);

        assertEq(tokenId, 1);
        assertEq(uint256(token.getTier(alice)), uint256(IdentityToken.IdentityTier.LinkedID));
    }

    function test_Mint_IncrementsTokenId() public {
        vm.startPrank(registry);

        uint256 token1 = token.mint(alice, IdentityToken.IdentityTier.PrimaryID);
        uint256 token2 = token.mint(bob, IdentityToken.IdentityTier.PrimaryID);
        uint256 token3 = token.mint(charlie, IdentityToken.IdentityTier.LinkedID);

        assertEq(token1, 1);
        assertEq(token2, 2);
        assertEq(token3, 3);

        vm.stopPrank();
    }

    function test_Mint_RevertsIfNotRegistry() public {
        vm.prank(unauthorized);
        vm.expectRevert("Only registry can call");
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);
    }

    function test_Mint_RevertsIfZeroAddress() public {
        vm.prank(registry);
        vm.expectRevert("Cannot mint to zero address");
        token.mint(address(0), IdentityToken.IdentityTier.PrimaryID);
    }

    function test_Mint_RevertsIfAlreadyHasToken() public {
        vm.startPrank(registry);

        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.expectRevert("Account already has identity token");
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.stopPrank();
    }

    function test_Mint_RevertsIfGreyGhost() public {
        vm.prank(registry);
        vm.expectRevert("Cannot mint GreyGhost tier");
        token.mint(alice, IdentityToken.IdentityTier.GreyGhost);
    }

    function test_MintPrimaryID_CreatesPrimary() public {
        vm.prank(registry);

        uint256 tokenId = token.mintPrimaryID(alice);

        assertEq(tokenId, 1);
        assertEq(uint256(token.getTier(alice)), uint256(IdentityToken.IdentityTier.PrimaryID));
        assertTrue(token.isPrimary(alice));
    }

    function test_MintLinkedID_CreatesLinked() public {
        vm.prank(registry);

        uint256 tokenId = token.mintLinkedID(alice);

        assertEq(tokenId, 1);
        assertEq(uint256(token.getTier(alice)), uint256(IdentityToken.IdentityTier.LinkedID));
        assertFalse(token.isPrimary(alice));
    }

    // ============ Tier Management Tests ============

    function test_UpgradeToOracle_UpgradesPrimary() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.prank(owner);
        vm.expectEmit(true, true, true, true);
        emit TierUpgraded(alice, IdentityToken.IdentityTier.Oracle);
        token.upgradeToOracle(alice);

        assertEq(uint256(token.getTier(alice)), uint256(IdentityToken.IdentityTier.Oracle));
        assertTrue(token.isOracle(alice));
    }

    function test_UpgradeToOracle_RevertsIfNotOwner() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.prank(unauthorized);
        vm.expectRevert();
        token.upgradeToOracle(alice);
    }

    function test_UpgradeToOracle_RevertsIfNoToken() public {
        vm.prank(owner);
        vm.expectRevert("Account has no identity token");
        token.upgradeToOracle(alice);
    }

    function test_UpgradeToOracle_RevertsIfNotPrimary() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.LinkedID);

        vm.prank(owner);
        vm.expectRevert("Must be PrimaryID to upgrade");
        token.upgradeToOracle(alice);
    }

    function test_DowngradeFromOracle_DowngradesToPrimary() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.startPrank(owner);
        token.upgradeToOracle(alice);

        vm.expectEmit(true, true, true, true);
        emit TierDowngraded(alice, IdentityToken.IdentityTier.PrimaryID);
        token.downgradeFromOracle(alice);

        assertEq(uint256(token.getTier(alice)), uint256(IdentityToken.IdentityTier.PrimaryID));
        assertFalse(token.isOracle(alice));
        assertTrue(token.isPrimary(alice));

        vm.stopPrank();
    }

    function test_DowngradeFromOracle_RevertsIfNotOwner() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.prank(owner);
        token.upgradeToOracle(alice);

        vm.prank(unauthorized);
        vm.expectRevert();
        token.downgradeFromOracle(alice);
    }

    function test_DowngradeFromOracle_RevertsIfNoToken() public {
        vm.prank(owner);
        vm.expectRevert("Account has no identity token");
        token.downgradeFromOracle(alice);
    }

    function test_DowngradeFromOracle_RevertsIfNotOracle() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.prank(owner);
        vm.expectRevert("Not an Oracle");
        token.downgradeFromOracle(alice);
    }

    // ============ Revocation Tests ============

    function test_Revoke_BurnsToken() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.prank(registry);
        vm.expectEmit(true, true, true, true);
        emit IdentityRevoked(alice, 1);
        token.revoke(alice);

        assertEq(token.balanceOf(alice), 0);
        assertEq(uint256(token.getTier(alice)), uint256(IdentityToken.IdentityTier.GreyGhost));
        assertEq(token.accountToTokenId(alice), 0);
        assertEq(token.tokenIdToAccount(1), address(0));
    }

    function test_Revoke_RevertsIfNotRegistry() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.prank(unauthorized);
        vm.expectRevert("Only registry can call");
        token.revoke(alice);
    }

    function test_Revoke_RevertsIfNoToken() public {
        vm.prank(registry);
        vm.expectRevert("Account has no identity token");
        token.revoke(alice);
    }

    // ============ Voting Weight Tests ============

    function test_GetVotingWeight_ReturnsZeroForNoToken() public view {
        assertEq(token.getVotingWeight(alice), 0);
    }

    function test_GetVotingWeight_ReturnsZeroForLinkedID() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.LinkedID);

        assertEq(token.getVotingWeight(alice), 0);
    }

    function test_GetVotingWeight_ReturnsOneForPrimaryID() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        assertEq(token.getVotingWeight(alice), 1);
    }

    function test_GetVotingWeight_ReturnsHundredForOracle() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.prank(owner);
        token.upgradeToOracle(alice);

        assertEq(token.getVotingWeight(alice), 100);
    }

    function test_CanVote_ReturnsFalseForNoToken() public view {
        assertFalse(token.canVote(alice));
    }

    function test_CanVote_ReturnsFalseForLinkedID() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.LinkedID);

        assertFalse(token.canVote(alice));
    }

    function test_CanVote_ReturnsTrueForPrimaryID() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        assertTrue(token.canVote(alice));
    }

    function test_CanVote_ReturnsTrueForOracle() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.prank(owner);
        token.upgradeToOracle(alice);

        assertTrue(token.canVote(alice));
    }

    // ============ Identity Check Tests ============

    function test_IsOracle_ReturnsTrueForOracle() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.prank(owner);
        token.upgradeToOracle(alice);

        assertTrue(token.isOracle(alice));
    }

    function test_IsOracle_ReturnsFalseForPrimary() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        assertFalse(token.isOracle(alice));
    }

    function test_IsOracle_ReturnsFalseForNoToken() public view {
        assertFalse(token.isOracle(alice));
    }

    function test_IsPrimary_ReturnsTrueForPrimary() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        assertTrue(token.isPrimary(alice));
    }

    function test_IsPrimary_ReturnsFalseForLinked() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.LinkedID);

        assertFalse(token.isPrimary(alice));
    }

    function test_IsPrimary_ReturnsFalseForNoToken() public view {
        assertFalse(token.isPrimary(alice));
    }

    function test_GetTier_ReturnsGreyGhostForNoToken() public view {
        assertEq(uint256(token.getTier(alice)), uint256(IdentityToken.IdentityTier.GreyGhost));
    }

    function test_GetTier_ReturnsCorrectTier() public {
        vm.startPrank(registry);

        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);
        token.mint(bob, IdentityToken.IdentityTier.LinkedID);

        assertEq(uint256(token.getTier(alice)), uint256(IdentityToken.IdentityTier.PrimaryID));
        assertEq(uint256(token.getTier(bob)), uint256(IdentityToken.IdentityTier.LinkedID));

        vm.stopPrank();
    }

    // ============ Soul-Bound: Transfer Prevention Tests ============

    function test_Transfer_RevertsForSoulBound() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.prank(alice);
        vm.expectRevert("Identity tokens are soul-bound");
        token.transferFrom(alice, bob, 1);
    }

    function test_SafeTransfer_RevertsForSoulBound() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.prank(alice);
        vm.expectRevert("Identity tokens are soul-bound");
        token.safeTransferFrom(alice, bob, 1);
    }

    function test_Approve_Reverts() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.prank(alice);
        vm.expectRevert("Identity tokens cannot be approved");
        token.approve(bob, 1);
    }

    function test_SetApprovalForAll_Reverts() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.prank(alice);
        vm.expectRevert("Identity tokens cannot be approved");
        token.setApprovalForAll(bob, true);
    }

    // ============ Token URI Tests ============

    function test_TokenURI_ReturnsOracleForOracle() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.prank(owner);
        token.upgradeToOracle(alice);

        assertEq(token.tokenURI(1), "Oracle");
    }

    function test_TokenURI_ReturnsPrimaryIDForPrimary() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        assertEq(token.tokenURI(1), "PrimaryID");
    }

    function test_TokenURI_ReturnsLinkedIDForLinked() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.LinkedID);

        assertEq(token.tokenURI(1), "LinkedID");
    }

    function test_TokenURI_RevertsForInvalidToken() public {
        vm.expectRevert();
        token.tokenURI(999);
    }

    // ============ Integration Tests ============

    function test_CompleteFlow_MintUpgradeRevoke() public {
        // Mint Primary
        vm.prank(registry);
        uint256 tokenId = token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        assertEq(tokenId, 1);
        assertTrue(token.isPrimary(alice));
        assertEq(token.getVotingWeight(alice), 1);

        // Upgrade to Oracle
        vm.prank(owner);
        token.upgradeToOracle(alice);

        assertTrue(token.isOracle(alice));
        assertEq(token.getVotingWeight(alice), 100);

        // Downgrade to Primary
        vm.prank(owner);
        token.downgradeFromOracle(alice);

        assertTrue(token.isPrimary(alice));
        assertEq(token.getVotingWeight(alice), 1);

        // Revoke
        vm.prank(registry);
        token.revoke(alice);

        assertEq(token.balanceOf(alice), 0);
        assertEq(token.getVotingWeight(alice), 0);
    }

    function test_MultipleIdentities() public {
        vm.startPrank(registry);

        // Mint different tiers
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);
        token.mint(bob, IdentityToken.IdentityTier.LinkedID);
        token.mint(charlie, IdentityToken.IdentityTier.PrimaryID);

        // Verify independence
        assertEq(token.balanceOf(alice), 1);
        assertEq(token.balanceOf(bob), 1);
        assertEq(token.balanceOf(charlie), 1);

        assertEq(token.accountToTokenId(alice), 1);
        assertEq(token.accountToTokenId(bob), 2);
        assertEq(token.accountToTokenId(charlie), 3);

        vm.stopPrank();

        // Upgrade one to Oracle
        vm.prank(owner);
        token.upgradeToOracle(alice);

        assertEq(token.getVotingWeight(alice), 100);
        assertEq(token.getVotingWeight(bob), 0);
        assertEq(token.getVotingWeight(charlie), 1);
    }

    // ============ Edge Case Tests ============

    function test_RevokeAfterUpgrade() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        vm.prank(owner);
        token.upgradeToOracle(alice);

        // Revoke Oracle
        vm.prank(registry);
        token.revoke(alice);

        assertEq(token.balanceOf(alice), 0);
        assertFalse(token.isOracle(alice));
    }

    function test_CannotMintAfterRevoke() public {
        vm.startPrank(registry);

        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);
        token.revoke(alice);

        // Should be able to mint again after revocation
        uint256 newTokenId = token.mint(alice, IdentityToken.IdentityTier.PrimaryID);
        assertEq(newTokenId, 2); // Next token ID

        vm.stopPrank();
    }

    function test_Mappings_ConsistentAfterRevoke() public {
        vm.prank(registry);
        token.mint(alice, IdentityToken.IdentityTier.PrimaryID);

        uint256 tokenId = token.accountToTokenId(alice);
        assertEq(token.tokenIdToAccount(tokenId), alice);

        vm.prank(registry);
        token.revoke(alice);

        assertEq(token.accountToTokenId(alice), 0);
        assertEq(token.tokenIdToAccount(tokenId), address(0));
    }
}
