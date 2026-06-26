import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSession } from '../context/sessionContext'
import './SessionEnded.css'

/**
 * End-of-session screen reached via function code EXIT, mirroring the
 * `SET SESSION-TERMINATED TO TRUE` / `EXEC CICS RETURN` path in INQONLN.cbl.
 * Offers a way to start a new session (re-enter the menu).
 */
export default function SessionEnded() {
  const navigate = useNavigate()
  const { endSession, resetSession } = useSession()

  useEffect(() => {
    endSession()
  }, [endSession])

  return (
    <section className="session-ended">
      <h2 className="session-ended__title">Session Ended</h2>
      <p className="session-ended__note">
        Thank you for using the Portfolio Management System.
      </p>
      <button
        type="button"
        className="session-ended__restart"
        onClick={() => {
          resetSession()
          navigate('/menu')
        }}
      >
        Start New Session
      </button>
    </section>
  )
}
