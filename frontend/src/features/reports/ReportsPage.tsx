import { useCallback } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useReportService } from '../../services/servicesContext';
import type { ReportsOutletContext } from './reportsOutlet';
import { useReportData } from './useReportData';
import { useReportFilters } from './useReportFilters';

const REPORT_TABS = [
  { to: 'positions', label: 'Positions', program: 'RPTPOS00' },
  { to: 'audit', label: 'Audit', program: 'RPTAUD00' },
  { to: 'statistics', label: 'Statistics', program: 'RPTSTA00' },
  { to: 'returns', label: 'Return Analysis', program: 'RTNANA00' },
];

/**
 * Shell for the reporting area: report selector plus the filter values shared
 * by the four views. Filters live in the query string, so the tab links carry
 * the current selection across reports.
 */
export function ReportsPage() {
  const service = useReportService();
  const { search } = useReportFilters();

  const loadOptions = useCallback(() => service.getFilterOptions(), [service]);
  const { data: options } = useReportData(loadOptions);

  return (
    <section>
      <div className="page-header">
        <div>
          <h1 className="page-header__title">Reports &amp; Analytics</h1>
          <p className="page-header__subtitle">
            Batch report programs RPTPOS00, RPTAUD00, RPTSTA00 and RTNANA00, on
            demand
          </p>
        </div>
      </div>

      <nav className="report-tabs" aria-label="Reports">
        {REPORT_TABS.map((tab) => (
          <NavLink
            key={tab.to}
            to={{ pathname: tab.to, search }}
            className={({ isActive }) =>
              `report-tab${isActive ? ' report-tab--active' : ''}`
            }
            title={tab.program}
          >
            {tab.label}
          </NavLink>
        ))}
      </nav>

      {options ? (
        <Outlet context={{ options } satisfies ReportsOutletContext} />
      ) : (
        <p className="state-msg">Loading report data…</p>
      )}
    </section>
  );
}
