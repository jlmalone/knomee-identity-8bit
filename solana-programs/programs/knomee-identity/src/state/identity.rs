use anchor_lang::prelude::*;

#[derive(AnchorSerialize, AnchorDeserialize, Clone, Copy, PartialEq, Eq)]
pub enum IdentityTier {
    GreyGhost,   // 0: Unverified
    LinkedID,    // 1: Secondary identity linked to Primary
    PrimaryID,   // 2: Blue Checkmark (verified unique human, gets UBI)
    Oracle,      // 3: High-weight verifier
}

impl Default for IdentityTier {
    fn default() -> Self {
        IdentityTier::GreyGhost
    }
}

impl IdentityTier {
    pub fn to_u8(&self) -> u8 {
        match self {
            IdentityTier::GreyGhost => 0,
            IdentityTier::LinkedID => 1,
            IdentityTier::PrimaryID => 2,
            IdentityTier::Oracle => 3,
        }
    }

    pub fn from_u8(value: u8) -> Option<Self> {
        match value {
            0 => Some(IdentityTier::GreyGhost),
            1 => Some(IdentityTier::LinkedID),
            2 => Some(IdentityTier::PrimaryID),
            3 => Some(IdentityTier::Oracle),
            _ => None,
        }
    }

    pub fn can_vote(&self) -> bool {
        matches!(self, IdentityTier::PrimaryID | IdentityTier::Oracle)
    }
}

#[account]
#[derive(Default)]
pub struct Identity {
    /// The address this identity belongs to
    pub owner: Pubkey,

    /// Current identity tier
    pub tier: IdentityTier,

    /// If LinkedID, points to the Primary address; otherwise self-referential
    pub primary_address: Pubkey,

    /// When this identity was verified
    pub verified_at: i64,

    /// Total vouches received (across all claims)
    pub total_vouches_received: u64,

    /// Total stake received in vouches
    pub total_stake_received: u64,

    /// Whether currently under duplicate challenge
    pub under_challenge: bool,

    /// Active challenge claim ID (if under_challenge is true)
    pub challenge_claim_id: u64,

    /// When oracle decay started (for Oracle tier)
    pub oracle_decay_start: i64,

    /// Number of linked identities (for Primary IDs)
    pub linked_count: u16,

    /// Timestamp of last failed claim (for cooldown enforcement)
    pub last_failed_claim_at: i64,

    /// Bump seed for PDA
    pub bump: u8,
}

impl Identity {
    pub const LEN: usize = 8 + // discriminator
        32 + // owner
        1 +  // tier (enum stored as u8)
        32 + // primary_address
        8 +  // verified_at
        8 +  // total_vouches_received
        8 +  // total_stake_received
        1 +  // under_challenge
        8 +  // challenge_claim_id
        8 +  // oracle_decay_start
        2 +  // linked_count
        8 +  // last_failed_claim_at
        1;   // bump

    pub fn voting_weight(&self, params: &crate::state::GovernanceParams) -> u64 {
        match self.tier {
            IdentityTier::GreyGhost => 0,
            IdentityTier::LinkedID => 0, // LinkedIDs cannot vote
            IdentityTier::PrimaryID => params.primary_vote_weight,
            IdentityTier::Oracle => params.oracle_vote_weight,
        }
    }

    pub fn is_primary(&self) -> bool {
        matches!(self.tier, IdentityTier::PrimaryID | IdentityTier::Oracle)
    }

    pub fn is_oracle(&self) -> bool {
        matches!(self.tier, IdentityTier::Oracle)
    }
}

/// Linked identity record (PDA derived from primary address + platform)
#[account]
#[derive(Default)]
pub struct LinkedIdentity {
    /// Primary address this is linked to
    pub primary_address: Pubkey,

    /// Linked (secondary) address
    pub linked_address: Pubkey,

    /// Platform name (e.g., "LinkedIn", "Instagram", "GitHub-work")
    pub platform: String,

    /// When this link was established
    pub linked_at: i64,

    /// Bump seed for PDA
    pub bump: u8,
}

impl LinkedIdentity {
    // Space calculation: discriminator + fields + string overhead
    pub fn space(platform_len: usize) -> usize {
        8 +  // discriminator
        32 + // primary_address
        32 + // linked_address
        4 + platform_len + // platform (String with length prefix)
        8 +  // linked_at
        1    // bump
    }
}
