import { useState, useMemo } from "react";
import { Search, ArrowUpDown } from "lucide-react";
import type { ErrorLogEntry } from "../../types";

interface ErrorLogTableProps {
  entries: ErrorLogEntry[];
}

const severityBadge = {
  critical: "bg-red-100 text-red-700",
  warning: "bg-yellow-100 text-yellow-700",
  info: "bg-blue-100 text-blue-700",
};

type SortKey = keyof ErrorLogEntry;
type SortDir = "asc" | "desc";

export default function ErrorLogTable({ entries }: ErrorLogTableProps) {
  const [filter, setFilter] = useState("");
  const [sortKey, setSortKey] = useState<SortKey>("timestamp");
  const [sortDir, setSortDir] = useState<SortDir>("desc");

  const handleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortKey(key);
      setSortDir("asc");
    }
  };

  const filtered = useMemo(() => {
    const q = filter.toLowerCase();
    return entries.filter(
      (e) =>
        e.errorCode.toLowerCase().includes(q) ||
        e.program.toLowerCase().includes(q) ||
        e.message.toLowerCase().includes(q)
    );
  }, [entries, filter]);

  const sorted = useMemo(() => {
    return [...filtered].sort((a, b) => {
      const aVal = a[sortKey];
      const bVal = b[sortKey];
      const cmp = String(aVal).localeCompare(String(bVal));
      return sortDir === "asc" ? cmp : -cmp;
    });
  }, [filtered, sortKey, sortDir]);

  const columns: { key: SortKey; label: string; className: string }[] = [
    { key: "timestamp", label: "Timestamp", className: "w-40" },
    { key: "errorCode", label: "Error Code", className: "w-36" },
    { key: "program", label: "Program", className: "w-28" },
    { key: "severity", label: "Severity", className: "w-24" },
    { key: "message", label: "Message", className: "" },
  ];

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h3 className="text-sm font-semibold text-gray-700">
          Recent Error Log
        </h3>
        <div className="relative">
          <Search
            size={14}
            className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400"
          />
          <input
            type="text"
            placeholder="Filter by code, program, or message..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="rounded-md border border-gray-300 py-1.5 pl-8 pr-3 text-xs text-gray-700 placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            aria-label="Filter error log entries"
          />
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead>
            <tr className="border-b border-gray-200">
              {columns.map((col) => (
                <th
                  key={col.key}
                  className={`cursor-pointer px-3 py-2 font-medium text-gray-500 hover:text-gray-700 ${col.className}`}
                  onClick={() => handleSort(col.key)}
                >
                  <div className="flex items-center gap-1">
                    {col.label}
                    <ArrowUpDown
                      size={12}
                      className={
                        sortKey === col.key
                          ? "text-blue-500"
                          : "text-gray-300"
                      }
                    />
                  </div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {sorted.map((entry, idx) => (
              <tr
                key={`${entry.timestamp}-${idx}`}
                className="border-b border-gray-100 hover:bg-gray-50"
              >
                <td className="whitespace-nowrap px-3 py-2 text-gray-600">
                  {entry.timestamp}
                </td>
                <td className="whitespace-nowrap px-3 py-2 font-mono text-gray-700">
                  {entry.errorCode}
                </td>
                <td className="whitespace-nowrap px-3 py-2 font-mono text-gray-700">
                  {entry.program}
                </td>
                <td className="whitespace-nowrap px-3 py-2">
                  <span
                    className={`inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-medium ${severityBadge[entry.severity]}`}
                  >
                    {entry.severity}
                  </span>
                </td>
                <td className="px-3 py-2 text-gray-600">{entry.message}</td>
              </tr>
            ))}
            {sorted.length === 0 && (
              <tr>
                <td colSpan={5} className="px-3 py-6 text-center text-gray-400">
                  No matching entries found
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <p className="mt-3 text-[10px] text-gray-400">
        Showing {sorted.length} of {entries.length} most recent entries
      </p>
    </div>
  );
}
