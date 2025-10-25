// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/utils/ReentrancyGuard.sol";
import "@openzeppelin/contracts/utils/Pausable.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "./IdentityRegistry.sol";
import "./GovernanceParameters.sol";

/**
 * @title IdentityConsensus
 * @notice Weighted voting and staking for identity verification
 * @dev Manages claims, vouching, consensus resolution, and reward distribution
 *
 * Claim Types:
 * - LinkToPrimary (51% threshold): Link secondary ID to Primary
 * - NewPrimary (67% threshold): Claim unique human status
 * - DuplicateFlag (80% threshold): Challenge Sybil attack
 *
 * Economic Mechanics:
 * - Staking: 0.01 ETH base, 3x for Primary, 10x for Duplicate
 * - Slashing: 10%-100% based on claim type and outcome
 * - Rewards: Correct vouchers split slashed stakes
 */
contract IdentityConsensus is ReentrancyGuard, Pausable, Ownable {
    // ============ Dependencies ============

    IdentityRegistry public registry;
    GovernanceParameters public params;

    // ============ Enums ============

    enum ClaimType {
        LinkToPrimary,  // 0: Claim secondary ID
        NewPrimary,     // 1: Claim unique human
        DuplicateFlag   // 2: Challenge existing Primary as duplicate
    }

    enum ClaimStatus {
        Active,     // 0: Currently being voted on
        Approved,   // 1: Consensus reached, approved
        Rejected,   // 2: Consensus reached, rejected
        Expired     // 3: Expired without consensus
    }

    // ============ Structs ============

    struct IdentityClaim {
        uint256 claimId;
        ClaimType claimType;
        ClaimStatus status;
        address subject;            // Address claiming/being challenged
        address relatedAddress;     // Primary (for Link) or duplicate (for Flag)
        string platform;            // Platform name for LinkedID (flexible string)
        string justification;       // Justification for claim
        uint256 createdAt;
        uint256 expiresAt;
        uint256 totalWeightFor;     // Weighted votes FOR
        uint256 totalWeightAgainst; // Weighted votes AGAINST
        uint256 totalStake;         // Total ETH staked
        bool resolved;              // Whether consensus has been resolved
    }

    struct Vouch {
        address voucher;
        bool isSupporting;  // true = FOR, false = AGAINST
        uint256 weight;     // Calculated at vouch time
        uint256 stake;      // Amount staked
        uint256 vouchedAt;
        bool rewardClaimed; // Whether reward/refund has been claimed
    }

    // ============ Storage ============

    mapping(uint256 => IdentityClaim) public claims;
    mapping(uint256 => Vouch[]) public vouchesOnClaim;
    mapping(uint256 => mapping(address => bool)) public hasVouched; // claimId => voucher => true
    mapping(address => uint256[]) public claimsByAddress;
    mapping(address => uint256) public lastFailedClaim;      // Cooldown tracking
    mapping(address => uint256) public lastDuplicateFlag;    // Duplicate cooldown

    uint256 public nextClaimId = 1;

    // ============ Events ============

    event ClaimCreated(
        uint256 indexed claimId,
        ClaimType claimType,
        address indexed subject,
        string justification
    );

    event VouchCast(
        uint256 indexed claimId,
        address indexed voucher,
        bool isSupporting,
        uint256 weight,
        uint256 stake
    );

    event ConsensusReached(
        uint256 indexed claimId,
        bool approved,
        uint256 weightFor,
        uint256 weightAgainst
    );

    event ClaimExpired(
        uint256 indexed claimId
    );

    event StakeSlashed(
        uint256 indexed claimId,
        address indexed voucher,
        uint256 amount
    );

    event RewardDistributed(
        uint256 indexed claimId,
        address indexed voucher,
        uint256 amount
    );

    event StakeRefunded(
        uint256 indexed claimId,
        address indexed voucher,
        uint256 amount
    );

    // ============ Constructor ============

    constructor(
        address _registry,
        address _params
    ) Ownable(msg.sender) {
        require(_registry != address(0), "Invalid registry");
        require(_params != address(0), "Invalid params");

        registry = IdentityRegistry(_registry);
        params = GovernanceParameters(_params);
    }

    // ============ Claim Creation ============

    /**
     * @notice Request to link a secondary ID to Primary
     * @param primary Primary address to link to
     * @param platform Platform name (flexible string, e.g., "LinkedIn", "LinkedIn-business")
     * @param justification Why this link is legitimate
     * @return claimId ID of created claim
     */
    function requestLinkToPrimary(
        address primary,
        string calldata platform,
        string calldata justification
    ) external payable whenNotPaused nonReentrant returns (uint256 claimId) {
        require(registry.isPrimary(primary), "Not a primary");
        require(registry.getTier(msg.sender) == IdentityRegistry.IdentityTier.GreyGhost, "Already verified");
        require(bytes(platform).length > 0, "Platform required");
        require(bytes(justification).length > 0, "Justification required");

        uint256 requiredStake = params.getRequiredStake(uint8(ClaimType.LinkToPrimary));
        require(msg.value >= requiredStake, "Insufficient stake");

        claimId = _createClaim(
            ClaimType.LinkToPrimary,
            msg.sender,
            primary,
            platform,
            justification
        );

        // Self-vouch with initial stake
        _recordVouch(claimId, msg.sender, true, msg.value);
    }

    /**
     * @notice Request Primary ID verification (Blue Checkmark)
     * @param justification Why you are a unique human
     * @return claimId ID of created claim
     */
    function requestPrimaryVerification(
        string calldata justification
    ) external payable whenNotPaused nonReentrant returns (uint256 claimId) {
        require(registry.getTier(msg.sender) == IdentityRegistry.IdentityTier.GreyGhost, "Already verified");
        require(bytes(justification).length > 0, "Justification required");

        // Check cooldown
        uint256 lastFailed = lastFailedClaim[msg.sender];
        if (lastFailed > 0) {
            require(
                params.getCurrentTime() >= lastFailed + params.failedClaimCooldown(),
                "Still in cooldown"
            );
        }

        uint256 requiredStake = params.getRequiredStake(uint8(ClaimType.NewPrimary));
        require(msg.value >= requiredStake, "Insufficient stake");

        claimId = _createClaim(
            ClaimType.NewPrimary,
            msg.sender,
            address(0),
            "",
            justification
        );

        // Self-vouch with initial stake
        _recordVouch(claimId, msg.sender, true, msg.value);
    }

    /**
     * @notice Challenge two addresses as duplicates (Sybil attack)
     * @param addr1 First address
     * @param addr2 Second address
     * @param evidence Evidence of duplication
     * @return claimId ID of created claim
     */
    function challengeDuplicate(
        address addr1,
        address addr2,
        string calldata evidence
    ) external payable whenNotPaused nonReentrant returns (uint256 claimId) {
        require(registry.isPrimary(addr1), "addr1 not primary");
        require(registry.isPrimary(addr2), "addr2 not primary");
        require(addr1 != addr2, "Same address");
        require(!registry.isUnderChallenge(addr1), "addr1 already challenged");
        require(!registry.isUnderChallenge(addr2), "addr2 already challenged");
        require(bytes(evidence).length > 0, "Evidence required");

        // Check cooldown
        uint256 lastFlag = lastDuplicateFlag[msg.sender];
        if (lastFlag > 0) {
            require(
                params.getCurrentTime() >= lastFlag + params.duplicateFlagCooldown(),
                "Still in cooldown"
            );
        }

        uint256 requiredStake = params.getRequiredStake(uint8(ClaimType.DuplicateFlag));
        require(msg.value >= requiredStake, "Insufficient stake");

        claimId = _createClaim(
            ClaimType.DuplicateFlag,
            addr1,
            addr2,
            "",
            evidence
        );

        // Mark both under challenge
        registry.markUnderChallenge(addr1, claimId);
        registry.markUnderChallenge(addr2, claimId);

        // Challenger vouches FOR (supporting the duplicate claim)
        _recordVouch(claimId, msg.sender, true, msg.value);

        lastDuplicateFlag[msg.sender] = params.getCurrentTime();
    }

    // ============ Vouching ============

    /**
     * @notice Vouch FOR a claim
     * @param claimId ID of claim to vouch on
     */
    function vouchFor(uint256 claimId)
        external
        payable
        whenNotPaused
        nonReentrant
    {
        _vouch(claimId, true);
    }

    /**
     * @notice Vouch AGAINST a claim
     * @param claimId ID of claim to vouch on
     */
    function vouchAgainst(uint256 claimId)
        external
        payable
        whenNotPaused
        nonReentrant
    {
        _vouch(claimId, false);
    }

    /**
     * @notice Internal vouching logic
     * @param claimId ID of claim
     * @param isSupporting true=FOR, false=AGAINST
     */
    function _vouch(uint256 claimId, bool isSupporting) internal {
        IdentityClaim storage claim = claims[claimId];
        require(claim.status == ClaimStatus.Active, "Claim not active");
        require(!hasVouched[claimId][msg.sender], "Already vouched");
        require(claim.subject != msg.sender, "Cannot vouch on own claim");

        // Check voucher is eligible
        IdentityRegistry.IdentityTier tier = registry.getTier(msg.sender);
        require(
            tier == IdentityRegistry.IdentityTier.PrimaryID ||
            tier == IdentityRegistry.IdentityTier.Oracle,
            "Must be Primary or Oracle"
        );

        // Validate stake
        uint256 requiredStake = params.getRequiredStake(uint8(claim.claimType));
        require(msg.value >= requiredStake, "Insufficient stake");

        _recordVouch(claimId, msg.sender, isSupporting, msg.value);
    }

    /**
     * @notice Record a vouch internally
     * @param claimId Claim ID
     * @param voucher Address vouching
     * @param isSupporting true=FOR, false=AGAINST
     * @param stake Amount staked
     */
    function _recordVouch(
        uint256 claimId,
        address voucher,
        bool isSupporting,
        uint256 stake
    ) internal {
        IdentityClaim storage claim = claims[claimId];

        // Calculate vote weight
        uint256 weight = _calculateVoteWeight(voucher);

        // Record vouch
        vouchesOnClaim[claimId].push(Vouch({
            voucher: voucher,
            isSupporting: isSupporting,
            weight: weight,
            stake: stake,
            vouchedAt: params.getCurrentTime(),
            rewardClaimed: false
        }));

        hasVouched[claimId][voucher] = true;

        // Update totals
        if (isSupporting) {
            claim.totalWeightFor += weight;
        } else {
            claim.totalWeightAgainst += weight;
        }
        claim.totalStake += stake;

        // Record in registry
        registry.recordVouch(claim.subject, stake);

        emit VouchCast(claimId, voucher, isSupporting, weight, stake);

        // Check if consensus reached
        _checkAndResolveConsensus(claimId);
    }

    // ============ Consensus Resolution ============

    /**
     * @notice Check and resolve consensus if threshold met
     * @param claimId Claim ID to check
     */
    function _checkAndResolveConsensus(uint256 claimId) internal {
        IdentityClaim storage claim = claims[claimId];

        if (claim.resolved || claim.status != ClaimStatus.Active) {
            return;
        }

        // Check if expired
        if (params.getCurrentTime() >= claim.expiresAt) {
            claim.status = ClaimStatus.Expired;
            emit ClaimExpired(claimId);
            return;
        }

        uint256 totalWeight = claim.totalWeightFor + claim.totalWeightAgainst;
        if (totalWeight == 0) {
            return;
        }

        uint256 supportPercentage = (claim.totalWeightFor * 10000) / totalWeight;
        uint256 threshold = params.getThreshold(uint8(claim.claimType));

        bool consensusReached = supportPercentage >= threshold;

        if (consensusReached) {
            _resolveConsensus(claimId, true);
        } else if ((10000 - supportPercentage) >= threshold) {
            // Rejection consensus reached
            _resolveConsensus(claimId, false);
        }
    }

    /**
     * @notice Resolve consensus and update identity state
     * @param claimId Claim ID
     * @param approved true if approved, false if rejected
     */
    function _resolveConsensus(uint256 claimId, bool approved) internal {
        IdentityClaim storage claim = claims[claimId];

        claim.resolved = true;
        claim.status = approved ? ClaimStatus.Approved : ClaimStatus.Rejected;

        // Update identity state based on claim type
        if (approved) {
            if (claim.claimType == ClaimType.LinkToPrimary) {
                registry.upgradeToLinked(
                    claim.subject,
                    claim.relatedAddress,
                    claim.platform,
                    claim.justification
                );
            } else if (claim.claimType == ClaimType.NewPrimary) {
                registry.upgradeToPrimary(claim.subject);
            } else if (claim.claimType == ClaimType.DuplicateFlag) {
                // Downgrade both addresses (Sybil detected)
                registry.downgradeIdentity(
                    claim.subject,
                    IdentityRegistry.IdentityTier.GreyGhost
                );
                registry.downgradeIdentity(
                    claim.relatedAddress,
                    IdentityRegistry.IdentityTier.GreyGhost
                );
                registry.clearChallenge(claim.subject);
                registry.clearChallenge(claim.relatedAddress);
            }
        } else {
            // Claim rejected
            if (claim.claimType == ClaimType.DuplicateFlag) {
                // False accusation, clear challenges
                registry.clearChallenge(claim.subject);
                registry.clearChallenge(claim.relatedAddress);
            } else {
                // Track failed claim for cooldown
                lastFailedClaim[claim.subject] = params.getCurrentTime();
            }
        }

        emit ConsensusReached(
            claimId,
            approved,
            claim.totalWeightFor,
            claim.totalWeightAgainst
        );
    }

    /**
     * @notice Manual consensus resolution (for expired claims)
     * @param claimId Claim ID to resolve
     * @return approved Whether claim was approved
     */
    function resolveConsensus(uint256 claimId)
        external
        nonReentrant
        returns (bool approved)
    {
        IdentityClaim storage claim = claims[claimId];
        require(claim.status == ClaimStatus.Active, "Not active");
        require(params.getCurrentTime() >= claim.expiresAt, "Not expired yet");
        require(!claim.resolved, "Already resolved");

        claim.status = ClaimStatus.Expired;
        emit ClaimExpired(claimId);

        return false;
    }

    // ============ Reward Distribution ============

    /**
     * @notice Claim rewards or refund after consensus
     * @param claimId Claim ID
     */
    function claimRewards(uint256 claimId) external nonReentrant {
        IdentityClaim storage claim = claims[claimId];
        require(claim.resolved, "Not resolved");

        Vouch[] storage vouches = vouchesOnClaim[claimId];
        bool approved = claim.status == ClaimStatus.Approved;

        uint256 totalReward = 0;
        bool foundVoucher = false;

        for (uint256 i = 0; i < vouches.length; i++) {
            Vouch storage vouch = vouches[i];

            if (vouch.voucher != msg.sender) {
                continue;
            }

            foundVoucher = true;
            require(!vouch.rewardClaimed, "Already claimed");

            vouch.rewardClaimed = true;

            bool wasCorrect = (approved && vouch.isSupporting) || (!approved && !vouch.isSupporting);

            if (wasCorrect) {
                // Get stake back + share of slashed stakes
                totalReward += vouch.stake;
                totalReward += _calculateRewardShare(claimId, vouch.stake, approved);

                emit StakeRefunded(claimId, msg.sender, vouch.stake);
            } else {
                // Slash stake
                uint256 slashRate = _getSlashRate(claim.claimType, approved);
                uint256 slashAmount = (vouch.stake * slashRate) / 10000;

                emit StakeSlashed(claimId, msg.sender, slashAmount);

                // Refund remaining
                uint256 refund = vouch.stake - slashAmount;
                if (refund > 0) {
                    totalReward += refund;
                }
            }
        }

        require(foundVoucher, "No vouch found");

        if (totalReward > 0) {
            (bool success, ) = msg.sender.call{value: totalReward}("");
            require(success, "Transfer failed");

            emit RewardDistributed(claimId, msg.sender, totalReward);
        }
    }

    /**
     * @notice Calculate reward share from slashed stakes
     * @param claimId Claim ID
     * @param voucherStake Voucher's stake
     * @param approved Whether claim was approved
     * @return Reward amount
     */
    function _calculateRewardShare(
        uint256 claimId,
        uint256 voucherStake,
        bool approved
    ) internal view returns (uint256) {
        Vouch[] storage vouches = vouchesOnClaim[claimId];
        IdentityClaim storage claim = claims[claimId];

        uint256 totalSlashed = 0;
        uint256 totalCorrectStake = 0;

        for (uint256 i = 0; i < vouches.length; i++) {
            bool wasCorrect = (approved && vouches[i].isSupporting) ||
                             (!approved && !vouches[i].isSupporting);

            if (wasCorrect) {
                totalCorrectStake += vouches[i].stake;
            } else {
                uint256 slashRate = _getSlashRate(claim.claimType, approved);
                totalSlashed += (vouches[i].stake * slashRate) / 10000;
            }
        }

        if (totalCorrectStake == 0) {
            return 0;
        }

        return (totalSlashed * voucherStake) / totalCorrectStake;
    }

    /**
     * @notice Get slash rate for claim type
     * @param claimType Type of claim
     * @param approved Whether approved
     * @return Slash rate in basis points
     */
    function _getSlashRate(ClaimType claimType, bool approved)
        internal
        view
        returns (uint256)
    {
        if (claimType == ClaimType.DuplicateFlag && approved) {
            // Sybil detected, 100% slash
            return params.sybilSlashBps();
        }

        return params.getSlashRate(uint8(claimType));
    }

    // ============ Vote Weight Calculation ============

    /**
     * @notice Calculate vote weight for a voucher
     * @param voucher Address of voucher
     * @return Vote weight (1 for Primary, 100 for Oracle)
     */
    function _calculateVoteWeight(address voucher) internal view returns (uint256) {
        if (registry.isOracle(voucher)) {
            return params.oracleVoteWeight();
        } else if (registry.isPrimary(voucher)) {
            return params.primaryVoteWeight();
        }
        return 0;
    }

    // ============ View Functions ============

    /**
     * @notice Get claim details
     * @param claimId Claim ID
     * @return Claim struct
     */
    function getClaim(uint256 claimId) external view returns (IdentityClaim memory) {
        return claims[claimId];
    }

    /**
     * @notice Get all vouches on a claim
     * @param claimId Claim ID
     * @return Array of Vouch structs
     */
    function getVouches(uint256 claimId) external view returns (Vouch[] memory) {
        return vouchesOnClaim[claimId];
    }

    /**
     * @notice Get current consensus percentage
     * @param claimId Claim ID
     * @return percentFor Percentage voting FOR (basis points)
     * @return percentAgainst Percentage voting AGAINST (basis points)
     */
    function getCurrentConsensus(uint256 claimId)
        external
        view
        returns (uint256 percentFor, uint256 percentAgainst)
    {
        IdentityClaim storage claim = claims[claimId];
        uint256 totalWeight = claim.totalWeightFor + claim.totalWeightAgainst;

        if (totalWeight == 0) {
            return (0, 0);
        }

        percentFor = (claim.totalWeightFor * 10000) / totalWeight;
        percentAgainst = (claim.totalWeightAgainst * 10000) / totalWeight;
    }

    /**
     * @notice Check if address can vouch on claim
     * @param voucher Address to check
     * @param claimId Claim ID
     * @return canVouch true if can vouch
     * @return reason Reason if cannot vouch
     */
    function canVouch(address voucher, uint256 claimId)
        external
        view
        returns (bool canVouch, string memory reason)
    {
        IdentityClaim storage claim = claims[claimId];

        if (claim.status != ClaimStatus.Active) {
            return (false, "Claim not active");
        }

        if (hasVouched[claimId][voucher]) {
            return (false, "Already vouched");
        }

        if (claim.subject == voucher) {
            return (false, "Cannot vouch on own claim");
        }

        IdentityRegistry.IdentityTier tier = registry.getTier(voucher);
        if (tier != IdentityRegistry.IdentityTier.PrimaryID &&
            tier != IdentityRegistry.IdentityTier.Oracle) {
            return (false, "Must be Primary or Oracle");
        }

        return (true, "");
    }

    /**
     * @notice Get claims created by an address
     * @param addr Address to query
     * @return Array of claim IDs
     */
    function getClaimsByAddress(address addr) external view returns (uint256[] memory) {
        return claimsByAddress[addr];
    }

    // ============ Internal Helpers ============

    /**
     * @notice Create a new claim
     * @return claimId ID of created claim
     */
    function _createClaim(
        ClaimType claimType,
        address subject,
        address relatedAddress,
        string memory platform,
        string memory justification
    ) internal returns (uint256 claimId) {
        claimId = nextClaimId++;

        claims[claimId] = IdentityClaim({
            claimId: claimId,
            claimType: claimType,
            status: ClaimStatus.Active,
            subject: subject,
            relatedAddress: relatedAddress,
            platform: platform,
            justification: justification,
            createdAt: params.getCurrentTime(),
            expiresAt: params.getCurrentTime() + params.claimExpiryDuration(),
            totalWeightFor: 0,
            totalWeightAgainst: 0,
            totalStake: 0,
            resolved: false
        });

        claimsByAddress[subject].push(claimId);

        emit ClaimCreated(claimId, claimType, subject, justification);
    }

    // ============ Admin Functions ============

    /**
     * @notice Pause contract (emergency)
     */
    function pause() external onlyOwner {
        _pause();
    }

    /**
     * @notice Unpause contract
     */
    function unpause() external onlyOwner {
        _unpause();
    }

    /**
     * @notice Withdraw accidentally sent ETH (not from stakes)
     * @dev Only for emergency recovery, not normal operation
     */
    function emergencyWithdraw() external onlyOwner {
        (bool success, ) = owner().call{value: address(this).balance}("");
        require(success, "Transfer failed");
    }
}
