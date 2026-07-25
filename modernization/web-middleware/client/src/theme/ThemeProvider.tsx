import { ReactNode, useEffect } from 'react';
import { cssVariables, ocbcTheme } from '@theme';

/** Applies the OCBC theme tokens globally as CSS custom properties. */
export default function ThemeProvider({ children }: { children: ReactNode }) {
  useEffect(() => {
    const root = document.documentElement;
    Object.entries(cssVariables(ocbcTheme)).forEach(([name, value]) =>
      root.style.setProperty(name, value)
    );
  }, []);
  return <>{children}</>;
}
