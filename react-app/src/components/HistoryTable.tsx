/**
 * HistoryTable — paginated 10-row table with Previous/Next buttons.
 * Matches the BMS HISMAP layout (INQSET.bms lines 61-84):
 *   Columns: Date (10), Type (4), Units (10), Price (10), Amount (12)
 *   10 data rows (ROW1–ROW10)
 *   Navigation: PF3=Exit, PF7=Previous, PF8=Next
 */

import type { HistoryEntry } from "../api/history";

interface Props {
  entries: HistoryEntry[];
  currentPage: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  onNext: () => void;
  onPrevious: () => void;
  onExit: () => void;
  message?: string;
}

export default function HistoryTable({
  entries,
  currentPage,
  totalPages,
  hasNext,
  hasPrevious,
  onNext,
  onPrevious,
  onExit,
  message,
}: Props) {
  // Pad to exactly 10 rows to match BMS ROW1-ROW10
  const rows = [...entries];
  while (rows.length < 10) {
    rows.push({
      transDate: "",
      transType: "",
      transUnits: 0,
      transPrice: 0,
      transAmount: 0,
    });
  }

  return (
    <div>
      <table style={styles.table}>
        <thead>
          <tr>
            <th style={{ ...styles.th, width: 110 }}>Date</th>
            <th style={{ ...styles.th, width: 60 }}>Type</th>
            <th style={{ ...styles.th, width: 110, textAlign: "right" }}>
              Units
            </th>
            <th style={{ ...styles.th, width: 110, textAlign: "right" }}>
              Price
            </th>
            <th style={{ ...styles.th, width: 130, textAlign: "right" }}>
              Amount
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, idx) => (
            <tr key={idx} style={styles.row}>
              <td style={styles.td}>{row.transDate}</td>
              <td style={styles.td}>{row.transType}</td>
              <td style={{ ...styles.td, textAlign: "right" }}>
                {row.transDate
                  ? row.transUnits.toFixed(3)
                  : ""}
              </td>
              <td style={{ ...styles.td, textAlign: "right" }}>
                {row.transDate
                  ? `$${row.transPrice.toFixed(2)}`
                  : ""}
              </td>
              <td style={{ ...styles.td, textAlign: "right" }}>
                {row.transDate
                  ? `$${row.transAmount.toLocaleString("en-US", {
                      minimumFractionDigits: 2,
                    })}`
                  : ""}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Navigation — mirrors PF3=Exit  PF7=Previous  PF8=Next */}
      <div style={styles.nav}>
        <div style={styles.navButtons}>
          <button style={styles.btn} onClick={onExit}>
            Exit (PF3)
          </button>
          <button
            style={styles.btn}
            onClick={onPrevious}
            disabled={!hasPrevious}
          >
            Previous (PF7)
          </button>
          <button
            style={styles.btn}
            onClick={onNext}
            disabled={!hasNext}
          >
            Next (PF8)
          </button>
        </div>
        <span style={styles.pageInfo}>
          Page {currentPage} of {totalPages}
        </span>
      </div>

      {/* Message area — maps to HISMSG field in BMS map */}
      {message && <div style={styles.message}>{message}</div>}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  table: {
    width: "100%",
    borderCollapse: "collapse" as const,
    marginBottom: 16,
  },
  th: {
    textAlign: "left" as const,
    padding: "10px 12px",
    borderBottom: "2px solid #334455",
    color: "#aabbcc",
    fontSize: 13,
    fontWeight: 600,
  },
  row: {
    borderBottom: "1px solid #222e44",
  },
  td: {
    padding: "8px 12px",
    fontSize: 14,
    color: "#40e0d0",
    height: 20,
  },
  nav: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginTop: 8,
  },
  navButtons: {
    display: "flex",
    gap: 8,
  },
  btn: {
    padding: "8px 16px",
    borderRadius: 4,
    border: "1px solid #334455",
    background: "#0f3460",
    color: "#e0e0e0",
    fontSize: 13,
    cursor: "pointer",
  },
  pageInfo: {
    fontSize: 13,
    color: "#556677",
  },
  message: {
    marginTop: 12,
    padding: "8px 12px",
    borderRadius: 4,
    background: "#2a1a2e",
    color: "#ff6b6b",
    fontSize: 13,
  },
};
