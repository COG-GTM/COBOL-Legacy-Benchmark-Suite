import { FormEvent, useState } from "react";
import { getPosition, toErrorMessage } from "../api/client";
import { PositionResponse } from "../api/types";

const currency = (value: number, code: string) =>
  new Intl.NumberFormat("en-US", { style: "currency", currency: code }).format(
    value
  );

// Mirrors POSMAP / INQPORT: account input + position details (Fund ID, Fund
// Name, Units, Cost Basis, Market Value) with loading/not-found/error states.
export default function PositionPage() {
  const [account, setAccount] = useState("1000000001");
  const [position, setPosition] = useState<PositionResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const accountNo = account.trim();
    if (!accountNo) {
      setError("Please enter an account number.");
      return;
    }
    setLoading(true);
    setError(null);
    setPosition(null);
    try {
      setPosition(await getPosition(accountNo));
    } catch (err) {
      setError(
        toErrorMessage(err, `Position not found for account ${accountNo}`)
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="card">
      <h2>Portfolio Position Inquiry</h2>
      <form className="inquiry-form" onSubmit={onSubmit}>
        <label htmlFor="account">Account:</label>
        <input
          id="account"
          value={account}
          onChange={(e) => setAccount(e.target.value)}
          placeholder="e.g. 1000000001"
          maxLength={10}
          autoComplete="off"
        />
        <button type="submit" disabled={loading}>
          {loading ? "Loading…" : "Inquire"}
        </button>
      </form>

      {loading && <p className="status">Retrieving position…</p>}
      {error && <p className="status error">{error}</p>}

      {position && !loading && !error && (
        <dl className="detail-grid">
          <dt>Account</dt>
          <dd>{position.accountNo}</dd>
          <dt>Fund ID</dt>
          <dd>{position.fundId}</dd>
          <dt>Fund Name</dt>
          <dd>{position.fundName}</dd>
          <dt>Units</dt>
          <dd>{position.units.toLocaleString("en-US")}</dd>
          <dt>Cost Basis</dt>
          <dd>{currency(position.costBasis, position.currencyCode)}</dd>
          <dt>Market Value</dt>
          <dd>{currency(position.marketValue, position.currencyCode)}</dd>
          <dt>Position Date</dt>
          <dd>{position.positionDate}</dd>
        </dl>
      )}
    </section>
  );
}
