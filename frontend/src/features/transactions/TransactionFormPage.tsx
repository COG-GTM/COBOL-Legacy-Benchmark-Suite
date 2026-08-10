import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FormField } from '../../components/FormField';
import { useTransactionService } from '../../services/servicesContext';
import {
  InsufficientUnitsError,
  UnknownPortfolioError,
} from '../../services/transactionService';
import {
  SUBMITTABLE_TRANSACTION_TYPES,
  TRANSACTION_CURRENCIES,
  TRANSACTION_FIELD_LENGTHS,
  TRANSACTION_TYPE_LABELS,
  type TransactionInput,
  type TransactionType,
} from '../../types/transaction';
import {
  compareDecimals,
  formatCurrency,
  formatQuantity,
} from '../../utils/decimal';
import { calculateAmount } from './amount';
import {
  hasErrors,
  validateTransaction,
  type TransactionErrors,
} from './validation';

const EMPTY_FORM: TransactionInput = {
  portfolioId: '',
  investmentId: '',
  type: 'BU',
  quantity: '',
  price: '',
  currency: 'USD',
};

type Step = 'entry' | 'review';

/**
 * Transaction submission screen — the web replacement for keying a TRANFILE
 * record for PORTTRAN. Entry and review are two steps of the same wizard so the
 * user confirms the computed TRN-AMOUNT before the record is written.
 */
export function TransactionFormPage() {
  const service = useTransactionService();
  const navigate = useNavigate();

  const [step, setStep] = useState<Step>('entry');
  const [values, setValues] = useState<TransactionInput>(EMPTY_FORM);
  const [errors, setErrors] = useState<TransactionErrors>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [checking, setChecking] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const amount = calculateAmount(values.type, values.quantity, values.price);
  const isTransfer = values.type === 'TR';

  const setField = <K extends keyof TransactionInput>(
    key: K,
    value: TransactionInput[K],
  ) => {
    setValues((v) => ({ ...v, [key]: value }));
    setErrors((prev) => ({ ...prev, [key]: undefined }));
  };

  const onReview = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError(null);
    const validationErrors = validateTransaction(values);
    setErrors(validationErrors);
    if (hasErrors(validationErrors)) return;

    // PORTTRAN 2220-PROCESS-SELL rejects a sale larger than the units held, so
    // the balance is checked here rather than after the user has confirmed.
    if (values.type === 'SL') {
      setChecking(true);
      try {
        const available = await service.availableUnits(
          values.portfolioId.trim().toUpperCase(),
          values.investmentId.trim().toUpperCase(),
        );
        if (available === null) {
          setErrors({
            investmentId: 'This portfolio holds no units of that investment.',
          });
          return;
        }
        if (compareDecimals(available, values.quantity) < 0) {
          setErrors({
            quantity: `Insufficient units for sale: ${formatQuantity(available)} available.`,
          });
          return;
        }
      } catch {
        setSubmitError('Unable to verify the available balance. Try again.');
        return;
      } finally {
        setChecking(false);
      }
    }

    setStep('review');
  };

  const onSubmit = async () => {
    setSubmitError(null);
    setSubmitting(true);
    try {
      const transaction = await service.submit({
        ...values,
        portfolioId: values.portfolioId.trim().toUpperCase(),
        investmentId: values.investmentId.trim().toUpperCase(),
      });
      navigate('/transactions', {
        state: { submitted: transaction },
      });
    } catch (err) {
      setStep('entry');
      if (err instanceof UnknownPortfolioError) {
        setErrors((prev) => ({ ...prev, portfolioId: err.message }));
      } else if (err instanceof InsufficientUnitsError) {
        setErrors((prev) => ({
          ...prev,
          quantity: `Insufficient units for sale: ${formatQuantity(err.available)} available.`,
        }));
      } else {
        setSubmitError('Unable to submit the transaction. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <div className="page-header">
        <div>
          <h1 className="page-header__title">New Transaction</h1>
          <p className="page-header__subtitle">
            Submit a buy, sell or transfer (PORTTRAN / TRANFILE)
          </p>
        </div>
      </div>

      <ol className="steps" aria-label="Submission progress">
        <li
          className={step === 'entry' ? 'steps__item is-current' : 'steps__item'}
          aria-current={step === 'entry' ? 'step' : undefined}
        >
          1. Details
        </li>
        <li
          className={
            step === 'review' ? 'steps__item is-current' : 'steps__item'
          }
          aria-current={step === 'review' ? 'step' : undefined}
        >
          2. Review &amp; confirm
        </li>
      </ol>

      {submitError && (
        <div className="alert alert--error" role="alert">
          {submitError}
        </div>
      )}

      {step === 'entry' ? (
        <form className="card form" onSubmit={onReview} noValidate>
          <div className="form__grid">
            <FormField
              id="type"
              label="Transaction Type"
              required
              error={errors.type}
              hint="TRN-TYPE · BU / SL / TR"
            >
              <select
                id="type"
                value={values.type}
                onChange={(e) =>
                  setField('type', e.target.value as TransactionType)
                }
              >
                {SUBMITTABLE_TRANSACTION_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {TRANSACTION_TYPE_LABELS[t]}
                  </option>
                ))}
              </select>
            </FormField>

            <FormField
              id="portfolioId"
              label="Portfolio ID"
              required
              error={errors.portfolioId}
              hint="TRN-PORTFOLIO-ID · e.g. PORT0001"
            >
              <input
                id="portfolioId"
                type="text"
                value={values.portfolioId}
                maxLength={TRANSACTION_FIELD_LENGTHS.portfolioId}
                onChange={(e) =>
                  setField('portfolioId', e.target.value.toUpperCase())
                }
                aria-invalid={!!errors.portfolioId}
              />
            </FormField>

            <FormField
              id="investmentId"
              label="Investment ID"
              required
              error={errors.investmentId}
              hint="TRN-INVESTMENT-ID · up to 10 characters"
            >
              <input
                id="investmentId"
                type="text"
                value={values.investmentId}
                maxLength={TRANSACTION_FIELD_LENGTHS.investmentId}
                onChange={(e) =>
                  setField('investmentId', e.target.value.toUpperCase())
                }
                aria-invalid={!!errors.investmentId}
              />
            </FormField>

            <FormField
              id="currency"
              label="Currency"
              required
              error={errors.currency}
              hint="TRN-CURRENCY · ISO code"
            >
              <select
                id="currency"
                value={values.currency}
                onChange={(e) => setField('currency', e.target.value)}
              >
                {TRANSACTION_CURRENCIES.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </FormField>

            <FormField
              id="quantity"
              label="Quantity"
              required
              error={errors.quantity}
              hint="TRN-QUANTITY · S9(11)V9(4)"
            >
              <input
                id="quantity"
                type="text"
                inputMode="decimal"
                value={values.quantity}
                onChange={(e) => setField('quantity', e.target.value)}
                aria-invalid={!!errors.quantity}
                placeholder="0.0000"
              />
            </FormField>

            <FormField
              id="price"
              label="Price per Unit"
              required={!isTransfer}
              error={errors.price}
              hint={
                isTransfer
                  ? 'Not applicable to transfers'
                  : 'TRN-PRICE · S9(11)V9(4)'
              }
            >
              <input
                id="price"
                type="text"
                inputMode="decimal"
                value={isTransfer ? '' : values.price}
                disabled={isTransfer}
                onChange={(e) => setField('price', e.target.value)}
                aria-invalid={!!errors.price}
                placeholder="0.0000"
              />
            </FormField>

            <FormField
              id="amount"
              label="Amount"
              error={errors.amount}
              hint="TRN-AMOUNT · quantity × price, S9(13)V9(2)"
              wide
            >
              <output
                id="amount"
                htmlFor="quantity price"
                className="field__computed"
                data-testid="computed-amount"
              >
                {amount ? formatCurrency(amount, values.currency) : '—'}
              </output>
            </FormField>
          </div>

          <div className="form__actions">
            <Link to="/transactions" className="btn btn--ghost">
              Cancel
            </Link>
            <button
              type="submit"
              className="btn btn--primary"
              disabled={checking}
            >
              {checking ? 'Checking…' : 'Review'}
            </button>
          </div>
        </form>
      ) : (
        <div className="card form">
          <h2 className="detail-card__title">Review transaction</h2>
          <p className="review__intro">
            Confirm the details below. The transaction is written with status
            Pending and settled by the next PORTTRAN run.
          </p>
          <dl className="detail-list">
            <ReviewRow
              label="Transaction Type"
              value={TRANSACTION_TYPE_LABELS[values.type]}
            />
            <ReviewRow label="Portfolio ID" value={values.portfolioId} />
            <ReviewRow label="Investment ID" value={values.investmentId} />
            <ReviewRow
              label="Quantity"
              value={formatQuantity(values.quantity)}
            />
            <ReviewRow
              label="Price per Unit"
              value={isTransfer ? '—' : formatCurrency(values.price, values.currency)}
            />
            <ReviewRow label="Currency" value={values.currency} />
            <ReviewRow
              label="Amount"
              value={formatCurrency(amount || '0', values.currency)}
            />
          </dl>

          <div className="form__actions">
            <button
              type="button"
              className="btn btn--ghost"
              onClick={() => setStep('entry')}
              disabled={submitting}
            >
              Back
            </button>
            <button
              type="button"
              className="btn btn--primary"
              onClick={() => void onSubmit()}
              disabled={submitting}
            >
              {submitting ? 'Submitting…' : 'Confirm & Submit'}
            </button>
          </div>
        </div>
      )}
    </section>
  );
}

function ReviewRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="detail-list__row">
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}
