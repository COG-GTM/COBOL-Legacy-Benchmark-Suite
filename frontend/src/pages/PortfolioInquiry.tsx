import { Layout } from '../components/Layout';
import { BackToMenuLink } from '../components/BackToMenuLink';

/**
 * Portfolio Position Inquiry screen, route `/portfolio`.
 *
 * Placeholder for legacy function code `INQP`, handled by `INQPORT`
 * (LINKed from `P300-PORTFOLIO-INQUIRY` in `INQONLN.cbl`). No data retrieval
 * yet — the backend will be wired in a later phase.
 */
export function PortfolioInquiry() {
  return (
    <Layout title="Portfolio Position Inquiry">
      <p className="placeholder-note">Backend pending &mdash; coming soon.</p>
      <p className="placeholder-detail">
        This screen will display fund positions for an account (legacy function
        code <code>INQP</code>, program <code>INQPORT</code>).
      </p>
      <BackToMenuLink />
    </Layout>
  );
}
