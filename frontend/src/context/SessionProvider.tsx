import { useCallback, useMemo, useState, type ReactNode } from 'react'
import {
  EMPTY_COMMAREA,
  ResponseCode,
  type InqCommArea,
} from '../legacy/commarea'
import { FunctionCode } from '../legacy/functionCodes'
import { SessionContext, type SessionContextValue } from './sessionContext'

export function SessionProvider({ children }: { children: ReactNode }) {
  const [commarea, setCommarea] = useState<InqCommArea>(EMPTY_COMMAREA)
  const [terminated, setTerminated] = useState(false)

  const setFunction = useCallback((func: FunctionCode) => {
    setCommarea((prev) => ({ ...prev, func }))
  }, [])

  const setAccountNo = useCallback((accountNo: string) => {
    setCommarea((prev) => ({ ...prev, accountNo }))
  }, [])

  const raiseError = useCallback(
    (errorMsg: string, responseCode: number = ResponseCode.INVREQ) => {
      setCommarea((prev) => ({ ...prev, errorMsg, responseCode }))
    },
    [],
  )

  const clearError = useCallback(() => {
    setCommarea((prev) => ({
      ...prev,
      errorMsg: '',
      responseCode: ResponseCode.NORMAL,
    }))
  }, [])

  const endSession = useCallback(() => {
    setCommarea((prev) => ({ ...prev, func: FunctionCode.EXIT }))
    setTerminated(true)
  }, [])

  const resetSession = useCallback(() => {
    setCommarea(EMPTY_COMMAREA)
    setTerminated(false)
  }, [])

  const value = useMemo<SessionContextValue>(
    () => ({
      commarea,
      terminated,
      setFunction,
      setAccountNo,
      raiseError,
      clearError,
      endSession,
      resetSession,
    }),
    [
      commarea,
      terminated,
      setFunction,
      setAccountNo,
      raiseError,
      clearError,
      endSession,
      resetSession,
    ],
  )

  return <SessionContext value={value}>{children}</SessionContext>
}
