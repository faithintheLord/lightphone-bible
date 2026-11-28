import { requireSession } from "@/lib/auth";
import { DEMO_TRIP_ID } from "@/lib/constants";
import { buildQuote } from "@/lib/demo";
import { bearerToken, error, json, options } from "@/lib/http";
import { ensureTripState, putSession, saveQuote } from "@/lib/store";
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
  if (draft.upgradeId) state.upgradeId = draft.upgradeId;
  if (draft.seatCode) state.seatCode = draft.seatCode;
  if (typeof draft.bagCount === "number") state.bagCount = draft.bagCount;

  const quote = buildQuote(tripId, draft, state);
  saveQuote(state, quote);
  await putSession(record);
  return json(quote);
}
