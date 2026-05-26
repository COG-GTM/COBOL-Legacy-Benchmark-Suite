import Link from "next/link";
import { getPortfolio } from "@/app/actions/portfolio";
import { DeleteButton } from "./delete-button";

const STATUS_LABELS: Record<string, string> = {
  A: "Active",
  C: "Closed",
  S: "Suspended",
};

const CLIENT_TYPE_LABELS: Record<string, string> = {
  I: "Individual",
  C: "Corporate",
  T: "Trust",
};

export default async function PortfolioDetailPage({
  params,
}: {
  params: { id: string };
}) {
  const result = await getPortfolio(params.id);

  if (!result.success || !result.data) {
    return (
      <div>
        <h1 className="page-title">Portfolio Not Found</h1>
        <div className="alert alert-error">
          {result.error ?? "Portfolio not found"}
        </div>
        <Link href="/portfolios" className="btn btn-secondary">
          Back to Portfolios
        </Link>
      </div>
    );
  }

  const p = result.data;
  const statusClass =
    p.status === "A"
      ? "badge-active"
      : p.status === "C"
        ? "badge-closed"
        : "badge-suspended";

  return (
    <div>
      <div className="top-bar">
        <h1 className="page-title">Portfolio {p.portfolio_id}</h1>
        <div className="btn-group" style={{ marginTop: 0 }}>
          <Link
            href={`/portfolios/${p.portfolio_id}/edit`}
            className="btn btn-primary"
          >
            Edit
          </Link>
          <DeleteButton portfolioId={p.portfolio_id} />
        </div>
      </div>

      <div className="card">
        <div className="detail-grid">
          <div className="detail-item">
            <dt>Portfolio ID</dt>
            <dd>{p.portfolio_id}</dd>
          </div>
          <div className="detail-item">
            <dt>Account Number</dt>
            <dd>{p.account_no}</dd>
          </div>
          <div className="detail-item">
            <dt>Client Name</dt>
            <dd>{p.client_name}</dd>
          </div>
          <div className="detail-item">
            <dt>Client Type</dt>
            <dd>{CLIENT_TYPE_LABELS[p.client_type] ?? p.client_type}</dd>
          </div>
          <div className="detail-item">
            <dt>Portfolio Name</dt>
            <dd>{p.portfolio_name || "—"}</dd>
          </div>
          <div className="detail-item">
            <dt>Status</dt>
            <dd>
              <span className={`badge ${statusClass}`}>
                {STATUS_LABELS[p.status] ?? p.status}
              </span>
            </dd>
          </div>
          <div className="detail-item">
            <dt>Currency</dt>
            <dd>{p.currency_code}</dd>
          </div>
          <div className="detail-item">
            <dt>Risk Level</dt>
            <dd>{p.risk_level || "—"}</dd>
          </div>
          <div className="detail-item">
            <dt>Branch ID</dt>
            <dd>{p.branch_id || "—"}</dd>
          </div>
          <div className="detail-item">
            <dt>Total Value</dt>
            <dd>
              {parseFloat(p.total_value || "0").toLocaleString("en-US", {
                style: "currency",
                currency: p.currency_code || "USD",
              })}
            </dd>
          </div>
          <div className="detail-item">
            <dt>Cash Balance</dt>
            <dd>
              {parseFloat(p.cash_balance || "0").toLocaleString("en-US", {
                style: "currency",
                currency: p.currency_code || "USD",
              })}
            </dd>
          </div>
          <div className="detail-item">
            <dt>Open Date</dt>
            <dd>{p.open_date || "—"}</dd>
          </div>
          <div className="detail-item">
            <dt>Close Date</dt>
            <dd>{p.close_date || "—"}</dd>
          </div>
          <div className="detail-item">
            <dt>Last Updated By</dt>
            <dd>{p.updated_by}</dd>
          </div>
          <div className="detail-item">
            <dt>Created At</dt>
            <dd>{p.created_at || "—"}</dd>
          </div>
          <div className="detail-item">
            <dt>Updated At</dt>
            <dd>{p.updated_at || "—"}</dd>
          </div>
        </div>
      </div>

      <Link href="/portfolios" className="btn btn-secondary">
        Back to Portfolios
      </Link>
    </div>
  );
}
