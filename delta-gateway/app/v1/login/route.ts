import { loginWithPassword } from "@/lib/auth";
import { error, json, options } from "@/lib/http";

export const runtime = "nodejs";

export function OPTIONS() {
  return options();
}

export async function POST(req: Request) {
  let body: { skymilesNumber?: string; password?: string };
  try {
    body = (await req.json()) as { skymilesNumber?: string; password?: string };
  } catch {
    return error(400, "Invalid JSON body.");
  }

  const skymilesNumber = (body.skymilesNumber ?? "").trim();
  const password = body.password ?? "";
  if (!skymilesNumber || !password) {
    return error(400, "skymilesNumber and password are required.", "invalid_request");
  }

  const session = await loginWithPassword(skymilesNumber, password);
  if (!session) {
    return error(401, "Invalid credentials.", "invalid_credentials");
  }
  return json(session);
}
