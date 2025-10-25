// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../contracts/identity/GovernanceParameters.sol";

contract GovernanceParametersTest is Test {
    GovernanceParameters public params;

    address public admin = address(1);
    address public governance = address(2);
    address public unauthorized = address(3);

    event ThresholdsUpdated(uint256 link, uint256 primary, uint256 duplicate);
    event StakingUpdated(uint256 minStake, uint256 primaryMult, uint256 duplicateMult);
    event SlashingUpdated(uint256 link, uint256 primary, uint256 duplicate, uint256 sybil);
    event VotingWeightsUpdated(uint256 primaryWeight, uint256 oracleWeight);
    event CooldownsUpdated(uint256 failedClaim, uint256 duplicateFlag, uint256 claimExpiry);
    event DecayRatesUpdated(uint256 oracleRate, uint256 adminRate);
    event TimeWarped(uint256 secondsAdded, uint256 newTotalWarp);
    event GodModeRenounced(address indexed renouncer);

    function setUp() public {
        vm.startPrank(admin);
        params = new GovernanceParameters();

        // Grant governance role to governance address
        params.grantRole(params.GOVERNANCE_ROLE(), governance);
        vm.stopPrank();
    }

    // ============ Constructor Tests ============

    function test_Constructor_GrantsRoles() public view {
        assertTrue(params.hasRole(params.DEFAULT_ADMIN_ROLE(), admin));
        assertTrue(params.hasRole(params.GOVERNANCE_ROLE(), admin));
        assertTrue(params.hasRole(params.GOD_MODE_ROLE(), admin));
    }

    function test_Constructor_SetsDefaultValues() public view {
        assertEq(params.linkThreshold(), 5100);
        assertEq(params.primaryThreshold(), 6700);
        assertEq(params.duplicateThreshold(), 8000);
        assertEq(params.minStakeWei(), 0.01 ether);
        assertEq(params.primaryStakeMultiplier(), 3);
        assertEq(params.duplicateStakeMultiplier(), 10);
        assertEq(params.linkSlashBps(), 1000);
        assertEq(params.primarySlashBps(), 3000);
        assertEq(params.duplicateSlashBps(), 5000);
        assertEq(params.sybilSlashBps(), 10000);
        assertEq(params.primaryVoteWeight(), 1);
        assertEq(params.oracleVoteWeight(), 100);
        assertTrue(params.godModeActive());
        assertEq(params.timeWarpSeconds(), 0);
    }

    // ============ Time Warp Tests ============

    function test_WarpTime_AddsSeconds() public {
        vm.startPrank(admin);

        uint256 initialTime = block.timestamp;

        vm.expectEmit(true, true, true, true);
        emit TimeWarped(7 days, 7 days);
        params.warpTime(7 days);

        assertEq(params.getCurrentTime(), initialTime + 7 days);
        assertEq(params.timeWarpSeconds(), 7 days);

        vm.stopPrank();
    }

    function test_WarpTime_Cumulative() public {
        vm.startPrank(admin);

        params.warpTime(1 days);
        params.warpTime(2 days);
        params.warpTime(4 days);

        assertEq(params.timeWarpSeconds(), 7 days);

        vm.stopPrank();
    }

    function test_WarpTime_RevertsIfNotGodMode() public {
        vm.prank(unauthorized);
        vm.expectRevert();
        params.warpTime(1 days);
    }

    function test_WarpTime_RevertsIfGodModeDisabled() public {
        vm.startPrank(admin);
        params.renounceGodMode();

        // After renouncing, role is revoked, so AccessControl error comes first
        vm.expectRevert();
        params.warpTime(1 days);

        vm.stopPrank();
    }

    function test_RenounceGodMode_DisablesGodMode() public {
        vm.startPrank(admin);

        assertTrue(params.godModeActive());

        vm.expectEmit(true, true, true, true);
        emit GodModeRenounced(admin);
        params.renounceGodMode();

        assertFalse(params.godModeActive());
        assertEq(params.timeWarpSeconds(), 0);
        assertFalse(params.hasRole(params.GOD_MODE_ROLE(), admin));

        vm.stopPrank();
    }

    function test_RenounceGodMode_ClearsTimeWarp() public {
        vm.startPrank(admin);

        // Warp time first
        params.warpTime(7 days);
        assertEq(params.timeWarpSeconds(), 7 days);

        // Renounce god mode
        params.renounceGodMode();

        // Time warp should be cleared
        assertEq(params.timeWarpSeconds(), 0);
        assertFalse(params.godModeActive());

        vm.stopPrank();
    }

    // ============ Threshold Tests ============

    function test_SetThresholds_UpdatesValues() public {
        vm.startPrank(governance);

        vm.expectEmit(true, true, true, true);
        emit ThresholdsUpdated(6000, 7500, 8500);
        params.setThresholds(6000, 7500, 8500);

        assertEq(params.linkThreshold(), 6000);
        assertEq(params.primaryThreshold(), 7500);
        assertEq(params.duplicateThreshold(), 8500);

        vm.stopPrank();
    }

    function test_SetThresholds_RevertsIfNotGovernance() public {
        vm.prank(unauthorized);
        vm.expectRevert();
        params.setThresholds(6000, 7500, 8500);
    }

    function test_SetThresholds_RevertsIfLinkTooLow() public {
        vm.prank(governance);
        vm.expectRevert("Invalid link threshold");
        params.setThresholds(5000, 6700, 8000);
    }

    function test_SetThresholds_RevertsIfLinkTooHigh() public {
        vm.prank(governance);
        vm.expectRevert("Invalid link threshold");
        params.setThresholds(10001, 6700, 8000);
    }

    function test_SetThresholds_RevertsIfPrimaryTooLow() public {
        vm.prank(governance);
        vm.expectRevert("Invalid primary threshold");
        params.setThresholds(5100, 5000, 8000);
    }

    function test_SetThresholds_RevertsIfDuplicateTooLow() public {
        vm.prank(governance);
        vm.expectRevert("Invalid duplicate threshold");
        params.setThresholds(5100, 6700, 5000);
    }

    // ============ Staking Tests ============

    function test_SetStaking_UpdatesValues() public {
        vm.startPrank(governance);

        vm.expectEmit(true, true, true, true);
        emit StakingUpdated(0.02 ether, 5, 15);
        params.setStaking(0.02 ether, 5, 15);

        assertEq(params.minStakeWei(), 0.02 ether);
        assertEq(params.primaryStakeMultiplier(), 5);
        assertEq(params.duplicateStakeMultiplier(), 15);

        vm.stopPrank();
    }

    function test_SetStaking_RevertsIfNotGovernance() public {
        vm.prank(unauthorized);
        vm.expectRevert();
        params.setStaking(0.02 ether, 5, 15);
    }

    function test_SetStaking_RevertsIfStakeZero() public {
        vm.prank(governance);
        vm.expectRevert("Stake must be > 0");
        params.setStaking(0, 5, 15);
    }

    function test_SetStaking_RevertsIfPrimaryMultTooLow() public {
        vm.prank(governance);
        vm.expectRevert("Primary mult must be >= 1");
        params.setStaking(0.01 ether, 0, 15);
    }

    function test_SetStaking_RevertsIfDuplicateMultTooLow() public {
        vm.prank(governance);
        vm.expectRevert("Duplicate mult must be >= primary");
        params.setStaking(0.01 ether, 10, 5);
    }

    // ============ Slashing Tests ============

    function test_SetSlashing_UpdatesValues() public {
        vm.startPrank(governance);

        vm.expectEmit(true, true, true, true);
        emit SlashingUpdated(2000, 4000, 6000, 10000);
        params.setSlashing(2000, 4000, 6000, 10000);

        assertEq(params.linkSlashBps(), 2000);
        assertEq(params.primarySlashBps(), 4000);
        assertEq(params.duplicateSlashBps(), 6000);
        assertEq(params.sybilSlashBps(), 10000);

        vm.stopPrank();
    }

    function test_SetSlashing_RevertsIfNotGovernance() public {
        vm.prank(unauthorized);
        vm.expectRevert();
        params.setSlashing(2000, 4000, 6000, 10000);
    }

    function test_SetSlashing_RevertsIfLinkTooHigh() public {
        vm.prank(governance);
        vm.expectRevert("Link slash too high");
        params.setSlashing(10001, 3000, 5000, 10000);
    }

    function test_SetSlashing_RevertsIfSybilTooHigh() public {
        vm.prank(governance);
        vm.expectRevert("Sybil slash too high");
        params.setSlashing(1000, 3000, 5000, 10001);
    }

    // ============ Voting Weight Tests ============

    function test_SetVotingWeights_UpdatesValues() public {
        vm.startPrank(governance);

        vm.expectEmit(true, true, true, true);
        emit VotingWeightsUpdated(2, 200);
        params.setVotingWeights(2, 200);

        assertEq(params.primaryVoteWeight(), 2);
        assertEq(params.oracleVoteWeight(), 200);

        vm.stopPrank();
    }

    function test_SetVotingWeights_RevertsIfNotGovernance() public {
        vm.prank(unauthorized);
        vm.expectRevert();
        params.setVotingWeights(2, 200);
    }

    function test_SetVotingWeights_RevertsIfPrimaryZero() public {
        vm.prank(governance);
        vm.expectRevert("Primary weight must be > 0");
        params.setVotingWeights(0, 100);
    }

    function test_SetVotingWeights_RevertsIfOracleLessThanPrimary() public {
        vm.prank(governance);
        vm.expectRevert("Oracle weight must be >= primary");
        params.setVotingWeights(100, 50);
    }

    // ============ Cooldown Tests ============

    function test_SetCooldowns_UpdatesValues() public {
        vm.startPrank(governance);

        vm.expectEmit(true, true, true, true);
        emit CooldownsUpdated(14 days, 60 days, 60 days);
        params.setCooldowns(14 days, 60 days, 60 days);

        assertEq(params.failedClaimCooldown(), 14 days);
        assertEq(params.duplicateFlagCooldown(), 60 days);
        assertEq(params.claimExpiryDuration(), 60 days);

        vm.stopPrank();
    }

    function test_SetCooldowns_RevertsIfNotGovernance() public {
        vm.prank(unauthorized);
        vm.expectRevert();
        params.setCooldowns(14 days, 60 days, 60 days);
    }

    function test_SetCooldowns_RevertsIfFailedClaimZero() public {
        vm.prank(governance);
        vm.expectRevert("Failed claim cooldown must be > 0");
        params.setCooldowns(0, 60 days, 60 days);
    }

    // ============ Decay Rate Tests ============

    function test_SetDecayRates_UpdatesValues() public {
        vm.startPrank(governance);

        vm.expectEmit(true, true, true, true);
        emit DecayRatesUpdated(20, 100);
        params.setDecayRates(20, 100);

        assertEq(params.oracleDecayRateBps(), 20);
        assertEq(params.adminDecayRateBps(), 100);

        vm.stopPrank();
    }

    function test_SetDecayRates_RevertsIfNotGovernance() public {
        vm.prank(unauthorized);
        vm.expectRevert();
        params.setDecayRates(20, 100);
    }

    function test_SetDecayRates_RevertsIfOracleTooHigh() public {
        vm.prank(governance);
        vm.expectRevert("Oracle decay too high");
        params.setDecayRates(10001, 50);
    }

    // ============ View Function Tests ============

    function test_GetRequiredStake_Link() public view {
        assertEq(params.getRequiredStake(0), 0.01 ether);
    }

    function test_GetRequiredStake_Primary() public view {
        assertEq(params.getRequiredStake(1), 0.01 ether * 3);
    }

    function test_GetRequiredStake_Duplicate() public view {
        assertEq(params.getRequiredStake(2), 0.01 ether * 10);
    }

    function test_GetRequiredStake_RevertsOnInvalid() public {
        vm.expectRevert("Invalid claim type");
        params.getRequiredStake(3);
    }

    function test_GetThreshold_Link() public view {
        assertEq(params.getThreshold(0), 5100);
    }

    function test_GetThreshold_Primary() public view {
        assertEq(params.getThreshold(1), 6700);
    }

    function test_GetThreshold_Duplicate() public view {
        assertEq(params.getThreshold(2), 8000);
    }

    function test_GetThreshold_RevertsOnInvalid() public {
        vm.expectRevert("Invalid claim type");
        params.getThreshold(3);
    }

    function test_GetSlashRate_Link() public view {
        assertEq(params.getSlashRate(0), 1000);
    }

    function test_GetSlashRate_Primary() public view {
        assertEq(params.getSlashRate(1), 3000);
    }

    function test_GetSlashRate_Duplicate() public view {
        assertEq(params.getSlashRate(2), 5000);
    }

    function test_GetSlashRate_Sybil() public view {
        assertEq(params.getSlashRate(3), 10000);
    }

    function test_GetSlashRate_RevertsOnInvalid() public {
        vm.expectRevert("Invalid claim type");
        params.getSlashRate(4);
    }

    // ============ Edge Case Tests ============

    function test_GetCurrentTime_WithoutWarp() public view {
        assertEq(params.getCurrentTime(), block.timestamp);
    }

    function test_GetCurrentTime_WithWarp() public {
        vm.prank(admin);
        params.warpTime(100 days);

        assertEq(params.getCurrentTime(), block.timestamp + 100 days);
    }

    function test_MultipleRoleGrants() public {
        vm.startPrank(admin);

        address newGovernance = address(4);
        params.grantRole(params.GOVERNANCE_ROLE(), newGovernance);

        assertTrue(params.hasRole(params.GOVERNANCE_ROLE(), governance));
        assertTrue(params.hasRole(params.GOVERNANCE_ROLE(), newGovernance));

        vm.stopPrank();
    }
}
