package com.knomee.identity.blockchain

/**
 * Identity tier enum matching Solidity contract
 * Maps to: GreyGhost(0), LinkedID(1), PrimaryID(2), Oracle(3)
 */
enum class IdentityTier(val value: Int, val displayName: String) {
    GREYGHOST(0, "GREYGHOST"),
    LINKEDID(1, "LINKEDID"),
    PRIMARYID(2, "PRIMARYID"),
    ORACLE(3, "ORACLE");

    companion object {
        fun fromValue(value: Int): IdentityTier = when (value) {
            0 -> GREYGHOST
            1 -> LINKEDID
            2 -> PRIMARYID
            3 -> ORACLE
            else -> GREYGHOST
        }

        fun fromBigInteger(value: java.math.BigInteger): IdentityTier =
            fromValue(value.toInt())
    }
}
