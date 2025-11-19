/// Seed for governance PDA
pub const GOVERNANCE_SEED: &[u8] = b"governance";

/// Seed for identity account PDAs
pub const IDENTITY_SEED: &[u8] = b"identity";

/// Seed for claim account PDAs
pub const CLAIM_SEED: &[u8] = b"claim";

/// Seed for vouch account PDAs
pub const VOUCH_SEED: &[u8] = b"vouch";

/// Seed for linked identity PDAs
pub const LINKED_IDENTITY_SEED: &[u8] = b"linked_identity";

/// Default consensus thresholds (basis points, 10000 = 100%)
pub const DEFAULT_LINK_THRESHOLD: u16 = 5100; // 51%
pub const DEFAULT_PRIMARY_THRESHOLD: u16 = 6700; // 67%
pub const DEFAULT_DUPLICATE_THRESHOLD: u16 = 8000; // 80%

/// Default stake multipliers
pub const DEFAULT_PRIMARY_STAKE_MULTIPLIER: u8 = 3;
pub const DEFAULT_DUPLICATE_STAKE_MULTIPLIER: u8 = 10;

/// Default slashing rates (basis points)
pub const DEFAULT_LINK_SLASH_BPS: u16 = 1000; // 10%
pub const DEFAULT_PRIMARY_SLASH_BPS: u16 = 3000; // 30%
pub const DEFAULT_DUPLICATE_SLASH_BPS: u16 = 5000; // 50%
pub const DEFAULT_SYBIL_SLASH_BPS: u16 = 10000; // 100%

/// Default voting weights
pub const DEFAULT_PRIMARY_VOTE_WEIGHT: u64 = 1;
pub const DEFAULT_ORACLE_VOTE_WEIGHT: u64 = 100;

/// Time periods (in seconds)
pub const SECONDS_PER_DAY: i64 = 86400;
pub const DEFAULT_FAILED_CLAIM_COOLDOWN: i64 = 7 * SECONDS_PER_DAY; // 7 days
pub const DEFAULT_DUPLICATE_FLAG_COOLDOWN: i64 = 30 * SECONDS_PER_DAY; // 30 days
pub const DEFAULT_CLAIM_EXPIRY_DURATION: i64 = 30 * SECONDS_PER_DAY; // 30 days

/// Oracle decay rates (basis points per day)
pub const DEFAULT_ORACLE_DECAY_RATE_BPS: u16 = 10; // 0.1% per day
pub const DEFAULT_ADMIN_DECAY_RATE_BPS: u16 = 50; // 0.5% per day

/// Early adopter incentives
pub const EARLY_ADOPTER_MULTIPLIER: u8 = 2;
pub const EARLY_ADOPTER_PERIOD: i64 = 180 * SECONDS_PER_DAY; // 180 days

/// Basis points denominator
pub const BASIS_POINTS: u16 = 10000;

/// Maximum platform name length
pub const MAX_PLATFORM_NAME_LEN: usize = 32;

/// Maximum justification length
pub const MAX_JUSTIFICATION_LEN: usize = 500;

/// Maximum evidence length
pub const MAX_EVIDENCE_LEN: usize = 1000;

/// Minimum KNOW stake (in lamports, assuming 9 decimals)
pub const DEFAULT_MIN_STAKE_LAMPORTS: u64 = 10_000_000; // 0.01 KNOW (assuming 9 decimals)
