// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/Ownable.sol";
import "./IdentityToken.sol";
import "./KnomeeToken.sol";

/**
 * @title IdentityRegistry
 * @notice State management for Knomee identity system
 * @dev Manages identity tiers, linked IDs, and verification state
 *
 * Identity Tiers:
 * - Grey Ghost (0): Unverified address
 * - Linked ID (1): Secondary identity linked to a Primary
 * - Primary ID (2): Verified unique human (Blue Checkmark, UBI recipient)
 * - Oracle (3): High-weight verifier (100x voting power)
 *
 * Design Decision: Flexible string-based platform linking
 * - No hardcoded enum for platforms
 * - Allows: "LinkedIn", "LinkedIn-business", "TikTok", future platforms
 * - Multiple accounts per platform allowed with consensus approval
 */
contract IdentityRegistry is Ownable {
    // ============ Enums ============

    enum IdentityTier {
        GreyGhost,   // 0: Unverified
        LinkedID,    // 1: Secondary identity
        PrimaryID,   // 2: Blue Checkmark (UBI recipient)
        Oracle       // 3: High-weight verifier
    }

    // ============ Structs ============

    struct Identity {
        IdentityTier tier;
        address primaryAddress;       // If Linked, points to Primary
        uint256 verifiedAt;           // Timestamp of verification
        uint256 totalVouchesReceived; // Cumulative vouches
        uint256 totalStakeReceived;   // Cumulative stake (in wei)
        bool underChallenge;          // Currently being investigated
        uint256 challengeId;          // Active duplicate challenge ID
        uint256 oracleGrantedAt;      // When Oracle status was granted (0 if not Oracle)
    }

    struct LinkedPlatform {
        address linkedAddress;
        string platform;          // e.g. "LinkedIn", "Instagram", "TikTok"
        string justification;     // Why this link is legitimate
        uint256 linkedAt;
    }

    // ============ Storage ============

    /// @notice Main identity registry
    mapping(address => Identity) public identities;

    /// @notice Flexible platform linking: primary => platform name => linked address
    /// @dev Allows multiple accounts per platform (e.g., "LinkedIn", "LinkedIn-business")
    mapping(address => mapping(string => address)) public linkedIds;

    /// @notice Track all linked platforms for a primary
    mapping(address => LinkedPlatform[]) public linkedPlatforms;

    /// @notice Reverse lookup: linked address => primary address
    mapping(address => address) public linkedToPrimary;

    /// @notice Address authorized to modify state (IdentityConsensus contract)
    address public consensusContract;

    /// @notice Identity Token (soul-bound NFT) contract
    IdentityToken public identityToken;

    /// @notice Knomee Token (ERC-20 rewards) contract
    KnomeeToken public knomeeToken;

    // ============ Events ============

    event IdentityVerified(
        address indexed addr,
        IdentityTier tier,
        uint256 timestamp
    );

    event IdentityLinked(
        address indexed secondary,
        address indexed primary,
        string platform,
        string justification
    );

    event IdentityUpgraded(
        address indexed addr,
        IdentityTier from,
        IdentityTier to
    );

    event IdentityDowngraded(
        address indexed addr,
        IdentityTier from,
        IdentityTier to
    );

    event IdentityChallenged(
        address indexed addr,
        uint256 challengeId
    );

    event ChallengeCleared(
        address indexed addr,
        uint256 challengeId
    );

    event ConsensusContractUpdated(
        address indexed oldContract,
        address indexed newContract
    );

    // ============ Modifiers ============

    modifier onlyConsensus() {
        require(msg.sender == consensusContract, "Only consensus contract");
        _;
    }

    // ============ Constructor ============

    constructor() Ownable(msg.sender) {}

    // ============ Admin Functions ============

    /**
     * @notice Set the consensus contract address
     * @param _consensusContract Address of IdentityConsensus contract
     * @dev Only callable by owner during initial setup
     */
    function setConsensusContract(address _consensusContract) external onlyOwner {
        require(_consensusContract != address(0), "Invalid address");
        emit ConsensusContractUpdated(consensusContract, _consensusContract);
        consensusContract = _consensusContract;
    }

    /**
     * @notice Set the Identity Token contract address
     * @param _identityToken Address of IdentityToken contract
     * @dev Only callable by owner during initial setup
     */
    function setIdentityToken(address _identityToken) external onlyOwner {
        require(_identityToken != address(0), "Invalid address");
        identityToken = IdentityToken(_identityToken);
    }

    /**
     * @notice Set the Knomee Token contract address
     * @param _knomeeToken Address of KnomeeToken contract
     * @dev Only callable by owner during initial setup
     */
    function setKnomeeToken(address _knomeeToken) external onlyOwner {
        require(_knomeeToken != address(0), "Invalid address");
        knomeeToken = KnomeeToken(_knomeeToken);
    }

    /**
     * @notice Grant Oracle status (admin-controlled in Phase 1)
     * @param addr Address to grant Oracle status
     * @dev Future: Will be earned through meritocracy in Phase 3
     */
    function upgradeToOracle(address addr) external onlyOwner {
        Identity storage identity = identities[addr];
        require(identity.tier == IdentityTier.PrimaryID, "Must be Primary first");

        IdentityTier oldTier = identity.tier;
        identity.tier = IdentityTier.Oracle;
        identity.oracleGrantedAt = block.timestamp;

        emit IdentityUpgraded(addr, oldTier, IdentityTier.Oracle);
    }

    // ============ Consensus Contract Functions ============

    /**
     * @notice Upgrade address to Linked ID status
     * @param addr Address to upgrade
     * @param primary Primary address this is linked to
     * @param platform Platform name (flexible string)
     * @param justification Why this link is legitimate
     */
    function upgradeToLinked(
        address addr,
        address primary,
        string calldata platform,
        string calldata justification
    ) external onlyConsensus {
        Identity storage identity = identities[addr];
        Identity storage primaryIdentity = identities[primary];

        require(identity.tier == IdentityTier.GreyGhost, "Already verified");
        require(primaryIdentity.tier == IdentityTier.PrimaryID ||
                primaryIdentity.tier == IdentityTier.Oracle, "Invalid primary");
        require(bytes(platform).length > 0, "Platform required");

        // Update identity
        identity.tier = IdentityTier.LinkedID;
        identity.primaryAddress = primary;
        identity.verifiedAt = block.timestamp;

        // Create bidirectional link
        linkedIds[primary][platform] = addr;
        linkedToPrimary[addr] = primary;

        // Track platform
        linkedPlatforms[primary].push(LinkedPlatform({
            linkedAddress: addr,
            platform: platform,
            justification: justification,
            linkedAt: block.timestamp
        }));

        // Mint Identity Token (soul-bound NFT)
        if (address(identityToken) != address(0)) {
            identityToken.mintLinkedID(addr);
        }

        emit IdentityVerified(addr, IdentityTier.LinkedID, block.timestamp);
        emit IdentityLinked(addr, primary, platform, justification);
    }

    /**
     * @notice Upgrade address to Primary ID status
     * @param addr Address to upgrade
     */
    function upgradeToPrimary(address addr) external onlyConsensus {
        Identity storage identity = identities[addr];
        require(identity.tier == IdentityTier.GreyGhost, "Already verified");

        identity.tier = IdentityTier.PrimaryID;
        identity.verifiedAt = block.timestamp;

        // Mint Identity Token (soul-bound NFT)
        if (address(identityToken) != address(0)) {
            identityToken.mintPrimaryID(addr);
        }

        // Mint KNOW token reward for successful Primary verification
        if (address(knomeeToken) != address(0)) {
            knomeeToken.mintPrimaryIDReward(addr);
        }

        emit IdentityVerified(addr, IdentityTier.PrimaryID, block.timestamp);
    }

    /**
     * @notice Downgrade identity (e.g., Sybil detected)
     * @param addr Address to downgrade
     * @param newTier Target tier
     */
    function downgradeIdentity(address addr, IdentityTier newTier) external onlyConsensus {
        Identity storage identity = identities[addr];
        IdentityTier oldTier = identity.tier;

        require(uint8(newTier) < uint8(oldTier), "Not a downgrade");

        identity.tier = newTier;

        // If downgrading from Primary, clear any linked IDs
        if (oldTier >= IdentityTier.PrimaryID && newTier < IdentityTier.PrimaryID) {
            _clearLinkedIds(addr);
        }

        // If downgrading to GreyGhost, revoke identity token
        if (newTier == IdentityTier.GreyGhost && address(identityToken) != address(0)) {
            // Check if account has a token before trying to revoke
            if (identityToken.balanceOf(addr) > 0) {
                identityToken.revoke(addr);
            }
        }

        emit IdentityDowngraded(addr, oldTier, newTier);
    }

    /**
     * @notice Mark identity as under challenge
     * @param addr Address being challenged
     * @param challengeId ID of the challenge
     */
    function markUnderChallenge(address addr, uint256 challengeId) external onlyConsensus {
        Identity storage identity = identities[addr];
        identity.underChallenge = true;
        identity.challengeId = challengeId;

        emit IdentityChallenged(addr, challengeId);
    }

    /**
     * @notice Clear challenge status
     * @param addr Address to clear
     */
    function clearChallenge(address addr) external onlyConsensus {
        Identity storage identity = identities[addr];
        uint256 clearedChallengeId = identity.challengeId;

        identity.underChallenge = false;
        identity.challengeId = 0;

        emit ChallengeCleared(addr, clearedChallengeId);
    }

    /**
     * @notice Record vouch received
     * @param addr Address receiving vouch
     * @param stake Amount of stake in vouch
     */
    function recordVouch(address addr, uint256 stake) external onlyConsensus {
        Identity storage identity = identities[addr];
        identity.totalVouchesReceived++;
        identity.totalStakeReceived += stake;
    }

    // ============ View Functions ============

    /**
     * @notice Get complete identity data
     * @param addr Address to query
     * @return Identity struct
     */
    function getIdentity(address addr) external view returns (Identity memory) {
        return identities[addr];
    }

    /**
     * @notice Get identity tier
     * @param addr Address to query
     * @return IdentityTier enum value
     */
    function getTier(address addr) external view returns (IdentityTier) {
        return identities[addr].tier;
    }

    /**
     * @notice Check if address is a Primary ID
     * @param addr Address to check
     * @return true if Primary or Oracle
     */
    function isPrimary(address addr) external view returns (bool) {
        IdentityTier tier = identities[addr].tier;
        return tier == IdentityTier.PrimaryID || tier == IdentityTier.Oracle;
    }

    /**
     * @notice Check if address is an Oracle
     * @param addr Address to check
     * @return true if Oracle
     */
    function isOracle(address addr) external view returns (bool) {
        return identities[addr].tier == IdentityTier.Oracle;
    }

    /**
     * @notice Get all linked platforms for a Primary
     * @param primary Primary address
     * @return Array of LinkedPlatform structs
     */
    function getLinkedPlatforms(address primary) external view returns (LinkedPlatform[] memory) {
        return linkedPlatforms[primary];
    }

    /**
     * @notice Get linked address for a specific platform
     * @param primary Primary address
     * @param platform Platform name
     * @return Linked address (0x0 if not linked)
     */
    function getLinkedAddress(address primary, string calldata platform)
        external
        view
        returns (address)
    {
        return linkedIds[primary][platform];
    }

    /**
     * @notice Get primary address for a linked ID
     * @param linked Linked address
     * @return Primary address (0x0 if not linked)
     */
    function getPrimaryAddress(address linked) external view returns (address) {
        return linkedToPrimary[linked];
    }

    /**
     * @notice Check if address is under challenge
     * @param addr Address to check
     * @return true if currently challenged
     */
    function isUnderChallenge(address addr) external view returns (bool) {
        return identities[addr].underChallenge;
    }

    /**
     * @notice Get active challenge ID for an address
     * @param addr Address to check
     * @return Challenge ID (0 if no active challenge)
     */
    function getChallengeId(address addr) external view returns (uint256) {
        return identities[addr].challengeId;
    }

    /**
     * @notice Get total linked platforms count
     * @param primary Primary address
     * @return Number of linked platforms
     */
    function getLinkedCount(address primary) external view returns (uint256) {
        return linkedPlatforms[primary].length;
    }

    // ============ Internal Functions ============

    /**
     * @notice Clear all linked IDs when downgrading Primary
     * @param primary Primary address losing status
     * @dev Called during downgrade to maintain consistency
     */
    function _clearLinkedIds(address primary) internal {
        LinkedPlatform[] storage platforms = linkedPlatforms[primary];

        // Clear reverse mappings
        for (uint256 i = 0; i < platforms.length; i++) {
            address linkedAddr = platforms[i].linkedAddress;
            delete linkedToPrimary[linkedAddr];
            delete linkedIds[primary][platforms[i].platform];

            // Downgrade linked IDs to Grey Ghost
            identities[linkedAddr].tier = IdentityTier.GreyGhost;
            identities[linkedAddr].primaryAddress = address(0);
        }

        // Clear platform array
        delete linkedPlatforms[primary];
    }
}
