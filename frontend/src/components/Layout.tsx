import { useId, type ReactNode } from 'react';

interface LayoutProps {
  /** Screen title, mirrors the top-line title of a legacy BMS map. */
  title: string;
  children: ReactNode;
}

/**
 * Application shell that frames every screen with a common header and footer,
 * echoing the fixed banner/status-line layout of the legacy 3270 maps.
 */
export function Layout({ title, children }: LayoutProps) {
  const titleId = useId();
  return (
    <div className="app-shell">
      <header className="app-header">
        <span className="app-header__brand">CLBS</span>
        <span className="app-header__system">Portfolio Management System</span>
      </header>
      <main className="app-main" aria-labelledby={titleId}>
        <h1 id={titleId} className="screen-title">
          {title}
        </h1>
        {children}
      </main>
      <footer className="app-footer">
        Legacy CICS transaction INQONLN &middot; modernization preview
      </footer>
    </div>
  );
}
