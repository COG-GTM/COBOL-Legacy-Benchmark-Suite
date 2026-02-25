/**
 * ErrorPage — system error display.
 * Replaces ERRMAP in INQSET.bms (lines 89-100).
 *
 * Layout:
 *   Title: "System Error"
 *   Error Code (ERRCOUT) — shown in red
 *   Details (ERRDOUT) — shown in red
 *   "Press ENTER to continue" button
 */

import { useNavigate, useSearchParams } from "react-router-dom";

export default function ErrorPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();

  const errorCode = params.get("code") || "ERR-UNKNOWN";
  const errorDetails =
    params.get("details") || "An unexpected system error occurred.";

  function handleContinue() {
    navigate("/menu");
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h1 style={styles.title}>System Error</h1>

        <div style={styles.field}>
          <span style={styles.label}>Error Code:</span>
          <span style={styles.errorValue}>{errorCode}</span>
        </div>

        <div style={styles.field}>
          <span style={styles.label}>Details:</span>
          <span style={styles.errorValue}>{errorDetails}</span>
        </div>

        <div style={styles.nav}>
          <button style={styles.btn} onClick={handleContinue}>
            Press ENTER to continue
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
    alignItems: "center",
    minHeight: "100vh",
    background: "#1a1a2e",
    fontFamily: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif",
  },
  card: {
    background: "#16213e",
    borderRadius: 8,
    padding: "40px 48px",
    boxShadow: "0 4px 24px rgba(0,0,0,0.4)",
    width: 520,
    color: "#e0e0e0",
  },
  title: {
    margin: "0 0 28px",
    fontSize: 22,
    color: "#00d4ff",
  },
  field: {
    marginBottom: 20,
  },
  label: {
    display: "block",
    fontSize: 13,
    color: "#aabbcc",
    marginBottom: 4,
  },
  errorValue: {
    display: "block",
    fontSize: 16,
    color: "#ff6b6b",
    fontWeight: 600,
  },
  nav: {
    marginTop: 32,
  },
  btn: {
    padding: "12px 24px",
    borderRadius: 4,
    border: "none",
    background: "#00d4ff",
    color: "#1a1a2e",
    fontSize: 15,
    fontWeight: 600,
    cursor: "pointer",
  },
};
