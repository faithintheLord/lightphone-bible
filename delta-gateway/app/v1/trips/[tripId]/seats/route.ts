import { requireSession } from "@/lib/auth";
import { DEMO_TRIP_ID, TAKEN_SEATS } from "@/lib/constants";
import { seatMap } from "@/lib/demo";
import { bearerToken, error, json, noContent, options } from "@/lib/http";
import { ensureTripState, putSession } from "@/lib/store";

export const runtime = "nodejs";

export function OPTIONS() {
  return options();
}

export async function GET(
  req: Request,
  ctx: { params: Promise<{ tripId: string }> },
) {
  const record = await requireSession(bearerToken(req));
  if (!record) return error(401, "Unauthorized.", "unauthorized");
  const { tripId } = await ctx.params;
  if (tripId !== DEMO_TRIP_ID) return error(404, "Trip not found.", "not_found");

  const state = ensureTripState(record, tripId);
  return json(seatMap(state.seatCode || "24A"));
}

export async function POST(
  req: Request,
  ctx: { params: Promise<{ tripId: string }> },
) {
  const record = await requireSession(bearerToken(req));
  if (!record) return error(401, "Unauthorized.", "unauthorized");
  const { tripId } = await ctx.params;
  if (tripId !== DEMO_TRIP_ID) return error(404, "Trip not found.", "not_found");

  let body: { seatCode?: string };
  try {
    body = (await req.json()) as { seatCode?: string };
  } catch {
    return error(400, "Invalid JSON body.");
  }
  const seatCode = body.seatCode?.trim().toUpperCase();
  if (!seatCode) return error(400, "seatCode is required.");
  if (TAKEN_SEATS.has(seatCode)) {
    return error(409, "Seat taken.", "seat_taken");
  }

  const state = ensureTripState(record, tripId);
  state.seatCode = seatCode;
  await putSession(record);
  return noContent();
}
