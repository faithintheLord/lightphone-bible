import { createHmac, randomBytes, timingSafeEqual } from "crypto";

function secret(): string {
  const value = process.env.SESSION_HMAC_SECRET;
  if (value && value.length >= 16) return value;
  // Deterministic fallback for free-tier demos only — set SESSION_HMAC_SECRET in Vercel for real use.
  return "lightphone-delta-gateway-demo-hmac-change-me";
}

export function signToken(payload: Record<string, unknown>): string {
  const body = Buffer.from(JSON.stringify(payload), "utf8").toString("base64url");
  const sig = createHmac("sha256", secret()).update(body).digest("base64url");
  return `${body}.${sig}`;
}

export function verifyToken(token: string): Record<string, unknown> | null {
  const parts = token.split(".");
  if (parts.length !== 2) return null;
  const [body, sig] = parts;
  if (!body || !sig) return null;
  const expected = createHmac("sha256", secret()).update(body).digest("base64url");
  const a = Buffer.from(sig);
  const b = Buffer.from(expected);
  if (a.length !== b.length || !timingSafeEqual(a, b)) return null;
  try {
    return JSON.parse(Buffer.from(body, "base64url").toString("utf8")) as Record<string, unknown>;
  } catch {
    return null;
  }
}

export function newSessionId(): string {
  return randomBytes(16).toString("hex");
}
