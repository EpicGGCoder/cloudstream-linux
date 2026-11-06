/**
 * Client-side Score formatting helper.
 *
 * The Score class in `src/lib/plugins/types.ts` stores scores as integers
 * scaled to SCORE_MAX = 1_000_000_000. When the server serializes a Score
 * instance to JSON, only the `data` field survives — the methods are lost.
 *
 * This helper reads the deserialized `{ data: number }` shape and formats
 * it to a display string (e.g. "8.2"), matching the server-side
 * `Score.toString(maxScore, decimals)` semantics.
 */

const SCORE_MAX = 1_000_000_000;

/** A Score instance after JSON round-trip — only the `data` field survives. */
export interface SerializedScore {
  data?: number;
}

/**
 * Format a Score-like value for display.
 *
 * @param score    The raw `{ data: number }` shape (or null/undefined).
 * @param maxScore The maximum on the display scale (10 by default).
 * @param decimals Number of decimals (1 by default).
 * @returns A string like "8.2", or `null` if score is missing/invalid.
 */
export function formatScore(
  score: SerializedScore | null | undefined,
  maxScore = 10,
  decimals = 1,
): string | null {
  if (!score || typeof score.data !== "number") return null;
  if (!Number.isFinite(score.data)) return null;
  const v = (score.data / SCORE_MAX) * maxScore;
  return v.toFixed(decimals);
}
