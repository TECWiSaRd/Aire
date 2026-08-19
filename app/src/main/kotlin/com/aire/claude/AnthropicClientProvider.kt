package com.aire.claude

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient

/**
 * How the app reaches Claude.
 */
data class ClaudeConfig(
    val useProxy: Boolean,
    val proxyBaseUrl: String,
    val directApiKey: String,
    val proxyAuthToken: String,
    val model: String = "claude-haiku-4-5"
) {
    /** Whether the configuration has enough credentials to attempt an AI call. */
    val isConfigured: Boolean = if (useProxy) proxyBaseUrl.isNotBlank() else directApiKey.isNotBlank()
}

/**
 * Single place the Anthropic client is constructed.
 */
object AnthropicClientProvider {

    @Volatile
    private var client: AnthropicClient? = null
    
    @Volatile
    private var currentConfig: ClaudeConfig? = null

    fun get(config: ClaudeConfig): AnthropicClient {
        val existing = client
        if (existing != null && currentConfig == config) {
            return existing
        }
        
        return synchronized(this) {
            val locked = client
            if (locked != null && currentConfig == config) {
                locked
            } else {
                build(config).also {
                    client = it
                    currentConfig = config
                }
            }
        }
    }

    private fun build(config: ClaudeConfig): AnthropicClient {
        val builder = AnthropicOkHttpClient.builder()

        if (config.useProxy) {
            builder.baseUrl(config.proxyBaseUrl.ifBlank { "https://localhost" })
            builder.apiKey(config.proxyAuthToken.ifBlank { "unconfigured" })
        } else {
            builder.apiKey(config.directApiKey.ifBlank { "unconfigured" })
        }

        return builder.build()
    }
    
    fun reset() {
        synchronized(this) {
            client = null
            currentConfig = null
        }
    }
}
