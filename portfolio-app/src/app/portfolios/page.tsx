import Link from "next/link";
import { listPortfolios } from "@/app/actions/portfolio";

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

function StatusBadge({ status }: { status: string }) {
  const cls =
    status === "A"
      ? "badge badge-active"
      : status === "C"
        ? "badge badge-closed"
        : "badge badge-suspended";
  return <span className={cls}>{STATUS_LABELS[status] ?? status}</span>;
}

export default async function PortfoliosPage({
  searchParams,
}: {
  searchParams: { status?: string; clientType?: string; search?: string; page?: string };
}) {
  const page = parseInt(searchParams.page ?? "1", 10);
  const result = await listPortfolios({
    status: searchParams.status || undefined,
    clientType: searchParams.clientType || undefined,
    search: searchParams.search || undefined,
    page,
    pageSize: 20,
  });

  const data = result.data;

  return (
    <div>
      <div className="top-bar">
        <h1 className="page-title">Portfolios</h1>
        <Link href="/portfolios/new" className="btn btn-primary">
          New Portfolio
        </Link>
      </div>

      <div className="filter-bar">
        <form method="GET" style={{ display: "flex", gap: "1rem", alignItems: "end" }}>
          <div className="form-group">
            <label htmlFor="status">Status</label>
            <select id="status" name="status" defaultValue={searchParams.status ?? ""}>
              <option value="">All</option>
              <option value="A">Active</option>
              <option value="C">Closed</option>
              <option value="S">Suspended</option>
            </select>
          </div>
          <div className="form-group">
            <label htmlFor="clientType">Client Type</label>
            <select id="clientType" name="clientType" defaultValue={searchParams.clientType ?? ""}>
              <option value="">All</option>
              <option value="I">Individual</option>
              <option value="C">Corporate</option>
              <option value="T">Trust</option>
            </select>
          </div>
          <div className="form-group">
            <label htmlFor="search">Search Name</label>
            <input
              id="search"
              name="search"
              type="text"
              placeholder="Client name..."
              defaultValue={searchParams.search ?? ""}
            />
          </div>
          <button type="submit" className="btn btn-primary">
            Filter
          </button>
        </form>
      </div>

      {!result.success && (
        <div className="alert alert-error">{result.error}</div>
      )}

      {result.success && data && data.items.length === 0 && (
        <div className="empty-state">
          <p>No portfolios found.</p>
          <Link href="/portfolios/new" className="btn btn-primary" style={{ marginTop: "1rem" }}>
            Create your first portfolio
          </Link>
        </div>
      )}

      {result.success && data && data.items.length > 0 && (
        <>
          <div className="card" style={{ padding: 0, overflow: "hidden" }}>
            <table>
              <thead>
                <tr>
                  <th>Portfolio ID</th>
                  <th>Account No</th>
                  <th>Client Name</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Total Value</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.items.map((p) => (
                  <tr key={p.portfolio_id}>
                    <td>
                      <Link href={`/portfolios/${p.portfolio_id}`}>
                        {p.portfolio_id}
                      </Link>
                    </td>
                    <td>{p.account_no}</td>
                    <td>{p.client_name}</td>
                    <td>{CLIENT_TYPE_LABELS[p.client_type] ?? p.client_type}</td>
                    <td>
                      <StatusBadge status={p.status} />
                    </td>
                    <td>
                      {parseFloat(p.total_value || "0").toLocaleString("en-US", {
                        style: "currency",
                        currency: p.currency_code || "USD",
                      })}
                    </td>
                    <td>
                      <div className="btn-group" style={{ marginTop: 0 }}>
                        <Link
                          href={`/portfolios/${p.portfolio_id}`}
                          className="btn btn-secondary"
                        >
                          View
                        </Link>
                        <Link
                          href={`/portfolios/${p.portfolio_id}/edit`}
                          className="btn btn-secondary"
                        >
                          Edit
                        </Link>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="pagination">
            {page > 1 && (
              <Link
                href={`/portfolios?page=${page - 1}${searchParams.status ? `&status=${searchParams.status}` : ""}${searchParams.clientType ? `&clientType=${searchParams.clientType}` : ""}${searchParams.search ? `&search=${searchParams.search}` : ""}`}
                className="btn btn-secondary"
              >
                Previous
              </Link>
            )}
            <span className="btn btn-secondary" style={{ cursor: "default" }}>
              Page {page} of {Math.ceil((data.total || 1) / data.pageSize)}
            </span>
            {page * data.pageSize < data.total && (
              <Link
                href={`/portfolios?page=${page + 1}${searchParams.status ? `&status=${searchParams.status}` : ""}${searchParams.clientType ? `&clientType=${searchParams.clientType}` : ""}${searchParams.search ? `&search=${searchParams.search}` : ""}`}
                className="btn btn-secondary"
              >
                Next
              </Link>
            )}
          </div>
        </>
      )}
    </div>
  );
}
