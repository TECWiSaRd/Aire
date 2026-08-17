package com.aire.domain

import kotlinx.serialization.Serializable

/**
 * The kind of thing a captured record represents. Claude classifies each input
 * into one of these during extraction. Kept deliberately small for the MVP —
 * new categories can be added as use cases expand (see CLAUDE.md "Use Cases").
 *
 * Serialized by name (e.g. "RECEIPT"), which is also what the extraction prompt
 * asks Claude to emit, so the wire format and the enum stay in lockstep.
 */
@Serializable
enum class MemoryCategory {
    /** Store/online purchase: vendor, amount, date, line items. */
    RECEIPT,

    /** Something happening at a time/place: flyer, invite, appointment. */
    EVENT,

    /** An actionable to-do, optionally with a due date. */
    TASK,

    /** A person's details: business card, signature block. */
    CONTACT,

    /** A bill/invoice/warranty/manual worth keeping. */
    DOCUMENT,

    /** Free-form note or reminder with no stronger structure. */
    NOTE,

    /** Anything that doesn't fit the buckets above. */
    OTHER,
}
