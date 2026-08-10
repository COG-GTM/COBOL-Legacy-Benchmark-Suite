import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { StatusBadge } from '../../components/StatusBadge';
import { usePortfolioService } from '../../services/servicesContext';
import {
  CLIENT_TYPE_LABELS,
  type Portfolio,
} from '../../types/portfolio';
import { formatCurrency } from '../../utils/decimal';
import { formatCobolDate } from '../../utils/date';

export function PortfolioDetailPage() {
  const { id } = useParams<{ id: string }>();
  const service = usePortfolioService();
  const navigate = useNavigate();

  const [portfolio, setPortfolio] = useState<Portfolio | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const result = await service.get(id);
      setPortfolio(result ?? null);
    } catch {
      setError('Unable to load portfolio.');
    } finally {
      setLoading(false);
    }
  }, [id, service]);

  useEffect(() => {
    void load();
  }, [load]);

  const onDelete = async () => {
    if (!id) return;
    setDeleting(true);
    try {
      await service.remove(id);
      navigate('/portfolios');
    } catch {
      setError('Unable to delete portfolio.');
      setConfirmOpen(false);
    } finally {
      setDeleting(false);
    }
  };

  if (loading) {
    return <p className="state-msg">Loading portfolio…</p>;
  }

  if (error) {
    return (
      <section>
        <div className="alert alert--error" role="alert">
          {error}
        </div>
        <Link to="/portfolios" className="btn btn--ghost">
          Back to portfolios
        </Link>
      </section>
    );
  }

  if (!portfolio) {
    return (
      <section>
        <div className="alert alert--error" role="alert">
          Portfolio “{id}” was not found.
        </div>
        <Link to="/portfolios" className="btn btn--ghost">
          Back to portfolios
        </Link>
      </section>
    );
  }

  return (
    <section>
      <div className="breadcrumb">
        <Link to="/portfolios">Portfolios</Link>
        <span aria-hidden="true"> / </span>
        <span>{portfolio.portId}</span>
      </div>

      <div className="page-header">
        <div>
          <h1 className="page-header__title">{portfolio.clientName}</h1>
          <p className="page-header__subtitle">
            {portfolio.portId} · {portfolio.accountNo}
          </p>
        </div>
        <div className="page-header__actions">
          <StatusBadge status={portfolio.status} />
          <Link
            to={`/portfolios/${portfolio.portId}/edit`}
            className="btn btn--primary"
          >
            Edit
          </Link>
          <button
            type="button"
            className="btn btn--danger"
            onClick={() => setConfirmOpen(true)}
          >
            Delete
          </button>
        </div>
      </div>

      <div className="detail-grid">
        <DetailCard title="Client Information">
          <DetailRow label="Portfolio ID (PORT-ID)" value={portfolio.portId} />
          <DetailRow
            label="Account Number (PORT-ACCOUNT-NO)"
            value={portfolio.accountNo}
          />
          <DetailRow
            label="Client Name (PORT-CLIENT-NAME)"
            value={portfolio.clientName}
          />
          <DetailRow
            label="Client Type (PORT-CLIENT-TYPE)"
            value={`${CLIENT_TYPE_LABELS[portfolio.clientType]} (${portfolio.clientType})`}
          />
          <DetailRow
            label="Status (PORT-STATUS)"
            value={<StatusBadge status={portfolio.status} />}
          />
        </DetailCard>

        <DetailCard title="Financial Information">
          <DetailRow
            label="Total Value (PORT-TOTAL-VALUE)"
            value={formatCurrency(portfolio.totalValue)}
          />
          <DetailRow
            label="Cash Balance (PORT-CASH-BALANCE)"
            value={formatCurrency(portfolio.cashBalance)}
          />
        </DetailCard>

        <DetailCard title="Portfolio Dates">
          <DetailRow
            label="Create Date (PORT-CREATE-DATE)"
            value={formatCobolDate(portfolio.createDate)}
          />
          <DetailRow
            label="Last Maintenance (PORT-LAST-MAINT)"
            value={formatCobolDate(portfolio.lastMaintDate)}
          />
        </DetailCard>

        <DetailCard title="Audit Information">
          <DetailRow
            label="Last User (PORT-LAST-USER)"
            value={portfolio.lastUser}
          />
          <DetailRow
            label="Last Transaction (PORT-LAST-TRANS)"
            value={portfolio.lastTransId}
          />
        </DetailCard>
      </div>

      <ConfirmDialog
        open={confirmOpen}
        title="Delete portfolio?"
        message={`This will permanently delete portfolio ${portfolio.portId} (${portfolio.clientName}). This action cannot be undone.`}
        confirmLabel="Delete"
        busy={deleting}
        onConfirm={onDelete}
        onCancel={() => setConfirmOpen(false)}
      />
    </section>
  );
}

function DetailCard({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="card detail-card">
      <h2 className="detail-card__title">{title}</h2>
      <dl className="detail-list">{children}</dl>
    </div>
  );
}

function DetailRow({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className="detail-list__row">
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}
