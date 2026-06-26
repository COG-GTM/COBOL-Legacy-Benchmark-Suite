import { useEffect } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useSession } from '../context/sessionContext'

/**
 * Catch-all for unknown routes. Mirrors the `WHEN OTHER` branch in
 * `INQONLN.cbl`: an unrecognized destination is funnelled into the error
 * routine and displayed on the error screen.
 */
export default function NotFound() {
  const location = useLocation()
  const { raiseError } = useSession()

  useEffect(() => {
    raiseError(`Invalid navigation: ${location.pathname}`)
  }, [raiseError, location.pathname])

  return <Navigate to="/error" replace />
}
