/**
 * PortfolioPage — portfolio positions view.
 * Replaces POSMAP BMS screen + INQPORT program logic.
 * Maps to the WHEN 'INQP' branch in INQONLN.cbl.
 *
 * Displays: Account, Fund ID, Fund Name, Units, Cost Basis, Market Value.
 */

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { fetchDefaultPortfolio, type PortfolioPosition } from "../api/portfolio";
import ErrorBanner from "../components/ErrorBanner";

export default function PortfolioPage() {
  const [positions, setPositions] = useState<PortfolioPosition[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    fetchDefaultPortfolio()
      .then((data) => {
        setPositions(data);
        setLoading(false);
      })
      .catch(() => {
        setError("Failed to retrieve portfolio data");
        setLoading(false);
      });
  }, []);

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h1 style={styles.title}>Portfolio Position Inquiry</h1>

        <div style={styles.accountRow}>
          <span style={styles.label}>Account:</span>
          <span style={styles.value}>
            {positions[0]?.accountNumber ?? "—"}
          </span>
        </div>

        {error && <ErrorBanner message={error} />}

        {loading ? (
          <p style={styles.loading}>Loading positions...</p>
        ) : (
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Fund ID</th>
                <th style={styles.th}>Fund Name</th>
                <th style={{ ...styles.th, textAlign: "right" }}>Units</th>
                <th style={{ ...styles.th, textAlign: "right" }}>
                  Cost Basis
                </th>
                <th style={{ ...styles.th, textAlign: "right" }}>
                  Market Value
                </th>
              </tr>
            </thead>
            <tbody>
              {positions.map((p) => (
                <tr key={p.fundId}>
                  <td style={styles.td}>{p.fundId}</td>
                  <td style={styles.td}>{p.fundName}</td>
                  <td style={{ ...styles.td, textAlign: "right" }}>
                    {p.units.toFixed(3)}
                  </td>
                  <td style={{ ...styles.td, textAlign: "right" }}>
                    ${p.costBasis.toLocaleString("en-US", { minimumFractionDigits: 2 })}
                  </td>
                  <td style={{ ...styles.td, textAlign: "right" }}>
                    ${p.marketValue.toLocaleString("en-US", { minimumFractionDigits: 2 })}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <div style={styles.nav}>
          <span style={styles.navHint}>PF3=Exit</span>
          <button style={styles.btn} onClick={() => navigate("/menu")}>
            Exit
          </button>
        </div>

      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: "flex",
    justifyContent: "center",
    alignItems: "flex-start",
    minHeight: "100vh",
    padding: "40px 16px",
    background: "#0F172A",
    fontFamily: "Inter, Roboto, 'Segoe UI', system-ui, sans-serif",
  },
  card: {
    background: "#1E293B",
    borderRadius: 12,
    padding: "32px 40px",
    boxShadow: "0 4px 24px rgba(0,0,0,0.4)",
    width: "100%",
    maxWidth: 800,
    color: "#FFFFFF",
  },
  title: {
    margin: "0 0 20px",
    fontSize: 20,
    color: "#22D3EE",
  },
  accountRow: {
    marginBottom: 20,
    fontSize: 14,
  },
  label: {
    color: "#94A3B8",
    marginRight: 8,
  },
  value: {
    color: "#22D3EE",
  },
  loading: {
    color: "#94A3B8",
    fontSize: 14,
  },
  table: {
    width: "100%",
    borderCollapse: "collapse" as const,
    marginBottom: 24,
  },
  th: {
    textAlign: "left" as const,
    padding: "10px 12px",
    borderBottom: "2px solid #334155",
    color: "#E2E8F0",
    fontSize: 13,
    fontWeight: 600,
  },
  td: {
    padding: "10px 12px",
    borderBottom: "1px solid #243449",
    fontSize: 14,
    color: "#22D3EE",
  },
  nav: {
    display: "flex",
    alignItems: "center",
    gap: 16,
  },
  navHint: {
    fontSize: 12,
    color: "#94A3B8",
  },
  btn: {
    padding: "8px 20px",
    borderRadius: 12,
    border: "1px solid #334155",
    background: "#243449",
    color: "#FFFFFF",
    fontSize: 13,
    cursor: "pointer",
  },
};
