use anchor_lang::prelude::*;
use crate::{constants::*, errors::KnomeeError, state::*};

// ============================================================
// INITIALIZE IDENTITY
// ============================================================

#[derive(Accounts)]
pub struct InitializeIdentity<'info> {
    #[account(
        init,
        payer = payer,
        space = Identity::LEN,
        seeds = [IDENTITY_SEED, owner.key().as_ref()],
        bump
    )]
    pub identity: Account<'info, Identity>,

    /// The address this identity is for
    /// CHECK: Can be any address
    pub owner: UncheckedAccount<'info>,

    #[account(mut)]
    pub payer: Signer<'info>,

    pub system_program: Program<'info, System>,
}

pub fn initialize_identity(ctx: Context<InitializeIdentity>) -> Result<()> {
    let identity = &mut ctx.accounts.identity;
    let current_time = Clock::get()?.unix_timestamp;

    identity.owner = ctx.accounts.owner.key();
    identity.tier = IdentityTier::GreyGhost;
    identity.primary_address = ctx.accounts.owner.key(); // Self-referential for non-linked
    identity.verified_at = 0;
    identity.total_vouches_received = 0;
    identity.total_stake_received = 0;
    identity.under_challenge = false;
    identity.challenge_claim_id = 0;
    identity.oracle_decay_start = 0;
    identity.linked_count = 0;
    identity.last_failed_claim_at = 0;
    identity.bump = ctx.bumps.identity;

    msg!("Identity initialized for: {}", identity.owner);
    msg!("Initial tier: GreyGhost");

    Ok(())
}

// ============================================================
// UPGRADE TO ORACLE
// ============================================================

#[derive(Accounts)]
pub struct UpgradeToOracle<'info> {
    #[account(
        seeds = [GOVERNANCE_SEED],
        bump = governance.bump
    )]
    pub governance: Account<'info, Governance>,

    #[account(
        mut,
        seeds = [IDENTITY_SEED, identity.owner.as_ref()],
        bump = identity.bump
    )]
    pub identity: Account<'info, Identity>,

    #[account(constraint = authority.key() == governance.authority @ KnomeeError::UnauthorizedGovernance)]
    pub authority: Signer<'info>,
}

pub fn upgrade_to_oracle(ctx: Context<UpgradeToOracle>) -> Result<()> {
    let identity = &mut ctx.accounts.identity;
    let governance = &ctx.accounts.governance;
    let current_time = governance.current_time();

    // Must be a Primary ID to upgrade to Oracle
    require!(
        matches!(identity.tier, IdentityTier::PrimaryID),
        KnomeeError::MustBePrimaryToUpgrade
    );

    identity.tier = IdentityTier::Oracle;
    identity.oracle_decay_start = current_time;

    msg!("Identity upgraded to Oracle: {}", identity.owner);
    msg!("Oracle decay starts at: {}", current_time);

    Ok(())
}

// ============================================================
// LINK IDENTITY (AFTER CONSENSUS APPROVAL)
// ============================================================

#[derive(Accounts)]
#[instruction(platform: String)]
pub struct LinkIdentity<'info> {
    #[account(
        seeds = [GOVERNANCE_SEED],
        bump = governance.bump
    )]
    pub governance: Account<'info, Governance>,

    #[account(
        mut,
        seeds = [IDENTITY_SEED, primary_identity.owner.as_ref()],
        bump = primary_identity.bump,
        constraint = primary_identity.is_primary() @ KnomeeError::NotAPrimaryId
    )]
    pub primary_identity: Account<'info, Identity>,

    #[account(
        mut,
        seeds = [IDENTITY_SEED, linked_identity.owner.as_ref()],
        bump = linked_identity.bump
    )]
    pub linked_identity: Account<'info, Identity>,

    #[account(
        init,
        payer = payer,
        space = LinkedIdentity::space(platform.len()),
        seeds = [
            LINKED_IDENTITY_SEED,
            primary_identity.owner.as_ref(),
            platform.as_bytes()
        ],
        bump
    )]
    pub linked_identity_record: Account<'info, LinkedIdentity>,

    #[account(mut)]
    pub payer: Signer<'info>,

    pub system_program: Program<'info, System>,
}

pub fn link_identity(ctx: Context<LinkIdentity>, platform: String) -> Result<()> {
    require!(
        platform.len() <= MAX_PLATFORM_NAME_LEN,
        KnomeeError::PlatformNameTooLong
    );

    let primary_identity = &mut ctx.accounts.primary_identity;
    let linked_identity = &mut ctx.accounts.linked_identity;
    let linked_record = &mut ctx.accounts.linked_identity_record;
    let current_time = ctx.accounts.governance.current_time();

    // Update linked identity
    linked_identity.tier = IdentityTier::LinkedID;
    linked_identity.primary_address = primary_identity.owner;
    linked_identity.verified_at = current_time;

    // Create linked identity record
    linked_record.primary_address = primary_identity.owner;
    linked_record.linked_address = linked_identity.owner;
    linked_record.platform = platform.clone();
    linked_record.linked_at = current_time;
    linked_record.bump = ctx.bumps.linked_identity_record;

    // Increment linked count on primary
    primary_identity.linked_count = primary_identity
        .linked_count
        .checked_add(1)
        .ok_or(KnomeeError::ArithmeticOverflow)?;

    msg!(
        "Linked identity {} to primary {} on platform: {}",
        linked_identity.owner,
        primary_identity.owner,
        platform
    );

    Ok(())
}
