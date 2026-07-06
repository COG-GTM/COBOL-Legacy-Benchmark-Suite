import { Layout } from '../components/Layout';
import { BackToMenuLink } from '../components/BackToMenuLink';

/**
 * Transaction History screen, route `/history`.
 *
 * Placeholder for legacy function code `INQH`, handled by `INQHIST`
 * (LINKed from `P400-HISTORY-INQUIRY` in `INQONLN.cbl`). No data retrieval
 * yet — the backend will be wired in a later phase.
 */
export function TransactionHistory() {
  return (
    <Layout title="Transaction History Inquiry">
      <p className="placeholder-note">Backend pending &mdash; coming soon.</p>
      <p className="placeholder-detail">
        This screen will list account transaction history (legacy function code{' '}
        <code>INQH</code>, program <code>INQHIST</code>).
      </p>
      <BackToMenuLink />
    </Layout>
  );
}
