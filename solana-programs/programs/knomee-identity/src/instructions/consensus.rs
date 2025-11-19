use anchor_lang::prelude::*;
use anchor_spl::token::{self, Token, TokenAccount, Transfer};
use crate::{constants::*, errors::KnomeeError, state::*};

// ============================================================
// REQUEST LINK TO PRIMARY
// ============================================================

#[derive(Accounts)]
#[instruction(
    primary_address: Pubkey,
    platform: String,
    justification: String,
    stake_amount: u64
)]
pub struct RequestLinkToPrimary<'info> {
    #[account(
        seeds = [GOVERNANCE_SEED],
        bump = governance.bump
    )]
    pub governance: Account<'info, Governance>,

    #[account(
        init,
        payer = subject,
        space = IdentityClaim::space(platform.len(), justification.len()),
        seeds = [CLAIM_SEED, &governance.initialized_at.to_le_bytes(), &next_claim_id().to_le_bytes()],
        bump
    )]
    pub claim: Account<'info, IdentityClaim>,

    #[account(
        mut,
        seeds = [IDENTITY_SEED, subject.key().as_ref()],
        bump = subject_identity.bump
    )]
    pub subject_identity: Account<'info, Identity>,

    /// Primary identity to link to
    /// CHECK: Validated in handler
    pub primary: UncheckedAccount<'info>,

    #[account(mut)]
    pub subject: Signer<'info>,

    /// KNOW token account of subject
    #[account(mut)]
    pub subject_token_account: Account<'info, TokenAccount>,

    /// Escrow account to hold staked KNOW
    #[account(mut)]
    pub stake_escrow: Account<'info, TokenAccount>,

    pub token_program: Program<'info, Token>,
    pub system_program: Program<'info, System>,
}

pub fn request_link_to_primary(
    ctx: Context<RequestLinkToPrimary>,
    primary_address: Pubkey,
    platform: String,
    justification: String,
    stake_amount: u64,
) -> Result<()> {
    require!(
        platform.len() <= MAX_PLATFORM_NAME_LEN,
        KnomeeError::PlatformNameTooLong
    );
    require!(
        justification.len() <= MAX_JUSTIFICATION_LEN,
        KnomeeError::JustificationTooLong
    );

    let governance = &ctx.accounts.governance;
    let claim = &mut ctx.accounts.claim;
    let subject_identity = &mut ctx.accounts.subject_identity;
    let current_time = governance.current_time();

    // Validate stake amount
    let min_stake = governance.params.min_stake_lamports;
    require!(stake_amount >= min_stake, KnomeeError::InsufficientStake);

    // Check cooldown
    if subject_identity.last_failed_claim_at > 0 {
        let cooldown = ClaimType::LinkToPrimary.cooldown_period(&governance.params);
        require!(
            current_time >= subject_identity.last_failed_claim_at + cooldown,
            KnomeeError::CooldownNotElapsed
        );
    }

    // Check if under challenge
    require!(
        !subject_identity.under_challenge,
        KnomeeError::AddressUnderChallenge
    );

    // Transfer stake to escrow
    let transfer_ctx = CpiContext::new(
        ctx.accounts.token_program.to_account_info(),
        Transfer {
            from: ctx.accounts.subject_token_account.to_account_info(),
            to: ctx.accounts.stake_escrow.to_account_info(),
            authority: ctx.accounts.subject.to_account_info(),
        },
    );
    token::transfer(transfer_ctx, stake_amount)?;

    // Initialize claim
    let claim_id = next_claim_id();
    claim.claim_id = claim_id;
    claim.claim_type = ClaimType::LinkToPrimary;
    claim.status = ClaimStatus::Active;
    claim.subject = ctx.accounts.subject.key();
    claim.related_address = primary_address;
    claim.platform = platform;
    claim.justification = justification;
    claim.created_at = current_time;
    claim.expires_at = current_time + governance.params.claim_expiry_duration;
    claim.total_votes_for = 0;
    claim.total_votes_against = 0;
    claim.total_stake = stake_amount;
    claim.total_slashed = 0;
    claim.vouch_count = 0;
    claim.rewards_distributed = false;
    claim.bump = ctx.bumps.claim;

    msg!("LinkToPrimary claim created: {}", claim_id);
    msg!("Subject: {}", claim.subject);
    msg!("Primary: {}", primary_address);
    msg!("Platform: {}", claim.platform);

    Ok(())
}

// Helper to generate next claim ID (would typically use a counter account)
fn next_claim_id() -> u64 {
    Clock::get().unwrap().unix_timestamp as u64
}

// ============================================================
// REQUEST PRIMARY VERIFICATION
// ============================================================

#[derive(Accounts)]
#[instruction(justification: String, stake_amount: u64)]
pub struct RequestPrimaryVerification<'info> {
    #[account(
        seeds = [GOVERNANCE_SEED],
        bump = governance.bump
    )]
    pub governance: Account<'info, Governance>,

    #[account(
        init,
        payer = subject,
        space = IdentityClaim::space(0, justification.len()),
        seeds = [CLAIM_SEED, &governance.initialized_at.to_le_bytes(), &next_claim_id().to_le_bytes()],
        bump
    )]
    pub claim: Account<'info, IdentityClaim>,

    #[account(
        mut,
        seeds = [IDENTITY_SEED, subject.key().as_ref()],
        bump = subject_identity.bump
    )]
    pub subject_identity: Account<'info, Identity>,

    #[account(mut)]
    pub subject: Signer<'info>,

    #[account(mut)]
    pub subject_token_account: Account<'info, TokenAccount>,

    #[account(mut)]
    pub stake_escrow: Account<'info, TokenAccount>,

    pub token_program: Program<'info, Token>,
    pub system_program: Program<'info, System>,
}

pub fn request_primary_verification(
    ctx: Context<RequestPrimaryVerification>,
    justification: String,
    stake_amount: u64,
) -> Result<()> {
    require!(
        justification.len() <= MAX_JUSTIFICATION_LEN,
        KnomeeError::JustificationTooLong
    );

    let governance = &ctx.accounts.governance;
    let claim = &mut ctx.accounts.claim;
    let subject_identity = &mut ctx.accounts.subject_identity;
    let current_time = governance.current_time();

    // Validate stake (must be 3x minimum for Primary claims)
    let min_stake = governance.params.min_stake_lamports
        * governance.params.primary_stake_multiplier as u64;
    require!(stake_amount >= min_stake, KnomeeError::InsufficientStake);

    // Check cooldown
    if subject_identity.last_failed_claim_at > 0 {
        let cooldown = ClaimType::NewPrimary.cooldown_period(&governance.params);
        require!(
            current_time >= subject_identity.last_failed_claim_at + cooldown,
            KnomeeError::CooldownNotElapsed
        );
    }

    require!(
        !subject_identity.under_challenge,
        KnomeeError::AddressUnderChallenge
    );

    // Transfer stake
    let transfer_ctx = CpiContext::new(
        ctx.accounts.token_program.to_account_info(),
        Transfer {
            from: ctx.accounts.subject_token_account.to_account_info(),
            to: ctx.accounts.stake_escrow.to_account_info(),
            authority: ctx.accounts.subject.to_account_info(),
        },
    );
    token::transfer(transfer_ctx, stake_amount)?;

    // Initialize claim
    let claim_id = next_claim_id();
    claim.claim_id = claim_id;
    claim.claim_type = ClaimType::NewPrimary;
    claim.status = ClaimStatus::Active;
    claim.subject = ctx.accounts.subject.key();
    claim.related_address = Pubkey::default();
    claim.platform = String::new();
    claim.justification = justification;
    claim.created_at = current_time;
    claim.expires_at = current_time + governance.params.claim_expiry_duration;
    claim.total_votes_for = 0;
    claim.total_votes_against = 0;
    claim.total_stake = stake_amount;
    claim.total_slashed = 0;
    claim.vouch_count = 0;
    claim.rewards_distributed = false;
    claim.bump = ctx.bumps.claim;

    msg!("NewPrimary claim created: {}", claim_id);
    msg!("Subject: {}", claim.subject);

    Ok(())
}

// ============================================================
// CHALLENGE DUPLICATE
// ============================================================

#[derive(Accounts)]
#[instruction(addr1: Pubkey, addr2: Pubkey, evidence: String, stake_amount: u64)]
pub struct ChallengeDuplicate<'info> {
    #[account(
        seeds = [GOVERNANCE_SEED],
        bump = governance.bump
    )]
    pub governance: Account<'info, Governance>,

    #[account(
        init,
        payer = challenger,
        space = IdentityClaim::space(0, evidence.len()),
        seeds = [CLAIM_SEED, &governance.initialized_at.to_le_bytes(), &next_claim_id().to_le_bytes()],
        bump
    )]
    pub claim: Account<'info, IdentityClaim>,

    #[account(
        mut,
        seeds = [IDENTITY_SEED, addr1.as_ref()],
        bump = identity1.bump
    )]
    pub identity1: Account<'info, Identity>,

    #[account(
        mut,
        seeds = [IDENTITY_SEED, addr2.as_ref()],
        bump = identity2.bump
    )]
    pub identity2: Account<'info, Identity>,

    #[account(mut)]
    pub challenger: Signer<'info>,

    #[account(mut)]
    pub challenger_token_account: Account<'info, TokenAccount>,

    #[account(mut)]
    pub stake_escrow: Account<'info, TokenAccount>,

    pub token_program: Program<'info, Token>,
    pub system_program: Program<'info, System>,
}

pub fn challenge_duplicate(
    ctx: Context<ChallengeDuplicate>,
    addr1: Pubkey,
    addr2: Pubkey,
    evidence: String,
    stake_amount: u64,
) -> Result<()> {
    require!(
        evidence.len() <= MAX_EVIDENCE_LEN,
        KnomeeError::EvidenceTooLong
    );
    require!(
        addr1 != addr2,
        KnomeeError::CannotChallengeSameAddress
    );

    let governance = &ctx.accounts.governance;
    let claim = &mut ctx.accounts.claim;
    let identity1 = &mut ctx.accounts.identity1;
    let identity2 = &mut ctx.accounts.identity2;
    let current_time = governance.current_time();

    // Both must be Primary IDs
    require!(identity1.is_primary(), KnomeeError::NotAPrimaryId);
    require!(identity2.is_primary(), KnomeeError::NotAPrimaryId);

    // Validate stake (10x minimum for duplicate challenges)
    let min_stake = governance.params.min_stake_lamports
        * governance.params.duplicate_stake_multiplier as u64;
    require!(stake_amount >= min_stake, KnomeeError::InsufficientStake);

    // Transfer stake
    let transfer_ctx = CpiContext::new(
        ctx.accounts.token_program.to_account_info(),
        Transfer {
            from: ctx.accounts.challenger_token_account.to_account_info(),
            to: ctx.accounts.stake_escrow.to_account_info(),
            authority: ctx.accounts.challenger.to_account_info(),
        },
    );
    token::transfer(transfer_ctx, stake_amount)?;

    // Mark both identities as under challenge
    let claim_id = next_claim_id();
    identity1.under_challenge = true;
    identity1.challenge_claim_id = claim_id;
    identity2.under_challenge = true;
    identity2.challenge_claim_id = claim_id;

    // Initialize claim
    claim.claim_id = claim_id;
    claim.claim_type = ClaimType::DuplicateFlag;
    claim.status = ClaimStatus::Active;
    claim.subject = addr1;
    claim.related_address = addr2;
    claim.platform = String::new();
    claim.justification = evidence;
    claim.created_at = current_time;
    claim.expires_at = current_time + governance.params.claim_expiry_duration;
    claim.total_votes_for = 0;
    claim.total_votes_against = 0;
    claim.total_stake = stake_amount;
    claim.total_slashed = 0;
    claim.vouch_count = 0;
    claim.rewards_distributed = false;
    claim.bump = ctx.bumps.claim;

    msg!("DuplicateFlag claim created: {}", claim_id);
    msg!("Address 1: {}", addr1);
    msg!("Address 2: {}", addr2);
    msg!("Both addresses marked as under challenge");

    Ok(())
}

// ============================================================
// VOUCH FOR
// ============================================================

#[derive(Accounts)]
#[instruction(claim_id: u64, stake_amount: u64)]
pub struct VouchFor<'info> {
    #[account(
        seeds = [GOVERNANCE_SEED],
        bump = governance.bump
    )]
    pub governance: Account<'info, Governance>,

    #[account(
        mut,
        constraint = claim.claim_id == claim_id @ KnomeeError::InvalidClaimStatus,
        constraint = claim.status == ClaimStatus::Active @ KnomeeError::ClaimAlreadyResolved
    )]
    pub claim: Account<'info, IdentityClaim>,

    #[account(
        seeds = [IDENTITY_SEED, voucher.key().as_ref()],
        bump = voucher_identity.bump,
        constraint = voucher_identity.tier.can_vote() @ KnomeeError::InsufficientVotingWeight
    )]
    pub voucher_identity: Account<'info, Identity>,

    #[account(
        init,
        payer = voucher,
        space = Vouch::LEN,
        seeds = [VOUCH_SEED, &claim_id.to_le_bytes(), voucher.key().as_ref()],
        bump
    )]
    pub vouch: Account<'info, Vouch>,

    #[account(mut)]
    pub voucher: Signer<'info>,

    #[account(mut)]
    pub voucher_token_account: Account<'info, TokenAccount>,

    #[account(mut)]
    pub stake_escrow: Account<'info, TokenAccount>,

    pub token_program: Program<'info, Token>,
    pub system_program: Program<'info, System>,
}

pub fn vouch_for(
    ctx: Context<VouchFor>,
    claim_id: u64,
    stake_amount: u64,
) -> Result<()> {
    let governance = &ctx.accounts.governance;
    let claim = &mut ctx.accounts.claim;
    let voucher_identity = &ctx.accounts.voucher_identity;
    let vouch = &mut ctx.accounts.vouch;
    let current_time = governance.current_time();

    // Check claim not expired
    require!(
        current_time < claim.expires_at,
        KnomeeError::ClaimExpired
    );

    // Validate minimum stake
    require!(
        stake_amount >= governance.params.min_stake_lamports,
        KnomeeError::InsufficientStake
    );

    // Transfer stake
    let transfer_ctx = CpiContext::new(
        ctx.accounts.token_program.to_account_info(),
        Transfer {
            from: ctx.accounts.voucher_token_account.to_account_info(),
            to: ctx.accounts.stake_escrow.to_account_info(),
            authority: ctx.accounts.voucher.to_account_info(),
        },
    );
    token::transfer(transfer_ctx, stake_amount)?;

    // Calculate voting weight
    let weight = voucher_identity.voting_weight(&governance.params);

    // Initialize vouch
    vouch.claim_id = claim_id;
    vouch.voucher = ctx.accounts.voucher.key();
    vouch.supports = true;
    vouch.weight = weight;
    vouch.stake = stake_amount;
    vouch.vouched_at = current_time;
    vouch.rewards_claimed = false;
    vouch.reward_amount = 0;
    vouch.bump = ctx.bumps.vouch;

    // Update claim totals
    let weighted_vote = vouch.weighted_vote();
    claim.total_votes_for = claim
        .total_votes_for
        .checked_add(weighted_vote)
        .ok_or(KnomeeError::ArithmeticOverflow)?;
    claim.total_stake = claim
        .total_stake
        .checked_add(stake_amount)
        .ok_or(KnomeeError::ArithmeticOverflow)?;
    claim.vouch_count = claim
        .vouch_count
        .checked_add(1)
        .ok_or(KnomeeError::ArithmeticOverflow)?;

    msg!("Vouch FOR cast on claim {}", claim_id);
    msg!("Voucher: {}", vouch.voucher);
    msg!("Weight: {}, Stake: {}", weight, stake_amount);

    Ok(())
}

// ============================================================
// VOUCH AGAINST
// ============================================================

#[derive(Accounts)]
#[instruction(claim_id: u64, stake_amount: u64)]
pub struct VouchAgainst<'info> {
    #[account(
        seeds = [GOVERNANCE_SEED],
        bump = governance.bump
    )]
    pub governance: Account<'info, Governance>,

    #[account(
        mut,
        constraint = claim.claim_id == claim_id @ KnomeeError::InvalidClaimStatus,
        constraint = claim.status == ClaimStatus::Active @ KnomeeError::ClaimAlreadyResolved
    )]
    pub claim: Account<'info, IdentityClaim>,

    #[account(
        seeds = [IDENTITY_SEED, voucher.key().as_ref()],
        bump = voucher_identity.bump,
        constraint = voucher_identity.tier.can_vote() @ KnomeeError::InsufficientVotingWeight
    )]
    pub voucher_identity: Account<'info, Identity>,

    #[account(
        init,
        payer = voucher,
        space = Vouch::LEN,
        seeds = [VOUCH_SEED, &claim_id.to_le_bytes(), voucher.key().as_ref()],
        bump
    )]
    pub vouch: Account<'info, Vouch>,

    #[account(mut)]
    pub voucher: Signer<'info>,

    #[account(mut)]
    pub voucher_token_account: Account<'info, TokenAccount>,

    #[account(mut)]
    pub stake_escrow: Account<'info, TokenAccount>,

    pub token_program: Program<'info, Token>,
    pub system_program: Program<'info, System>,
}

pub fn vouch_against(
    ctx: Context<VouchAgainst>,
    claim_id: u64,
    stake_amount: u64,
) -> Result<()> {
    let governance = &ctx.accounts.governance;
    let claim = &mut ctx.accounts.claim;
    let voucher_identity = &ctx.accounts.voucher_identity;
    let vouch = &mut ctx.accounts.vouch;
    let current_time = governance.current_time();

    require!(
        current_time < claim.expires_at,
        KnomeeError::ClaimExpired
    );

    require!(
        stake_amount >= governance.params.min_stake_lamports,
        KnomeeError::InsufficientStake
    );

    // Transfer stake
    let transfer_ctx = CpiContext::new(
        ctx.accounts.token_program.to_account_info(),
        Transfer {
            from: ctx.accounts.voucher_token_account.to_account_info(),
            to: ctx.accounts.stake_escrow.to_account_info(),
            authority: ctx.accounts.voucher.to_account_info(),
        },
    );
    token::transfer(transfer_ctx, stake_amount)?;

    let weight = voucher_identity.voting_weight(&governance.params);

    vouch.claim_id = claim_id;
    vouch.voucher = ctx.accounts.voucher.key();
    vouch.supports = false;
    vouch.weight = weight;
    vouch.stake = stake_amount;
    vouch.vouched_at = current_time;
    vouch.rewards_claimed = false;
    vouch.reward_amount = 0;
    vouch.bump = ctx.bumps.vouch;

    let weighted_vote = vouch.weighted_vote();
    claim.total_votes_against = claim
        .total_votes_against
        .checked_add(weighted_vote)
        .ok_or(KnomeeError::ArithmeticOverflow)?;
    claim.total_stake = claim
        .total_stake
        .checked_add(stake_amount)
        .ok_or(KnomeeError::ArithmeticOverflow)?;
    claim.vouch_count = claim
        .vouch_count
        .checked_add(1)
        .ok_or(KnomeeError::ArithmeticOverflow)?;

    msg!("Vouch AGAINST cast on claim {}", claim_id);
    msg!("Voucher: {}, Weight: {}, Stake: {}", vouch.voucher, weight, stake_amount);

    Ok(())
}

// ============================================================
// RESOLVE CONSENSUS
// ============================================================

#[derive(Accounts)]
#[instruction(claim_id: u64)]
pub struct ResolveConsensus<'info> {
    #[account(
        seeds = [GOVERNANCE_SEED],
        bump = governance.bump
    )]
    pub governance: Account<'info, Governance>,

    #[account(
        mut,
        constraint = claim.claim_id == claim_id @ KnomeeError::InvalidClaimStatus
    )]
    pub claim: Account<'info, IdentityClaim>,

    #[account(
        mut,
        seeds = [IDENTITY_SEED, claim.subject.as_ref()],
        bump = subject_identity.bump
    )]
    pub subject_identity: Account<'info, Identity>,
}

pub fn resolve_consensus(
    ctx: Context<ResolveConsensus>,
    claim_id: u64,
) -> Result<()> {
    let governance = &ctx.accounts.governance;
    let claim = &mut ctx.accounts.claim;
    let subject_identity = &mut ctx.accounts.subject_identity;
    let current_time = governance.current_time();

    require!(
        matches!(claim.status, ClaimStatus::Active),
        KnomeeError::ClaimAlreadyResolved
    );

    // Check if expired
    if current_time >= claim.expires_at {
        claim.status = ClaimStatus::Expired;
        subject_identity.last_failed_claim_at = current_time;
        msg!("Claim {} expired without consensus", claim_id);
        return Ok(());
    }

    // Check consensus
    let consensus_result = claim.consensus_reached(&governance.params);

    match consensus_result {
        Some(true) => {
            // APPROVED
            claim.status = ClaimStatus::Approved;

            match claim.claim_type {
                ClaimType::LinkToPrimary => {
                    // LinkToPrimary approved - identity linking will happen in separate instruction
                    msg!("LinkToPrimary claim approved");
                }
                ClaimType::NewPrimary => {
                    // Upgrade to Primary ID
                    subject_identity.tier = IdentityTier::PrimaryID;
                    subject_identity.verified_at = current_time;
                    msg!("NewPrimary claim approved - identity upgraded to PrimaryID");
                }
                ClaimType::DuplicateFlag => {
                    // Both addresses downgraded to GreyGhost
                    subject_identity.tier = IdentityTier::GreyGhost;
                    subject_identity.verified_at = 0;
                    msg!("DuplicateFlag claim approved - Sybil detected");
                }
            }
        }
        Some(false) => {
            // REJECTED
            claim.status = ClaimStatus::Rejected;
            subject_identity.last_failed_claim_at = current_time;
            msg!("Claim {} rejected", claim_id);
        }
        None => {
            return Err(KnomeeError::ClaimNotReadyToResolve.into());
        }
    }

    // Clear challenge status if applicable
    if matches!(claim.claim_type, ClaimType::DuplicateFlag) {
        subject_identity.under_challenge = false;
        subject_identity.challenge_claim_id = 0;
    }

    msg!("Claim {} resolved: {:?}", claim_id, claim.status);

    Ok(())
}

// ============================================================
// CLAIM REWARDS
// ============================================================

#[derive(Accounts)]
#[instruction(claim_id: u64)]
pub struct ClaimRewards<'info> {
    #[account(
        mut,
        constraint = claim.claim_id == claim_id @ KnomeeError::InvalidClaimStatus,
        constraint = claim.status.is_resolved() @ KnomeeError::ClaimNotReadyToResolve
    )]
    pub claim: Account<'info, IdentityClaim>,

    #[account(
        mut,
        seeds = [VOUCH_SEED, &claim_id.to_le_bytes(), voucher.key().as_ref()],
        bump = vouch.bump,
        constraint = vouch.voucher == voucher.key() @ KnomeeError::NotAVoter,
        constraint = !vouch.rewards_claimed @ KnomeeError::RewardsAlreadyClaimed
    )]
    pub vouch: Account<'info, Vouch>,

    #[account(mut)]
    pub voucher: Signer<'info>,

    #[account(mut)]
    pub voucher_token_account: Account<'info, TokenAccount>,

    #[account(mut)]
    pub stake_escrow: Account<'info, TokenAccount>,

    pub token_program: Program<'info, Token>,
}

pub fn claim_rewards(
    ctx: Context<ClaimRewards>,
    claim_id: u64,
) -> Result<()> {
    let claim = &ctx.accounts.claim;
    let vouch = &mut ctx.accounts.vouch;

    // Determine if this voucher was on winning side
    let is_winner = match claim.status {
        ClaimStatus::Approved => vouch.supports,
        ClaimStatus::Rejected => !vouch.supports,
        _ => false,
    };

    if is_winner {
        // Return stake + share of slashed stakes
        // Simplified: just return stake for now
        // In production, calculate proportional reward from slashed stakes
        let reward = vouch.stake;

        let transfer_ctx = CpiContext::new(
            ctx.accounts.token_program.to_account_info(),
            Transfer {
                from: ctx.accounts.stake_escrow.to_account_info(),
                to: ctx.accounts.voucher_token_account.to_account_info(),
                authority: ctx.accounts.stake_escrow.to_account_info(), // Would use PDA signer
            },
        );
        token::transfer(transfer_ctx, reward)?;

        vouch.reward_amount = reward;
        msg!("Reward claimed: {} KNOW", reward);
    } else {
        // Losing side - stake is slashed (already in escrow, will be burned/redistributed)
        msg!("Stake slashed - no reward");
    }

    vouch.rewards_claimed = true;

    Ok(())
}
