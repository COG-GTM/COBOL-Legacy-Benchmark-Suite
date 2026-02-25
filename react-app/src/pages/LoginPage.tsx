import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import ErrorBanner from "../components/ErrorBanner";

/**
 * Login Page — maps to SECMGR.cbl SEC-VALIDATE (P100-VALIDATE-USER).
 * Simple username/password form that validates credentials via AuthContext.
 */
export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [userId, setUserId] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    const success = await login(userId, password);
    setLoading(false);

    if (success) {
      navigate("/menu");
    } else {
      setError("User validation failed — invalid credentials.");
    }
  };

  return (
    <div style={containerStyle}>
      <div style={terminalStyle}>
        <h1 style={titleStyle}>Portfolio Management System</h1>
        <div style={subtitleStyle}>CICS Terminal Sign-On</div>

        <form onSubmit={handleSubmit}>
          <div style={fieldRowStyle}>
            <label style={labelStyle} htmlFor="userId">
              User ID:
            </label>
            <input
              id="userId"
              type="text"
              maxLength={8}
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              style={inputStyle}
              autoFocus
              placeholder="Enter User ID"
            />
          </div>

          <div style={fieldRowStyle}>
            <label style={labelStyle} htmlFor="password">
              Password:
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              style={inputStyle}
              placeholder="Enter Password"
            />
          </div>

          <button type="submit" style={submitStyle} disabled={loading}>
            {loading ? "Validating..." : "Sign On"}
          </button>
        </form>

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
  maxWidth: "480px",
  width: "100%",
  boxShadow: "0 4px 24px rgba(0, 0, 0, 0.4)",
};

const titleStyle: React.CSSProperties = {
  color: "#ffffff",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "22px",
  fontWeight: "bold",
  marginBottom: "4px",
};

const subtitleStyle: React.CSSProperties = {
  color: "#888",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "13px",
  marginBottom: "32px",
};

const fieldRowStyle: React.CSSProperties = {
  marginBottom: "20px",
};

const labelStyle: React.CSSProperties = {
  display: "block",
  color: "#a0c4e8",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "14px",
  marginBottom: "6px",
};

const inputStyle: React.CSSProperties = {
  width: "100%",
  padding: "10px 14px",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "14px",
  backgroundColor: "#0a1929",
  color: "#00b7c3",
  border: "1px solid #1e3a5f",
  borderRadius: "4px",
  boxSizing: "border-box",
};

const submitStyle: React.CSSProperties = {
  width: "100%",
  padding: "12px",
  backgroundColor: "#1e3a5f",
  color: "#ffffff",
  border: "none",
  borderRadius: "4px",
  cursor: "pointer",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "15px",
  fontWeight: "bold",
  marginTop: "8px",
};
