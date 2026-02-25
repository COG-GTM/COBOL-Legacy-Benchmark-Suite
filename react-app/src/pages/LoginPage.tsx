/**
 * LoginPage — simple username/password form.
 * Replaces SECMGR P100-VALIDATE-USER credential validation.
 */

import { useState, type FormEvent } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const [userId, setUserId] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from =
    (location.state as { from?: { pathname: string } })?.from?.pathname ||
    "/menu";

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);

    const success = await login(userId, password);
    setLoading(false);

    if (success) {
      navigate(from, { replace: true });
    } else {
      setError("User validation failed — invalid credentials");
    }
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h1 style={styles.title}>Portfolio Management System</h1>
        <p style={styles.subtitle}>Sign In</p>

        <form onSubmit={handleSubmit} style={styles.form}>
          <div style={styles.field}>
            <label htmlFor="userId" style={styles.label}>
              User ID:
            </label>
            <input
              id="userId"
              type="text"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              style={styles.input}
              maxLength={8}
              autoFocus
              required
            />
          </div>

          <div style={styles.field}>
            <label htmlFor="password" style={styles.label}>
              Password:
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              style={styles.input}
              required
            />
          </div>

          {error && <div style={styles.error}>{error}</div>}

          <button type="submit" disabled={loading} style={styles.button}>
            {loading ? "Authenticating..." : "Login"}
          </button>
        </form>

        <p style={styles.hint}>
          Enter any user ID and password to sign in
        </p>
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
    width: 380,
    color: "#e0e0e0",
  },
  title: {
    margin: "0 0 4px",
    fontSize: 22,
    color: "#00d4ff",
    textAlign: "center" as const,
  },
  subtitle: {
    margin: "0 0 28px",
    fontSize: 14,
    color: "#8899aa",
    textAlign: "center" as const,
  },
  form: {
    display: "flex",
    flexDirection: "column" as const,
    gap: 16,
  },
  field: {
    display: "flex",
    flexDirection: "column" as const,
    gap: 4,
  },
  label: {
    fontSize: 13,
    color: "#aabbcc",
  },
  input: {
    padding: "10px 12px",
    borderRadius: 4,
    border: "1px solid #334455",
    background: "#0f3460",
    color: "#e0e0e0",
    fontSize: 14,
    outline: "none",
  },
  error: {
    color: "#ff6b6b",
    fontSize: 13,
    textAlign: "center" as const,
  },
  button: {
    padding: "12px",
    borderRadius: 4,
    border: "none",
    background: "#00d4ff",
    color: "#1a1a2e",
    fontSize: 15,
    fontWeight: 600,
    cursor: "pointer",
    marginTop: 8,
  },
  hint: {
    marginTop: 20,
    fontSize: 12,
    color: "#556677",
    textAlign: "center" as const,
  },
};
