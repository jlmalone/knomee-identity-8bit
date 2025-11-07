package com.knomee.identity.config

import java.io.File
import java.util.Properties

/**
 * Configuration management for Knomee Identity Desktop Client
 *
 * Loads configuration from:
 * 1. Environment variables (highest priority)
 * 2. config.properties file
 * 3. Default values (lowest priority)
 */
object AppConfig {

    private val properties = Properties()

    init {
        // Try to load from config.properties in user home
        val configFile = File(System.getProperty("user.home"), ".knomee/config.properties")
        if (configFile.exists()) {
            configFile.inputStream().use { properties.load(it) }
        }
    }

    // Network Configuration
    val rpcUrl: String
        get() = System.getenv("KNOMEE_RPC_URL")
            ?: properties.getProperty("rpc.url")
            ?: "http://127.0.0.1:8545"

    val chainId: Long
        get() = (System.getenv("KNOMEE_CHAIN_ID")
            ?: properties.getProperty("chain.id")
            ?: "31337").toLong()

    // Private Key (NEVER commit actual keys!)
    val privateKey: String?
        get() = System.getenv("KNOMEE_PRIVATE_KEY")
            ?: properties.getProperty("private.key")
            // For testing ONLY - should be removed in production
            ?: if (isDevelopment) "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80" else null

    // Contract Addresses
    val governanceAddress: String
        get() = System.getenv("KNOMEE_GOVERNANCE_ADDRESS")
            ?: properties.getProperty("contract.governance")
            ?: ""

    val registryAddress: String
        get() = System.getenv("KNOMEE_REGISTRY_ADDRESS")
            ?: properties.getProperty("contract.registry")
            ?: ""

    val consensusAddress: String
        get() = System.getenv("KNOMEE_CONSENSUS_ADDRESS")
            ?: properties.getProperty("contract.consensus")
            ?: ""

    val identityTokenAddress: String
        get() = System.getenv("KNOMEE_IDENTITY_TOKEN_ADDRESS")
            ?: properties.getProperty("contract.identityToken")
            ?: ""

    val knomeeTokenAddress: String
        get() = System.getenv("KNOMEE_KNOW_TOKEN_ADDRESS")
            ?: properties.getProperty("contract.knomeeToken")
            ?: ""

    // Application Settings
    val isDevelopment: Boolean
        get() = System.getenv("KNOMEE_ENV")?.lowercase() == "development"
            || properties.getProperty("env")?.lowercase() == "development"
            || true // Default to development

    val logLevel: String
        get() = System.getenv("KNOMEE_LOG_LEVEL")
            ?: properties.getProperty("log.level")
            ?: "INFO"

    // Transaction Settings
    val defaultGasLimit: Long
        get() = (System.getenv("KNOMEE_GAS_LIMIT")
            ?: properties.getProperty("tx.gasLimit")
            ?: "500000").toLong()

    val txTimeoutSeconds: Int
        get() = (System.getenv("KNOMEE_TX_TIMEOUT")
            ?: properties.getProperty("tx.timeoutSeconds")
            ?: "60").toInt()

    // Validation
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (privateKey.isNullOrBlank()) {
            errors.add("Private key not configured. Set KNOMEE_PRIVATE_KEY environment variable.")
        }

        if (governanceAddress.isBlank()) {
            errors.add("Governance contract address not configured.")
        }

        if (registryAddress.isBlank()) {
            errors.add("Registry contract address not configured.")
        }

        if (consensusAddress.isBlank()) {
            errors.add("Consensus contract address not configured.")
        }

        return errors
    }

    /**
     * Save current configuration to config.properties file
     */
    fun save(
        governance: String? = null,
        registry: String? = null,
        consensus: String? = null,
        identityToken: String? = null,
        knomeeToken: String? = null
    ) {
        val configDir = File(System.getProperty("user.home"), ".knomee")
        configDir.mkdirs()

        val configFile = File(configDir, "config.properties")

        governance?.let { properties.setProperty("contract.governance", it) }
        registry?.let { properties.setProperty("contract.registry", it) }
        consensus?.let { properties.setProperty("contract.consensus", it) }
        identityToken?.let { properties.setProperty("contract.identityToken", it) }
        knomeeToken?.let { properties.setProperty("contract.knomeeToken", it) }

        configFile.outputStream().use { output ->
            properties.store(output, "Knomee Identity Protocol Configuration")
        }
    }

    /**
     * Create sample configuration file
     */
    fun createSampleConfig() {
        val configDir = File(System.getProperty("user.home"), ".knomee")
        configDir.mkdirs()

        val sampleFile = File(configDir, "config.properties.sample")

        sampleFile.writeText("""
            # Knomee Identity Protocol Configuration
            # Copy this file to config.properties and fill in your values

            # Network Configuration
            rpc.url=http://127.0.0.1:8545
            chain.id=31337

            # Private Key (NEVER commit this file with real keys!)
            # Use environment variable KNOMEE_PRIVATE_KEY instead for production
            # private.key=0x...

            # Contract Addresses (from deployment)
            contract.governance=0x...
            contract.registry=0x...
            contract.consensus=0x...
            contract.identityToken=0x...
            contract.knomeeToken=0x...

            # Application Settings
            env=development
            log.level=INFO

            # Transaction Settings
            tx.gasLimit=500000
            tx.timeoutSeconds=60
        """.trimIndent())
    }
}
