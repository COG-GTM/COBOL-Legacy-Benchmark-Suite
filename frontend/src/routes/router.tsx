import { createBrowserRouter, Navigate } from 'react-router-dom'
import AppShell from '../layout/AppShell'
import MainMenu from '../pages/MainMenu'
import Portfolio from '../pages/Portfolio'
import History from '../pages/History'
import SessionEnded from '../pages/SessionEnded'
import ErrorPage from '../pages/ErrorPage'
import NotFound from '../pages/NotFound'

/**
 * Client-side routes for the inquiry subsystem. The mapping from legacy
 * function codes to paths lives in `src/legacy/functionCodes.ts`:
 *   INQP -> /portfolio, INQH -> /history, EXIT -> /exit, MENU -> /menu.
 */
export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    children: [
      { index: true, element: <Navigate to="/menu" replace /> },
      { path: 'menu', element: <MainMenu /> },
      { path: 'portfolio', element: <Portfolio /> },
      { path: 'history', element: <History /> },
      { path: 'exit', element: <SessionEnded /> },
      { path: 'error', element: <ErrorPage /> },
      { path: '*', element: <NotFound /> },
    ],
  },
])
