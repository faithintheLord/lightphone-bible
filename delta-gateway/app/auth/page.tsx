"use client";

import { FormEvent, useState, type CSSProperties } from "react";

export default function AuthPage() {
  const [displayName, setDisplayName] = useState("Warren");
  const [skymilesNumber, setSkymilesNumber] = useState("");
  const [token, setToken] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setToken(null);
    try {
      const res = await fetch("/auth/start", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ displayName, skymilesNumber }),
      });
      const data = (await res.json()) as { token?: string; error?: string };
      if (!res.ok || !data.token) {
        setError(data.error || "Pairing failed");
        return;
      }
      setToken(data.token);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Pairing failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main style={{ maxWidth: 560, margin: "0 auto", padding: "48px 20px" }}>
      <a href="/" style={{ color: "#8ec7ff", textDecoration: "none" }}>
        ← Gateway
      </a>
      <h1 style={{ fontSize: 32, margin: "16px 0" }}>Browser pairing</h1>
      <p style={{ opacity: 0.85, lineHeight: 1.5 }}>
        Creates a short-lived gateway session for the phone. This does <strong>not</strong> log into
        delta.com and does not scrape. When Delta partner APIs are approved, this page becomes the
        OAuth/PKCE landing step and the gateway will hold the partner session server-side.
      </p>

      <form onSubmit={onSubmit} style={{ marginTop: 24, display: "grid", gap: 12 }}>
        <label style={{ display: "grid", gap: 6 }}>
          Display name
          <input
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            required
            style={inputStyle}
          />
        </label>
        <label style={{ display: "grid", gap: 6 }}>
          SkyMiles number (optional label)
          <input
            value={skymilesNumber}
            onChange={(e) => setSkymilesNumber(e.target.value)}
            inputMode="numeric"
            style={inputStyle}
          />
        </label>
        <button
          type="submit"
          disabled={busy}
          style={{
            marginTop: 8,
            padding: "12px 16px",
            borderRadius: 8,
            border: 0,
            background: "#3d7ea6",
            color: "white",
            fontWeight: 600,
            cursor: "pointer",
          }}
        >
          {busy ? "Creating…" : "Create gateway session"}
        </button>
      </form>

      {error && <p style={{ color: "#ff8e8e", marginTop: 16 }}>{error}</p>}

      {token && (
        <section style={{ marginTop: 24, padding: 16, background: "#182029", borderRadius: 12 }}>
          <h2 style={{ marginTop: 0, fontSize: 16 }}>Session token</h2>
          <p style={{ opacity: 0.8, fontSize: 14 }}>
            For now the phone still signs in via <code>POST /v1/login</code> with demo credentials.
            Keep this token for partner/OAuth wiring tests (Authorization bearer).
          </p>
          <code
            style={{
              display: "block",
              padding: 12,
              background: "#0f1419",
              borderRadius: 8,
              wordBreak: "break-all",
              fontSize: 12,
            }}
          >
            {token}
          </code>
        </section>
      )}
    </main>
  );
}

const inputStyle: CSSProperties = {
  padding: "10px 12px",
  borderRadius: 8,
  border: "1px solid #31404f",
  background: "#0f1419",
  color: "#e8eef4",
};
