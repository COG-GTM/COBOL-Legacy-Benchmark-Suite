"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { deletePortfolio } from "@/app/actions/portfolio";

const REASON_OPTIONS = [
  { value: "01", label: "Account Closed" },
  { value: "02", label: "Transferred" },
  { value: "03", label: "Client Requested" },
];

export function DeleteButton({ portfolioId }: { portfolioId: string }) {
  const router = useRouter();
  const [showModal, setShowModal] = useState(false);
  const [reasonCode, setReasonCode] = useState("01");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);

  const handleDelete = async () => {
    setPending(true);
    setError("");
    const result = await deletePortfolio(portfolioId, reasonCode);
    if (result.success) {
      router.push("/portfolios");
    } else {
      setError(result.error ?? "Delete failed");
      setPending(false);
    }
  };

  if (!showModal) {
    return (
      <button
        type="button"
        className="btn btn-danger"
        onClick={() => setShowModal(true)}
      >
        Delete
      </button>
    );
  }

  return (
    <div
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(0,0,0,0.5)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 50,
      }}
    >
      <div className="card" style={{ maxWidth: 400, width: "100%" }}>
        <h2 style={{ marginBottom: "1rem" }}>Delete Portfolio</h2>
        <p style={{ marginBottom: "1rem", fontSize: "0.875rem" }}>
          Are you sure you want to delete portfolio{" "}
          <strong>{portfolioId}</strong>? This action cannot be undone.
        </p>

        {error && <div className="alert alert-error">{error}</div>}

        <div className="form-group">
          <label htmlFor="reason_code">Reason Code</label>
          <select
            id="reason_code"
            value={reasonCode}
            onChange={(e) => setReasonCode(e.target.value)}
          >
            {REASON_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.value} - {opt.label}
              </option>
            ))}
          </select>
        </div>

        <div className="btn-group">
          <button
            type="button"
            className="btn btn-danger"
            disabled={pending}
            onClick={handleDelete}
          >
            {pending ? "Deleting..." : "Confirm Delete"}
          </button>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => setShowModal(false)}
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}
