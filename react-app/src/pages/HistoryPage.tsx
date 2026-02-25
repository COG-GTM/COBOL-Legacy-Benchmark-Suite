/**
 * HistoryPage — transaction history view.
 * Replaces HISMAP BMS screen + INQHIST program logic.
 * Maps to the WHEN 'INQH' branch in INQONLN.cbl.
 *
 * Fetches paginated history data (10 rows per page, matching
 * the 10-row BMS map ROW1–ROW10 in INQSET.bms).
 */

import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { fetchTransactionHistory, type HistoryPage as HistoryPageData } from "../api/history";
import HistoryTable from "../components/HistoryTable";

export default function HistoryPage() {
  const [data, setData] = useState<HistoryPageData | null>(null);
  const [page, setPage] = useState(1);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const loadPage = useCallback((p: number) => {
    setLoading(true);
    setMessage("");
    fetchTransactionHistory(p)
      .then((result) => {
        setData(result);
        setPage(result.currentPage);
        setLoading(false);
        if (result.entries.length === 0) {
          setMessage("No history records found");
        }
      })
      .catch(() => {
        setMessage("Error retrieving history data");
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    loadPage(1);
  }, [loadPage]);

  function handleNext() {
    if (data?.hasNext) loadPage(page + 1);
  }

  function handlePrevious() {
    if (data?.hasPrevious) loadPage(page - 1);
  }

  function handleExit() {
    navigate("/menu");
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h1 style={styles.title}>Transaction History Inquiry</h1>

        <div style={styles.accountRow}>
          <span style={styles.label}>Account:</span>
          <span style={styles.value}>1001234567</span>
        </div>

        {loading ? (
          <p style={styles.loading}>Loading history...</p>
        ) : data ? (
          <HistoryTable
            entries={data.entries}
            currentPage={data.currentPage}
            totalPages={data.totalPages}
            hasNext={data.hasNext}
            hasPrevious={data.hasPrevious}
            onNext={handleNext}
            onPrevious={handlePrevious}
            onExit={handleExit}
            message={message}
          />
        ) : message ? (
          <p style={{ color: "#F87171", fontSize: 14 }}>{message}</p>
        ) : null}
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: "flex",
    justifyContent: "center",
    alignItems: "flex-start",
    minHeight: "100vh",
    padding: "40px 16px",
    background: "#0F172A",
    fontFamily: "Inter, Roboto, 'Segoe UI', system-ui, sans-serif",
  },
  card: {
    background: "#1E293B",
    borderRadius: 12,
    padding: "32px 40px",
    boxShadow: "0 4px 24px rgba(0,0,0,0.4)",
    width: "100%",
    maxWidth: 750,
    color: "#FFFFFF",
  },
  title: {
    margin: "0 0 20px",
    fontSize: 20,
    color: "#22D3EE",
  },
  accountRow: {
    marginBottom: 20,
    fontSize: 14,
  },
  label: {
    color: "#94A3B8",
    marginRight: 8,
  },
  value: {
    color: "#22D3EE",
  },
  loading: {
    color: "#94A3B8",
    fontSize: 14,
  },
};
