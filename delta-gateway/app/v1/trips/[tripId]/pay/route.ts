import { requireSession } from "@/lib/auth";
import { DEMO_TRIP_ID } from "@/lib/constants";
import { bearerToken, error, noContent, options } from "@/lib/http";
import { ensureTripState, isPaid, markPaid } from "@/lib/store";

export const runtime = "nodejs";

export function OPTIONS() {
  return options();
}

export async function POST(
  req: Request,
  ctx: { params: Promise<{ tripId: string }> },
) {
  const record = requireSession(bearerToken(req));
  if (!record) return error(401, "Unauthorized.", "unauthorized");
  const { tripId } = await ctx.params;
  if (tripId !== DEMO_TRIP_ID) return error(404, "Trip not found.", "not_found");

  let body: { quoteId?: string };
  try {
    body = (await req.json()) as { quoteId?: string };
  } catch {
    return error(400, "Invalid JSON body.");
  }
  const quoteId = body.quoteId?.trim();
  if (!quoteId) return error(400, "quoteId is required.");

  const state = ensureTripState(record, tripId);
  const quote = state.quotes[quoteId];
  if (!quote) return error(400, "Unknown payment quote.");
  if (quote.totalCents > 0 && !isPaid(state, quoteId)) {
    markPaid(state, quoteId);
  }
  return noContent();
}
