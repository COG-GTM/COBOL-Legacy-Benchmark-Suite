import { FormEvent, useState } from "react";
import { getHistory, toErrorMessage } from "../api/client";
import { HistoryResponse, TRANSACTION_TYPE_LABELS } from "../api/types";

const currency = (value: number) =>
  new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
  }).format(value);

// Mirrors HISMAP / INQHIST: account input + a table of dated transactions
// (Date, Type, Units, Price, Amount) with loading/not-found/error states.
export default function HistoryPage() {
  const [account, setAccount] = useState("1000000001");
  const [history, setHistory] = useState<HistoryResponse | null>(null);
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
    setHistory(null);
    try {
      setHistory(await getHistory(accountNo));
    } catch (err) {
      setError(
        toErrorMessage(
          err,
          `No transaction history found for account ${accountNo}`
        )
      );
    } finally {
      setLoading(false);
    }
  };

  const rows = history?.transactions ?? [];

  return (
    <section className="card">
      <h2>Transaction History Inquiry</h2>
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

      {loading && <p className="status">Retrieving history…</p>}
      {error && <p className="status error">{error}</p>}

      {history && !loading && !error && (
        <>
          {rows.length === 0 ? (
            <p className="status">
              No transactions found for account {history.accountNo}.
            </p>
          ) : (
            <table className="history-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Type</th>
                  <th className="num">Units</th>
                  <th className="num">Price</th>
                  <th className="num">Amount</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((t, i) => (
                  <tr key={`${t.date}-${i}`}>
                    <td>{t.date}</td>
                    <td>
                      {TRANSACTION_TYPE_LABELS[t.type] ?? t.type} ({t.type})
                    </td>
                    <td className="num">
                      {t.units.toLocaleString("en-US")}
                    </td>
                    <td className="num">{currency(t.price)}</td>
                    <td className="num">{currency(t.amount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </section>
  );
}
