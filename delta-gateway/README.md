# Light Phone Delta Gateway

Vercel HTTPS proxy that implements the `HttpDeltaGateway` `/v1/*` contract for
[`lightphone-deltaairlines`](https://github.com/faithintheLord/lightphone-deltaairlines).

Not affiliated with Delta Air Lines. Does **not** scrape delta.com.

## Phone setup

1. Deploy this `gateway/` folder to Vercel (root directory = `gateway`).
2. On the Light Phone III Trips tool: **Settings → API base** = the deployment origin, e.g. `https://your-app.vercel.app` (no trailing path, no `/v1`).
3. Sign in with SkyMiles `1234567890` / password `demo` for the fixture boarding pass.

## Local

```bash
cd gateway
npm install
npm run dev
node scripts/smoke.mjs http://127.0.0.1:3000
```

## Env (Vercel dashboard — never commit)

| Variable | Purpose |
| --- | --- |
| `SESSION_HMAC_SECRET` | HMAC secret for bearer tokens (≥16 chars) |
| `DELTA_PARTNER_CLIENT_ID` | Optional — after Delta partner approval |
| `DELTA_PARTNER_CLIENT_SECRET` | Optional — after Delta partner approval |
| `DELTA_PARTNER_TOKEN_URL` | Optional partner token endpoint |
| `DELTA_PARTNER_API_BASE` | Optional partner API base |

See [docs/real-access.md](docs/real-access.md) for the blocked real-flight path and the exact next legal step.
