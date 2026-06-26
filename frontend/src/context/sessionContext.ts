import { createContext, useContext } from 'react'
import type { InqCommArea } from '../legacy/commarea'
import type { FunctionCode } from '../legacy/functionCodes'

export interface SessionContextValue {
  /** Current COMMAREA state (see INQCOM.cpy). */
  commarea: InqCommArea
  /** Whether the session has been terminated (EXIT / SESSION-TERMINATED). */
  terminated: boolean
  /** Set the active function code (INQCOM-FUNCTION). */
  setFunction: (func: FunctionCode) => void
  /** Set the account number context (INQCOM-ACCOUNT-NO). */
  setAccountNo: (accountNo: string) => void
  /** Record an error, mirroring the P900-ERROR-ROUTINE in INQONLN.cbl. */
  raiseError: (errorMsg: string, responseCode?: number) => void
  /** Clear any error / status and reset the response code to NORMAL. */
  clearError: () => void
  /** End the session (SET SESSION-TERMINATED TO TRUE). */
  endSession: () => void
  /** Reset back to a fresh menu session. */
  resetSession: () => void
}

export const SessionContext = createContext<SessionContextValue | undefined>(
  undefined,
)

export function useSession(): SessionContextValue {
  const ctx = useContext(SessionContext)
  if (!ctx) {
    throw new Error('useSession must be used within a SessionProvider')
  }
  return ctx
}
