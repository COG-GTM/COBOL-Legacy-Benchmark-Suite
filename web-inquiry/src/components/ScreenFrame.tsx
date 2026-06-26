import type { ReactNode } from 'react';
import { FunctionKeyBar, type FunctionKey } from './FunctionKeyBar';

/**
 * Shared 3270-style screen frame: title bar (with the BMS map id), an optional
 * inline error message line (the ERRMSG/POSMSG/HISMSG fields), the screen body,
 * and the PF-key function bar at the bottom.
 */
interface ScreenFrameProps {
  /** Screen title (BMS heading). */
  title: string;
  /** BMS map name shown as a badge, e.g. MENMAP. */
  mapId: string;
  /** Inline error message line (COLOR=RED), if any. */
  errorMsg?: string;
  /** PF keys for the function bar. */
  functionKeys?: FunctionKey[];
  children: ReactNode;
}

export function ScreenFrame({
  title,
  mapId,
  errorMsg,
  functionKeys,
  children,
}: ScreenFrameProps) {
  return (
    <div className="screen">
      <div className="screen-head">
        <h1>{title}</h1>
        <span className="map-id">{mapId}</span>
      </div>
      {errorMsg ? (
        <div className="error-line" role="alert">
          {errorMsg}
        </div>
      ) : null}
      <div className="screen-body">{children}</div>
      {functionKeys && functionKeys.length > 0 ? (
        <FunctionKeyBar keys={functionKeys} />
      ) : null}
    </div>
  );
}
