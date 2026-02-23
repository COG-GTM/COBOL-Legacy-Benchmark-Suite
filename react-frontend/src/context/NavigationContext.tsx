/**
 * NavigationContext - Replaces CURSMGR (Cursor Manager) COBOL program
 *
 * In the COBOL system, CURSMGR handled:
 * - Cursor positioning between fields
 * - Screen navigation (PF key processing)
 * - Field selection management
 *
 * Combined with INQONLN's EVALUATE WS-COMMAREA-FUNCTION logic:
 * - 'MENU' → Display Menu
 * - 'INQP' → Portfolio Inquiry
 * - 'INQH' → History Inquiry
 * - 'EXIT' → Terminate Session
 *
 * This React context manages which screen is active,
 * replacing the CICS SEND MAP / RECEIVE MAP flow.
 */

import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import type { NavigationState } from '../types';

interface NavigationContextType {
  navigation: NavigationState;
  navigateTo: (screen: NavigationState['currentScreen']) => void;
  goBack: () => void;
}

const NavigationContext = createContext<NavigationContextType | undefined>(undefined);

export function NavigationProvider({ children }: { children: ReactNode }) {
  const [navigation, setNavigation] = useState<NavigationState>({
    currentScreen: 'MENU',
    previousScreen: null,
  });

  const navigateTo = useCallback((screen: NavigationState['currentScreen']) => {
    setNavigation((prev) => ({
      currentScreen: screen,
      previousScreen: prev.currentScreen,
    }));
  }, []);

  const goBack = useCallback(() => {
    setNavigation((prev) => ({
      currentScreen: prev.previousScreen || 'MENU',
      previousScreen: null,
    }));
  }, []);

  return (
    <NavigationContext.Provider value={{ navigation, navigateTo, goBack }}>
      {children}
    </NavigationContext.Provider>
  );
}

export function useNavigation(): NavigationContextType {
  const context = useContext(NavigationContext);
  if (context === undefined) {
    throw new Error('useNavigation must be used within a NavigationProvider');
  }
  return context;
}
