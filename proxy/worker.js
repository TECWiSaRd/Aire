/**
 * Aire Claude proxy — reference implementation (Cloudflare Worker).
 *
 * Purpose: keep the Anthropic API key OFF the device. The Android app (release
 * builds) points the SDK's baseUrl at this Worker and sends its own app token in
 * the `x-api-key` header. This Worker validates that token, then forwards the
 * request to api.anthropic.com with the REAL key injected server-side.
 *
 * Secrets (set with `wrangler secret put`, never in code):
 *   ANTHROPIC_API_KEY  — the real sk-ant-... key
 *   AIRE_APP_TOKEN     — shared token the app must present (stopgap; see README)
 *
 * SECURITY NOTE: a single shared AIRE_APP_TOKEN is a stopgap for a private
 * deployment. It prevents casual abuse but can still be extracted from an APK.
 * For production, replace the token check below with real per-request
 * verification — Google Play Integrity or a signed-in user's session — so there
 * is no shared secret in the app at all.
 */

const ANTHROPIC_BASE = "https://api.anthropic.com";

export default {
  async fetch(request, env) {
    if (request.method !== "POST") {
      return json({ error: "method_not_allowed" }, 405);
    }

    const url = new URL(request.url);
    // Only proxy the Messages API surface the app uses.
    if (!url.pathname.startsWith("/v1/messages")) {
      return json({ error: "not_found" }, 404);
    }

    // 1. Authenticate the app. The SDK put the app token in `x-api-key`.
    const presented = request.headers.get("x-api-key") || "";
    if (!env.AIRE_APP_TOKEN || !timingSafeEqual(presented, env.AIRE_APP_TOKEN)) {
      return json({ error: "unauthorized" }, 401);
    }

    // 2. Forward to Anthropic with the real key swapped in. Preserve everything
    //    else (anthropic-version, content-type, streaming, the JSON body).
    const forwarded = new Headers(request.headers);
    forwarded.set("x-api-key", env.ANTHROPIC_API_KEY);
    forwarded.delete("host");

    const upstream = await fetch(ANTHROPIC_BASE + url.pathname + url.search, {
      method: "POST",
      headers: forwarded,
      body: request.body,
    });

    // Pass the response straight back (works for both JSON and SSE streaming).
    return new Response(upstream.body, {
      status: upstream.status,
      headers: upstream.headers,
    });
  },
};

function json(obj, status) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { "content-type": "application/json" },
  });
}

function timingSafeEqual(a, b) {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}
