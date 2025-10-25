package com.knomee.identity.blockchain

import java.math.BigInteger

/**
 * Identity data class representing on-chain identity state
 */
data class IdentityData(
    val address: String,
    val tier: IdentityTier,
    val primaryAccount: String,
    val linkedAccounts: Map<String, String>, // platform -> address
    val underChallenge: Boolean,
    val vouchesGiven: Int,
    val vouchesReceived: Int,
    val reputationScore: BigInteger
) {
    fun isPrimary(): Boolean = tier == IdentityTier.PRIMARYID || tier == IdentityTier.ORACLE
    fun isOracle(): Boolean = tier == IdentityTier.ORACLE
    fun getVotingWeight(): Int = when (tier) {
        IdentityTier.GREYGHOST -> 0
        IdentityTier.LINKEDID -> 0
        IdentityTier.PRIMARYID -> 1
        IdentityTier.ORACLE -> 100
    }
}

/**
 * Claim data class representing an active identity claim
 */
data class ClaimData(
    val claimId: BigInteger,
    val claimant: String,
    val claimType: ClaimType,
    val targetAddress: String?,
    val platform: String?,
    val justification: String,
    val stakeAmount: BigInteger,
    val vouchesFor: BigInteger,
    val vouchesAgainst: BigInteger,
    val weightedFor: BigInteger,
    val weightedAgainst: BigInteger,
    val status: ClaimStatus,
    val createdAt: BigInteger,
    val expiresAt: BigInteger
) {
    fun isActive(): Boolean = status == ClaimStatus.ACTIVE
    fun hasExpired(currentTime: BigInteger): Boolean = currentTime > expiresAt
    fun getProgress(): Double {
        val totalWeight = weightedFor + weightedAgainst
        return if (totalWeight > BigInteger.ZERO) {
            weightedFor.toDouble() / totalWeight.toDouble()
        } else 0.0
    }
}

/**
 * Claim type enum
 */
enum class ClaimType(val value: Int, val displayName: String) {
    LINK_TO_PRIMARY(0, "Link to Primary"),
    NEW_PRIMARY(1, "New Primary ID"),
    DUPLICATE_FLAG(2, "Duplicate Flag");

    companion object {
        fun fromValue(value: Int): ClaimType = when (value) {
            0 -> LINK_TO_PRIMARY
            1 -> NEW_PRIMARY
            2 -> DUPLICATE_FLAG
            else -> NEW_PRIMARY
        }
    }
}

/**
 * Claim status enum
 */
enum class ClaimStatus(val value: Int, val displayName: String) {
    ACTIVE(0, "Active"),
    APPROVED(1, "Approved"),
    REJECTED(2, "Rejected"),
    EXPIRED(3, "Expired");

    companion object {
        fun fromValue(value: Int): ClaimStatus = when (value) {
            0 -> ACTIVE
            1 -> APPROVED
            2 -> REJECTED
            3 -> EXPIRED
            else -> ACTIVE
        }
    }
}
