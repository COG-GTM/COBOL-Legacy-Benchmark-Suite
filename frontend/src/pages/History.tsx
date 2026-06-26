import BackToMenu from '../components/BackToMenu'
import './PlaceholderPage.css'

/**
 * Placeholder for the Transaction History screen (legacy HISMAP, reached via
 * function code INQH / LINK INQHIST). Detailed transaction rows and PF7/PF8
 * paging are intentionally out of scope for this phase.
 */
export default function History() {
  return (
    <section className="placeholder-page">
      <h2 className="placeholder-page__title">Transaction History</h2>
      <p className="placeholder-page__code">Function code: INQH</p>
      <p className="placeholder-page__note">
        Placeholder screen. Transaction rows, account lookup, and PF7/PF8 paging
        will be implemented in a later phase.
      </p>
      <BackToMenu />
    </section>
  )
}
