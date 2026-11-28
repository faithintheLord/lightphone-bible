import { createPairedMemberSession } from "@/lib/auth";
import { error, json, options } from "@/lib/http";

export const runtime = "nodejs";

export function OPTIONS() {
  return options();
}

export async function POST(req: Request) {
  let body: { displayName?: string; skymilesNumber?: string };
  try {
    body = (await req.json()) as { displayName?: string; skymilesNumber?: string };
  } catch {
    return error(400, "Invalid JSON body.");
  }

  const displayName = (body.displayName ?? "").trim() || "SkyMiles Member";
  const skymilesNumber = (body.skymilesNumber ?? "").trim();
  const session = createPairedMemberSession(displayName, skymilesNumber);
  return json({
    token: session.token,
    skymilesMasked: session.skymilesMasked,
    displayName: session.displayName,
    medallionTier: session.medallionTier,
    note: "Gateway-held session for future partner OAuth. Phone login for the demo fixture remains POST /v1/login with 1234567890 / demo.",
  });
}
