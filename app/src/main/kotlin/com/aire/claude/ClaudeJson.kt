package com.aire.claude

import kotlinx.serialization.json.Json

/**
 * Shared JSON parser for Claude's structured output.
 *
 * `isLenient` + `ignoreUnknownKeys` make parsing resilient to the small
 * formatting variations a model can produce (an extra field, a trailing token).
 * `coerceInputValues` falls back to a property's default when the model sends
 * null or an unrecognized enum value instead of throwing.
 */
internal val ClaudeJson: Json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/**
 * Claude is asked to return raw JSON, but models sometimes wrap it in a
 * ```json ... ``` fence or add a stray sentence. This pulls out the outermost
 * JSON object so parsing sees clean input.
 */
internal fun extractJsonObject(raw: String): String {
    val start = raw.indexOf('{')
    val end = raw.lastIndexOf('}')
    require(start != -1 && end > start) { "No JSON object found in model output: $raw" }
    return raw.substring(start, end + 1)
}
