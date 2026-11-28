import { partnerStatus } from "@/lib/partner";
import { json, options } from "@/lib/http";

export const runtime = "nodejs";

export function OPTIONS() {
  return options();
}

export async function GET() {
  const partner = partnerStatus();
  return json({
    ok: true,
    service: "lightphone-delta-gateway",
    contract: "/v1/*",
    demoLogin: { skymilesNumber: "1234567890", password: "demo" },
    partner,
  });
}
