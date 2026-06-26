import BackToMenu from '../components/BackToMenu'
import './PlaceholderPage.css'

/**
 * Placeholder for the Portfolio Position Inquiry screen (legacy POSMAP,
 * reached via function code INQP / LINK INQPORT). Detailed fields and
 * PF7/PF8 paging are intentionally out of scope for this phase.
 */
export default function Portfolio() {
  return (
    <section className="placeholder-page">
      <h2 className="placeholder-page__title">Portfolio Position Inquiry</h2>
      <p className="placeholder-page__code">Function code: INQP</p>
      <p className="placeholder-page__note">
        Placeholder screen. Position details, account lookup, and PF7/PF8 paging
        will be implemented in a later phase.
      </p>
      <BackToMenu />
    </section>
  )
}
