use anchor_lang::prelude::*;
use crate::constants::*;

#[account]
#[derive(Default)]
pub struct Governance {
    /// Authority that can update governance parameters
    pub authority: Pubkey,

    /// God mode authority (for testing only)
    pub god_mode_authority: Pubkey,

    /// Whether god mode is still active
    pub god_mode_active: bool,

    /// Time warp offset for testing (in seconds)
    pub time_warp_seconds: i64,

    /// Governance parameters
    pub params: GovernanceParams,

    /// When governance was initialized
    pub initialized_at: i64,

    /// Bump seed for PDA
    pub bump: u8,
}

impl Governance {
    pub const LEN: usize = 8 + // discriminator
        32 + // authority
        32 + // god_mode_authority
        1 +  // god_mode_active
        8 +  // time_warp_seconds
        GovernanceParams::LEN + // params
        8 +  // initialized_at
        1;   // bump

    /// Get current time accounting for time warp
    pub fn current_time(&self) -> i64 {
        Clock::get().unwrap().unix_timestamp + self.time_warp_seconds
    }
}

#[derive(AnchorSerialize, AnchorDeserialize, Clone, Default)]
pub struct GovernanceParams {
    // Consensus thresholds (basis points, 10000 = 100%)
    pub link_threshold: u16,
    pub primary_threshold: u16,
    pub duplicate_threshold: u16,

    // Minimum stake amount (in KNOW token lamports)
    pub min_stake_lamports: u64,

    // Stake multipliers
    pub primary_stake_multiplier: u8,
    pub duplicate_stake_multiplier: u8,

    // Slashing rates (basis points)
    pub link_slash_bps: u16,
    pub primary_slash_bps: u16,
    pub duplicate_slash_bps: u16,
    pub sybil_slash_bps: u16,

    // Voting weights
    pub primary_vote_weight: u64,
    pub oracle_vote_weight: u64,

    // Cooldowns (in seconds)
    pub failed_claim_cooldown: i64,
    pub duplicate_flag_cooldown: i64,
    pub claim_expiry_duration: i64,

    // Oracle decay rates (basis points per day)
    pub oracle_decay_rate_bps: u16,
    pub admin_decay_rate_bps: u16,
}

impl GovernanceParams {
    pub const LEN: usize =
        2 +  // link_threshold
        2 +  // primary_threshold
        2 +  // duplicate_threshold
        8 +  // min_stake_lamports
        1 +  // primary_stake_multiplier
        1 +  // duplicate_stake_multiplier
        2 +  // link_slash_bps
        2 +  // primary_slash_bps
        2 +  // duplicate_slash_bps
        2 +  // sybil_slash_bps
        8 +  // primary_vote_weight
        8 +  // oracle_vote_weight
        8 +  // failed_claim_cooldown
        8 +  // duplicate_flag_cooldown
        8 +  // claim_expiry_duration
        2 +  // oracle_decay_rate_bps
        2;   // admin_decay_rate_bps

    pub fn default() -> Self {
        Self {
            link_threshold: DEFAULT_LINK_THRESHOLD,
            primary_threshold: DEFAULT_PRIMARY_THRESHOLD,
            duplicate_threshold: DEFAULT_DUPLICATE_THRESHOLD,
            min_stake_lamports: DEFAULT_MIN_STAKE_LAMPORTS,
            primary_stake_multiplier: DEFAULT_PRIMARY_STAKE_MULTIPLIER,
            duplicate_stake_multiplier: DEFAULT_DUPLICATE_STAKE_MULTIPLIER,
            link_slash_bps: DEFAULT_LINK_SLASH_BPS,
            primary_slash_bps: DEFAULT_PRIMARY_SLASH_BPS,
            duplicate_slash_bps: DEFAULT_DUPLICATE_SLASH_BPS,
            sybil_slash_bps: DEFAULT_SYBIL_SLASH_BPS,
            primary_vote_weight: DEFAULT_PRIMARY_VOTE_WEIGHT,
            oracle_vote_weight: DEFAULT_ORACLE_VOTE_WEIGHT,
            failed_claim_cooldown: DEFAULT_FAILED_CLAIM_COOLDOWN,
            duplicate_flag_cooldown: DEFAULT_DUPLICATE_FLAG_COOLDOWN,
            claim_expiry_duration: DEFAULT_CLAIM_EXPIRY_DURATION,
            oracle_decay_rate_bps: DEFAULT_ORACLE_DECAY_RATE_BPS,
            admin_decay_rate_bps: DEFAULT_ADMIN_DECAY_RATE_BPS,
        }
    }
}
