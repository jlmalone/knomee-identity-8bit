package com.knomee.identity.utils

import com.knomee.identity.config.AppConfig
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Simple logging utility for desktop client
 * Replaces println() calls with proper structured logging
 */
object Logger {

    enum class Level {
        DEBUG, INFO, WARN, ERROR
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private fun shouldLog(level: Level): Boolean {
        val configLevel = when (AppConfig.logLevel.uppercase()) {
            "DEBUG" -> Level.DEBUG
            "INFO" -> Level.INFO
            "WARN" -> Level.WARN
            "ERROR" -> Level.ERROR
            else -> Level.INFO
        }

        return level.ordinal >= configLevel.ordinal
    }

    private fun log(level: Level, tag: String, message: String, throwable: Throwable? = null) {
        if (!shouldLog(level)) return

        val timestamp = LocalDateTime.now().format(dateFormatter)
        val logMessage = "[$timestamp] [${level.name}] [$tag] $message"

        println(logMessage)

        throwable?.let {
            println("  Exception: ${it.message}")
            if (level == Level.ERROR || AppConfig.isDevelopment) {
                it.printStackTrace()
            }
        }
    }

    fun debug(tag: String, message: String) {
        log(Level.DEBUG, tag, message)
    }

    fun info(tag: String, message: String) {
        log(Level.INFO, tag, message)
    }

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.WARN, tag, message, throwable)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.ERROR, tag, message, throwable)
    }
}

/**
 * Extension function to create a logger for a class
 */
fun Any.logger(tag: String = this::class.simpleName ?: "Unknown") = object {
    fun debug(message: String) = Logger.debug(tag, message)
    fun info(message: String) = Logger.info(tag, message)
    fun warn(message: String, throwable: Throwable? = null) = Logger.warn(tag, message, throwable)
    fun error(message: String, throwable: Throwable? = null) = Logger.error(tag, message, throwable)
}
