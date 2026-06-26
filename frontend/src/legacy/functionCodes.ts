/**
 * Function-code definitions ported from the legacy COMMAREA copybook
 * `src/copybook/online/INQCOM.cpy` (field `INQCOM-FUNCTION PIC X(4)`).
 *
 * The CICS controller `src/programs/online/INQONLN.cbl` routes on this
 * 4-character code (see its `EVALUATE WS-COMMAREA-FUNCTION`):
 *   MENU -> display main menu
 *   INQP -> portfolio position inquiry  (LINK INQPORT)
 *   INQH -> transaction history inquiry (LINK INQHIST)
 *   EXIT -> end the session
 *   other -> error routine (the WHEN OTHER branch)
 */
export const FunctionCode = {
  MENU: 'MENU',
  PORTFOLIO: 'INQP',
  HISTORY: 'INQH',
  EXIT: 'EXIT',
} as const

export type FunctionCode = (typeof FunctionCode)[keyof typeof FunctionCode]

export const FUNCTION_CODES: readonly FunctionCode[] =
  Object.values(FunctionCode)

export function isFunctionCode(value: string): value is FunctionCode {
  return (FUNCTION_CODES as readonly string[]).includes(value)
}

/**
 * Maps a legacy 4-char function code to the corresponding client-side route.
 * Returns `null` for codes that have no destination (mirrors the legacy
 * `WHEN OTHER` branch which performs the error routine).
 */
export function routeForFunction(code: string): string | null {
  switch (code) {
    case FunctionCode.MENU:
      return '/menu'
    case FunctionCode.PORTFOLIO:
      return '/portfolio'
    case FunctionCode.HISTORY:
      return '/history'
    case FunctionCode.EXIT:
      return '/exit'
    default:
      return null
  }
}
