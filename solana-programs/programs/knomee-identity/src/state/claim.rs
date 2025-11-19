use anchor_lang::prelude::*;

#[derive(AnchorSerialize, AnchorDeserialize, Clone, Copy, PartialEq, Eq)]
pub enum ClaimType {
    LinkToPrimary,   // 51% threshold
    NewPrimary,      // 67% threshold
    DuplicateFlag,   // 80% threshold
}

impl ClaimType {
    pub fn to_u8(&self) -> u8 {
        match self {
            ClaimType::LinkToPrimary => 0,
            ClaimType::NewPrimary => 1,
            ClaimType::DuplicateFlag => 2,
        }
    }

    pub fn from_u8(value: u8) -> Option<Self> {
        match value {
            0 => Some(ClaimType::LinkToPrimary),
            1 => Some(ClaimType::NewPrimary),
            2 => Some(ClaimType::DuplicateFlag),
            _ => None,
        }
    }

    pub fn required_threshold(&self, params: &crate::state::GovernanceParams) -> u16 {
        match self {
            ClaimType::LinkToPrimary => params.link_threshold,
            ClaimType::NewPrimary => params.primary_threshold,
            ClaimType::DuplicateFlag => params.duplicate_threshold,
        }
    }

    pub fn slash_rate(&self, params: &crate::state::GovernanceParams, is_sybil: bool) -> u16 {
        if is_sybil && matches!(self, ClaimType::DuplicateFlag) {
            return params.sybil_slash_bps;
        }
        match self {
            ClaimType::LinkToPrimary => params.link_slash_bps,
            ClaimType::NewPrimary => params.primary_slash_bps,
            ClaimType::DuplicateFlag => params.duplicate_slash_bps,
        }
    }

    pub fn required_stake_multiplier(&self, params: &crate::state::GovernanceParams) -> u64 {
        match self {
            ClaimType::LinkToPrimary => 1,
            ClaimType::NewPrimary => params.primary_stake_multiplier as u64,
            ClaimType::DuplicateFlag => params.duplicate_stake_multiplier as u64,
        }
    }

    pub fn cooldown_period(&self, params: &crate::state::GovernanceParams) -> i64 {
        match self {
            ClaimType::LinkToPrimary => params.failed_claim_cooldown,
            ClaimType::NewPrimary => params.failed_claim_cooldown,
            ClaimType::DuplicateFlag => params.duplicate_flag_cooldown,
        }
    }
}

#[derive(AnchorSerialize, AnchorDeserialize, Clone, Copy, PartialEq, Eq)]
pub enum ClaimStatus {
    Active,      // Currently accepting votes
    Approved,    // Consensus reached, claim approved
    Rejected,    // Consensus reached, claim rejected
    Expired,     // Expired without reaching consensus
}

impl ClaimStatus {
    pub fn to_u8(&self) -> u8 {
        match self {
            ClaimStatus::Active => 0,
            ClaimStatus::Approved => 1,
            ClaimStatus::Rejected => 2,
            ClaimStatus::Expired => 3,
        }
    }

    pub fn from_u8(value: u8) -> Option<Self> {
        match value {
            0 => Some(ClaimStatus::Active),
            1 => Some(ClaimStatus::Approved),
            2 => Some(ClaimStatus::Rejected),
            3 => Some(ClaimStatus::Expired),
            _ => None,
        }
    }

    pub fn is_resolved(&self) -> bool {
        !matches!(self, ClaimStatus::Active)
    }
}

#[account]
pub struct IdentityClaim {
    /// Unique claim ID
    pub claim_id: u64,

    /// Type of claim
    pub claim_type: ClaimType,

    /// Current status
    pub status: ClaimStatus,

    /// Address making the claim / being challenged
    pub subject: Pubkey,

    /// Related address (primary for Link, duplicate for Flag, unused for NewPrimary)
    pub related_address: Pubkey,

    /// Platform name (for LinkToPrimary claims)
    pub platform: String,

    /// Justification / evidence
    pub justification: String,

    /// When claim was created
    pub created_at: i64,

    /// When claim expires
    pub expires_at: i64,

    /// Total weighted votes FOR
    pub total_votes_for: u128,

    /// Total weighted votes AGAINST
    pub total_votes_against: u128,

    /// Total KNOW staked on this claim
    pub total_stake: u64,

    /// Total KNOW slashed from incorrect votes
    pub total_slashed: u64,

    /// Number of vouches (for iteration)
    pub vouch_count: u32,

    /// Whether rewards have been distributed
    pub rewards_distributed: bool,

    /// Bump seed for PDA
    pub bump: u8,
}

impl IdentityClaim {
    // Dynamic size based on string lengths
    pub fn space(platform_len: usize, justification_len: usize) -> usize {
        8 +    // discriminator
        8 +    // claim_id
        1 +    // claim_type
        1 +    // status
        32 +   // subject
        32 +   // related_address
        4 + platform_len + // platform (String with length prefix)
        4 + justification_len + // justification
        8 +    // created_at
        8 +    // expires_at
        16 +   // total_votes_for (u128)
        16 +   // total_votes_against (u128)
        8 +    // total_stake
        8 +    // total_slashed
        4 +    // vouch_count
        1 +    // rewards_distributed
        1      // bump
    }

    /// Calculate current consensus percentage (in basis points)
    pub fn consensus_for_bps(&self) -> u16 {
        let total_votes = self.total_votes_for + self.total_votes_against;
        if total_votes == 0 {
            return 0;
        }
        let percentage = (self.total_votes_for * 10000) / total_votes;
        percentage.min(10000) as u16
    }

    /// Check if consensus threshold is met
    pub fn consensus_reached(&self, params: &crate::state::GovernanceParams) -> Option<bool> {
        if !matches!(self.status, ClaimStatus::Active) {
            return None;
        }

        let threshold = self.claim_type.required_threshold(params);
        let consensus_bps = self.consensus_for_bps();

        if consensus_bps >= threshold {
            Some(true) // Approved
        } else if consensus_bps <= (10000 - threshold) {
            Some(false) // Rejected (inverse threshold met)
        } else {
            None // Still undecided
        }
    }
}
