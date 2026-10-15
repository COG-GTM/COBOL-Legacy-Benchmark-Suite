import { createContext, useContext } from 'react'

export type StatusTone = 'info' | 'error'

export interface StatusMessage {
  text: string
  tone: StatusTone
}

export interface ShellContextValue {
  message: StatusMessage | null
  setMessage: (message: StatusMessage | null) => void
  requestExit: () => void
}

export const ShellContext = createContext<ShellContextValue | null>(null)

export function useShell(): ShellContextValue {
  const value = useContext(ShellContext)
  if (!value) throw new Error('useShell must be used inside <AppLayout>')
  return value
}
