/**
 * Central mapping of legacy CICS function codes to modern client-side routes.
 *
 * These codes mirror `INQCOM-FUNCTION` in
 * `src/copybook/online/INQCOM.cpy` and the routing performed by the
 * `INQONLN` front controller (`src/programs/online/INQONLN.cbl`).
 *
 * Keeping this mapping in one place means later phases can attach real
 * portfolio/history data and authentication while staying faithful to the
 * legacy 4-char function codes.
 */

/** Legacy 4-character function codes (INQCOM-FUNCTION values). */
export const FunctionCode = {
  /** MENU  -> P200-DISPLAY-MENU (main menu screen) */
  MENU: 'MENU',
  /** INQP  -> P300-PORTFOLIO-INQUIRY (LINK PROGRAM 'INQPORT') */
  PORTFOLIO: 'INQP',
  /** INQH  -> P400-HISTORY-INQUIRY (LINK PROGRAM 'INQHIST') */
  HISTORY: 'INQH',
  /** EXIT  -> SET SESSION-TERMINATED TO TRUE (terminates the session) */
  EXIT: 'EXIT',
} as const;

export type FunctionCode = (typeof FunctionCode)[keyof typeof FunctionCode];

/** Client-side route paths. */
export const Routes = {
  MENU: '/',
  PORTFOLIO: '/portfolio',
  HISTORY: '/history',
  EXIT: '/exit',
} as const;

export type RoutePath = (typeof Routes)[keyof typeof Routes];

/**
 * Maps each legacy function code to the route the modern UI navigates to.
 * `EXIT` has no data screen; it ends the session (mirrors
 * `SET SESSION-TERMINATED TO TRUE`).
 */
export const FUNCTION_CODE_TO_ROUTE: Record<FunctionCode, RoutePath> = {
  [FunctionCode.MENU]: Routes.MENU,
  [FunctionCode.PORTFOLIO]: Routes.PORTFOLIO,
  [FunctionCode.HISTORY]: Routes.HISTORY,
  [FunctionCode.EXIT]: Routes.EXIT,
};

/** A single selectable option on the main menu, in legacy `MENMAP` order. */
export interface MenuOption {
  /** Display order / selection number as shown on the legacy screen. */
  option: number;
  /** Label matching the legacy `MENMAP` DFHMDF INITIAL text. */
  label: string;
  /** Legacy 4-char function code this option dispatches. */
  code: FunctionCode;
  /** Route the option navigates to. */
  route: RoutePath;
}

/**
 * The three menu options, faithful to the order and labels defined in
 * `MENMAP` (`src/maps/INQSET.bms`).
 */
export const MENU_OPTIONS: readonly MenuOption[] = [
  {
    option: 1,
    label: 'Portfolio Position Inquiry',
    code: FunctionCode.PORTFOLIO,
  },
  { option: 2, label: 'Transaction History', code: FunctionCode.HISTORY },
  { option: 3, label: 'Exit', code: FunctionCode.EXIT },
].map(({ option, label, code }) => ({
  option,
  label,
  code,
  // Derive the route from the single-source-of-truth code->route mapping so the
  // two can never drift apart.
  route: FUNCTION_CODE_TO_ROUTE[code],
}));
