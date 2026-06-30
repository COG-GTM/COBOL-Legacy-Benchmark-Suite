import { Fragment } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { buildCrumbs } from '../nav/breadcrumbs';

/**
 * Breadcrumb navigation that replaces the legacy PF-key flow (PF3=Exit,
 * PF7=Previous, PF8=Next) with a standard, clickable trail.
 */
export function Breadcrumbs() {
  const { pathname } = useLocation();
  const crumbs = buildCrumbs(pathname);

  return (
    <nav className="breadcrumbs" aria-label="Breadcrumb">
      <ol className="breadcrumbs__list">
        {crumbs.map((crumb, index) => {
          const isLast = index === crumbs.length - 1;
          return (
            <Fragment key={crumb.path}>
              <li className="breadcrumbs__item">
                {isLast ? (
                  <span className="breadcrumbs__current" aria-current="page">
                    {crumb.label}
                  </span>
                ) : (
                  <Link className="breadcrumbs__link" to={crumb.path}>
                    {crumb.label}
                  </Link>
                )}
              </li>
              {!isLast && (
                <li className="breadcrumbs__sep" aria-hidden="true">
                  /
                </li>
              )}
            </Fragment>
          );
        })}
      </ol>
    </nav>
  );
}
