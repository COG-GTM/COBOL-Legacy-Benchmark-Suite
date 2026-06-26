import { useNavigate } from 'react-router-dom'
import { useSession } from '../context/sessionContext'
import { ResponseCode } from '../legacy/commarea'
import { FunctionCode } from '../legacy/functionCodes'
import './ErrorPage.css'

/**
 * Error view, modeled on the BMS `ERRMAP` screen in `src/maps/INQSET.bms` and
 * the `P900-ERROR-ROUTINE` in `INQONLN.cbl` (the `WHEN OTHER` branch). Shows
 * the COMMAREA response code and error message, then returns to the menu.
 */
export default function ErrorPage() {
  const navigate = useNavigate()
  const { commarea, clearError, setFunction } = useSession()

  const responseCode = commarea.responseCode || ResponseCode.INVREQ
  const errorMsg = commarea.errorMsg || 'An unexpected error occurred.'

  return (
    <section className="error-page">
      <h2 className="error-page__title">System Error</h2>

      <dl className="error-page__details">
        <dt>Error Code:</dt>
        <dd className="error-page__code">{responseCode}</dd>
        <dt>Details:</dt>
        <dd className="error-page__message">{errorMsg}</dd>
      </dl>

      <button
        type="button"
        className="error-page__continue"
        onClick={() => {
          clearError()
          setFunction(FunctionCode.MENU)
          navigate('/menu')
        }}
      >
        Press to continue
      </button>
    </section>
  )
}
