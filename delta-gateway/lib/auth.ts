import { DEMO_PASSWORD, DEMO_SKYMILES } from "./constants";
import { createDemoSession, createPairedSession, getSession } from "./store";
import { newSessionId, signToken, verifyToken } from "./session-crypto";
import type { Session, SessionRecord } from "./types";

export function maskSkymiles(number: string): string {
  const digits = number.replace(/\D/g, "");
  const last4 = digits.slice(-4).padStart(4, "0");
  return `••••${last4}`;
}

export function loginWithPassword(skymilesNumber: string, password: string): Session | null {
  const digits = skymilesNumber.replace(/\D/g, "");
  if (digits === DEMO_SKYMILES && password === DEMO_PASSWORD) {
    const token = signToken({
      sid: newSessionId(),
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
    createDemoSession(token, session);
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

export function requireSession(token: string | null): SessionRecord | null {
  if (!token) return null;
  const existing = getSession(token);
  if (existing) return existing;

  const claims = verifyToken(token);
  if (!claims || typeof claims.sid !== "string") return null;

  const session: Session = {
    token,
    skymilesMasked: typeof claims.skymiles === "string" ? maskSkymiles(claims.skymiles) : "••••0000",
    displayName: typeof claims.name === "string" ? claims.name : "SkyMiles Member",
    medallionTier: typeof claims.tier === "string" ? claims.tier : "Member",
  };
  const mode = claims.mode === "paired" ? "paired" : "demo";
  return mode === "paired" ? createPairedSession(token, session) : createDemoSession(token, session);
}

export function createPairedMemberSession(displayName: string, skymilesNumber: string): Session {
  const digits = skymilesNumber.replace(/\D/g, "") || "0000000000";
  const token = signToken({
    sid: newSessionId(),
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
  createPairedSession(token, session);
  return session;
}
