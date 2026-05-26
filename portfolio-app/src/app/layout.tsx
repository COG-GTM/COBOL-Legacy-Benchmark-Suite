import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Portfolio Management System",
  description:
    "Investment Portfolio Management — migrated from COBOL/VSAM/DB2 to Next.js/Redis/PostgreSQL",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <header className="header">
          <nav className="nav">
            <a href="/portfolios" className="nav-brand">
              Portfolio Management System
            </a>
            <div className="nav-links">
              <a href="/portfolios">Portfolios</a>
              <a href="/portfolios/new">New Portfolio</a>
            </div>
          </nav>
        </header>
        <main className="main">{children}</main>
      </body>
    </html>
  );
}
