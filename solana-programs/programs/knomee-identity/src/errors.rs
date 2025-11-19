use anchor_lang::prelude::*;

#[error_code]
pub enum KnomeeError {
    #[msg("Governance has not been initialized")]
    GovernanceNotInitialized,

    #[msg("God mode is not active")]
    GodModeNotActive,

    #[msg("God mode has already been renounced")]
    GodModeAlreadyRenounced,

    #[msg("Unauthorized: only governance authority can perform this action")]
    UnauthorizedGovernance,

    #[msg("Unauthorized: only god mode can perform this action")]
    UnauthorizedGodMode,

    #[msg("Invalid threshold value (must be between 5100 and 10000 basis points)")]
    InvalidThreshold,

    #[msg("Invalid stake multiplier")]
    InvalidStakeMultiplier,

    #[msg("Invalid slash rate (must be between 0 and 10000 basis points)")]
    InvalidSlashRate,

    #[msg("Identity already initialized")]
    IdentityAlreadyInitialized,

    #[msg("Identity tier cannot be downgraded via this instruction")]
    CannotDowngradeTier,

    #[msg("Only Primary IDs can be upgraded to Oracle")]
    MustBePrimaryToUpgrade,

    #[msg("Address is already an Oracle")]
    AlreadyOracle,

    #[msg("Insufficient voting weight (GreyGhost and LinkedID cannot vote)")]
    InsufficientVotingWeight,

    #[msg("Minimum stake requirement not met")]
    InsufficientStake,

    #[msg("Claim has expired")]
    ClaimExpired,

    #[msg("Claim has already been resolved")]
    ClaimAlreadyResolved,

    #[msg("Claim is not yet ready to resolve (voting period ongoing)")]
    ClaimNotReadyToResolve,

    #[msg("Address is under duplicate challenge")]
    AddressUnderChallenge,

    #[msg("Cooldown period not elapsed since last failed claim")]
    CooldownNotElapsed,

    #[msg("Primary address does not have Primary tier")]
    NotAPrimaryId,

    #[msg("Cannot challenge the same address")]
    CannotChallengeSameAddress,

    #[msg("Already voted on this claim")]
    AlreadyVoted,

    #[msg("Platform name exceeds maximum length")]
    PlatformNameTooLong,

    #[msg("Justification exceeds maximum length")]
    JustificationTooLong,

    #[msg("Evidence exceeds maximum length")]
    EvidenceTooLong,

    #[msg("Invalid claim type")]
    InvalidClaimType,

    #[msg("No rewards available for this claim")]
    NoRewardsAvailable,

    #[msg("Rewards already claimed")]
    RewardsAlreadyClaimed,

    #[msg("Not a voter on this claim")]
    NotAVoter,

    #[msg("Arithmetic overflow")]
    ArithmeticOverflow,

    #[msg("Invalid identity tier")]
    InvalidIdentityTier,

    #[msg("Linked identity already exists for this platform")]
    LinkedIdentityAlreadyExists,

    #[msg("Maximum linked identities reached")]
    MaxLinkedIdentitiesReached,

    #[msg("Subject address mismatch")]
    SubjectAddressMismatch,

    #[msg("Invalid claim status")]
    InvalidClaimStatus,
}
