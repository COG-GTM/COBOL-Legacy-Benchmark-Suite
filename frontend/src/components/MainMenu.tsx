import { useNavigate } from "react-router-dom";
import { useAuthContext } from "../contexts/AuthContext";
import { useAuth } from "../hooks/useAuth";
import { useErrorContext } from "../contexts/ErrorContext";
import { useState } from "react";

export default function MainMenu() {
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuthContext();
  const { loading, handleLogin, handleLogout } = useAuth();
  const { error, clearError } = useErrorContext();
  const [userId, setUserId] = useState("");
  const [password, setPassword] = useState("");

  const onLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    const success = await handleLogin(userId, password);
    if (success) {
      setUserId("");
      setPassword("");
    }
  };

  if (!isAuthenticated) {
    return (
      <div className="screen">
        <h1 className="screen-title">Portfolio Management System</h1>
        <div className="login-panel">
          <h2>Sign In</h2>
          <form onSubmit={onLogin}>
            <div className="field-row">
              <label htmlFor="userId">User ID:</label>
              <input
                id="userId"
                type="text"
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
                maxLength={8}
                autoFocus
              />
            </div>
            <div className="field-row">
              <label htmlFor="password">Password:</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            <button type="submit" disabled={loading} className="btn btn-primary">
              {loading ? "Signing in..." : "Sign In"}
            </button>
          </form>
          {error && (
            <div className="error-bar" role="alert">
              {error.message}
              <button onClick={clearError} className="btn-dismiss">
                Dismiss
              </button>
            </div>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="screen">
      <h1 className="screen-title">Portfolio Management System</h1>
      <p className="welcome-text">Welcome, {user?.userId}</p>
      <div className="menu-label">Select Option:</div>
      <nav className="menu-options">
        <button
          className="menu-item"
          onClick={() => navigate("/portfolio")}
        >
          1. Portfolio Position Inquiry
        </button>
        <button
          className="menu-item"
          onClick={() => navigate("/history")}
        >
          2. Transaction History
        </button>
        <button className="menu-item" onClick={handleLogout}>
          3. Exit
        </button>
      </nav>
      {error && (
        <div className="error-bar" role="alert">
          {error.message}
          <button onClick={clearError} className="btn-dismiss">
            Dismiss
          </button>
        </div>
      )}
    </div>
  );
}
