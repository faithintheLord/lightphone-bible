/**
 * Hooks for Delta API Suite (apiportal.delta.com) once Warren has partner approval.
 * No scraping. No unofficial cookie replay.
 *
 * Env (set in Vercel, never commit):
 * - DELTA_PARTNER_CLIENT_ID
 * - DELTA_PARTNER_CLIENT_SECRET
 * - DELTA_PARTNER_TOKEN_URL (optional)
 * - DELTA_PARTNER_API_BASE (optional)
 */

export type PartnerStatus = {
  configured: boolean;
  blockedReason: string | null;
  nextStep: string;
};

export function partnerStatus(): PartnerStatus {
  const configured = Boolean(
    process.env.DELTA_PARTNER_CLIENT_ID && process.env.DELTA_PARTNER_CLIENT_SECRET,
  );
  if (!configured) {
    return {
      configured: false,
      blockedReason:
        "Official Delta member password / check-in / boarding-pass APIs are not publicly available. Access requires approved partner credentials from the Delta API Suite.",
      nextStep:
        "Register at https://apiportal.delta.com (publish / partner onboarding). After approval, set DELTA_PARTNER_CLIENT_ID and DELTA_PARTNER_CLIENT_SECRET on this Vercel project, then wire journey/order endpoints in lib/partner.ts.",
    };
  }
  return {
    configured: true,
    blockedReason:
      "Partner client credentials are present, but member SkyMiles password login and mobile boarding-pass issuance still require Delta-approved scopes that are not self-serve. Until Delta grants those APIs, this gateway serves the documented demo fixture and browser pairing tokens only.",
    nextStep:
      "Ask Delta partner support which customer-journey / order APIs return boarding pass barcode payloads for an authenticated member, then implement them in fetchPartnerTrips / fetchPartnerBoardingPass.",
  };
}

export async function fetchPartnerClientToken(): Promise<string | null> {
  const clientId = process.env.DELTA_PARTNER_CLIENT_ID;
  const clientSecret = process.env.DELTA_PARTNER_CLIENT_SECRET;
  const tokenUrl = process.env.DELTA_PARTNER_TOKEN_URL;
  if (!clientId || !clientSecret || !tokenUrl) return null;

  const body = new URLSearchParams({
    grant_type: "client_credentials",
    client_id: clientId,
    client_secret: clientSecret,
  });
  const res = await fetch(tokenUrl, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body,
  });
  if (!res.ok) return null;
  const json = (await res.json()) as { access_token?: string };
  return json.access_token ?? null;
}

/** Placeholder — implement after Delta documents member trip endpoints for your app. */
export async function fetchPartnerTrips(_memberToken: string): Promise<null> {
  return null;
}

/** Placeholder — implement after Delta documents boarding-pass barcode endpoints. */
export async function fetchPartnerBoardingPass(
  _memberToken: string,
  _tripId: string,
): Promise<null> {
  return null;
}
