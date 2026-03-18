import { useState, useMemo, useEffect } from 'react';
import { ChevronUp, ChevronDown, ChevronsUpDown } from 'lucide-react';
import { Pagination } from '@/components/shared/Pagination';
import { cn } from '@/lib/utils';

export interface ColumnDef<T> {
  key: string;
  header: string;
  sortable?: boolean;
  render?: (row: T) => React.ReactNode;
}

interface DataTableProps<T> {
  columns: ColumnDef<T>[];
  data: T[];
  pageSize?: number;
  expandableRow?: (row: T) => React.ReactNode;
  groupBy?: keyof T & string;
  groupSummary?: (groupKey: string, rows: T[]) => Record<string, React.ReactNode>;
  totalRow?: Record<string, React.ReactNode>;
  rowClassName?: (row: T) => string;
  getRowKey?: (row: T, index: number) => string;
}

type SortDirection = 'asc' | 'desc' | null;

export function DataTable<T extends object>({
  columns,
  data,
  pageSize = 10,
  expandableRow,
  groupBy,
  groupSummary,
  totalRow,
  rowClassName,
  getRowKey,
}: DataTableProps<T>) {
  const [sortKey, setSortKey] = useState<string | null>(null);
  const [sortDir, setSortDir] = useState<SortDirection>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [expandedRows, setExpandedRows] = useState<Set<number>>(new Set());

  useEffect(() => {
    setCurrentPage(1);
    setExpandedRows(new Set());
  }, [data]);

  const sortedData = useMemo(() => {
    if (!sortKey || !sortDir) return data;
    return [...data].sort((a, b) => {
      const aVal = (a as Record<string, unknown>)[sortKey];
      const bVal = (b as Record<string, unknown>)[sortKey];
      if (aVal == null && bVal == null) return 0;
      if (aVal == null) return 1;
      if (bVal == null) return -1;
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return sortDir === 'asc' ? aVal - bVal : bVal - aVal;
      }
      const aStr = String(aVal);
      const bStr = String(bVal);
      return sortDir === 'asc' ? aStr.localeCompare(bStr) : bStr.localeCompare(aStr);
    });
  }, [data, sortKey, sortDir]);

  const totalPages = Math.max(1, Math.ceil(sortedData.length / pageSize));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const paginatedData = sortedData.slice(
    (safeCurrentPage - 1) * pageSize,
    safeCurrentPage * pageSize
  );

  function handleSort(key: string) {
    if (sortKey === key) {
      if (sortDir === 'asc') setSortDir('desc');
      else if (sortDir === 'desc') {
        setSortKey(null);
        setSortDir(null);
      }
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
    setCurrentPage(1);
    setExpandedRows(new Set());
  }

  function toggleRow(index: number) {
    setExpandedRows((prev) => {
      const next = new Set(prev);
      if (next.has(index)) next.delete(index);
      else next.add(index);
      return next;
    });
  }

  function getAriaSortValue(key: string): 'ascending' | 'descending' | 'none' {
    if (sortKey !== key || !sortDir) return 'none';
    return sortDir === 'asc' ? 'ascending' : 'descending';
  }

  // Group-based rendering: group full dataset first, then paginate by groups
  if (groupBy && groupSummary) {
    const allGroups = new Map<string, T[]>();
    for (const row of sortedData) {
      const key = String((row as Record<string, unknown>)[groupBy]);
      if (!allGroups.has(key)) allGroups.set(key, []);
      allGroups.get(key)!.push(row);
    }

    // Paginate by groups: keep groups intact, fill pages up to ~pageSize rows
    const groupEntries = Array.from(allGroups.entries());
    const groupPages: [string, T[]][][] = [[]];
    let currentPageRows = 0;
    for (const entry of groupEntries) {
      const groupRows = entry[1].length;
      // If adding this group would exceed pageSize and we already have groups on this page, start a new page
      if (currentPageRows > 0 && currentPageRows + groupRows > pageSize) {
        groupPages.push([]);
        currentPageRows = 0;
      }
      groupPages[groupPages.length - 1].push(entry);
      currentPageRows += groupRows;
    }

    const groupTotalPages = Math.max(1, groupPages.length);
    const safeCurrentPage = Math.min(currentPage, groupTotalPages);
    const currentGroups = groupPages[safeCurrentPage - 1] ?? [];
    const currentPageItemCount = currentGroups.reduce((sum, [, rows]) => sum + rows.length, 0);
    const itemsBefore = groupPages.slice(0, safeCurrentPage - 1).reduce(
      (sum, page) => sum + page.reduce((s, [, rows]) => s + rows.length, 0),
      0
    );
    const totalRows = sortedData.length;

    return (
      <div className="space-y-4">
        <div className="overflow-x-auto rounded-lg border border-[#334155]">
          <div className="relative">
            <div className="pointer-events-none absolute right-0 top-0 h-full w-8 bg-gradient-to-l from-[#1E293B] to-transparent md:hidden" />
            <table className="w-full min-w-[900px] text-sm">
              <thead>
                <tr className="border-b border-[#334155] bg-[#0F172A]">
                  {columns.map((col) => (
                    <th
                      key={col.key}
                      scope="col"
                      aria-sort={col.sortable ? getAriaSortValue(col.key) : undefined}
                      className={cn(
                        'px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-[#94A3B8]',
                        col.sortable && 'cursor-pointer select-none hover:text-white'
                      )}
                      onClick={col.sortable ? () => handleSort(col.key) : undefined}
                    >
                      <span className="flex items-center gap-1">
                        {col.header}
                        {col.sortable && <SortIcon sortKey={sortKey} sortDir={sortDir} colKey={col.key} />}
                      </span>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {currentGroups.map(([groupKey, rows], groupIdx) => {
                  const summary = groupSummary(groupKey, rows);
                  const offset = currentGroups
                    .slice(0, groupIdx)
                    .reduce((sum, [, r]) => sum + r.length, 0);
                  return (
                    <GroupRows
                      key={groupKey}
                      rows={rows}
                      columns={columns}
                      summary={summary}
                      rowClassName={rowClassName}
                      getRowKey={getRowKey}
                      globalOffset={offset}
                    />
                  );
                })}
                {totalRow && (
                  <tr className="border-t-2 border-[#22D3EE]/30 bg-[#0F172A] font-bold">
                    {columns.map((col) => (
                      <td key={col.key} className="px-4 py-3 text-white">
                        {totalRow[col.key] ?? ''}
                      </td>
                    ))}
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
        <Pagination
          currentPage={safeCurrentPage}
          totalPages={groupTotalPages}
          onPageChange={setCurrentPage}
          totalItems={totalRows}
          startItem={totalRows > 0 ? itemsBefore + 1 : 0}
          endItem={itemsBefore + currentPageItemCount}
        />
      </div>
    );
  }

  // Standard (non-grouped) rendering
  return (
    <div className="space-y-4">
      <div className="overflow-x-auto rounded-lg border border-[#334155]">
        <div className="relative">
          <div className="pointer-events-none absolute right-0 top-0 h-full w-8 bg-gradient-to-l from-[#1E293B] to-transparent md:hidden" />
          <table className="w-full min-w-[900px] text-sm">
            <thead>
              <tr className="border-b border-[#334155] bg-[#0F172A]">
                {columns.map((col) => (
                  <th
                    key={col.key}
                    scope="col"
                    aria-sort={col.sortable ? getAriaSortValue(col.key) : undefined}
                    className={cn(
                      'px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-[#94A3B8]',
                      col.sortable && 'cursor-pointer select-none hover:text-white'
                    )}
                    onClick={col.sortable ? () => handleSort(col.key) : undefined}
                  >
                    <span className="flex items-center gap-1">
                      {col.header}
                      {col.sortable && <SortIcon sortKey={sortKey} sortDir={sortDir} colKey={col.key} />}
                    </span>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {paginatedData.map((row, i) => {
                const globalIndex = (currentPage - 1) * pageSize + i;
                const rowKey = getRowKey ? getRowKey(row, globalIndex) : globalIndex;
                const isExpanded = expandedRows.has(globalIndex);
                return (
                  <TableRow
                    key={rowKey}
                    row={row}
                    columns={columns}
                    index={globalIndex}
                    isExpanded={isExpanded}
                    expandableRow={expandableRow}
                    onToggle={expandableRow ? () => toggleRow(globalIndex) : undefined}
                    rowClassName={rowClassName}
                    isEven={i % 2 === 0}
                  />
                );
              })}
              {paginatedData.length === 0 && (
                <tr>
                  <td colSpan={columns.length} className="px-4 py-8 text-center text-[#94A3B8]">
                    No data available
                  </td>
                </tr>
              )}
              {totalRow && (
                <tr className="border-t-2 border-[#22D3EE]/30 bg-[#0F172A] font-bold">
                  {columns.map((col) => (
                    <td key={col.key} className="px-4 py-3 text-white">
                      {totalRow[col.key] ?? ''}
                    </td>
                  ))}
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
      <Pagination
        currentPage={safeCurrentPage}
        totalPages={totalPages}
        onPageChange={setCurrentPage}
        totalItems={sortedData.length}
        pageSize={pageSize}
      />
    </div>
  );
}

function SortIcon({
  sortKey,
  sortDir,
  colKey,
}: {
  sortKey: string | null;
  sortDir: SortDirection;
  colKey: string;
}) {
  if (sortKey !== colKey || !sortDir) {
    return <ChevronsUpDown className="h-3.5 w-3.5 text-[#94A3B8]" />;
  }
  if (sortDir === 'asc') return <ChevronUp className="h-3.5 w-3.5 text-[#22D3EE]" />;
  return <ChevronDown className="h-3.5 w-3.5 text-[#22D3EE]" />;
}

function TableRow<T extends object>({
  row,
  columns,
  index,
  isExpanded,
  expandableRow,
  onToggle,
  rowClassName,
  isEven,
}: {
  row: T;
  columns: ColumnDef<T>[];
  index: number;
  isExpanded: boolean;
  expandableRow?: (row: T) => React.ReactNode;
  onToggle?: () => void;
  rowClassName?: (row: T) => string;
  isEven: boolean;
}) {
  const customClass = rowClassName ? rowClassName(row) : '';
  return (
    <>
      <tr
        className={cn(
          'border-b border-[#334155]/50 transition-colors',
          isEven ? 'bg-[#1E293B]' : 'bg-[#1E293B]/70',
          'hover:bg-[#22D3EE]/5',
          expandableRow && 'cursor-pointer',
          customClass
        )}
        onClick={onToggle}
        aria-expanded={expandableRow ? isExpanded : undefined}
        tabIndex={expandableRow ? 0 : undefined}
        onKeyDown={
          onToggle
            ? (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  onToggle();
                }
              }
            : undefined
        }
      >
        {columns.map((col) => (
          <td key={`${index}-${col.key}`} className="px-4 py-3 text-[#CBD5E1]">
            {col.render ? col.render(row) : (String((row as Record<string, unknown>)[col.key] ?? ''))}
          </td>
        ))}
      </tr>
      {expandableRow && isExpanded && (
        <tr className="bg-[#0F172A]">
          <td colSpan={columns.length} className="px-4 py-3">
            {expandableRow(row)}
          </td>
        </tr>
      )}
    </>
  );
}

function GroupRows<T extends object>({
  rows,
  columns,
  summary,
  rowClassName,
  getRowKey,
  globalOffset,
}: {
  rows: T[];
  columns: ColumnDef<T>[];
  summary: Record<string, React.ReactNode>;
  rowClassName?: (row: T) => string;
  getRowKey?: (row: T, index: number) => string;
  globalOffset: number;
}) {
  return (
    <>
      {rows.map((row, i) => {
        const customClass = rowClassName ? rowClassName(row) : '';
        const rowKey = getRowKey ? getRowKey(row, globalOffset + i) : `${globalOffset}-${i}`;
        return (
          <tr
            key={rowKey}
            className={cn(
              'border-b border-[#334155]/50 transition-colors',
              i % 2 === 0 ? 'bg-[#1E293B]' : 'bg-[#1E293B]/70',
              'hover:bg-[#22D3EE]/5',
              customClass
            )}
          >
            {columns.map((col) => (
              <td key={`${rowKey}-${col.key}`} className="px-4 py-3 text-[#CBD5E1]">
                {col.render ? col.render(row) : (String((row as Record<string, unknown>)[col.key] ?? ''))}
              </td>
            ))}
          </tr>
        );
      })}
      <tr className="border-b border-[#334155] bg-[#0F172A]/80 font-semibold">
        {columns.map((col) => (
          <td key={`summary-${col.key}`} className="px-4 py-2 text-[#94A3B8]">
            {summary[col.key] ?? ''}
          </td>
        ))}
      </tr>
    </>
  );
}
