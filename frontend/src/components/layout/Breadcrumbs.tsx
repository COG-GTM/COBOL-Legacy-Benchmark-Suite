import { Link, useLocation } from 'react-router-dom';
import { ChevronRight, Home } from 'lucide-react';

const routeLabels: Record<string, string> = {
  '/': 'Dashboard',
  '/portfolio': 'Portfolio Inquiry',
  '/transactions': 'Transaction History',
  '/login': 'Login',
};

export function Breadcrumbs() {
  const location = useLocation();

  if (location.pathname === '/' || location.pathname === '/login') {
    return null;
  }

  const pathSegments = location.pathname.split('/').filter(Boolean);
  const breadcrumbs = pathSegments.map((_, index) => {
    const path = '/' + pathSegments.slice(0, index + 1).join('/');
    return {
      label: routeLabels[path] ?? pathSegments[index],
      path,
      isLast: index === pathSegments.length - 1,
    };
  });

  return (
    <nav aria-label="Breadcrumb" className="mb-4">
      <ol className="flex items-center gap-1.5 text-sm text-[#94A3B8]">
        <li>
          <Link
            to="/"
            className="flex items-center gap-1 transition-colors hover:text-white"
          >
            <Home className="h-4 w-4" />
            <span>Home</span>
          </Link>
        </li>
        {breadcrumbs.map((crumb) => (
          <li key={crumb.path} className="flex items-center gap-1.5">
            <ChevronRight className="h-3.5 w-3.5" />
            {crumb.isLast ? (
              <span className="font-medium text-white">{crumb.label}</span>
            ) : (
              <Link to={crumb.path} className="transition-colors hover:text-white">
                {crumb.label}
              </Link>
            )}
          </li>
        ))}
      </ol>
    </nav>
  );
}
