import { requireSession } from "@/lib/auth";
import { DEMO_TRIP_ID } from "@/lib/constants";
import { buildBoardingPass, buildQuote } from "@/lib/demo";
import { bearerToken, error, json, options } from "@/lib/http";
import {
  ensureTripState,
  isPaid,
  putSession,
  setBoardingPass,
} from "@/lib/store";
import type { CheckInDraft } from "@/lib/types";

export const runtime = "nodejs";

export function OPTIONS() {
  return options();
}

export async function POST(
  req: Request,
  ctx: { params: Promise<{ tripId: string }> },
) {
  const record = await requireSession(bearerToken(req));
  if (!record) return error(401, "Unauthorized.", "unauthorized");
  const { tripId } = await ctx.params;
  if (tripId !== DEMO_TRIP_ID) return error(404, "Trip not found.", "not_found");

  let draft: CheckInDraft;
  try {
    draft = (await req.json()) as CheckInDraft;
  } catch {
    return error(400, "Invalid JSON body.");
  }

  const state = ensureTripState(record, tripId);
  if (draft.seatCode) state.seatCode = draft.seatCode;
  if (draft.upgradeId) state.upgradeId = draft.upgradeId;
  if (typeof draft.bagCount === "number") state.bagCount = draft.bagCount;

  // Chargeable selections require a matching, known, paid quote — including when
  // paymentQuoteId is missing or unknown.
  const expected = buildQuote(tripId, draft, state);
  if (expected.totalCents > 0) {
    const quoteId = draft.paymentQuoteId?.trim();
    const quote = quoteId ? state.quotes[quoteId] : undefined;
    if (
      !quote ||
      quote.totalCents !== expected.totalCents ||
      !isPaid(state, quote.id)
    ) {
      return error(402, "Payment required.", "payment_declined");
    }
  }

  const seat = draft.seatCode || state.seatCode || "24A";
  const pass = buildBoardingPass(record.session, seat);
  setBoardingPass(state, pass);
  await putSession(record);
  return json(pass);
}
