import type { BoardingPass, PaymentQuote, Session, SessionRecord, TripState } from "./types";
import { DEMO_TRIP_ID } from "./constants";

type GlobalStore = {
  sessions: Map<string, SessionRecord>;
  pairingCodes: Map<string, { createdAt: number; used?: boolean }>;
};

function globalStore(): GlobalStore {
  const g = globalThis as typeof globalThis & { __deltaGatewayStore?: GlobalStore };
  if (!g.__deltaGatewayStore) {
    g.__deltaGatewayStore = {
      sessions: new Map(),
      pairingCodes: new Map(),
    };
  }
  return g.__deltaGatewayStore;
}

function emptyTripState(): TripState {
  return {
    quotes: {},
    paid: new Set(),
    checkedIn: false,
  };
}

function normalizePaid(paid: Set<string> | string[]): Set<string> {
  return paid instanceof Set ? paid : new Set(paid);
}

export function putSession(record: SessionRecord): void {
  const store = globalStore();
  store.sessions.set(record.session.token, record);
}

export function getSession(token: string): SessionRecord | null {
  return globalStore().sessions.get(token) ?? null;
}

export function ensureTripState(record: SessionRecord, tripId: string): TripState {
  if (!record.trips[tripId]) {
    record.trips[tripId] = emptyTripState();
  }
  const state = record.trips[tripId];
  state.paid = normalizePaid(state.paid);
  return state;
}

export function createDemoSession(token: string, session: Session): SessionRecord {
  const record: SessionRecord = {
    session,
    mode: "demo",
    createdAt: Date.now(),
    trips: {
      [DEMO_TRIP_ID]: emptyTripState(),
    },
  };
  putSession(record);
  return record;
}

export function createPairedSession(token: string, session: Session): SessionRecord {
  const record: SessionRecord = {
    session,
    mode: "paired",
    createdAt: Date.now(),
    trips: {
      [DEMO_TRIP_ID]: emptyTripState(),
    },
  };
  putSession(record);
  return record;
}

export function saveQuote(state: TripState, quote: PaymentQuote): void {
  state.quotes[quote.id] = quote;
}

export function markPaid(state: TripState, quoteId: string): void {
  const paid = normalizePaid(state.paid);
  paid.add(quoteId);
  state.paid = paid;
}

export function isPaid(state: TripState, quoteId: string): boolean {
  return normalizePaid(state.paid).has(quoteId);
}

export function setBoardingPass(state: TripState, pass: BoardingPass): void {
  state.checkedIn = true;
  state.boardingPass = pass;
}

export function mintPairingCode(): string {
  const code = Array.from({ length: 6 }, () => Math.floor(Math.random() * 10)).join("");
  globalStore().pairingCodes.set(code, { createdAt: Date.now() });
  return code;
}

export function consumePairingCode(code: string): boolean {
  const store = globalStore();
  const entry = store.pairingCodes.get(code.trim());
  if (!entry || entry.used) return false;
  if (Date.now() - entry.createdAt > 15 * 60 * 1000) {
    store.pairingCodes.delete(code);
    return false;
  }
  entry.used = true;
  return true;
}
