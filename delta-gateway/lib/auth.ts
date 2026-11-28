import { DEMO_PASSWORD, DEMO_SKYMILES } from "./constants";
import {
  createDemoSession,
  createPairedSession,
  getSession,
  getSessionBySid,
} from "./store";
import { newSessionId, signToken, verifyToken } from "./session-crypto";
import type { Session, SessionRecord } from "./types";

export function maskSkymiles(number: string): string {
  const digits = number.replace(/\D/g, "");
  const last4 = digits.slice(-4).padStart(4, "0");
  return `••••${last4}`;
}

export async function loginWithPassword(
  skymilesNumber: string,
  password: string,
): Promise<Session | null> {
  const digits = skymilesNumber.replace(/\D/g, "");
  if (digits === DEMO_SKYMILES && password === DEMO_PASSWORD) {
    const sid = newSessionId();
    const token = signToken({
      sid,
      mode: "demo",
      skymiles: digits,
      name: "Alex Morgan",
      tier: "Gold Medallion",
      iat: Date.now(),
    });
    const session: Session = {
      token,
      skymilesMasked: maskSkymiles(digits),
      displayName: "Alex Morgan",
      medallionTier: "Gold Medallion",
    };
    await createDemoSession(sid, token, session);
    return session;
  }

  // Real SkyMiles password APIs are partner-only. Without DELTA_PARTNER_* credentials,
  // reject non-demo logins rather than scrape delta.com.
  if (!process.env.DELTA_PARTNER_CLIENT_ID || !process.env.DELTA_PARTNER_CLIENT_SECRET) {
    return null;
  }

  // Partner credentials present: still no public member-password endpoint to call.
  // Keep password login demo-only; use /auth browser pairing for approved partner flows.
  return null;
}

export async function requireSession(token: string | null): Promise<SessionRecord | null> {
  if (!token) return null;

  const claims = verifyToken(token);
  if (!claims || typeof claims.sid !== "string") return null;
  const sid = claims.sid;

  const bySid = await getSessionBySid(sid);
  if (bySid) {
    bySid.session.token = token;
    return bySid;
  }

  const existing = await getSession(token);
  if (existing) return existing;

  // Valid signature but no stored state yet — should not happen after login.
  // Do not invent empty trip state for an old token; treat as unauthorized.
  return null;
}

export async function createPairedMemberSession(
  displayName: string,
  skymilesNumber: string,
): Promise<Session> {
  const digits = skymilesNumber.replace(/\D/g, "") || "0000000000";
  const sid = newSessionId();
  const token = signToken({
    sid,
    mode: "paired",
    skymiles: digits,
    name: displayName,
    tier: "Member",
    iat: Date.now(),
  });
  const session: Session = {
    token,
    skymilesMasked: maskSkymiles(digits),
    displayName,
    medallionTier: "Member",
  };
  await createPairedSession(sid, token, session);
  return session;
}
