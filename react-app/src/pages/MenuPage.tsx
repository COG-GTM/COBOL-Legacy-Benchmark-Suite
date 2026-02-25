/**
 * MenuPage — main menu screen.
 * Replaces MENMAP BMS screen and the WHEN 'MENU' branch
 * in INQONLN.cbl (lines 62-77).
 *
 * Options:
 *   1. Portfolio Position Inquiry  → /portfolio
 *   2. Transaction History         → /history
 *   3. Exit                        → logout
 */

import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function MenuPage() {
  const navigate = useNavigate();
  const { logout, userId } = useAuth();

  function handleExit() {
    logout();
    navigate("/login");
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h1 style={styles.title}>Portfolio Management System</h1>
        <p style={styles.user}>Logged in as: {userId}</p>
        <p style={styles.selectLabel}>Select Option:</p>

        <div style={styles.options}>
          <button
            style={styles.option}
            onClick={() => navigate("/portfolio")}
          >
            <span style={styles.optNum}>1.</span> Portfolio Position Inquiry
          </button>

          <button
            style={styles.option}
            onClick={() => navigate("/history")}
          >
            <span style={styles.optNum}>2.</span> Transaction History
          </button>

          <button
            style={{ ...styles.option, ...styles.exitOption }}
            onClick={handleExit}
          >
            <span style={styles.optNum}>3.</span> Exit
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
    width: 440,
    color: "#e0e0e0",
  },
  title: {
    margin: "0 0 8px",
    fontSize: 22,
    color: "#00d4ff",
  },
  user: {
    margin: "0 0 24px",
    fontSize: 13,
    color: "#556677",
  },
  selectLabel: {
    margin: "0 0 16px",
    fontSize: 14,
    color: "#aabbcc",
  },
  options: {
    display: "flex",
    flexDirection: "column" as const,
    gap: 12,
  },
  option: {
    display: "flex",
    alignItems: "center",
    gap: 8,
    padding: "14px 18px",
    borderRadius: 6,
    border: "1px solid #334455",
    background: "#0f3460",
    color: "#e0e0e0",
    fontSize: 15,
    cursor: "pointer",
    textAlign: "left" as const,
    transition: "background 0.15s",
  },
  exitOption: {
    borderColor: "#553333",
    background: "#2a1a2e",
  },
  optNum: {
    color: "#00d4ff",
    fontWeight: 700,
  },
};
