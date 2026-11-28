# Real Delta access: what works vs what is blocked

## Deployed contract (works now)

This gateway implements the Light Phone III `HttpDeltaGateway` JSON contract:

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/v1/login` | Demo: `1234567890` / `demo` |
| GET | `/v1/trips` | Demo ATL→LAX fixture |
| GET | `/v1/trips/{id}` | |
| GET/POST | `/v1/trips/{id}/upgrades` | Comfort+ / First |
| GET/POST | `/v1/trips/{id}/seats` | Seat map + select |
| POST | `/v1/trips/{id}/bags` | Bag count + prices |
| POST | `/v1/trips/{id}/payment-quote` | Quote from check-in draft |
| POST | `/v1/trips/{id}/pay` | Confirm quote id (card on file last4) |
| POST | `/v1/trips/{id}/check-in` | Returns boarding pass |
| GET | `/v1/trips/{id}/boarding-pass` | After check-in |

Phone **Settings → API base** = deployment origin only (no `/v1` suffix), e.g. `https://lightphone-delta-gateway.vercel.app`.

## What is real vs demo

| Capability | Status |
| --- | --- |
| Full `/v1/*` contract over HTTPS | **Real** (this gateway) |
| Demo SkyMiles login + boarding pass QR payload | **Demo fixture** (same as in-app `InProcessDeltaGateway`) |
| Live SkyMiles password login against Delta | **Blocked** — no public member password API |
| Live trips / check-in / boarding pass from Delta | **Blocked** without partner approval |
| Scraping delta.com / cookie replay | **Rejected** (ToS / Tool Library risk) |
| Browser pairing page `/auth` | **Scaffold** for future OAuth/PKCE; today mints a gateway-held session token |

## Exact next legal step for real boarding passes

1. Open [Delta API Suite](https://apiportal.delta.com) and complete partner / publish onboarding (`https://apiportal.delta.com/publish`).
2. Request scopes that cover **member journey / order** data and **boarding pass barcode** issuance (confirm with Delta partner support — member check-in is not self-serve).
3. In this Vercel project, set env vars (never commit):
   - `SESSION_HMAC_SECRET` (required for production token signing)
   - `BLOB_READ_WRITE_TOKEN` (private Blob store — already used for session/trip persistence)
   - `DELTA_PARTNER_CLIENT_ID`
   - `DELTA_PARTNER_CLIENT_SECRET`
   - `DELTA_PARTNER_TOKEN_URL` (from portal docs)
   - `DELTA_PARTNER_API_BASE` (from portal docs)
4. Implement the placeholders in `lib/partner.ts` (`fetchPartnerTrips`, `fetchPartnerBoardingPass`) and route authenticated `/v1/*` calls through them.
5. Turn `/auth` into OAuth authorization-code + PKCE: Warren authenticates in the browser; the gateway stores a short-lived partner session server-side; the phone receives only the gateway bearer token (same pattern as the Spotify LP3 plan).

Until step 2 succeeds, use demo credentials against this HTTPS base to exercise the phone end-to-end.
