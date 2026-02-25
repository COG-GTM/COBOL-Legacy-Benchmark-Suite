import { useNavigate, useSearchParams } from "react-router-dom";

/**
 * Error Display — replaces ERRMAP BMS screen (INQSET.bms lines 89–100).
 *
 * Layout from BMS:
 *   Row 1: "System Error" (PROT, BRT)
 *   Row 3: "Error Code:" + ERRCOUT (COLOR=RED)
 *   Row 5: "Details:" + ERRDOUT (COLOR=RED)
 *   Row 22: "Press ENTER to continue"
 */
export default function ErrorPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const errorCode = searchParams.get("code") || "ERR0000";
  const errorDetails =
    searchParams.get("details") || "An unexpected system error has occurred.";

  const handleContinue = () => {
    navigate("/menu");
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      handleContinue();
    }
  };

  return (
    <div style={containerStyle} onKeyDown={handleKeyDown} tabIndex={0}>
      <div style={terminalStyle}>
        {/* Row 1: Title */}
        <h1 style={titleStyle}>System Error</h1>

        {/* Row 3: Error Code — ERRCOUT, COLOR=RED */}
        <div style={fieldRowStyle}>
          <span style={labelStyle}>Error Code:</span>
          <span style={errorValueStyle}>{errorCode}</span>
        </div>

        {/* Row 5: Details — ERRDOUT, COLOR=RED */}
        <div style={fieldRowStyle}>
          <span style={labelStyle}>Details:</span>
          <span style={errorValueStyle}>{errorDetails}</span>
        </div>

        {/* Row 22: Press ENTER to continue */}
        <div style={continueRowStyle}>
          <button
            onClick={handleContinue}
            style={continueButtonStyle}
            autoFocus
          >
            Press ENTER to continue
          </button>
        </div>
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
  outline: "none",
};

const terminalStyle: React.CSSProperties = {
  backgroundColor: "#0d2137",
  border: "2px solid #ef4444",
  borderRadius: "8px",
  padding: "32px 40px",
  maxWidth: "600px",
  width: "100%",
  boxShadow: "0 4px 24px rgba(239, 68, 68, 0.2)",
};

const titleStyle: React.CSSProperties = {
  color: "#ef4444",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "22px",
  fontWeight: "bold",
  marginBottom: "32px",
};

const fieldRowStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "flex-start",
  gap: "12px",
  marginBottom: "20px",
};

const labelStyle: React.CSSProperties = {
  color: "#a0c4e8",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "14px",
  minWidth: "100px",
  flexShrink: 0,
};

const errorValueStyle: React.CSSProperties = {
  color: "#ef4444",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "14px",
  fontWeight: "bold",
};

const continueRowStyle: React.CSSProperties = {
  marginTop: "48px",
  textAlign: "center",
};

const continueButtonStyle: React.CSSProperties = {
  padding: "12px 32px",
  backgroundColor: "#1e3a5f",
  color: "#ffffff",
  border: "1px solid #1e3a5f",
  borderRadius: "4px",
  cursor: "pointer",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "14px",
  fontWeight: "bold",
};
