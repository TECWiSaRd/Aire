package com.aire.domain

import kotlinx.serialization.Serializable

/** How a record entered the system. */
@Serializable
enum class SourceType { TEXT, IMAGE, VOICE, SHARED }

/**
 * The condensed, structured record that is the primary object stored and
 * queried (see CLAUDE.md "Processing layer"). One captured input → one record.
 *
 * Fields split into three groups:
 *  - Extracted by Claude: [category], [title], [summary], [occurredOn],
 *    [attributes], [tags]. These are what [com.aire.claude.ExtractionService]
 *    fills from the raw input.
 *  - Provenance, set locally at capture time: [id], [capturedAt], [sourceText],
 *    [sourceType].
 *
 * [attributes] is an open key→value map (e.g. "vendor" -> "Blue Bottle",
 * "amount" -> "18.50", "currency" -> "USD") so the schema stays flexible across
 * categories without a rigid column per field. [tags] carries semantic hints
 * ("coffee", "food", "work") that support recall until real vector search lands.
 */
@Serializable
data class MemoryRecord(
    val id: String,
    val category: MemoryCategory,
    val title: String,
    val summary: String,
    /** ISO-8601 date (YYYY-MM-DD) the record is *about*, if any. Null when not applicable. */
    val occurredOn: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList(),
    /** Epoch millis when the input was captured on-device. */
    val capturedAt: Long,
    /** The original input text, retained for "show me the source" lookups. */
    val sourceText: String,
    val sourceType: SourceType = SourceType.TEXT,
    /** Local file path to the captured image, if any. */
    val imagePath: String? = null,
) {
    /**
     * A token-efficient version of the record for Claude to reason over during
     * recall. Excludes provenance (id, source text) that the model doesn't need.
     */
    fun toRecallSummary(): String = buildString {
        append("Title: $title | Category: $category")
        if (summary.isNotBlank()) append(" | Summary: $summary")
        occurredOn?.let { append(" | Date: $it") }
        if (attributes.isNotEmpty()) append(" | Details: $attributes")
        if (tags.isNotEmpty()) append(" | Tags: ${tags.joinToString()}")
    }
}

/**
 * The subset of a record that Claude produces during extraction. Kept separate
 * from [MemoryRecord] so the model output surface is exactly the fields Claude
 * is responsible for — provenance is added locally afterwards. Defaults make
 * parsing resilient to a field Claude omits.
 */
@Serializable
data class ExtractedFields(
    val category: MemoryCategory = MemoryCategory.OTHER,
    val title: String = "",
    val summary: String = "",
    val occurredOn: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList(),
)
