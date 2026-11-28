export type Session = {
  token: string;
  skymilesMasked: string;
  displayName: string;
  medallionTier: string;
};

export type FlightSegment = {
  id: string;
  origin: string;
  destination: string;
  departLocal: string;
  arriveLocal: string;
  flightNumber: string;
  aircraft: string;
};

export type Trip = {
  id: string;
  confirmationCode: string;
  flights: FlightSegment[];
  checkInEligible: boolean;
  checkInComplete: boolean;
};

export type UpgradeOffer = {
  id: string;
  cabin: string;
  priceCents: number;
  currency: string;
};

export type SeatDto = {
  code: string;
  state: "available" | "taken" | "current";
  premiumCents?: number;
};

export type SeatRowDto = {
  rowNumber: number;
  seats: SeatDto[];
};

export type SeatMapDto = {
  rows: SeatRowDto[];
};

export type BagOffer = {
  index: number;
  label: string;
  priceCents: number;
};

export type BagSelection = {
  count: number;
  offers: BagOffer[];
  totalCents: number;
};

export type PaymentLine = {
  description: string;
  amountCents: number;
};

export type PaymentQuote = {
  id: string;
  lines: PaymentLine[];
  totalCents: number;
  cardLast4: string;
};

export type CheckInDraft = {
  tripId: string;
  upgradeId?: string | null;
  seatCode?: string | null;
  bagCount?: number;
  paymentQuoteId?: string | null;
};

export type BoardingPass = {
  confirmation: string;
  passenger: string;
  flightNumber: string;
  origin: string;
  destination: string;
  seat: string;
  zone: string;
  gate: string;
  boardingTime: string;
  barcodePayload: string;
};

export type TripState = {
  upgradeId?: string;
  seatCode?: string;
  bagCount?: number;
  quotes: Record<string, PaymentQuote>;
  paid: Set<string> | string[];
  checkedIn: boolean;
  boardingPass?: BoardingPass;
};

export type SessionRecord = {
  session: Session;
  mode: "demo" | "paired";
  createdAt: number;
  trips: Record<string, TripState>;
};
