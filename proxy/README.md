# Aire Claude proxy (reference)

This keeps the Anthropic API key **off the device**. Release builds of the app
call this proxy instead of `api.anthropic.com`; the proxy holds the real key and
injects it server-side.

`worker.js` is a Cloudflare Worker, chosen because it's a single file, holds
secrets in the platform (not in code), and has no server to run. The same idea
works on any host (Node/Express, a Lambda, a Cloud Function) — forward
`POST /v1/messages*` to Anthropic with the real `x-api-key`.

## Deploy (Cloudflare Workers)

```bash
npm install -g wrangler
wrangler login
wrangler init aire-proxy   # or drop worker.js into an existing project
wrangler secret put ANTHROPIC_API_KEY   # paste your sk-ant-... key
wrangler secret put AIRE_APP_TOKEN      # any long random string
wrangler deploy
```

You'll get a URL like `https://aire-proxy.<you>.workers.dev`.

## Point the app at it

In `local.properties` (git-ignored), for release builds:

```
AIRE_PROXY_URL=https://aire-proxy.<you>.workers.dev
AIRE_APP_TOKEN=<the same random string you set above>
```

Debug builds ignore these and use `ANTHROPIC_API_KEY` directly.

## Security status

- ✅ The Anthropic key is never in the APK.
- ⚠️ `AIRE_APP_TOKEN` is a **stopgap**: it blocks casual abuse but can be
  extracted from an APK, so it's fine for a private/test deployment, not a public
  release. For production, replace the token check in `worker.js` with **Google
  Play Integrity** verification or a **signed-in user's session token**, so there
  is no shared secret compiled into the app. The app-side swap point is
  `AireConfig.proxyAuthToken`.
