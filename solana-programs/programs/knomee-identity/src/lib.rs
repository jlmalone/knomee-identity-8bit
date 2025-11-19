use anchor_lang::prelude::*;

pub mod constants;
pub mod errors;
pub mod instructions;
pub mod state;

use instructions::*;

declare_id!("KNoMeeID11111111111111111111111111111111111");

#[program]
pub mod knomee_identity {
    use super::*;

    // ============================================================
    // GOVERNANCE & INITIALIZATION
    // ============================================================

    /// Initialize the global governance parameters
    pub fn initialize_governance(
        ctx: Context<InitializeGovernance>,
        params: GovernanceParams,
    ) -> Result<()> {
        instructions::governance::initialize_governance(ctx, params)
    }

    /// Update governance parameters (only by governance authority)
    pub fn update_governance_params(
        ctx: Context<UpdateGovernance>,
        params: GovernanceParams,
    ) -> Result<()> {
        instructions::governance::update_governance_params(ctx, params)
    }

    /// Time warp for testing (god mode only)
    pub fn time_warp(ctx: Context<TimeWarp>, seconds_forward: i64) -> Result<()> {
        instructions::governance::time_warp(ctx, seconds_forward)
    }

    /// Renounce god mode permanently
    pub fn renounce_god_mode(ctx: Context<RenounceGodMode>) -> Result<()> {
        instructions::governance::renounce_god_mode(ctx)
    }

    // ============================================================
    // IDENTITY MANAGEMENT
    // ============================================================

    /// Initialize a new identity account for an address
    pub fn initialize_identity(ctx: Context<InitializeIdentity>) -> Result<()> {
        instructions::identity::initialize_identity(ctx)
    }

    /// Upgrade an identity to oracle status (admin/governance only)
    pub fn upgrade_to_oracle(ctx: Context<UpgradeToOracle>) -> Result<()> {
        instructions::identity::upgrade_to_oracle(ctx)
    }

    /// Link a secondary account to a primary identity
    pub fn link_identity(
        ctx: Context<LinkIdentity>,
        platform: String,
    ) -> Result<()> {
        instructions::identity::link_identity(ctx, platform)
    }

    // ============================================================
    // CONSENSUS & CLAIMS
    // ============================================================

    /// Request to link address to an existing Primary ID
    pub fn request_link_to_primary(
        ctx: Context<RequestLinkToPrimary>,
        primary_address: Pubkey,
        platform: String,
        justification: String,
        stake_amount: u64,
    ) -> Result<()> {
        instructions::consensus::request_link_to_primary(
            ctx,
            primary_address,
            platform,
            justification,
            stake_amount,
        )
    }

    /// Request Primary ID verification (Blue Checkmark)
    pub fn request_primary_verification(
        ctx: Context<RequestPrimaryVerification>,
        justification: String,
        stake_amount: u64,
    ) -> Result<()> {
        instructions::consensus::request_primary_verification(ctx, justification, stake_amount)
    }

    /// Challenge two Primary IDs as duplicates (Sybil detection)
    pub fn challenge_duplicate(
        ctx: Context<ChallengeDuplicate>,
        addr1: Pubkey,
        addr2: Pubkey,
        evidence: String,
        stake_amount: u64,
    ) -> Result<()> {
        instructions::consensus::challenge_duplicate(ctx, addr1, addr2, evidence, stake_amount)
    }

    /// Vote FOR a claim
    pub fn vouch_for(ctx: Context<VouchFor>, claim_id: u64, stake_amount: u64) -> Result<()> {
        instructions::consensus::vouch_for(ctx, claim_id, stake_amount)
    }

    /// Vote AGAINST a claim
    pub fn vouch_against(
        ctx: Context<VouchAgainst>,
        claim_id: u64,
        stake_amount: u64,
    ) -> Result<()> {
        instructions::consensus::vouch_against(ctx, claim_id, stake_amount)
    }

    /// Resolve a claim after voting period ends
    pub fn resolve_consensus(ctx: Context<ResolveConsensus>, claim_id: u64) -> Result<()> {
        instructions::consensus::resolve_consensus(ctx, claim_id)
    }

    /// Claim rewards from a resolved claim
    pub fn claim_rewards(ctx: Context<ClaimRewards>, claim_id: u64) -> Result<()> {
        instructions::consensus::claim_rewards(ctx, claim_id)
    }
}
