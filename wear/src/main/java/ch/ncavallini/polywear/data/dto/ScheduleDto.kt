package ch.ncavallini.polywear.data.dto

import kotlinx.serialization.Serializable

/**
 * Model of the eduapp schedule JSON, confirmed against a live response
 * (Phase 0). The endpoint returns a **top-level array** of these objects.
 *
 * Example element:
 * ```
 * {
 *   "id": 1318394, "type": "V", "code": "263-4640-00", "semkez": "2026W",
 *   "start": "2026-09-15T08:15:00Z", "end": "2026-09-15T10:00:00Z",
 *   "locations": ["HG E 1.2"], "title": { "de": "Network Security" }, "assets": {...}
 * }
 * ```
 *
 * Notes:
 *  - `start`/`end` are UTC (`Z`) and must be converted to Europe/Zurich.
 *  - `title` is a language map; `locations` may contain placeholders like "n / a".
 *  - `type`: V = Lecture, U = Exercise, P = Lab, S = Seminar.
 *  - `assets` is intentionally not modelled (ignored via ignoreUnknownKeys).
 */
@Serializable
data class LessonDto(
    val id: Long? = null,
    val type: String? = null,
    val code: String? = null,
    val semkez: String? = null,
    val start: String? = null,
    val end: String? = null,
    val locations: List<String> = emptyList(),
    val title: Map<String, String> = emptyMap(),
)
