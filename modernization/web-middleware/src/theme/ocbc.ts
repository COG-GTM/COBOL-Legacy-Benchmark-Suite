/**
 * OCBC theme tokens.
 *
 * Palette derived at build time from the OCBC brand source
 * https://api.ocbc.com/store (stylesheet /store/static/css/main.*.css),
 * where #d8232a is the dominant brand red and #4a4a4a the dominant text grey.
 *
 * TODO: if https://api.ocbc.com/store is unreachable or its stylesheet hash
 * changes, `npm run theme:sync` falls back to these documented values
 * (OCBC brand red dominant). Correct them against the brand source when
 * official brand guidelines are available.
 */
export const ocbcTheme = {
  primary: '#d8232a',
  primaryDark: '#a5161c',
  secondary: '#4a4a4a',
  background: '#f5f5f5',
  surface: '#ffffff',
  text: '#1a1a1a',
  textMuted: '#6b6b6b',
  border: '#e5e5e5',
  accent: '#0f7b8a',
  error: '#d9534f',
  success: '#3c763d',
} as const;

export type OcbcTheme = typeof ocbcTheme;

/**
 * BMS colour semantics (src/maps/INQSET.bms) mapped onto the theme.
 *   COLOR=RED       -> error fields (ERRMSG, POSMSG, HISMSG, ERRCOUT, ERRDOUT)
 *   COLOR=TURQUOISE -> data output fields (FUNDOUT, UNITOUT, ROW1..ROW10, ...)
 *   ATTRB=BRT       -> bright titles / column headers
 */
export const bmsColorMap = {
  RED: ocbcTheme.error,
  TURQUOISE: ocbcTheme.accent,
  BRT: ocbcTheme.primary,
  DEFAULT: ocbcTheme.text,
} as const;

export const cssVariables = (theme: OcbcTheme = ocbcTheme): Record<string, string> =>
  Object.fromEntries(
    Object.entries(theme).map(([key, value]) => [
      `--ocbc-${key.replace(/[A-Z]/g, (c) => `-${c.toLowerCase()}`)}`,
      value,
    ])
  );
