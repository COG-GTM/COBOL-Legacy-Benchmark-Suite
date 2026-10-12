import { useEffect, useRef, type ReactNode } from 'react';

/** h1 that can take focus on mount, for screens without an input to autofocus. */
export function PageHeading({ children, focusOnMount = false }: { children: ReactNode; focusOnMount?: boolean }) {
  const ref = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    if (focusOnMount) ref.current?.focus();
  }, [focusOnMount]);
  return (
    <h1 ref={ref} tabIndex={-1} className="page-title">
      {children}
    </h1>
  );
}
