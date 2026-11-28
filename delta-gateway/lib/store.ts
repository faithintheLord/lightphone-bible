import { get, put } from "@vercel/blob";
import { DEMO_TRIP_ID } from "./constants";
import type { BoardingPass, PaymentQuote, Session, SessionRecord, TripState } from "./types";

type GlobalStore = {
  sessions: Map<string, SessionRecord>;
  pairingCodes: Map<string, { createdAt: number; used?: boolean }>;
};

function memoryStore(): GlobalStore {
  const g = globalThis as typeof globalThis & { __deltaGatewayStore?: GlobalStore };
  if (!g.__deltaGatewayStore) {
    g.__deltaGatewayStore = {
      sessions: new Map(),
      pairingCodes: new Map(),
    };
  }
  return g.__deltaGatewayStore;
}

function blobEnabled(): boolean {
  return Boolean(process.env.BLOB_READ_WRITE_TOKEN);
}

function sessionPath(sid: string): string {
  return `sessions/${sid}.json`;
}

function emptyTripState(): TripState {
  return {
    quotes: {},
    paid: [],
    checkedIn: false,
  };
}

function normalizeRecord(record: SessionRecord): SessionRecord {
  for (const trip of Object.values(record.trips)) {
    trip.quotes = trip.quotes ?? {};
    trip.paid = Array.isArray(trip.paid) ? trip.paid : [];
    trip.checkedIn = Boolean(trip.checkedIn);
  }
  return record;
}

export async function putSession(record: SessionRecord): Promise<void> {
  const normalized = normalizeRecord(record);
  memoryStore().sessions.set(normalized.sid, normalized);
  memoryStore().sessions.set(normalized.session.token, normalized);

  if (!blobEnabled()) return;

  await put(sessionPath(normalized.sid), JSON.stringify(normalized), {
    access: "private",
    addRandomSuffix: false,
    allowOverwrite: true,
    contentType: "application/json",
    token: process.env.BLOB_READ_WRITE_TOKEN,
  });
}

export async function getSessionBySid(sid: string): Promise<SessionRecord | null> {
  const mem = memoryStore().sessions.get(sid);
  if (mem) return normalizeRecord(mem);

  if (!blobEnabled()) return null;

  try {
    const result = await get(sessionPath(sid), {
      access: "private",
      token: process.env.BLOB_READ_WRITE_TOKEN,
      useCache: false,
    });
    if (!result?.stream) return null;
    const text = await new Response(result.stream).text();
    const parsed = JSON.parse(text) as SessionRecord;
    const normalized = normalizeRecord(parsed);
    memoryStore().sessions.set(normalized.sid, normalized);
    memoryStore().sessions.set(normalized.session.token, normalized);
    return normalized;
  } catch {
    return null;
  }
}

export async function getSession(token: string): Promise<SessionRecord | null> {
  const mem = memoryStore().sessions.get(token);
  if (mem) return normalizeRecord(mem);
  return null;
}

export function ensureTripState(record: SessionRecord, tripId: string): TripState {
  if (!record.trips[tripId]) {
    record.trips[tripId] = emptyTripState();
  }
  const state = record.trips[tripId];
  state.paid = Array.isArray(state.paid) ? state.paid : [];
  state.quotes = state.quotes ?? {};
  return state;
}

export async function createDemoSession(
  sid: string,
  token: string,
  session: Session,
): Promise<SessionRecord> {
  const record: SessionRecord = {
    sid,
    session,
    mode: "demo",
    createdAt: Date.now(),
    trips: {
      [DEMO_TRIP_ID]: emptyTripState(),
    },
  };
  await putSession(record);
  return record;
}

export async function createPairedSession(
  sid: string,
  token: string,
  session: Session,
): Promise<SessionRecord> {
  const record: SessionRecord = {
    sid,
    session,
    mode: "paired",
    createdAt: Date.now(),
    trips: {
      [DEMO_TRIP_ID]: emptyTripState(),
    },
  };
  await putSession(record);
  return record;
}

export function saveQuote(state: TripState, quote: PaymentQuote): void {
  state.quotes[quote.id] = quote;
}

export function markPaid(state: TripState, quoteId: string): void {
  if (!state.paid.includes(quoteId)) {
    state.paid = [...state.paid, quoteId];
  }
}

export function isPaid(state: TripState, quoteId: string): boolean {
  return state.paid.includes(quoteId);
}

export function setBoardingPass(state: TripState, pass: BoardingPass): void {
  state.checkedIn = true;
  state.boardingPass = pass;
}

export function mintPairingCode(): string {
  const code = Array.from({ length: 6 }, () => Math.floor(Math.random() * 10)).join("");
  memoryStore().pairingCodes.set(code, { createdAt: Date.now() });
  return code;
}

export function consumePairingCode(code: string): boolean {
  const store = memoryStore();
  const entry = store.pairingCodes.get(code.trim());
  if (!entry || entry.used) return false;
  if (Date.now() - entry.createdAt > 15 * 60 * 1000) {
    store.pairingCodes.delete(code);
    return false;
  }
  entry.used = true;
  return true;
}

export function storageMode(): "blob" | "memory" {
  return blobEnabled() ? "blob" : "memory";
}
