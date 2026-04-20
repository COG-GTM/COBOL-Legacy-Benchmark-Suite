import { Outlet } from 'react-router-dom';
import ProtectedRoute from '../components/ProtectedRoute';

/**
 * App shell layout — placeholder for Child Session 1 to implement fully.
 * Will include sidebar/top nav matching legacy MENMAP menu options.
 */
export function Component() {
  return (
    <ProtectedRoute>
      <Outlet />
    </ProtectedRoute>
  );
}
