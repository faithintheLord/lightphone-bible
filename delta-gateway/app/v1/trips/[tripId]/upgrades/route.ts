import { requireSession } from "@/lib/auth";
import { DEMO_TRIP_ID } from "@/lib/constants";
import { listUpgrades } from "@/lib/demo";
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
  return json({ offers: listUpgrades() });
}

export async function POST(
  req: Request,
  ctx: { params: Promise<{ tripId: string }> },
) {
  const record = await requireSession(bearerToken(req));
  if (!record) return error(401, "Unauthorized.", "unauthorized");
  const { tripId } = await ctx.params;
  if (tripId !== DEMO_TRIP_ID) return error(404, "Trip not found.", "not_found");

  let body: { offerId?: string };
  try {
    body = (await req.json()) as { offerId?: string };
  } catch {
    return error(400, "Invalid JSON body.");
  }
  const offerId = body.offerId?.trim();
  if (!offerId) return error(400, "offerId is required.");

  const offers = listUpgrades();
  if (!offers.some((o) => o.id === offerId)) {
    return error(400, "Unknown upgrade offer.", "not_eligible");
  }

  const state = ensureTripState(record, tripId);
  state.upgradeId = offerId;
  await putSession(record);
  return noContent();
}
