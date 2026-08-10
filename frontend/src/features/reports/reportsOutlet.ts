import { useOutletContext } from 'react-router-dom';
import type { ReportFilterOptions } from '../../services/reportService';

/** Context the reports shell passes down to the individual report views. */
export interface ReportsOutletContext {
  /** Filter values collected from the report sources. */
  options: ReportFilterOptions;
}

export function useReportsOutletContext(): ReportsOutletContext {
  return useOutletContext<ReportsOutletContext>();
}
