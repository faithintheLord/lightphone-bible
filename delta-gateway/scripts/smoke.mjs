#!/usr/bin/env node
/**
 * End-to-end smoke against a deployed (or local) gateway base URL.
 * Usage: node scripts/smoke.mjs https://example.vercel.app
 */
const base = (process.argv[2] || "http://127.0.0.1:3000").replace(/\/$/, "");

async function req(method, path, { token, body } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(`${base}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
    redirect: "manual",
  });
  const text = await res.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = text;
  }
  if (!res.ok && res.status !== 204) {
    throw new Error(`${method} ${path} -> ${res.status} ${text}`);
  }
  return { status: res.status, json };
}

async function main() {
  console.log(`Smoke testing ${base}`);
  const health = await req("GET", "/api/health");
  console.log("health", health.json.ok);

  const login = await req("POST", "/v1/login", {
    body: { skymilesNumber: "1234567890", password: "demo" },
  });
  const token = login.json.token;
  console.log("login", login.json.displayName, login.json.skymilesMasked);

  const trips = await req("GET", "/v1/trips", { token });
  const trip = trips.json.trips[0];
  console.log("trip", trip.id, trip.confirmationCode);

  await req("POST", `/v1/trips/${trip.id}/upgrades`, {
    token,
    body: { offerId: "up-comfort" },
  });
  await req("POST", `/v1/trips/${trip.id}/seats`, {
    token,
    body: { seatCode: "22A" },
  });
  await req("POST", `/v1/trips/${trip.id}/bags`, {
    token,
    body: { count: 2 },
  });

  const draft = {
    tripId: trip.id,
    upgradeId: "up-comfort",
    seatCode: "22A",
    bagCount: 2,
  };
  const quote = await req("POST", `/v1/trips/${trip.id}/payment-quote`, {
    token,
    body: draft,
  });
  console.log("quote", quote.json.id, quote.json.totalCents);

  await req("POST", `/v1/trips/${trip.id}/pay`, {
    token,
    body: { quoteId: quote.json.id },
  });

  const pass = await req("POST", `/v1/trips/${trip.id}/check-in`, {
    token,
    body: { ...draft, paymentQuoteId: quote.json.id },
  });
  console.log("boarding pass", pass.json.seat, pass.json.gate, pass.json.barcodePayload.slice(0, 24) + "…");

  const fetched = await req("GET", `/v1/trips/${trip.id}/boarding-pass`, { token });
  if (fetched.json.barcodePayload !== pass.json.barcodePayload) {
    throw new Error("boarding pass mismatch");
  }

  console.log("SMOKE OK");
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
