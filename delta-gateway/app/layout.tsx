import type { Metadata } from "next";
import type { ReactNode } from "react";

export const metadata: Metadata = {
  title: "Light Phone Delta Gateway",
  description: "HTTPS /v1 gateway for the Light Phone III Trips tool",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body
        style={{
          margin: 0,
          fontFamily: "ui-sans-serif, system-ui, -apple-system, Segoe UI, sans-serif",
          background: "#0f1419",
          color: "#e8eef4",
          minHeight: "100vh",
        }}
      >
        {children}
      </body>
    </html>
  );
}
