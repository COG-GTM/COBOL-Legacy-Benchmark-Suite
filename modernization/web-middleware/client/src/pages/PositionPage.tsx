import { useState } from 'react';
import AccountInput from '../components/AccountInput';
import ErrorPanel from '../components/ErrorPanel';
import { fetchPosition, PositionRecord } from '../api/client';

const statusLabels: Record<string, string> = { A: 'Active', C: 'Closed', P: 'Pending' };

const money = (value: number, currency: string) =>
  `${currency} ${value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

/** POSMAP */
export default function PositionPage() {
  const [account, setAccount] = useState('');
  const [position, setPosition] = useState<PositionRecord | null>(null);
  const [error, setError] = useState('');

  const submit = async () => {
    const res = await fetchPosition(account.trim());
    if (!res.position) {
      setPosition(null);
      setError(res.commarea.inqcomErrorMsg.trim());
      return;
    }
    setError('');
    setPosition(res.position);
  };

  return (
    <section className="panel">
      <h1>Portfolio Position Inquiry</h1>
      <AccountInput value={account} onChange={setAccount} onSubmit={submit} />
      {error && <ErrorPanel code="INQP12" details={error} />}
      {position && (
        <dl className="field-grid">
          <dt>Fund ID:</dt>
          <dd className="data-value">{position.posInvestmentId}</dd>
          <dt>Fund Name:</dt>
          <dd className="data-value">{position.posFundName}</dd>
          <dt>Units:</dt>
          <dd className="data-value">{position.posQuantity.toLocaleString()}</dd>
          <dt>Cost Basis:</dt>
          <dd className="data-value">{money(position.posCostBasis, position.posCurrency)}</dd>
          <dt>Market Value:</dt>
          <dd className="data-value">{money(position.posMarketValue, position.posCurrency)}</dd>
          <dt>Status:</dt>
          <dd className="data-value">{statusLabels[position.posStatus] ?? position.posStatus}</dd>
        </dl>
      )}
    </section>
  );
}
