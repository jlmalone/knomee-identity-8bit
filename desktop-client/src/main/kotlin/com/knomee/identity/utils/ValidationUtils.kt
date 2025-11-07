package com.knomee.identity.utils

import org.web3j.crypto.Keys

/**
 * Utilities for validating Ethereum addresses and other inputs
 */
object ValidationUtils {

    /**
     * Validates an Ethereum address format
     * @param address Address to validate
     * @return true if valid format
     */
    fun isValidEthereumAddress(address: String): Boolean {
        if (address.isBlank()) return false

        // Remove 0x prefix if present
        val cleaned = address.removePrefix("0x")

        // Check length (40 hex characters)
        if (cleaned.length != 40) return false

        // Check if all characters are hex
        return cleaned.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    /**
     * Validates and checksums an Ethereum address
     * @param address Address to validate
     * @return Checksummed address or null if invalid
     */
    fun validateAndChecksumAddress(address: String): String? {
        if (!isValidEthereumAddress(address)) return null

        return try {
            Keys.toChecksumAddress(address)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Validates a private key format
     * @param privateKey Private key to validate
     * @return true if valid format
     */
    fun isValidPrivateKey(privateKey: String): Boolean {
        if (privateKey.isBlank()) return false

        val cleaned = privateKey.removePrefix("0x")

        // Private key should be 64 hex characters
        if (cleaned.length != 64) return false

        return cleaned.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    /**
     * Validates a stake amount
     * @param amount Amount to validate
     * @param minStake Minimum required stake
     * @return Error message or null if valid
     */
    fun validateStakeAmount(amount: String, minStake: Double): String? {
        val value = amount.toDoubleOrNull()

        return when {
            value == null -> "Please enter a valid number"
            value <= 0 -> "Stake amount must be positive"
            value < minStake -> "Minimum stake is $minStake KNOW"
            else -> null
        }
    }

    /**
     * Validates a justification string
     * @param justification Justification to validate
     * @param minLength Minimum required length
     * @return Error message or null if valid
     */
    fun validateJustification(justification: String, minLength: Int = 10): String? {
        return when {
            justification.isBlank() -> "Justification is required"
            justification.length < minLength -> "Please provide at least $minLength characters"
            else -> null
        }
    }

    /**
     * Validates a platform name
     * @param platform Platform name to validate
     * @return Error message or null if valid
     */
    fun validatePlatform(platform: String): String? {
        return when {
            platform.isBlank() -> "Platform name is required"
            platform.length < 2 -> "Platform name too short"
            platform.length > 50 -> "Platform name too long"
            !platform.matches(Regex("^[a-zA-Z0-9_-]+$")) ->
                "Platform name can only contain letters, numbers, hyphens, and underscores"
            else -> null
        }
    }

    /**
     * Truncates an Ethereum address for display
     * @param address Address to truncate
     * @param prefixLength Number of characters to show at start
     * @param suffixLength Number of characters to show at end
     * @return Truncated address like "0x1234...5678"
     */
    fun truncateAddress(address: String, prefixLength: Int = 6, suffixLength: Int = 4): String {
        if (address.length <= prefixLength + suffixLength + 3) return address

        val prefix = address.take(prefixLength)
        val suffix = address.takeLast(suffixLength)
        return "$prefix...$suffix"
    }

    /**
     * Formats a wei amount to ETH/KNOW with proper decimals
     * @param wei Amount in wei
     * @param decimals Number of decimal places
     * @return Formatted string
     */
    fun formatTokenAmount(wei: java.math.BigInteger, decimals: Int = 18): String {
        val divisor = java.math.BigDecimal.TEN.pow(decimals)
        val amount = java.math.BigDecimal(wei).divide(divisor)

        return when {
            amount.compareTo(java.math.BigDecimal.ZERO) == 0 -> "0"
            amount.compareTo(java.math.BigDecimal.ONE) < 0 ->
                amount.setScale(4, java.math.RoundingMode.DOWN).toPlainString()
            else ->
                amount.setScale(2, java.math.RoundingMode.DOWN).toPlainString()
        }
    }
}
