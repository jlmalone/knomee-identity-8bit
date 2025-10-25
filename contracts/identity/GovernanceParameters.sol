// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/AccessControl.sol";

/**
 * @title GovernanceParameters
 * @notice Configurable protocol parameters for Knomee Identity Consensus
 * @dev All critical protocol values are governance-controlled for flexibility
 *
 * This contract manages:
 * - Consensus thresholds for different claim types
 * - Staking requirements and multipliers
 * - Slashing percentages
 * - Voting weights by identity tier
 * - Cooldown periods
 * - God mode for testing (renounce-able)
 *
 * Design Decision: All parameters stored in basis points (10000 = 100%) for precision
 */
contract GovernanceParameters is AccessControl {
    bytes32 public constant GOVERNANCE_ROLE = keccak256("GOVERNANCE_ROLE");
    bytes32 public constant GOD_MODE_ROLE = keccak256("GOD_MODE_ROLE");

    // ============ Consensus Thresholds (basis points, 10000 = 100%) ============

    /// @notice Threshold for linking a secondary ID to a Primary (default: 51%)
    uint256 public linkThreshold = 5100;

    /// @notice Threshold for claiming new Primary status (default: 67%)
    uint256 public primaryThreshold = 6700;

    /// @notice Threshold for duplicate detection challenges (default: 80%)
    uint256 public duplicateThreshold = 8000;

    // ============ Staking Parameters ============

    /// @notice Minimum stake required to vouch (default: 0.01 ETH)
    uint256 public minStakeWei = 0.01 ether;

    /// @notice Stake multiplier for Primary verification claims (default: 3x)
    uint256 public primaryStakeMultiplier = 3;

    /// @notice Stake multiplier for duplicate challenges (default: 10x)
    uint256 public duplicateStakeMultiplier = 10;

    // ============ Slashing Rates (basis points) ============

    /// @notice Slash percentage for failed Link claims (default: 10%)
    uint256 public linkSlashBps = 1000;

    /// @notice Slash percentage for failed Primary claims (default: 30%)
    uint256 public primarySlashBps = 3000;

    /// @notice Slash percentage for incorrect duplicate challenges (default: 50%)
    uint256 public duplicateSlashBps = 5000;

    /// @notice Slash percentage for confirmed Sybil attackers (default: 100%)
    uint256 public sybilSlashBps = 10000;

    // ============ Voting Weights ============

    /// @notice Base voting weight for Primary IDs (default: 1)
    uint256 public primaryVoteWeight = 1;

    /// @notice Amplified voting weight for Oracles (default: 100)
    uint256 public oracleVoteWeight = 100;

    // ============ Cooldown Periods (seconds) ============

    /// @notice Cooldown after failed claim before retry (default: 7 days)
    uint256 public failedClaimCooldown = 7 days;

    /// @notice Cooldown after duplicate flag before re-flagging (default: 30 days)
    uint256 public duplicateFlagCooldown = 30 days;

    /// @notice Time before active claim expires (default: 30 days)
    uint256 public claimExpiryDuration = 30 days;

    // ============ Oracle Decay Rates (basis points per day) ============

    /// @notice Daily decay rate for oracle weight (default: 0.1%/day)
    uint256 public oracleDecayRateBps = 10;

    /// @notice Daily decay rate for admin oracle weight (default: 0.5%/day)
    uint256 public adminDecayRateBps = 50;

    // ============ God Mode (Testing Only) ============

    /// @notice Whether god mode is currently active
    bool public godModeActive = true;

    /// @notice Seconds to add to block.timestamp for time warp
    uint256 public timeWarpSeconds = 0;

    // ============ Events ============

    event ThresholdsUpdated(uint256 link, uint256 primary, uint256 duplicate);
    event StakingUpdated(uint256 minStake, uint256 primaryMult, uint256 duplicateMult);
    event SlashingUpdated(uint256 link, uint256 primary, uint256 duplicate, uint256 sybil);
    event VotingWeightsUpdated(uint256 primaryWeight, uint256 oracleWeight);
    event CooldownsUpdated(uint256 failedClaim, uint256 duplicateFlag, uint256 claimExpiry);
    event DecayRatesUpdated(uint256 oracleRate, uint256 adminRate);
    event TimeWarped(uint256 secondsAdded, uint256 newTotalWarp);
    event GodModeRenounced(address indexed renouncer);

    /**
     * @notice Constructor - grants all roles to deployer
     * @dev God mode is active by default for testing
     */
    constructor() {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _grantRole(GOVERNANCE_ROLE, msg.sender);
        _grantRole(GOD_MODE_ROLE, msg.sender);
    }

    // ============ Time Functions ============

    /**
     * @notice Get current time with optional time warp applied
     * @return Current block timestamp plus any warped seconds
     */
    function getCurrentTime() public view returns (uint256) {
        return block.timestamp + timeWarpSeconds;
    }

    /**
     * @notice Fast-forward time for testing purposes
     * @param seconds_ Number of seconds to advance
     * @dev Only callable in god mode
     */
    function warpTime(uint256 seconds_) external onlyRole(GOD_MODE_ROLE) {
        require(godModeActive, "God mode disabled");
        timeWarpSeconds += seconds_;
        emit TimeWarped(seconds_, timeWarpSeconds);
    }

    /**
     * @notice Permanently disable god mode
     * @dev This action is irreversible - use with caution
     */
    function renounceGodMode() external onlyRole(GOD_MODE_ROLE) {
        godModeActive = false;
        timeWarpSeconds = 0;
        revokeRole(GOD_MODE_ROLE, msg.sender);
        emit GodModeRenounced(msg.sender);
    }

    // ============ Governance Setters ============

    /**
     * @notice Update consensus thresholds
     * @param link Link to Primary threshold (basis points)
     * @param primary New Primary threshold (basis points)
     * @param duplicate Duplicate detection threshold (basis points)
     * @dev Only callable by governance
     */
    function setThresholds(
        uint256 link,
        uint256 primary,
        uint256 duplicate
    ) external onlyRole(GOVERNANCE_ROLE) {
        require(link >= 5100 && link <= 10000, "Invalid link threshold");
        require(primary >= 5100 && primary <= 10000, "Invalid primary threshold");
        require(duplicate >= 5100 && duplicate <= 10000, "Invalid duplicate threshold");

        linkThreshold = link;
        primaryThreshold = primary;
        duplicateThreshold = duplicate;

        emit ThresholdsUpdated(link, primary, duplicate);
    }

    /**
     * @notice Update staking parameters
     * @param minStake Minimum stake in wei
     * @param primaryMult Primary claim multiplier
     * @param duplicateMult Duplicate challenge multiplier
     * @dev Only callable by governance
     */
    function setStaking(
        uint256 minStake,
        uint256 primaryMult,
        uint256 duplicateMult
    ) external onlyRole(GOVERNANCE_ROLE) {
        require(minStake > 0, "Stake must be > 0");
        require(primaryMult >= 1, "Primary mult must be >= 1");
        require(duplicateMult >= primaryMult, "Duplicate mult must be >= primary");

        minStakeWei = minStake;
        primaryStakeMultiplier = primaryMult;
        duplicateStakeMultiplier = duplicateMult;

        emit StakingUpdated(minStake, primaryMult, duplicateMult);
    }

    /**
     * @notice Update slashing percentages
     * @param link Link claim slash rate (basis points)
     * @param primary Primary claim slash rate (basis points)
     * @param duplicate Duplicate challenge slash rate (basis points)
     * @param sybil Sybil attacker slash rate (basis points)
     * @dev Only callable by governance
     */
    function setSlashing(
        uint256 link,
        uint256 primary,
        uint256 duplicate,
        uint256 sybil
    ) external onlyRole(GOVERNANCE_ROLE) {
        require(link <= 10000, "Link slash too high");
        require(primary <= 10000, "Primary slash too high");
        require(duplicate <= 10000, "Duplicate slash too high");
        require(sybil <= 10000, "Sybil slash too high");

        linkSlashBps = link;
        primarySlashBps = primary;
        duplicateSlashBps = duplicate;
        sybilSlashBps = sybil;

        emit SlashingUpdated(link, primary, duplicate, sybil);
    }

    /**
     * @notice Update voting weights
     * @param primaryWeight Weight for Primary ID votes
     * @param oracleWeight Weight for Oracle votes
     * @dev Only callable by governance
     */
    function setVotingWeights(
        uint256 primaryWeight,
        uint256 oracleWeight
    ) external onlyRole(GOVERNANCE_ROLE) {
        require(primaryWeight > 0, "Primary weight must be > 0");
        require(oracleWeight >= primaryWeight, "Oracle weight must be >= primary");

        primaryVoteWeight = primaryWeight;
        oracleVoteWeight = oracleWeight;

        emit VotingWeightsUpdated(primaryWeight, oracleWeight);
    }

    /**
     * @notice Update cooldown periods
     * @param failedClaim Cooldown after failed claim (seconds)
     * @param duplicateFlag Cooldown after duplicate flag (seconds)
     * @param claimExpiry Time before claim expires (seconds)
     * @dev Only callable by governance
     */
    function setCooldowns(
        uint256 failedClaim,
        uint256 duplicateFlag,
        uint256 claimExpiry
    ) external onlyRole(GOVERNANCE_ROLE) {
        require(failedClaim > 0, "Failed claim cooldown must be > 0");
        require(duplicateFlag > 0, "Duplicate flag cooldown must be > 0");
        require(claimExpiry > 0, "Claim expiry must be > 0");

        failedClaimCooldown = failedClaim;
        duplicateFlagCooldown = duplicateFlag;
        claimExpiryDuration = claimExpiry;

        emit CooldownsUpdated(failedClaim, duplicateFlag, claimExpiry);
    }

    /**
     * @notice Update oracle decay rates
     * @param oracleRate Daily decay for oracle weight (basis points)
     * @param adminRate Daily decay for admin oracle weight (basis points)
     * @dev Only callable by governance
     */
    function setDecayRates(
        uint256 oracleRate,
        uint256 adminRate
    ) external onlyRole(GOVERNANCE_ROLE) {
        require(oracleRate <= 10000, "Oracle decay too high");
        require(adminRate <= 10000, "Admin decay too high");

        oracleDecayRateBps = oracleRate;
        adminDecayRateBps = adminRate;

        emit DecayRatesUpdated(oracleRate, adminRate);
    }

    // ============ View Functions ============

    /**
     * @notice Get required stake for a claim type
     * @param claimType 0=Link, 1=Primary, 2=Duplicate
     * @return Required stake in wei
     */
    function getRequiredStake(uint8 claimType) external view returns (uint256) {
        if (claimType == 0) {
            return minStakeWei; // Link
        } else if (claimType == 1) {
            return minStakeWei * primaryStakeMultiplier; // Primary
        } else if (claimType == 2) {
            return minStakeWei * duplicateStakeMultiplier; // Duplicate
        }
        revert("Invalid claim type");
    }

    /**
     * @notice Get consensus threshold for a claim type
     * @param claimType 0=Link, 1=Primary, 2=Duplicate
     * @return Threshold in basis points
     */
    function getThreshold(uint8 claimType) external view returns (uint256) {
        if (claimType == 0) {
            return linkThreshold;
        } else if (claimType == 1) {
            return primaryThreshold;
        } else if (claimType == 2) {
            return duplicateThreshold;
        }
        revert("Invalid claim type");
    }

    /**
     * @notice Get slash rate for a claim type
     * @param claimType 0=Link, 1=Primary, 2=Duplicate, 3=Sybil
     * @return Slash rate in basis points
     */
    function getSlashRate(uint8 claimType) external view returns (uint256) {
        if (claimType == 0) {
            return linkSlashBps;
        } else if (claimType == 1) {
            return primarySlashBps;
        } else if (claimType == 2) {
            return duplicateSlashBps;
        } else if (claimType == 3) {
            return sybilSlashBps;
        }
        revert("Invalid claim type");
    }
}
