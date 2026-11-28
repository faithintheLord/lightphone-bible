import {
  BAG_OFFERS,
  DEMO_CONFIRMATION,
  DEMO_TRIP_ID,
  DEMO_UPGRADES,
  TAKEN_SEATS,
} from "./constants";
import type {
  BagSelection,
  BoardingPass,
  CheckInDraft,
  PaymentLine,
  PaymentQuote,
  SeatMapDto,
  Session,
  Trip,
  TripState,
  UpgradeOffer,
} from "./types";

export function demoTrip(checkedIn: boolean): Trip {
  return {
    id: DEMO_TRIP_ID,
    confirmationCode: DEMO_CONFIRMATION,
    flights: [
      {
        id: "seg-1",
        origin: "Atlanta (ATL)",
        destination: "Los Angeles (LAX)",
        departLocal: "Fri 7:30 AM",
        arriveLocal: "Fri 9:15 AM PT",
        flightNumber: "DL 1042",
        aircraft: "A321",
      },
    ],
    checkInEligible: !checkedIn,
    checkInComplete: checkedIn,
  };
}

export function listUpgrades(): UpgradeOffer[] {
  return DEMO_UPGRADES.map((o) => ({ ...o }));
}

export function seatMap(currentSeat: string): SeatMapDto {
  const rows = [];
  for (let row = 20; row <= 28; row++) {
    const letters = ["A", "B", "C", "D", "E", "F"];
    rows.push({
      rowNumber: row,
      seats: letters.map((letter) => {
        const code = `${row}${letter}`;
        if (code === currentSeat) return { code, state: "current" as const };
        if (TAKEN_SEATS.has(code)) return { code, state: "taken" as const };
        if (letter === "A" || letter === "F") {
          return { code, state: "available" as const, premiumCents: 2500 };
        }
        return { code, state: "available" as const, premiumCents: 0 };
      }),
    });
  }
  return { rows };
}

export function bagSelection(count: number): BagSelection {
  const clamped = Math.max(0, Math.min(3, count));
  const total =
    clamped <= 1 ? 0 : clamped === 2 ? 3500 : 3500 + 4500;
  return {
    count: clamped,
    offers: BAG_OFFERS.map((o) => ({ ...o })),
    totalCents: total,
  };
}

export function buildQuote(tripId: string, draft: CheckInDraft, state: TripState): PaymentQuote {
  const lines: PaymentLine[] = [];
  if (draft.upgradeId === "up-comfort") {
    lines.push({ description: "Comfort+ upgrade", amountCents: 8900 });
  } else if (draft.upgradeId === "up-first") {
    lines.push({ description: "First Class upgrade", amountCents: 29900 });
  }

  const seatCode = draft.seatCode || state.seatCode || "24A";
  if (seatCode.endsWith("A") || seatCode.endsWith("F")) {
    lines.push({ description: `Preferred seat ${seatCode}`, amountCents: 2500 });
  }

  const bagCount = state.bagCount ?? draft.bagCount ?? 0;
  const bagTotal = bagCount <= 1 ? 0 : bagCount === 2 ? 3500 : 8000;
  if (bagTotal > 0) {
    lines.push({ description: "Checked bags", amountCents: bagTotal });
  }

  const totalCents = lines.reduce((sum, line) => sum + line.amountCents, 0);
  return {
    id: `quote-${tripId}-${lines.length}-${totalCents}`,
    lines,
    totalCents,
    cardLast4: "4242",
  };
}

export function buildBoardingPass(session: Session, seat: string): BoardingPass {
  const last = session.displayName.split(" ").slice(-1)[0]?.toUpperCase() || "PASSENGER";
  const first = session.displayName.split(" ")[0]?.toUpperCase() || "GUEST";
  return {
    confirmation: DEMO_CONFIRMATION,
    passenger: session.displayName,
    flightNumber: "DL 1042",
    origin: "ATL",
    destination: "LAX",
    seat,
    zone: "1",
    gate: "B12",
    boardingTime: "6:45 AM",
    barcodePayload: `M1${last}/${first}       E${DEMO_CONFIRMATION} ATLLAXDL 0104214${seat}0001`,
  };
}
