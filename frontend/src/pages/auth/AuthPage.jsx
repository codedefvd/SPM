import { useState } from "react";
import { authApi } from "../../api/auth";

export function AuthPage({ onLogin, onGoRegister }) {
  const [username, setUsername] = useState("reader");
  const [password, setPassword] = useState("123456");
  const [role, setRole] = useState("READER");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      if (role === "READER") {
        const result = await authApi.login({ username, password });
        onLogin({ username: result.username, role, token: result.token });
        return;
      }

      onLogin({
        username,
        role,
        token: `mock-token-for-${username}`,
      });
    } catch (requestError) {
      setError(requestError.message || "Login failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <section className="auth-hero">
          <span className="eyebrow">Frontend Collaboration</span>
          <h1>Build By Feature, Not By Chaos.</h1>
          <p>
            This workspace is intentionally structured so your team can split pages by business feature while still
            sharing one layout, one API style, and one visual language.
          </p>
          <div className="auth-points">
            <div className="auth-point">Reader, Librarian, and Admin each have their own page space.</div>
            <div className="auth-point">Reader login already calls the backend and stores a usable token.</div>
            <div className="auth-point">Librarian and Admin can use the seeded demo usernames for quick linkage.</div>
          </div>
        </section>

        <section className="auth-panel">
          <span className="eyebrow">Quick Start</span>
          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="field">
              <label>Workspace User</label>
              <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="reader" />
            </div>

            <div className="field">
              <label>Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="123456"
              />
            </div>

            <div className="field">
              <label>Role Workspace</label>
              <select value={role} onChange={(e) => setRole(e.target.value)}>
                <option value="READER">Reader</option>
                <option value="LIBRARIAN">Librarian</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>

            <button className="primary-button" type="submit" disabled={loading}>
              {loading ? "Entering..." : "Enter Workspace"}
            </button>

            {role === "READER" ? (
              <button
                className="secondary-button"
                type="button"
                onClick={onGoRegister}
                style={{ marginTop: "12px", width: "100%" }}
              >
                Go Register
              </button>
            ) : null}
          </form>

          <p className="page-note">
            Demo accounts in seeded SQL: `reader / 123456`, `librarian / 123456`, `admin / 123456`.
          </p>

          {error ? <p className="page-note">{error}</p> : null}
        </section>
      </div>
    </div>
  );
}