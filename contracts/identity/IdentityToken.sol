// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title IdentityToken
 * @notice Soul-bound NFT representing verified identity
 * @dev Non-transferable token proving unique human verification
 *
 * Key properties:
 * - Cannot be transferred (soul-bound)
 * - One per verified human
 * - Encodes voting weight based on identity tier
 * - Minted only by IdentityRegistry when claim approved
 */
contract IdentityToken is ERC721, Ownable {

    // ============ Enums ============

    enum IdentityTier {
        GreyGhost,    // 0 - Unverified (no token)
        LinkedID,     // 0 - Linked account (no voting power)
        PrimaryID,    // 1 - Verified human
        Oracle        // 100 - Trusted verifier
    }

    // ============ State Variables ============

    /// @notice Mapping from address to identity tier
    mapping(address => IdentityTier) public identityTiers;

    /// @notice Mapping from address to token ID
    mapping(address => uint256) public accountToTokenId;

    /// @notice Mapping from token ID to address
    mapping(uint256 => address) public tokenIdToAccount;

    /// @notice Next token ID to mint
    uint256 private _nextTokenId;

    /// @notice Address of IdentityRegistry contract (only minter)
    address public identityRegistry;

    /// @notice Whether transfers are permanently disabled
    bool public transfersDisabled = true;

    // ============ Events ============

    event IdentityMinted(address indexed account, uint256 indexed tokenId, IdentityTier tier);
    event TierUpgraded(address indexed account, IdentityTier newTier);
    event TierDowngraded(address indexed account, IdentityTier newTier);
    event IdentityRevoked(address indexed account, uint256 indexed tokenId);
    event RegistryUpdated(address indexed oldRegistry, address indexed newRegistry);

    // ============ Modifiers ============

    modifier onlyRegistry() {
        require(msg.sender == identityRegistry, "Only registry can call");
        _;
    }

    // ============ Constructor ============

    constructor() ERC721("Knomee Identity Token", "IDT") Ownable(msg.sender) {
        _nextTokenId = 1; // Start from 1 (0 = no token)
    }

    // ============ Admin Functions ============

    /**
     * @notice Set the IdentityRegistry contract address
     * @param _registry Address of IdentityRegistry
     */
    function setIdentityRegistry(address _registry) external onlyOwner {
        require(_registry != address(0), "Invalid registry address");
        address oldRegistry = identityRegistry;
        identityRegistry = _registry;
        emit RegistryUpdated(oldRegistry, _registry);
    }

    // ============ Minting Functions ============

    /**
     * @notice Mint identity token for verified account
     * @param account Address to mint token for
     * @param tier Identity tier to assign
     */
    function mint(address account, IdentityTier tier) public onlyRegistry returns (uint256) {
        require(account != address(0), "Cannot mint to zero address");
        require(balanceOf(account) == 0, "Account already has identity token");
        require(tier != IdentityTier.GreyGhost, "Cannot mint GreyGhost tier");

        uint256 tokenId = _nextTokenId++;
        _safeMint(account, tokenId);

        identityTiers[account] = tier;
        accountToTokenId[account] = tokenId;
        tokenIdToAccount[tokenId] = account;

        emit IdentityMinted(account, tokenId, tier);
        return tokenId;
    }

    /**
     * @notice Mint identity token for Primary ID
     * @param account Address to mint for
     */
    function mintPrimaryID(address account) external onlyRegistry returns (uint256) {
        return mint(account, IdentityTier.PrimaryID);
    }

    /**
     * @notice Mint identity token for Linked ID
     * @param account Address to mint for
     */
    function mintLinkedID(address account) external onlyRegistry returns (uint256) {
        return mint(account, IdentityTier.LinkedID);
    }

    // ============ Tier Management ============

    /**
     * @notice Upgrade account to Oracle tier
     * @param account Address to upgrade
     */
    function upgradeToOracle(address account) external onlyOwner {
        require(balanceOf(account) > 0, "Account has no identity token");
        require(identityTiers[account] == IdentityTier.PrimaryID, "Must be PrimaryID to upgrade");

        identityTiers[account] = IdentityTier.Oracle;
        emit TierUpgraded(account, IdentityTier.Oracle);
    }

    /**
     * @notice Downgrade Oracle to Primary ID
     * @param account Address to downgrade
     */
    function downgradeFromOracle(address account) external onlyOwner {
        require(balanceOf(account) > 0, "Account has no identity token");
        require(identityTiers[account] == IdentityTier.Oracle, "Not an Oracle");

        identityTiers[account] = IdentityTier.PrimaryID;
        emit TierDowngraded(account, IdentityTier.PrimaryID);
    }

    /**
     * @notice Revoke identity token (for duplicate detection)
     * @param account Address to revoke
     */
    function revoke(address account) external onlyRegistry {
        require(balanceOf(account) > 0, "Account has no identity token");

        uint256 tokenId = accountToTokenId[account];
        _burn(tokenId);

        identityTiers[account] = IdentityTier.GreyGhost;
        delete accountToTokenId[account];
        delete tokenIdToAccount[tokenId];

        emit IdentityRevoked(account, tokenId);
    }

    // ============ View Functions ============

    /**
     * @notice Get voting weight for an account
     * @param account Address to check
     * @return Voting weight (0, 1, or 100)
     */
    function getVotingWeight(address account) external view returns (uint256) {
        if (balanceOf(account) == 0) return 0;

        IdentityTier tier = identityTiers[account];
        if (tier == IdentityTier.Oracle) return 100;
        if (tier == IdentityTier.PrimaryID) return 1;
        return 0; // LinkedID and GreyGhost have no voting power
    }

    /**
     * @notice Check if account has voting rights
     * @param account Address to check
     * @return True if account can vote
     */
    function canVote(address account) external view returns (bool) {
        if (balanceOf(account) == 0) return false;

        IdentityTier tier = identityTiers[account];
        return tier == IdentityTier.PrimaryID || tier == IdentityTier.Oracle;
    }

    /**
     * @notice Get identity tier for an account
     * @param account Address to check
     * @return Identity tier
     */
    function getTier(address account) external view returns (IdentityTier) {
        if (balanceOf(account) == 0) return IdentityTier.GreyGhost;
        return identityTiers[account];
    }

    /**
     * @notice Check if account is an Oracle
     * @param account Address to check
     * @return True if Oracle
     */
    function isOracle(address account) external view returns (bool) {
        return balanceOf(account) > 0 && identityTiers[account] == IdentityTier.Oracle;
    }

    /**
     * @notice Check if account is Primary ID
     * @param account Address to check
     * @return True if Primary ID
     */
    function isPrimary(address account) external view returns (bool) {
        return balanceOf(account) > 0 && identityTiers[account] == IdentityTier.PrimaryID;
    }

    // ============ Soul-Bound: Prevent Transfers ============

    /**
     * @dev Override to prevent transfers (soul-bound)
     */
    function _update(address to, uint256 tokenId, address auth)
        internal
        virtual
        override
        returns (address)
    {
        address from = _ownerOf(tokenId);

        // Allow minting (from == 0) and burning (to == 0)
        if (from != address(0) && to != address(0)) {
            require(!transfersDisabled, "Identity tokens are soul-bound");
        }

        return super._update(to, tokenId, auth);
    }

    /**
     * @dev Override to prevent approvals
     */
    function approve(address, uint256) public virtual override {
        revert("Identity tokens cannot be approved");
    }

    /**
     * @dev Override to prevent approval for all
     */
    function setApprovalForAll(address, bool) public virtual override {
        revert("Identity tokens cannot be approved");
    }

    // ============ Metadata ============

    /**
     * @dev Returns token URI with tier information
     */
    function tokenURI(uint256 tokenId) public view virtual override returns (string memory) {
        _requireOwned(tokenId);

        address account = tokenIdToAccount[tokenId];
        IdentityTier tier = identityTiers[account];

        // In production, this would return proper metadata JSON
        // For now, return tier name
        if (tier == IdentityTier.Oracle) return "Oracle";
        if (tier == IdentityTier.PrimaryID) return "PrimaryID";
        if (tier == IdentityTier.LinkedID) return "LinkedID";
        return "Unknown";
    }
}
