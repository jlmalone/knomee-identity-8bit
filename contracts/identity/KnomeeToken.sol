// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/token/ERC20/extensions/ERC20Burnable.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title KnomeeToken
 * @notice ERC-20 token for staking on identity claims
 * @dev Used for:
 * - Staking on claim votes
 * - Slashing incorrect votes
 * - Rewarding correct votes
 * - Governance participation
 *
 * Cannot be used to buy identity - only for economic alignment
 */
contract KnomeeToken is ERC20, ERC20Burnable, Ownable {

    // ============ Constants ============

    /// @notice Maximum supply: 1 billion KNOW tokens
    uint256 public constant MAX_SUPPLY = 1_000_000_000 * 10**18;

    /// @notice Reward for successful Primary ID verification
    uint256 public constant PRIMARY_ID_REWARD = 100 * 10**18;

    /// @notice Reward for Oracle per resolved claim
    uint256 public constant ORACLE_REWARD_PER_CLAIM = 10 * 10**18;

    /// @notice Early adopter bonus multiplier (first 6 months)
    uint256 public constant EARLY_ADOPTER_MULTIPLIER = 2;

    // ============ State Variables ============

    /// @notice Address of IdentityConsensus contract (can mint rewards)
    address public consensusContract;

    /// @notice Address of IdentityRegistry contract (can mint rewards)
    address public registryContract;

    /// @notice Protocol launch timestamp
    uint256 public immutable launchTimestamp;

    /// @notice Early adopter period duration (6 months)
    uint256 public constant EARLY_ADOPTER_PERIOD = 180 days;

    /// @notice Total tokens minted as rewards
    uint256 public totalRewardsMinted;

    /// @notice Total tokens slashed and burned
    uint256 public totalSlashed;

    /// @notice Mapping of accounts to whether they've claimed Primary ID reward
    mapping(address => bool) public hasClaimedPrimaryReward;

    // ============ Events ============

    event ConsensusContractUpdated(address indexed oldContract, address indexed newContract);
    event RegistryContractUpdated(address indexed oldContract, address indexed newContract);
    event RewardMinted(address indexed recipient, uint256 amount, string reason);
    event TokensSlashed(address indexed account, uint256 amount, string reason);

    // ============ Modifiers ============

    modifier onlyConsensus() {
        require(msg.sender == consensusContract, "Only consensus can call");
        _;
    }

    modifier onlyRegistry() {
        require(msg.sender == registryContract, "Only registry can call");
        _;
    }

    // ============ Constructor ============

    constructor() ERC20("Knomee Token", "KNOW") Ownable(msg.sender) {
        launchTimestamp = block.timestamp;

        // Initial distribution:
        // 40% - Protocol rewards pool (this contract)
        // 30% - Community treasury (owner)
        // 20% - Team (owner for vesting)
        // 10% - Liquidity (owner for DEX)

        uint256 protocolRewards = (MAX_SUPPLY * 40) / 100;
        uint256 remainder = MAX_SUPPLY - protocolRewards;

        // Mint protocol rewards to this contract
        _mint(address(this), protocolRewards);

        // Mint remainder to owner for distribution
        _mint(owner(), remainder);
    }

    // ============ Admin Functions ============

    /**
     * @notice Set IdentityConsensus contract address
     * @param _consensus Address of IdentityConsensus
     */
    function setConsensusContract(address _consensus) external onlyOwner {
        require(_consensus != address(0), "Invalid consensus address");
        address oldContract = consensusContract;
        consensusContract = _consensus;
        emit ConsensusContractUpdated(oldContract, _consensus);
    }

    /**
     * @notice Set IdentityRegistry contract address
     * @param _registry Address of IdentityRegistry
     */
    function setRegistryContract(address _registry) external onlyOwner {
        require(_registry != address(0), "Invalid registry address");
        address oldContract = registryContract;
        registryContract = _registry;
        emit RegistryContractUpdated(oldContract, _registry);
    }

    // ============ Reward Functions ============

    /**
     * @notice Mint Primary ID reward to account
     * @param account Address to reward
     */
    function mintPrimaryIDReward(address account) external onlyRegistry {
        require(!hasClaimedPrimaryReward[account], "Already claimed Primary ID reward");
        require(account != address(0), "Invalid account");

        uint256 reward = PRIMARY_ID_REWARD;

        // Apply early adopter bonus
        if (block.timestamp < launchTimestamp + EARLY_ADOPTER_PERIOD) {
            reward = reward * EARLY_ADOPTER_MULTIPLIER;
        }

        // Transfer from rewards pool
        require(balanceOf(address(this)) >= reward, "Insufficient rewards pool");

        hasClaimedPrimaryReward[account] = true;
        totalRewardsMinted += reward;

        _transfer(address(this), account, reward);
        emit RewardMinted(account, reward, "Primary ID verification");
    }

    /**
     * @notice Mint Oracle reward for resolving claim
     * @param oracle Address of Oracle
     */
    function mintOracleReward(address oracle) external onlyConsensus {
        require(oracle != address(0), "Invalid oracle");

        uint256 reward = ORACLE_REWARD_PER_CLAIM;

        // Apply early adopter bonus
        if (block.timestamp < launchTimestamp + EARLY_ADOPTER_PERIOD) {
            reward = reward * EARLY_ADOPTER_MULTIPLIER;
        }

        // Transfer from rewards pool
        require(balanceOf(address(this)) >= reward, "Insufficient rewards pool");

        totalRewardsMinted += reward;

        _transfer(address(this), oracle, reward);
        emit RewardMinted(oracle, reward, "Oracle claim resolution");
    }

    /**
     * @notice Mint voting reward for correct vouch
     * @param voter Address of voter
     * @param baseReward Base reward amount calculated by consensus
     */
    function mintVotingReward(address voter, uint256 baseReward) external onlyConsensus {
        require(voter != address(0), "Invalid voter");
        require(baseReward > 0, "Invalid reward amount");

        uint256 reward = baseReward;

        // Apply early adopter bonus
        if (block.timestamp < launchTimestamp + EARLY_ADOPTER_PERIOD) {
            reward = reward * EARLY_ADOPTER_MULTIPLIER;
        }

        // Transfer from rewards pool
        require(balanceOf(address(this)) >= reward, "Insufficient rewards pool");

        totalRewardsMinted += reward;

        _transfer(address(this), voter, reward);
        emit RewardMinted(voter, reward, "Correct vote");
    }

    // ============ Slashing Functions ============

    /**
     * @notice Slash tokens from account for incorrect vote
     * @param account Address to slash
     * @param amount Amount to slash
     * @param reason Reason for slashing
     */
    function slash(address account, uint256 amount, string calldata reason) external onlyConsensus {
        require(account != address(0), "Invalid account");
        require(amount > 0, "Invalid slash amount");
        require(balanceOf(account) >= amount, "Insufficient balance to slash");

        totalSlashed += amount;

        // Burn the slashed tokens (remove from circulation)
        _burn(account, amount);

        emit TokensSlashed(account, amount, reason);
    }

    // ============ View Functions ============

    /**
     * @notice Check if account can stake minimum amount
     * @param account Address to check
     * @param minStake Minimum stake required
     * @return True if account has sufficient balance
     */
    function canStake(address account, uint256 minStake) external view returns (bool) {
        return balanceOf(account) >= minStake;
    }

    /**
     * @notice Get remaining rewards pool balance
     * @return Amount of KNOW tokens available for rewards
     */
    function rewardsPoolBalance() external view returns (uint256) {
        return balanceOf(address(this));
    }

    /**
     * @notice Check if early adopter period is active
     * @return True if within first 6 months
     */
    function isEarlyAdopterPeriod() external view returns (bool) {
        return block.timestamp < launchTimestamp + EARLY_ADOPTER_PERIOD;
    }

    /**
     * @notice Get current reward multiplier
     * @return Multiplier (1 or 2)
     */
    function currentRewardMultiplier() external view returns (uint256) {
        if (block.timestamp < launchTimestamp + EARLY_ADOPTER_PERIOD) {
            return EARLY_ADOPTER_MULTIPLIER;
        }
        return 1;
    }

    /**
     * @notice Calculate time remaining in early adopter period
     * @return Seconds remaining (0 if period ended)
     */
    function earlyAdopterTimeRemaining() external view returns (uint256) {
        uint256 endTime = launchTimestamp + EARLY_ADOPTER_PERIOD;
        if (block.timestamp >= endTime) return 0;
        return endTime - block.timestamp;
    }

    // ============ Emergency Functions ============

    /**
     * @notice Emergency withdrawal of rewards pool (only owner)
     * @param amount Amount to withdraw
     */
    function emergencyWithdrawRewards(uint256 amount) external onlyOwner {
        require(amount <= balanceOf(address(this)), "Insufficient pool balance");
        _transfer(address(this), owner(), amount);
    }
}
