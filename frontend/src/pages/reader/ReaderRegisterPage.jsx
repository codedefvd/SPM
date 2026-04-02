import { useState } from "react";
import { authApi } from "../../api/auth";

export function ReaderRegisterPage({ onGoLogin }) {
  const [form, setForm] = useState({
    username: "",
    password: "",
    fullName: "",
    studentNo: "",
    phone: "",
  });

  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("");

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async () => {
    if (!form.username || !form.password || !form.fullName || !form.studentNo || !form.phone) {
      setMessageType("error");
      setMessage("Please complete all fields.");
      return;
    }

    try {
      setSubmitting(true);
      setMessage("");
      await authApi.register(form);

      setMessageType("success");
      setMessage("Register success. Please go to login.");
    } catch (error) {
      setMessageType("error");
      setMessage(error?.message || "Register failed.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page-stack">
      <section className="page-card">
        <div className="section-head">
          <div>
            <span className="eyebrow">Reader Access</span>
            <h2 className="section-title">Reader Register</h2>
          </div>
          <p className="page-note">
            Create a new reader account for browsing, borrowing, and reservation access.
          </p>
        </div>

        <div className="table-wrap" style={{ padding: "16px 0" }}>
          <div style={{ display: "grid", gap: "16px", maxWidth: "520px" }}>
            <div>
              <label>Username</label>
              <input
                name="username"
                value={form.username}
                onChange={handleChange}
                placeholder="Enter username"
                style={inputStyle}
              />
            </div>

            <div>
              <label>Password</label>
              <input
                type="password"
                name="password"
                value={form.password}
                onChange={handleChange}
                placeholder="Enter password"
                style={inputStyle}
              />
            </div>

            <div>
              <label>Full Name</label>
              <input
                name="fullName"
                value={form.fullName}
                onChange={handleChange}
                placeholder="Enter full name"
                style={inputStyle}
              />
            </div>

            <div>
              <label>Student No</label>
              <input
                name="studentNo"
                value={form.studentNo}
                onChange={handleChange}
                placeholder="Enter student number"
                style={inputStyle}
              />
            </div>

            <div>
              <label>Phone</label>
              <input
                name="phone"
                value={form.phone}
                onChange={handleChange}
                placeholder="Enter phone number"
                style={inputStyle}
              />
            </div>

            {message ? (
              <div
                style={{
                  color: messageType === "success" ? "#67c23a" : "#f56c6c",
                  fontSize: "14px",
                }}
              >
                {message}
              </div>
            ) : null}

            <div className="inline-actions">
              <button
                className="primary-button"
                type="button"
                onClick={handleSubmit}
                disabled={submitting}
              >
                {submitting ? "Registering..." : "Register"}
              </button>
              <button
                className="secondary-button"
                type="button"
                onClick={onGoLogin}
              >
                Go Login
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

const inputStyle = {
  width: "100%",
  marginTop: "8px",
  padding: "10px 12px",
  border: "1px solid #dcdfe6",
  borderRadius: "8px",
  fontSize: "14px",
  boxSizing: "border-box",
};