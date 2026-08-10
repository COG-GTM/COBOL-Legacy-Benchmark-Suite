import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { usePortfolioService } from '../../services/servicesContext';
import {
  CLIENT_TYPES,
  CLIENT_TYPE_LABELS,
  PORTFOLIO_FIELD_LENGTHS,
  PORTFOLIO_STATUSES,
  PORTFOLIO_STATUS_LABELS,
  type ClientType,
  type PortfolioInput,
  type PortfolioStatus,
} from '../../types/portfolio';
import { DuplicatePortfolioError } from '../../services/portfolioService';
import {
  hasErrors,
  validatePortfolio,
  type PortfolioErrors,
} from './validation';

const EMPTY_FORM: PortfolioInput = {
  portId: '',
  accountNo: '',
  clientName: '',
  clientType: 'I',
  status: 'A',
  totalValue: '',
  cashBalance: '',
};

export function PortfolioFormPage({ mode }: { mode: 'create' | 'edit' }) {
  const isCreate = mode === 'create';
  const { id } = useParams<{ id: string }>();
  const service = usePortfolioService();
  const navigate = useNavigate();

  const [values, setValues] = useState<PortfolioInput>(EMPTY_FORM);
  const [errors, setErrors] = useState<PortfolioErrors>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [loading, setLoading] = useState(!isCreate);
  const [saving, setSaving] = useState(false);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (isCreate || !id) return;
    let active = true;
    setLoading(true);
    service
      .get(id)
      .then((p) => {
        if (!active) return;
        if (!p) {
          setNotFound(true);
          return;
        }
        setValues({
          portId: p.portId,
          accountNo: p.accountNo,
          clientName: p.clientName,
          clientType: p.clientType,
          status: p.status,
          totalValue: p.totalValue,
          cashBalance: p.cashBalance,
        });
      })
      .catch(() => active && setSubmitError('Unable to load portfolio.'))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [id, isCreate, service]);

  const title = isCreate ? 'New Portfolio' : `Edit Portfolio ${id ?? ''}`;
  const setField = <K extends keyof PortfolioInput>(
    key: K,
    value: PortfolioInput[K],
  ) => setValues((v) => ({ ...v, [key]: value }));

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError(null);
    const validationErrors = validatePortfolio(values, isCreate);
    setErrors(validationErrors);
    if (hasErrors(validationErrors)) return;

    setSaving(true);
    try {
      if (isCreate) {
        const created = await service.create(values);
        navigate(`/portfolios/${created.portId}`);
      } else if (id) {
        const updated = await service.update(id, values);
        navigate(`/portfolios/${updated.portId}`);
      }
    } catch (err) {
      if (err instanceof DuplicatePortfolioError) {
        setErrors((prev) => ({ ...prev, portId: err.message }));
      } else {
        setSubmitError('Unable to save portfolio. Please try again.');
      }
    } finally {
      setSaving(false);
    }
  };

  const cancelHref = useMemo(
    () => (isCreate || !id ? '/portfolios' : `/portfolios/${id}`),
    [isCreate, id],
  );

  if (notFound) {
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

  if (loading) {
    return <p className="state-msg">Loading…</p>;
  }

  return (
    <section>
      <div className="page-header">
        <h1 className="page-header__title">{title}</h1>
      </div>

      {submitError && (
        <div className="alert alert--error" role="alert">
          {submitError}
        </div>
      )}

      <form className="card form" onSubmit={onSubmit} noValidate>
        <div className="form__grid">
          <FormField
            id="portId"
            label="Portfolio ID"
            required
            error={errors.portId}
            hint="PORT-ID · up to 8 characters"
          >
            <input
              id="portId"
              type="text"
              value={values.portId}
              maxLength={PORTFOLIO_FIELD_LENGTHS.portId}
              disabled={!isCreate}
              onChange={(e) =>
                setField('portId', e.target.value.toUpperCase())
              }
              aria-invalid={!!errors.portId}
            />
          </FormField>

          <FormField
            id="accountNo"
            label="Account Number"
            required
            error={errors.accountNo}
            hint="PORT-ACCOUNT-NO · up to 10 characters"
          >
            <input
              id="accountNo"
              type="text"
              value={values.accountNo}
              maxLength={PORTFOLIO_FIELD_LENGTHS.accountNo}
              onChange={(e) =>
                setField('accountNo', e.target.value.toUpperCase())
              }
              aria-invalid={!!errors.accountNo}
            />
          </FormField>

          <FormField
            id="clientName"
            label="Client Name"
            required
            error={errors.clientName}
            hint="PORT-CLIENT-NAME · up to 30 characters"
            wide
          >
            <input
              id="clientName"
              type="text"
              value={values.clientName}
              maxLength={PORTFOLIO_FIELD_LENGTHS.clientName}
              onChange={(e) => setField('clientName', e.target.value)}
              aria-invalid={!!errors.clientName}
            />
          </FormField>

          <FormField
            id="clientType"
            label="Client Type"
            required
            error={errors.clientType}
          >
            <select
              id="clientType"
              value={values.clientType}
              onChange={(e) =>
                setField('clientType', e.target.value as ClientType)
              }
            >
              {CLIENT_TYPES.map((t) => (
                <option key={t} value={t}>
                  {CLIENT_TYPE_LABELS[t]}
                </option>
              ))}
            </select>
          </FormField>

          <FormField id="status" label="Status" required error={errors.status}>
            <select
              id="status"
              value={values.status}
              onChange={(e) =>
                setField('status', e.target.value as PortfolioStatus)
              }
            >
              {PORTFOLIO_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {PORTFOLIO_STATUS_LABELS[s]}
                </option>
              ))}
            </select>
          </FormField>

          <FormField
            id="totalValue"
            label="Total Value"
            required
            error={errors.totalValue}
            hint="PORT-TOTAL-VALUE · S9(13)V99"
          >
            <input
              id="totalValue"
              type="text"
              inputMode="decimal"
              value={values.totalValue}
              onChange={(e) => setField('totalValue', e.target.value)}
              aria-invalid={!!errors.totalValue}
              placeholder="0.00"
            />
          </FormField>

          <FormField
            id="cashBalance"
            label="Cash Balance"
            required
            error={errors.cashBalance}
            hint="PORT-CASH-BALANCE · S9(13)V99"
          >
            <input
              id="cashBalance"
              type="text"
              inputMode="decimal"
              value={values.cashBalance}
              onChange={(e) => setField('cashBalance', e.target.value)}
              aria-invalid={!!errors.cashBalance}
              placeholder="0.00"
            />
          </FormField>
        </div>

        <div className="form__actions">
          <Link to={cancelHref} className="btn btn--ghost">
            Cancel
          </Link>
          <button type="submit" className="btn btn--primary" disabled={saving}>
            {saving ? 'Saving…' : isCreate ? 'Create Portfolio' : 'Save Changes'}
          </button>
        </div>
      </form>
    </section>
  );
}

function FormField({
  id,
  label,
  required,
  error,
  hint,
  wide,
  children,
}: {
  id: string;
  label: string;
  required?: boolean;
  error?: string;
  hint?: string;
  wide?: boolean;
  children: React.ReactNode;
}) {
  return (
    <div className={wide ? 'field field--wide' : 'field'}>
      <label htmlFor={id}>
        {label}
        {required && <span className="field__required"> *</span>}
      </label>
      {children}
      {hint && !error && <span className="field__hint">{hint}</span>}
      {error && (
        <span className="field__error" role="alert">
          {error}
        </span>
      )}
    </div>
  );
}
