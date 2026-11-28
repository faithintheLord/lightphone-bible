import { requireSession } from "@/lib/auth";
import { demoTrip } from "@/lib/demo";
import { DEMO_TRIP_ID } from "@/lib/constants";
import { bearerToken, error, json, options } from "@/lib/http";
import { ensureTripState } from "@/lib/store";

export const runtime = "nodejs";

export function OPTIONS() {
  return options();
}

export async function GET(req: Request) {
  const record = await requireSession(bearerToken(req));
  if (!record) return error(401, "Unauthorized.", "unauthorized");

  const state = ensureTripState(record, DEMO_TRIP_ID);
  return json({ trips: [demoTrip(state.checkedIn)] });
}
