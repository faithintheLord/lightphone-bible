import { requireSession } from "@/lib/auth";
import { DEMO_TRIP_ID } from "@/lib/constants";
import { bagSelection } from "@/lib/demo";
import { bearerToken, error, json, options } from "@/lib/http";
import { ensureTripState, putSession } from "@/lib/store";

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

  let body: { count?: number };
  try {
    body = (await req.json()) as { count?: number };
  } catch {
    return error(400, "Invalid JSON body.");
  }
  const count = typeof body.count === "number" ? body.count : 0;
  const selection = bagSelection(count);
  const state = ensureTripState(record, tripId);
  state.bagCount = selection.count;
  await putSession(record);
  return json(selection);
}
