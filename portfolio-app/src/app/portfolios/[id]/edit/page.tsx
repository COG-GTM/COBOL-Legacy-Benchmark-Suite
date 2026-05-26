"use client";

import { useEffect, useState } from "react";
import { useFormState, useFormStatus } from "react-dom";
import { useParams } from "next/navigation";
import {
  getPortfolio,
  updatePortfolio,
  type ActionResult,
} from "@/app/actions/portfolio";
import type { PortfolioHash } from "@/lib/redis";

const initialState: ActionResult = { success: false };

function SubmitButton() {
  const { pending } = useFormStatus();
  return (
    <button type="submit" className="btn btn-primary" disabled={pending}>
      {pending ? "Updating..." : "Update Portfolio"}
    </button>
  );
}

export default function EditPortfolioPage() {
  const params = useParams<{ id: string }>();
  const [portfolio, setPortfolio] = useState<PortfolioHash | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const [state, formAction] = useFormState(
    async (_prev: ActionResult, formData: FormData) => {
      return updatePortfolio(formData);
    },
    initialState
  );

  useEffect(() => {
    async function load() {
      const result = await getPortfolio(params.id);
      if (result.success && result.data) {
        setPortfolio(result.data);
      } else {
        setLoadError(result.error ?? "Portfolio not found");
      }
      setLoading(false);
    }
    load();
  }, [params.id]);

  if (loading) {
    return <div className="card">Loading...</div>;
  }

  if (loadError || !portfolio) {
    return (
      <div>
        <h1 className="page-title">Edit Portfolio</h1>
        <div className="alert alert-error">{loadError}</div>
        <a href="/portfolios" className="btn btn-secondary">
          Back to Portfolios
        </a>
      </div>
    );
  }

  return (
    <div>
      <h1 className="page-title">Edit Portfolio {portfolio.portfolio_id}</h1>

      {state.error && <div className="alert alert-error">{state.error}</div>}
      {state.success && (
        <div className="alert alert-success">
          Portfolio updated successfully!{" "}
          <a href={`/portfolios/${portfolio.portfolio_id}`}>View portfolio</a>
        </div>
      )}

      <form action={formAction} className="card">
        <input
          type="hidden"
          name="portfolio_id"
          value={portfolio.portfolio_id}
        />
        <input type="hidden" name="updated_by" value="WEBUSER" />

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="portfolio_id_display">Portfolio ID</label>
            <input
              id="portfolio_id_display"
              type="text"
              value={portfolio.portfolio_id}
              disabled
            />
          </div>
          <div className="form-group">
            <label htmlFor="account_no_display">Account Number</label>
            <input
              id="account_no_display"
              type="text"
              value={portfolio.account_no}
              disabled
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
              defaultValue={portfolio.client_name}
              maxLength={30}
            />
          </div>
          <div className="form-group">
            <label htmlFor="client_type">Client Type</label>
            <select
              id="client_type"
              name="client_type"
              defaultValue={portfolio.client_type}
            >
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
              defaultValue={portfolio.portfolio_name}
              maxLength={50}
            />
          </div>
          <div className="form-group">
            <label htmlFor="status">Status</label>
            <select
              id="status"
              name="status"
              defaultValue={portfolio.status}
            >
              <option value="A">Active</option>
              <option value="C">Closed</option>
              <option value="S">Suspended</option>
            </select>
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="currency_code">Currency</label>
            <select
              id="currency_code"
              name="currency_code"
              defaultValue={portfolio.currency_code}
            >
              <option value="USD">USD</option>
              <option value="EUR">EUR</option>
              <option value="GBP">GBP</option>
              <option value="JPY">JPY</option>
            </select>
          </div>
          <div className="form-group">
            <label htmlFor="risk_level">Risk Level</label>
            <select
              id="risk_level"
              name="risk_level"
              defaultValue={portfolio.risk_level}
            >
              <option value="">Select...</option>
              <option value="L">Low</option>
              <option value="M">Medium</option>
              <option value="H">High</option>
            </select>
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="branch_id">Branch ID</label>
            <input
              id="branch_id"
              name="branch_id"
              type="text"
              defaultValue={portfolio.branch_id}
              maxLength={2}
            />
          </div>
          <div className="form-group">
            <label htmlFor="total_value">Total Value</label>
            <input
              id="total_value"
              name="total_value"
              type="text"
              defaultValue={portfolio.total_value}
            />
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="cash_balance">Cash Balance</label>
            <input
              id="cash_balance"
              name="cash_balance"
              type="text"
              defaultValue={portfolio.cash_balance}
            />
          </div>
          <div className="form-group">
            <label htmlFor="close_date">Close Date</label>
            <input
              id="close_date"
              name="close_date"
              type="date"
              defaultValue={portfolio.close_date || ""}
            />
          </div>
        </div>

        <div className="btn-group">
          <SubmitButton />
          <a
            href={`/portfolios/${portfolio.portfolio_id}`}
            className="btn btn-secondary"
          >
            Cancel
          </a>
        </div>
      </form>
    </div>
  );
}
