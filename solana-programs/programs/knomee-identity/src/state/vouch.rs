use anchor_lang::prelude::*;

#[account]
pub struct Vouch {
    /// Claim this vouch is for
    pub claim_id: u64,

    /// Address of the voucher
    pub voucher: Pubkey,

    /// Whether supporting (true = FOR, false = AGAINST)
    pub supports: bool,

    /// Voting weight at time of vouch
    pub weight: u64,

    /// Amount of KNOW staked
    pub stake: u64,

    /// When the vouch was cast
    pub vouched_at: i64,

    /// Whether rewards have been claimed
    pub rewards_claimed: bool,

    /// Calculated reward amount (set during resolution)
    pub reward_amount: u64,

    /// Bump seed for PDA
    pub bump: u8,
}

impl Vouch {
    pub const LEN: usize = 8 +  // discriminator
        8 +  // claim_id
        32 + // voucher
        1 +  // supports
        8 +  // weight
        8 +  // stake
        8 +  // vouched_at
        1 +  // rewards_claimed
        8 +  // reward_amount
        1;   // bump

    /// Calculate weighted vote contribution
    pub fn weighted_vote(&self) -> u128 {
        (self.weight as u128)
            .checked_mul(self.stake as u128)
            .unwrap_or(u128::MAX)
    }
}
