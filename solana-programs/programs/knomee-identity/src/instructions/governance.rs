use anchor_lang::prelude::*;
use crate::{constants::*, errors::KnomeeError, state::*};

// ============================================================
// INITIALIZE GOVERNANCE
// ============================================================

#[derive(Accounts)]
pub struct InitializeGovernance<'info> {
    #[account(
        init,
        payer = authority,
        space = Governance::LEN,
        seeds = [GOVERNANCE_SEED],
        bump
    )]
    pub governance: Account<'info, Governance>,

    #[account(mut)]
    pub authority: Signer<'info>,

    pub system_program: Program<'info, System>,
}

pub fn initialize_governance(
    ctx: Context<InitializeGovernance>,
    params: GovernanceParams,
) -> Result<()> {
    let governance = &mut ctx.accounts.governance;
    let current_time = Clock::get()?.unix_timestamp;

    governance.authority = ctx.accounts.authority.key();
    governance.god_mode_authority = ctx.accounts.authority.key();
    governance.god_mode_active = true;
    governance.time_warp_seconds = 0;
    governance.params = params;
    governance.initialized_at = current_time;
    governance.bump = ctx.bumps.governance;

    msg!("Governance initialized with authority: {}", governance.authority);
    msg!("God mode active: {}", governance.god_mode_active);

    Ok(())
}

// ============================================================
// UPDATE GOVERNANCE PARAMETERS
// ============================================================

#[derive(Accounts)]
pub struct UpdateGovernance<'info> {
    #[account(
        mut,
        seeds = [GOVERNANCE_SEED],
        bump = governance.bump,
        has_one = authority @ KnomeeError::UnauthorizedGovernance
    )]
    pub governance: Account<'info, Governance>,

    pub authority: Signer<'info>,
}

pub fn update_governance_params(
    ctx: Context<UpdateGovernance>,
    params: GovernanceParams,
) -> Result<()> {
    let governance = &mut ctx.accounts.governance;

    // Validate parameters
    require!(
        params.link_threshold >= 5100 && params.link_threshold <= 10000,
        KnomeeError::InvalidThreshold
    );
    require!(
        params.primary_threshold >= 5100 && params.primary_threshold <= 10000,
        KnomeeError::InvalidThreshold
    );
    require!(
        params.duplicate_threshold >= 5100 && params.duplicate_threshold <= 10000,
        KnomeeError::InvalidThreshold
    );

    governance.params = params;

    msg!("Governance parameters updated");

    Ok(())
}

// ============================================================
// TIME WARP (GOD MODE ONLY)
// ============================================================

#[derive(Accounts)]
pub struct TimeWarp<'info> {
    #[account(
        mut,
        seeds = [GOVERNANCE_SEED],
        bump = governance.bump,
        has_one = god_mode_authority @ KnomeeError::UnauthorizedGodMode
    )]
    pub governance: Account<'info, Governance>,

    pub god_mode_authority: Signer<'info>,
}

pub fn time_warp(ctx: Context<TimeWarp>, seconds_forward: i64) -> Result<()> {
    let governance = &mut ctx.accounts.governance;

    require!(
        governance.god_mode_active,
        KnomeeError::GodModeNotActive
    );

    governance.time_warp_seconds = governance
        .time_warp_seconds
        .checked_add(seconds_forward)
        .ok_or(KnomeeError::ArithmeticOverflow)?;

    msg!("Time warped forward by {} seconds", seconds_forward);
    msg!("Total time warp: {} seconds", governance.time_warp_seconds);

    Ok(())
}

// ============================================================
// RENOUNCE GOD MODE (PERMANENT)
// ============================================================

#[derive(Accounts)]
pub struct RenounceGodMode<'info> {
    #[account(
        mut,
        seeds = [GOVERNANCE_SEED],
        bump = governance.bump,
        has_one = god_mode_authority @ KnomeeError::UnauthorizedGodMode
    )]
    pub governance: Account<'info, Governance>,

    pub god_mode_authority: Signer<'info>,
}

pub fn renounce_god_mode(ctx: Context<RenounceGodMode>) -> Result<()> {
    let governance = &mut ctx.accounts.governance;

    require!(
        governance.god_mode_active,
        KnomeeError::GodModeAlreadyRenounced
    );

    governance.god_mode_active = false;

    msg!("God mode has been permanently renounced");
    msg!("Time warp is now disabled");

    Ok(())
}
