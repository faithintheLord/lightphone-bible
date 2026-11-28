import { requireSession } from "@/lib/auth";
import { DEMO_TRIP_ID } from "@/lib/constants";
import { buildBoardingPass } from "@/lib/demo";
import { bearerToken, error, json, options } from "@/lib/http";
import { ensureTripState } from "@/lib/store";

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
  if (!state.checkedIn) {
    return error(403, "Not eligible.", "not_eligible");
  }
  const pass =
    state.boardingPass ??
    buildBoardingPass(record.session, state.seatCode || "24A");
  return json(pass);
}
