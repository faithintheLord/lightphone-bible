import { partnerStatus } from "@/lib/partner";

export const dynamic = "force-dynamic";

export default function HomePage() {
  const partner = partnerStatus();
  return (
    <main style={{ maxWidth: 640, margin: "0 auto", padding: "48px 20px" }}>
      <p style={{ letterSpacing: "0.08em", textTransform: "uppercase", opacity: 0.7, fontSize: 12 }}>
        Light Phone III
      </p>
      <h1 style={{ fontSize: 36, lineHeight: 1.1, margin: "8px 0 16px" }}>Delta gateway</h1>
      <p style={{ opacity: 0.85, lineHeight: 1.5 }}>
        HTTPS JSON API matching <code>HttpDeltaGateway</code> under <code>/v1/*</code>. Paste this
        site&apos;s origin (no path) into Trips → Settings → API base.
      </p>

      <section style={{ marginTop: 32, padding: 20, background: "#182029", borderRadius: 12 }}>
        <h2 style={{ marginTop: 0, fontSize: 18 }}>Phone Settings</h2>
        <p style={{ marginBottom: 8, opacity: 0.85 }}>
          API base = this deployment origin, for example:
        </p>
        <code style={{ display: "block", padding: 12, background: "#0f1419", borderRadius: 8 }}>
          https://&lt;your-deployment&gt;.vercel.app
        </code>
        <p style={{ marginTop: 12, opacity: 0.85 }}>
          Demo login: SkyMiles <strong>1234567890</strong> / password <strong>demo</strong>
        </p>
      </section>

      <section style={{ marginTop: 24, padding: 20, background: "#182029", borderRadius: 12 }}>
        <h2 style={{ marginTop: 0, fontSize: 18 }}>Real flights</h2>
        <p style={{ opacity: 0.85, lineHeight: 1.5 }}>{partner.blockedReason}</p>
        <p style={{ opacity: 0.85, lineHeight: 1.5 }}>
          <strong>Next legal step:</strong> {partner.nextStep}
        </p>
        <p style={{ opacity: 0.85 }}>
          Browser pairing (short-lived gateway session, no delta.com scrape):{" "}
          <a href="/auth" style={{ color: "#8ec7ff" }}>
            /auth
          </a>
        </p>
      </section>

      <section style={{ marginTop: 24, padding: 20, background: "#182029", borderRadius: 12 }}>
        <h2 style={{ marginTop: 0, fontSize: 18 }}>Health</h2>
        <p style={{ opacity: 0.85 }}>
          <a href="/api/health" style={{ color: "#8ec7ff" }}>
            /api/health
          </a>{" "}
          · Partner configured: {partner.configured ? "yes" : "no"}
        </p>
      </section>
    </main>
  );
}
