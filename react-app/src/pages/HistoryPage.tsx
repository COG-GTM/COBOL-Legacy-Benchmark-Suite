import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { fetchHistory, type HistoryPage as HistoryPageData } from "../api/history";
import HistoryTable from "../components/HistoryTable";
import ErrorBanner from "../components/ErrorBanner";

/**
 * Transaction History Inquiry — replaces HISMAP BMS screen (INQSET.bms lines 53–85).
 * Maps to WHEN 'INQH' branch in INQONLN.cbl which calls INQHIST.
 *
 * Behavior from INQHIST.cbl:
 *   - P100: Initialize, connect to DB2
 *   - P200: Execute query with cursor (ORDER BY TRANS_DATE DESC)
 *   - P250: Fetch 10 rows at a time (array fetch)
 *   - P300: Format and send to HISMAP
 */
export default function HistoryPage() {
  const navigate = useNavigate();
  const [data, setData] = useState<HistoryPageData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [currentPage, setCurrentPage] = useState(1);

  const loadPage = useCallback(async (page: number) => {
    setLoading(true);
    setError("");
    try {
      const result = await fetchHistory(page);
      setData(result);
      setCurrentPage(result.currentPage);
    } catch {
      setError("Failed to fetch transaction history");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPage(1);
  }, [loadPage]);

  const handlePrevious = () => {
    if (data?.hasPrevious) {
      loadPage(currentPage - 1);
    }
  };

  const handleNext = () => {
    if (data?.hasNext) {
      loadPage(currentPage + 1);
    }
  };

  const handleExit = () => {
    navigate("/menu");
  };

  return (
    <div style={containerStyle}>
      <div style={terminalStyle}>
        {/* Row 1: Title */}
        <h1 style={titleStyle}>Transaction History Inquiry</h1>

        {/* Row 3: Account */}
        <div style={accountRowStyle}>
          <span style={labelStyle}>Account:</span>
          <span style={valueStyle}>1001234567</span>
        </div>

        {loading ? (
          <div style={loadingStyle}>Loading transaction history...</div>
        ) : data ? (
          <HistoryTable
            entries={data.entries}
            currentPage={data.currentPage}
            totalPages={data.totalPages}
            hasNext={data.hasNext}
            hasPrevious={data.hasPrevious}
            message={data.message}
            onPrevious={handlePrevious}
            onNext={handleNext}
            onExit={handleExit}
          />
        ) : null}

        {/* Error area */}
        {error && <ErrorBanner message={error} onDismiss={() => setError("")} />}
      </div>
    </div>
  );
}

const containerStyle: React.CSSProperties = {
  minHeight: "100vh",
  backgroundColor: "#0a1929",
  display: "flex",
  justifyContent: "center",
  alignItems: "center",
  padding: "20px",
};

const terminalStyle: React.CSSProperties = {
  backgroundColor: "#0d2137",
  border: "2px solid #1e3a5f",
  borderRadius: "8px",
  padding: "32px 40px",
  maxWidth: "800px",
  width: "100%",
  boxShadow: "0 4px 24px rgba(0, 0, 0, 0.4)",
};

const titleStyle: React.CSSProperties = {
  color: "#ffffff",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "22px",
  fontWeight: "bold",
  marginBottom: "16px",
};

const accountRowStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: "8px",
  marginBottom: "20px",
};

const labelStyle: React.CSSProperties = {
  color: "#a0c4e8",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "14px",
};

const valueStyle: React.CSSProperties = {
  color: "#00b7c3",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "14px",
};

const loadingStyle: React.CSSProperties = {
  color: "#a0c4e8",
  fontFamily: "'Courier New', Courier, monospace",
  fontSize: "14px",
  padding: "20px 0",
};
