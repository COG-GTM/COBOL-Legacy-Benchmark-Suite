import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { fetchAllPortfolios, type PortfolioPosition } from "../api/portfolio";
import ErrorBanner from "../components/ErrorBanner";

/**
 * Portfolio Position Inquiry — replaces POSMAP BMS screen (INQSET.bms lines 23–49).
 * Maps to WHEN 'INQP' branch in INQONLN.cbl which calls INQPORT.
 *
 * Layout from BMS:
 *   Row 1: "Portfolio Position Inquiry" (PROT, BRT)
 *   Row 3: Account input (ACCTIN)
 *   Row 5: Fund ID (FUNDOUT) + Fund Name (NAMEOUT) — COLOR=TURQUOISE
 *   Row 7: Units (UNITOUT) — COLOR=TURQUOISE
 *   Row 9: Cost Basis (COSTOUT) — COLOR=TURQUOISE
 *   Row 11: Market Value (VALOUT) — COLOR=TURQUOISE
 *   Row 22: "PF3=Exit  PF7=Previous  PF8=Next"
 *   Row 23: Message (POSMSG, COLOR=RED)
 */
export default function PortfolioPage() {
  const navigate = useNavigate();
  const [positions, setPositions] = useState<PortfolioPosition[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [currentIdx, setCurrentIdx] = useState(0);

  useEffect(() => {
    fetchAllPortfolios()
      .then(setPositions)
      .catch(() => setError("Failed to fetch portfolio data"))
      .finally(() => setLoading(false));
  }, []);

  const current = positions[currentIdx];

  return (
    <div style={containerStyle}>
      <div style={terminalStyle}>
        {/* Row 1: Title */}
        <h1 style={titleStyle}>Portfolio Position Inquiry</h1>

        {loading ? (
          <div style={loadingStyle}>Loading portfolio data...</div>
        ) : positions.length === 0 ? (
          <div style={loadingStyle}>No portfolio positions found.</div>
        ) : (
          <>
            {/* Row 3: Account */}
            <div style={fieldRowStyle}>
              <span style={labelStyle}>Account:</span>
              <span style={valueStyle}>{current.accountNo}</span>
            </div>

            {/* Row 5: Fund ID + Fund Name */}
            <div style={fieldRowStyle}>
              <span style={labelStyle}>Fund ID:</span>
              <span style={valueStyle}>{current.fundId}</span>
              <span style={{ ...labelStyle, marginLeft: "24px" }}>
                Fund Name:
              </span>
              <span style={valueStyle}>{current.fundName}</span>
            </div>

            {/* Row 7: Units */}
            <div style={fieldRowStyle}>
              <span style={labelStyle}>Units:</span>
              <span style={valueStyle}>{current.units.toFixed(3)}</span>
            </div>

            {/* Row 9: Cost Basis */}
            <div style={fieldRowStyle}>
              <span style={labelStyle}>Cost Basis:</span>
              <span style={valueStyle}>
                ${current.costBasis.toLocaleString("en-US", { minimumFractionDigits: 2 })}
              </span>
            </div>

            {/* Row 11: Market Value */}
            <div style={fieldRowStyle}>
              <span style={labelStyle}>Market Value:</span>
              <span style={valueStyle}>
                ${current.marketValue.toLocaleString("en-US", { minimumFractionDigits: 2 })}
              </span>
            </div>

            {/* Gain/Loss indicator */}
            <div style={fieldRowStyle}>
              <span style={labelStyle}>Gain/Loss:</span>
              <span
                style={{
                  ...valueStyle,
                  color:
                    current.marketValue >= current.costBasis
                      ? "#22c55e"
                      : "#ef4444",
                }}
              >
                ${(current.marketValue - current.costBasis).toLocaleString("en-US", {
                  minimumFractionDigits: 2,
                })}
                {" "}
                ({(
                  ((current.marketValue - current.costBasis) /
                    current.costBasis) *
                  100
                ).toFixed(2)}
                %)
              </span>
            </div>
          </>
        )}

        {/* Row 22: Navigation — PF3=Exit  PF7=Previous  PF8=Next */}
        <div style={navRowStyle}>
          <button onClick={() => navigate("/menu")} style={navButtonStyle}>
            Exit (PF3)
          </button>
          <div style={{ display: "flex", gap: "12px" }}>
            <button
              onClick={() => setCurrentIdx((i) => Math.max(0, i - 1))}
              disabled={currentIdx === 0}
              style={{
                ...navButtonStyle,
                opacity: currentIdx === 0 ? 0.4 : 1,
                cursor: currentIdx === 0 ? "not-allowed" : "pointer",
              }}
            >
              Previous (PF7)
            </button>
            <button
              onClick={() =>
                setCurrentIdx((i) => Math.min(positions.length - 1, i + 1))
              }
              disabled={currentIdx >= positions.length - 1}
              style={{
                ...navButtonStyle,
                opacity: currentIdx >= positions.length - 1 ? 0.4 : 1,
                cursor:
                  currentIdx >= positions.length - 1
                    ? "not-allowed"
                    : "pointer",
              }}
            >
              Next (PF8)
            </button>
          </div>
        </div>

        {/* Row 23: Message area (POSMSG) */}
        {error && <ErrorBanner message={error} onDismiss={() => setError("")} />}
      </div>
    </div>
  );
}

const containerStyle: React.CSSProperties = {
  minHeight: "100vh",
  backgroundColor: "#0a1929",
  display: "flex",
  justifyContent: "center",
  alignItems: "center",
  padding: "20px",
};

const terminalStyle: React.CSSProperties = {
  backgroundColor: "#0d2137",
  border: "2px solid #1e3a5f",
  borderRadius: "8px",
  padding: "32px 40px",
  maxWidth: "700px",
  width: "100%",
  boxShadow: "0 4px 24px rgba(0, 0, 0, 0.4)",
};

const titleStyle: React.CSSProperties = {
  color: "#ffffff",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "22px",
  fontWeight: "bold",
  marginBottom: "24px",
};

const loadingStyle: React.CSSProperties = {
  color: "#a0c4e8",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "14px",
  padding: "20px 0",
};

const fieldRowStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: "8px",
  marginBottom: "16px",
  flexWrap: "wrap",
};

const labelStyle: React.CSSProperties = {
  color: "#a0c4e8",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "14px",
  minWidth: "110px",
};

const valueStyle: React.CSSProperties = {
  color: "#00b7c3",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "14px",
};

const navRowStyle: React.CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  marginTop: "32px",
  paddingTop: "16px",
  borderTop: "1px solid #1e3a5f",
};

const navButtonStyle: React.CSSProperties = {
  padding: "8px 20px",
  backgroundColor: "#1e3a5f",
  color: "#ffffff",
  border: "none",
  borderRadius: "4px",
  cursor: "pointer",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "13px",
  fontWeight: "bold",
};
