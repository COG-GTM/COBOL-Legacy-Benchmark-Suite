import type { HistoryEntry } from "../api/history";

interface HistoryTableProps {
  entries: HistoryEntry[];
  currentPage: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  message: string;
  onPrevious: () => void;
  onNext: () => void;
  onExit: () => void;
}

/**
 * Paginated 10-row transaction history table.
 * Maps to HISMAP in INQSET.bms (lines 53–85):
 *   - Column headers: Date (10), Type (4), Units (10), Price (10), Amount (12)
 *   - 10 data rows: ROW1–ROW10
 *   - Navigation: PF3=Exit, PF7=Previous, PF8=Next
 *   - Message area: HISMSG
 */
export default function HistoryTable({
  entries,
  currentPage,
  totalPages,
  hasNext,
  hasPrevious,
  message,
  onPrevious,
  onNext,
  onExit,
}: HistoryTableProps) {
  return (
    <div>
      <table
        style={{
          width: "100%",
          borderCollapse: "collapse",
          fontFamily: "'Courier New', Courier, monospace",
          fontSize: "14px",
        }}
      >
        <thead>
          <tr
            style={{
              backgroundColor: "#1e3a5f",
              color: "#ffffff",
              fontWeight: "bold",
            }}
          >
            <th style={{ ...headerStyle, width: "120px" }}>Date</th>
            <th style={{ ...headerStyle, width: "60px" }}>Type</th>
            <th style={{ ...headerStyle, width: "120px", textAlign: "right" }}>
              Units
            </th>
            <th style={{ ...headerStyle, width: "120px", textAlign: "right" }}>
              Price
            </th>
            <th style={{ ...headerStyle, width: "140px", textAlign: "right" }}>
              Amount
            </th>
          </tr>
        </thead>
        <tbody>
          {entries.map((entry, idx) => (
            <tr
              key={`${entry.transDate}-${idx}`}
              style={{
                backgroundColor: idx % 2 === 0 ? "#f0f8ff" : "#ffffff",
                color: "#00b7c3",
              }}
            >
              <td style={cellStyle}>{entry.transDate}</td>
              <td style={cellStyle}>{entry.transType}</td>
              <td style={{ ...cellStyle, textAlign: "right" }}>
                {entry.transUnits.toFixed(3)}
              </td>
              <td style={{ ...cellStyle, textAlign: "right" }}>
                {entry.transPrice.toFixed(2)}
              </td>
              <td style={{ ...cellStyle, textAlign: "right" }}>
                {entry.transAmount.toFixed(2)}
              </td>
            </tr>
          ))}
          {/* Pad with empty rows to always show 10 rows */}
          {Array.from({ length: 10 - entries.length }).map((_, idx) => (
            <tr
              key={`empty-${idx}`}
              style={{
                backgroundColor:
                  (entries.length + idx) % 2 === 0 ? "#f0f8ff" : "#ffffff",
              }}
            >
              <td style={cellStyle}>&nbsp;</td>
              <td style={cellStyle}>&nbsp;</td>
              <td style={cellStyle}>&nbsp;</td>
              <td style={cellStyle}>&nbsp;</td>
              <td style={cellStyle}>&nbsp;</td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Navigation — maps to PF3=Exit  PF7=Previous  PF8=Next */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginTop: "16px",
          padding: "8px 0",
        }}
      >
        <button onClick={onExit} style={navButtonStyle}>
          Exit (PF3)
        </button>
        <div style={{ display: "flex", gap: "12px", alignItems: "center" }}>
          <button
            onClick={onPrevious}
            disabled={!hasPrevious}
            style={{
              ...navButtonStyle,
              opacity: hasPrevious ? 1 : 0.4,
              cursor: hasPrevious ? "pointer" : "not-allowed",
            }}
          >
            Previous (PF7)
          </button>
          <span
            style={{
              fontFamily: "'Courier New', Courier, monospace",
              fontSize: "13px",
              color: "#555",
            }}
          >
            Page {currentPage} of {totalPages}
          </span>
          <button
            onClick={onNext}
            disabled={!hasNext}
            style={{
              ...navButtonStyle,
              opacity: hasNext ? 1 : 0.4,
              cursor: hasNext ? "pointer" : "not-allowed",
            }}
          >
            Next (PF8)
          </button>
        </div>
      </div>

      {/* Message area — maps to HISMSG field */}
      {message && (
        <div
          style={{
            marginTop: "8px",
            padding: "8px 12px",
            backgroundColor: "#f0f0f0",
            borderLeft: "3px solid #1e3a5f",
            fontFamily: "'Courier New', Courier, monospace",
            fontSize: "13px",
            color: "#333",
          }}
        >
          {message}
        </div>
      )}
    </div>
  );
}

const headerStyle: React.CSSProperties = {
  padding: "10px 12px",
  textAlign: "left",
  borderBottom: "2px solid #0d2137",
};

const cellStyle: React.CSSProperties = {
  padding: "8px 12px",
  borderBottom: "1px solid #ddd",
};

const navButtonStyle: React.CSSProperties = {
  padding: "8px 20px",
  backgroundColor: "#1e3a5f",
  color: "#ffffff",
  border: "none",
  borderRadius: "4px",
  cursor: "pointer",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "13px",
  fontWeight: "bold",
};
