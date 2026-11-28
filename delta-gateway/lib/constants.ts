export const DEMO_SKYMILES = "1234567890";
export const DEMO_PASSWORD = "demo";
export const DEMO_TRIP_ID = "trip-demo-atl-lax";
export const DEMO_CONFIRMATION = "ABC123";
export const TAKEN_SEATS = new Set(["22B", "23C", "25D", "26E"]);

export const DEMO_UPGRADES = [
  { id: "up-comfort", cabin: "Delta Comfort+", priceCents: 8900, currency: "USD" },
  { id: "up-first", cabin: "First Class", priceCents: 29900, currency: "USD" },
] as const;

export const BAG_OFFERS = [
  { index: 1, label: "First bag", priceCents: 0 },
  { index: 2, label: "Second bag", priceCents: 3500 },
  { index: 3, label: "Third bag", priceCents: 4500 },
] as const;
