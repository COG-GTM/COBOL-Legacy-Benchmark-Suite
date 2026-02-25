import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import ErrorBanner from "../components/ErrorBanner";

/**
 * Main Menu — replaces MENMAP BMS screen (INQSET.bms lines 7–19).
 * Maps to WHEN 'MENU' branch in INQONLN.cbl (lines 62–77).
 *
 * Layout from BMS:
 *   Row 1: "Portfolio Management System" (PROT, BRT)
 *   Row 3: "Select Option:" (PROT)
 *   Row 5: "1. Portfolio Position Inquiry"
 *   Row 6: "2. Transaction History"
 *   Row 7: "3. Exit"
 *   Row 9: Option input field (UNPROT, NUM, IC)
 *   Row 23: Error message (ERRMSG, COLOR=RED)
 */
export default function MenuPage() {
  const navigate = useNavigate();
  const { logout, userId } = useAuth();
  const [option, setOption] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = () => {
    setError("");
    switch (option) {
      case "1":
        navigate("/portfolio");
        break;
      case "2":
        navigate("/history");
        break;
      case "3":
        logout();
        navigate("/login");
        break;
      default:
        setError("Invalid option. Please enter 1, 2, or 3.");
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      handleSubmit();
    }
  };

  return (
    <div style={containerStyle}>
      <div style={terminalStyle}>
        {/* Row 1: Title */}
        <h1 style={titleStyle}>Portfolio Management System</h1>

        {/* User info */}
        <div style={userInfoStyle}>User: {userId}</div>

        {/* Row 3: Select Option */}
        <div style={promptStyle}>Select Option:</div>

        {/* Menu options */}
        <div style={menuStyle}>
          <div
            style={menuItemStyle}
            onClick={() => {
              setOption("1");
              navigate("/portfolio");
            }}
          >
            1. Portfolio Position Inquiry
          </div>
          <div
            style={menuItemStyle}
            onClick={() => {
              setOption("2");
              navigate("/history");
            }}
          >
            2. Transaction History
          </div>
          <div
            style={menuItemStyle}
            onClick={() => {
              setOption("3");
              logout();
              navigate("/login");
            }}
          >
            3. Exit
          </div>
        </div>

        {/* Option input */}
        <div style={inputRowStyle}>
          <label style={inputLabelStyle}>Option:</label>
          <input
            type="text"
            maxLength={1}
            value={option}
            onChange={(e) => setOption(e.target.value)}
            onKeyDown={handleKeyDown}
            style={inputStyle}
            autoFocus
          />
          <button onClick={handleSubmit} style={submitButtonStyle}>
            Enter
          </button>
        </div>

        {/* Row 23: Error message area */}
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
  maxWidth: "600px",
  width: "100%",
  boxShadow: "0 4px 24px rgba(0, 0, 0, 0.4)",
};

const titleStyle: React.CSSProperties = {
  color: "#ffffff",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "22px",
  fontWeight: "bold",
  marginBottom: "8px",
};

const userInfoStyle: React.CSSProperties = {
  color: "#888",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "13px",
  marginBottom: "24px",
};

const promptStyle: React.CSSProperties = {
  color: "#a0c4e8",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "16px",
  marginBottom: "16px",
};

const menuStyle: React.CSSProperties = {
  marginLeft: "16px",
  marginBottom: "24px",
};

const menuItemStyle: React.CSSProperties = {
  color: "#00b7c3",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "16px",
  padding: "8px 12px",
  cursor: "pointer",
  borderRadius: "4px",
  transition: "background-color 0.15s",
};

const inputRowStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: "12px",
  marginTop: "8px",
};

const inputLabelStyle: React.CSSProperties = {
  color: "#a0c4e8",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "16px",
};

const inputStyle: React.CSSProperties = {
  width: "40px",
  padding: "8px 12px",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "16px",
  backgroundColor: "#0a1929",
  color: "#00b7c3",
  border: "1px solid #1e3a5f",
  borderRadius: "4px",
  textAlign: "center",
};

const submitButtonStyle: React.CSSProperties = {
  padding: "8px 24px",
  backgroundColor: "#1e3a5f",
  color: "#ffffff",
  border: "none",
  borderRadius: "4px",
  cursor: "pointer",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "14px",
  fontWeight: "bold",
};
