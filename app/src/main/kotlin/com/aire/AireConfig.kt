package com.aire

import com.aire.claude.ClaudeConfig

/**
 * Maps generated [BuildConfig] values (set per build type in app/build.gradle.kts)
 * into a [ClaudeConfig] the claude package can consume without depending on
 * BuildConfig itself.
 */
object AireConfig {

    fun claude(): ClaudeConfig = ClaudeConfig(
        useProxy = BuildConfig.USE_PROXY,
        proxyBaseUrl = BuildConfig.AIRE_PROXY_URL,
        directApiKey = BuildConfig.ANTHROPIC_API_KEY,
        // Stopgap for a private proxy deployment. TODO(auth): replace with a
        // runtime credential — Play Integrity token or a signed-in user's session
        // token — so no shared secret is compiled into the app.
        proxyAuthToken = BuildConfig.AIRE_APP_TOKEN,
    )
}
