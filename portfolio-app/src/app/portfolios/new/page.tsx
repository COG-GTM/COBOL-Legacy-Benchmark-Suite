"use client";

import { useFormState, useFormStatus } from "react-dom";
import { createPortfolio } from "@/app/actions/portfolio";
import type { ActionResult } from "@/app/actions/portfolio";
import type { PortfolioHash } from "@/lib/redis";

const initialState: ActionResult<PortfolioHash> = { success: false };

function SubmitButton() {
  const { pending } = useFormStatus();
  return (
    <button type="submit" className="btn btn-primary" disabled={pending}>
      {pending ? "Creating..." : "Create Portfolio"}
    </button>
  );
}

export default function NewPortfolioPage() {
  const [state, formAction] = useFormState(
    async (_prev: ActionResult<PortfolioHash>, formData: FormData) => {
      return createPortfolio(formData);
    },
    initialState
  );

  return (
    <div>
      <h1 className="page-title">Create New Portfolio</h1>

      {state.error && <div className="alert alert-error">{state.error}</div>}
      {state.success && (
        <div className="alert alert-success">
          Portfolio created successfully!{" "}
          <a href={`/portfolios/${state.data?.portfolio_id}`}>View portfolio</a>
        </div>
      )}

      <form action={formAction} className="card">
        <div className="form-row">
          <div className="form-group">
            <label htmlFor="portfolio_id">Portfolio ID</label>
            <input
              id="portfolio_id"
              name="portfolio_id"
              type="text"
              placeholder="PORT0001"
              maxLength={8}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="account_no">Account Number</label>
            <input
              id="account_no"
              name="account_no"
              type="text"
              placeholder="1234567890"
              maxLength={10}
              required
            />
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="client_name">Client Name</label>
            <input
              id="client_name"
              name="client_name"
              type="text"
              maxLength={30}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="client_type">Client Type</label>
            <select id="client_type" name="client_type" required>
              <option value="">Select...</option>
              <option value="I">Individual</option>
              <option value="C">Corporate</option>
              <option value="T">Trust</option>
            </select>
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="portfolio_name">Portfolio Name</label>
            <input
              id="portfolio_name"
              name="portfolio_name"
              type="text"
              maxLength={50}
            />
          </div>
          <div className="form-group">
            <label htmlFor="currency_code">Currency</label>
            <select id="currency_code" name="currency_code" defaultValue="USD">
              <option value="USD">USD</option>
              <option value="EUR">EUR</option>
              <option value="GBP">GBP</option>
              <option value="JPY">JPY</option>
            </select>
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="risk_level">Risk Level</label>
            <select id="risk_level" name="risk_level">
              <option value="">Select...</option>
              <option value="L">Low</option>
              <option value="M">Medium</option>
              <option value="H">High</option>
            </select>
          </div>
          <div className="form-group">
            <label htmlFor="branch_id">Branch ID</label>
            <input
              id="branch_id"
              name="branch_id"
              type="text"
              maxLength={2}
            />
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="total_value">Total Value</label>
            <input
              id="total_value"
              name="total_value"
              type="text"
              defaultValue="0"
            />
          </div>
          <div className="form-group">
            <label htmlFor="cash_balance">Cash Balance</label>
            <input
              id="cash_balance"
              name="cash_balance"
              type="text"
              defaultValue="0"
            />
          </div>
        </div>

        <input type="hidden" name="status" value="A" />
        <input type="hidden" name="updated_by" value="WEBUSER" />

        <div className="btn-group">
          <SubmitButton />
          <a href="/portfolios" className="btn btn-secondary">
            Cancel
          </a>
        </div>
      </form>
    </div>
  );
}
