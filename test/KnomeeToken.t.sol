// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../contracts/identity/KnomeeToken.sol";

contract KnomeeTokenTest is Test {
    KnomeeToken public token;

    address public owner = address(1);
    address public consensus = address(2);
    address public registry = address(3);
    address public alice = address(4);
    address public bob = address(5);
    address public unauthorized = address(6);

    event ConsensusContractUpdated(address indexed oldContract, address indexed newContract);
    event RegistryContractUpdated(address indexed oldContract, address indexed newContract);
    event RewardMinted(address indexed recipient, uint256 amount, string reason);
    event TokensSlashed(address indexed account, uint256 amount, string reason);

    function setUp() public {
        vm.startPrank(owner);
        token = new KnomeeToken();
        token.setConsensusContract(consensus);
        token.setRegistryContract(registry);
        vm.stopPrank();
    }

    // ============ Constructor Tests ============

    function test_Constructor_SetsOwner() public view {
        assertEq(token.owner(), owner);
    }

    function test_Constructor_SetsName() public view {
        assertEq(token.name(), "Knomee Token");
    }

    function test_Constructor_SetsSymbol() public view {
        assertEq(token.symbol(), "KNOW");
    }

    function test_Constructor_SetsLaunchTimestamp() public view {
        assertEq(token.launchTimestamp(), block.timestamp);
    }

    function test_Constructor_DistributesTokens() public view {
        uint256 maxSupply = token.MAX_SUPPLY();
        uint256 protocolRewards = (maxSupply * 40) / 100;
        uint256 remainder = maxSupply - protocolRewards;

        // Check protocol rewards pool
        assertEq(token.balanceOf(address(token)), protocolRewards);

        // Check owner receives remainder (60%)
        assertEq(token.balanceOf(owner), remainder);

        // Total supply should be MAX_SUPPLY
        assertEq(token.totalSupply(), maxSupply);
    }

    function test_Constants() public view {
        assertEq(token.MAX_SUPPLY(), 1_000_000_000 * 10**18);
        assertEq(token.PRIMARY_ID_REWARD(), 100 * 10**18);
        assertEq(token.ORACLE_REWARD_PER_CLAIM(), 10 * 10**18);
        assertEq(token.EARLY_ADOPTER_MULTIPLIER(), 2);
        assertEq(token.EARLY_ADOPTER_PERIOD(), 180 days);
    }

    // ============ Admin Function Tests ============

    function test_SetConsensusContract_UpdatesAddress() public {
        vm.startPrank(owner);

        address newConsensus = address(7);

        vm.expectEmit(true, true, true, true);
        emit ConsensusContractUpdated(consensus, newConsensus);
        token.setConsensusContract(newConsensus);

        assertEq(token.consensusContract(), newConsensus);

        vm.stopPrank();
    }

    function test_SetConsensusContract_RevertsIfNotOwner() public {
        vm.prank(unauthorized);
        vm.expectRevert();
        token.setConsensusContract(address(7));
    }

    function test_SetConsensusContract_RevertsIfZeroAddress() public {
        vm.prank(owner);
        vm.expectRevert("Invalid consensus address");
        token.setConsensusContract(address(0));
    }

    function test_SetRegistryContract_UpdatesAddress() public {
        vm.startPrank(owner);

        address newRegistry = address(8);

        vm.expectEmit(true, true, true, true);
        emit RegistryContractUpdated(registry, newRegistry);
        token.setRegistryContract(newRegistry);

        assertEq(token.registryContract(), newRegistry);

        vm.stopPrank();
    }

    function test_SetRegistryContract_RevertsIfNotOwner() public {
        vm.prank(unauthorized);
        vm.expectRevert();
        token.setRegistryContract(address(8));
    }

    function test_SetRegistryContract_RevertsIfZeroAddress() public {
        vm.prank(owner);
        vm.expectRevert("Invalid registry address");
        token.setRegistryContract(address(0));
    }

    // ============ Primary ID Reward Tests ============

    function test_MintPrimaryIDReward_MintsTokens() public {
        uint256 rewardAmount = token.PRIMARY_ID_REWARD();
        uint256 initialBalance = token.balanceOf(alice);

        vm.prank(registry);
        vm.expectEmit(true, true, true, true);
        emit RewardMinted(alice, rewardAmount, "Primary ID verification");
        token.mintPrimaryIDReward(alice);

        assertEq(token.balanceOf(alice), initialBalance + rewardAmount);
        assertTrue(token.hasClaimedPrimaryReward(alice));
        assertEq(token.totalRewardsMinted(), rewardAmount);
    }

    function test_MintPrimaryIDReward_AppliesEarlyAdopterBonus() public {
        // We're at launch time (in setUp), so early adopter bonus applies
        uint256 expectedReward = token.PRIMARY_ID_REWARD() * token.EARLY_ADOPTER_MULTIPLIER();

        vm.prank(registry);
        token.mintPrimaryIDReward(alice);

        assertEq(token.balanceOf(alice), expectedReward);
    }

    function test_MintPrimaryIDReward_NoEarlyAdopterBonusAfterPeriod() public {
        // Warp past early adopter period
        vm.warp(block.timestamp + token.EARLY_ADOPTER_PERIOD() + 1);

        uint256 expectedReward = token.PRIMARY_ID_REWARD(); // No multiplier

        vm.prank(registry);
        token.mintPrimaryIDReward(alice);

        assertEq(token.balanceOf(alice), expectedReward);
    }

    function test_MintPrimaryIDReward_RevertsIfAlreadyClaimed() public {
        vm.startPrank(registry);

        token.mintPrimaryIDReward(alice);

        vm.expectRevert("Already claimed Primary ID reward");
        token.mintPrimaryIDReward(alice);

        vm.stopPrank();
    }

    function test_MintPrimaryIDReward_RevertsIfNotRegistry() public {
        vm.prank(unauthorized);
        vm.expectRevert("Only registry can call");
        token.mintPrimaryIDReward(alice);
    }

    function test_MintPrimaryIDReward_RevertsIfZeroAddress() public {
        vm.prank(registry);
        vm.expectRevert("Invalid account");
        token.mintPrimaryIDReward(address(0));
    }

    function test_MintPrimaryIDReward_RevertsIfInsufficientPool() public {
        // This test would require draining the rewards pool first
        // In practice, the pool has 400M tokens so it's unlikely to run out
        // But we test the check exists

        vm.prank(owner);
        uint256 poolBalance = token.balanceOf(address(token));
        token.emergencyWithdrawRewards(poolBalance);

        vm.prank(registry);
        vm.expectRevert("Insufficient rewards pool");
        token.mintPrimaryIDReward(alice);
    }

    // ============ Oracle Reward Tests ============

    function test_MintOracleReward_MintsTokens() public {
        uint256 rewardAmount = token.ORACLE_REWARD_PER_CLAIM();

        vm.prank(consensus);
        vm.expectEmit(true, true, true, true);
        emit RewardMinted(alice, rewardAmount, "Oracle claim resolution");
        token.mintOracleReward(alice);

        assertEq(token.balanceOf(alice), rewardAmount);
        assertEq(token.totalRewardsMinted(), rewardAmount);
    }

    function test_MintOracleReward_AppliesEarlyAdopterBonus() public {
        uint256 expectedReward = token.ORACLE_REWARD_PER_CLAIM() * token.EARLY_ADOPTER_MULTIPLIER();

        vm.prank(consensus);
        token.mintOracleReward(alice);

        assertEq(token.balanceOf(alice), expectedReward);
    }

    function test_MintOracleReward_NoEarlyAdopterBonusAfterPeriod() public {
        vm.warp(block.timestamp + token.EARLY_ADOPTER_PERIOD() + 1);

        uint256 expectedReward = token.ORACLE_REWARD_PER_CLAIM();

        vm.prank(consensus);
        token.mintOracleReward(alice);

        assertEq(token.balanceOf(alice), expectedReward);
    }

    function test_MintOracleReward_CanClaimMultipleTimes() public {
        vm.startPrank(consensus);

        token.mintOracleReward(alice);
        uint256 firstReward = token.balanceOf(alice);

        token.mintOracleReward(alice);
        uint256 secondReward = token.balanceOf(alice);

        assertGt(secondReward, firstReward);

        vm.stopPrank();
    }

    function test_MintOracleReward_RevertsIfNotConsensus() public {
        vm.prank(unauthorized);
        vm.expectRevert("Only consensus can call");
        token.mintOracleReward(alice);
    }

    function test_MintOracleReward_RevertsIfZeroAddress() public {
        vm.prank(consensus);
        vm.expectRevert("Invalid oracle");
        token.mintOracleReward(address(0));
    }

    // ============ Voting Reward Tests ============

    function test_MintVotingReward_MintsTokens() public {
        uint256 baseReward = 5 * 10**18; // 5 KNOW

        vm.prank(consensus);
        vm.expectEmit(true, true, true, true);
        emit RewardMinted(alice, baseReward, "Correct vote");
        token.mintVotingReward(alice, baseReward);

        assertEq(token.balanceOf(alice), baseReward);
        assertEq(token.totalRewardsMinted(), baseReward);
    }

    function test_MintVotingReward_AppliesEarlyAdopterBonus() public {
        uint256 baseReward = 5 * 10**18;
        uint256 expectedReward = baseReward * token.EARLY_ADOPTER_MULTIPLIER();

        vm.prank(consensus);
        token.mintVotingReward(alice, baseReward);

        assertEq(token.balanceOf(alice), expectedReward);
    }

    function test_MintVotingReward_NoEarlyAdopterBonusAfterPeriod() public {
        vm.warp(block.timestamp + token.EARLY_ADOPTER_PERIOD() + 1);

        uint256 baseReward = 5 * 10**18;

        vm.prank(consensus);
        token.mintVotingReward(alice, baseReward);

        assertEq(token.balanceOf(alice), baseReward);
    }

    function test_MintVotingReward_RevertsIfNotConsensus() public {
        vm.prank(unauthorized);
        vm.expectRevert("Only consensus can call");
        token.mintVotingReward(alice, 5 * 10**18);
    }

    function test_MintVotingReward_RevertsIfZeroAddress() public {
        vm.prank(consensus);
        vm.expectRevert("Invalid voter");
        token.mintVotingReward(address(0), 5 * 10**18);
    }

    function test_MintVotingReward_RevertsIfZeroReward() public {
        vm.prank(consensus);
        vm.expectRevert("Invalid reward amount");
        token.mintVotingReward(alice, 0);
    }

    // ============ Slashing Tests ============

    function test_Slash_BurnsTokens() public {
        // Give Alice some tokens first
        vm.prank(owner);
        token.transfer(alice, 100 * 10**18);

        uint256 initialBalance = token.balanceOf(alice);
        uint256 slashAmount = 10 * 10**18;

        vm.prank(consensus);
        vm.expectEmit(true, true, true, true);
        emit TokensSlashed(alice, slashAmount, "Incorrect vote");
        token.slash(alice, slashAmount, "Incorrect vote");

        assertEq(token.balanceOf(alice), initialBalance - slashAmount);
        assertEq(token.totalSlashed(), slashAmount);
    }

    function test_Slash_ReducesTotalSupply() public {
        vm.prank(owner);
        token.transfer(alice, 100 * 10**18);

        uint256 initialSupply = token.totalSupply();
        uint256 slashAmount = 10 * 10**18;

        vm.prank(consensus);
        token.slash(alice, slashAmount, "Incorrect vote");

        assertEq(token.totalSupply(), initialSupply - slashAmount);
    }

    function test_Slash_RevertsIfNotConsensus() public {
        vm.prank(owner);
        token.transfer(alice, 100 * 10**18);

        vm.prank(unauthorized);
        vm.expectRevert("Only consensus can call");
        token.slash(alice, 10 * 10**18, "Test");
    }

    function test_Slash_RevertsIfZeroAddress() public {
        vm.prank(consensus);
        vm.expectRevert("Invalid account");
        token.slash(address(0), 10 * 10**18, "Test");
    }

    function test_Slash_RevertsIfZeroAmount() public {
        vm.prank(consensus);
        vm.expectRevert("Invalid slash amount");
        token.slash(alice, 0, "Test");
    }

    function test_Slash_RevertsIfInsufficientBalance() public {
        // Alice has no tokens
        vm.prank(consensus);
        vm.expectRevert("Insufficient balance to slash");
        token.slash(alice, 10 * 10**18, "Test");
    }

    // ============ View Function Tests ============

    function test_CanStake_ReturnsTrueIfSufficientBalance() public {
        vm.prank(owner);
        token.transfer(alice, 100 * 10**18);

        assertTrue(token.canStake(alice, 50 * 10**18));
        assertTrue(token.canStake(alice, 100 * 10**18));
    }

    function test_CanStake_ReturnsFalseIfInsufficientBalance() public {
        vm.prank(owner);
        token.transfer(alice, 100 * 10**18);

        assertFalse(token.canStake(alice, 101 * 10**18));
    }

    function test_RewardsPoolBalance_ReturnsCorrectAmount() public {
        uint256 maxSupply = token.MAX_SUPPLY();
        uint256 expectedPool = (maxSupply * 40) / 100;

        assertEq(token.rewardsPoolBalance(), expectedPool);
    }

    function test_RewardsPoolBalance_DecreasesAfterRewards() public {
        uint256 initialPool = token.rewardsPoolBalance();

        vm.prank(registry);
        token.mintPrimaryIDReward(alice);

        uint256 finalPool = token.rewardsPoolBalance();
        uint256 expectedReward = token.PRIMARY_ID_REWARD() * token.EARLY_ADOPTER_MULTIPLIER();

        assertEq(finalPool, initialPool - expectedReward);
    }

    function test_IsEarlyAdopterPeriod_ReturnsTrueAtLaunch() public view {
        assertTrue(token.isEarlyAdopterPeriod());
    }

    function test_IsEarlyAdopterPeriod_ReturnsFalseAfterPeriod() public {
        vm.warp(block.timestamp + token.EARLY_ADOPTER_PERIOD() + 1);
        assertFalse(token.isEarlyAdopterPeriod());
    }

    function test_CurrentRewardMultiplier_ReturnsTwoDuringEarlyPeriod() public view {
        assertEq(token.currentRewardMultiplier(), 2);
    }

    function test_CurrentRewardMultiplier_ReturnsOneAfterPeriod() public {
        vm.warp(block.timestamp + token.EARLY_ADOPTER_PERIOD() + 1);
        assertEq(token.currentRewardMultiplier(), 1);
    }

    function test_EarlyAdopterTimeRemaining_ReturnsCorrectTime() public {
        uint256 expected = token.EARLY_ADOPTER_PERIOD();
        assertEq(token.earlyAdopterTimeRemaining(), expected);
    }

    function test_EarlyAdopterTimeRemaining_ReturnsZeroAfterPeriod() public {
        vm.warp(block.timestamp + token.EARLY_ADOPTER_PERIOD() + 1);
        assertEq(token.earlyAdopterTimeRemaining(), 0);
    }

    function test_EarlyAdopterTimeRemaining_DecreasesOverTime() public {
        uint256 initialRemaining = token.earlyAdopterTimeRemaining();

        vm.warp(block.timestamp + 30 days);

        uint256 afterRemaining = token.earlyAdopterTimeRemaining();

        assertEq(afterRemaining, initialRemaining - 30 days);
    }

    // ============ Emergency Withdrawal Tests ============

    function test_EmergencyWithdrawRewards_WithdrawsTokens() public {
        uint256 initialPool = token.rewardsPoolBalance();
        uint256 withdrawAmount = 1000 * 10**18;

        vm.prank(owner);
        token.emergencyWithdrawRewards(withdrawAmount);

        assertEq(token.rewardsPoolBalance(), initialPool - withdrawAmount);
        assertEq(token.balanceOf(owner), token.MAX_SUPPLY() * 60 / 100 + withdrawAmount);
    }

    function test_EmergencyWithdrawRewards_RevertsIfNotOwner() public {
        vm.prank(unauthorized);
        vm.expectRevert();
        token.emergencyWithdrawRewards(1000 * 10**18);
    }

    function test_EmergencyWithdrawRewards_RevertsIfInsufficientBalance() public {
        uint256 poolBalance = token.rewardsPoolBalance();

        vm.prank(owner);
        vm.expectRevert("Insufficient pool balance");
        token.emergencyWithdrawRewards(poolBalance + 1);
    }

    // ============ ERC20 Standard Tests ============

    function test_Transfer_WorksNormally() public {
        vm.prank(owner);
        token.transfer(alice, 100 * 10**18);

        assertEq(token.balanceOf(alice), 100 * 10**18);
    }

    function test_Approve_WorksNormally() public {
        vm.prank(alice);
        token.approve(bob, 100 * 10**18);

        assertEq(token.allowance(alice, bob), 100 * 10**18);
    }

    function test_TransferFrom_WorksNormally() public {
        vm.prank(owner);
        token.transfer(alice, 100 * 10**18);

        vm.prank(alice);
        token.approve(bob, 50 * 10**18);

        vm.prank(bob);
        token.transferFrom(alice, bob, 30 * 10**18);

        assertEq(token.balanceOf(bob), 30 * 10**18);
        assertEq(token.balanceOf(alice), 70 * 10**18);
    }

    function test_Burn_WorksNormally() public {
        vm.prank(owner);
        token.transfer(alice, 100 * 10**18);

        uint256 initialSupply = token.totalSupply();

        vm.prank(alice);
        token.burn(50 * 10**18);

        assertEq(token.balanceOf(alice), 50 * 10**18);
        assertEq(token.totalSupply(), initialSupply - 50 * 10**18);
    }

    // ============ Integration Tests ============

    function test_CompleteRewardFlow() public {
        // Alice becomes Primary and gets reward
        vm.prank(registry);
        token.mintPrimaryIDReward(alice);

        uint256 primaryReward = token.PRIMARY_ID_REWARD() * 2; // Early adopter
        assertEq(token.balanceOf(alice), primaryReward);

        // Alice vouches correctly and gets voting reward
        vm.prank(consensus);
        token.mintVotingReward(alice, 5 * 10**18);

        uint256 votingReward = 5 * 10**18 * 2; // Early adopter
        assertEq(token.balanceOf(alice), primaryReward + votingReward);

        // Alice vouches incorrectly and gets slashed
        vm.prank(consensus);
        token.slash(alice, 10 * 10**18, "Wrong vote");

        assertEq(token.balanceOf(alice), primaryReward + votingReward - 10 * 10**18);
    }

    function test_MultipleOracleRewards() public {
        vm.startPrank(consensus);

        // Oracle resolves 3 claims
        token.mintOracleReward(alice);
        token.mintOracleReward(alice);
        token.mintOracleReward(alice);

        uint256 expectedTotal = token.ORACLE_REWARD_PER_CLAIM() * 3 * 2; // Early adopter
        assertEq(token.balanceOf(alice), expectedTotal);

        vm.stopPrank();
    }

    function test_RewardsTransitionAtPeriodBoundary() public {
        // Mint reward during early period
        vm.prank(registry);
        token.mintPrimaryIDReward(alice);

        uint256 earlyReward = token.balanceOf(alice);
        assertEq(earlyReward, token.PRIMARY_ID_REWARD() * 2);

        // Warp to end of early period
        vm.warp(block.timestamp + token.EARLY_ADOPTER_PERIOD());

        // Still in early period (< not <=)
        assertTrue(token.isEarlyAdopterPeriod());

        // Warp past early period
        vm.warp(block.timestamp + 1);
        assertFalse(token.isEarlyAdopterPeriod());

        // Mint reward after early period
        vm.prank(registry);
        token.mintPrimaryIDReward(bob);

        uint256 normalReward = token.balanceOf(bob);
        assertEq(normalReward, token.PRIMARY_ID_REWARD());
    }

    function test_PoolDepletion() public {
        // Track rewards pool depletion
        uint256 initialPool = token.rewardsPoolBalance();

        // Mint many rewards
        vm.startPrank(registry);
        for (uint i = 0; i < 10; i++) {
            token.mintPrimaryIDReward(address(uint160(100 + i)));
        }
        vm.stopPrank();

        uint256 expectedDepletion = token.PRIMARY_ID_REWARD() * 2 * 10; // Early adopter
        assertEq(token.rewardsPoolBalance(), initialPool - expectedDepletion);
    }

    // ============ Edge Case Tests ============

    function test_SlashingAndRewardsTracking() public {
        // Give tokens to alice
        vm.prank(owner);
        token.transfer(alice, 100 * 10**18);

        // Slash alice
        vm.prank(consensus);
        token.slash(alice, 10 * 10**18, "Test");

        assertEq(token.totalSlashed(), 10 * 10**18);

        // Reward bob
        vm.prank(consensus);
        token.mintVotingReward(bob, 5 * 10**18);

        assertEq(token.totalRewardsMinted(), 5 * 10**18 * 2); // Early adopter

        // Totals should be independent
        assertEq(token.totalSlashed(), 10 * 10**18);
        assertEq(token.totalRewardsMinted(), 5 * 10**18 * 2);
    }

    function test_PrimaryRewardOnlyOnce() public {
        vm.prank(registry);
        token.mintPrimaryIDReward(alice);

        assertTrue(token.hasClaimedPrimaryReward(alice));

        // Second claim should fail
        vm.prank(registry);
        vm.expectRevert("Already claimed Primary ID reward");
        token.mintPrimaryIDReward(alice);
    }

    function test_DifferentRewardTypes() public {
        uint256 primaryReward = token.PRIMARY_ID_REWARD() * 2;
        uint256 oracleReward = token.ORACLE_REWARD_PER_CLAIM() * 2;
        uint256 votingReward = 7 * 10**18 * 2;

        vm.prank(registry);
        token.mintPrimaryIDReward(alice);

        vm.prank(consensus);
        token.mintOracleReward(bob);

        vm.prank(consensus);
        token.mintVotingReward(charlie, 7 * 10**18);

        assertEq(token.balanceOf(alice), primaryReward);
        assertEq(token.balanceOf(bob), oracleReward);
        assertEq(token.balanceOf(charlie), votingReward);

        uint256 totalMinted = primaryReward + oracleReward + votingReward;
        assertEq(token.totalRewardsMinted(), totalMinted);
    }
}
