interface ErrorBannerProps {
  message: string;
  onDismiss?: () => void;
}

/**
 * Inline error message display.
 * Maps to ERRMSG/POSMSG/HISMSG fields in BMS maps (COLOR=RED, PROT, BRT).
 */
export default function ErrorBanner({ message, onDismiss }: ErrorBannerProps) {
  if (!message) return null;

  return (
    <div
      style={{
        backgroundColor: "#fee2e2",
        border: "1px solid #ef4444",
        color: "#dc2626",
        padding: "12px 16px",
        borderRadius: "4px",
        marginTop: "16px",
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        fontFamily: "'Courier New', Courier, monospace",
      }}
    >
      <span>{message}</span>
      {onDismiss && (
        <button
          onClick={onDismiss}
          style={{
            background: "none",
            border: "none",
            color: "#dc2626",
            cursor: "pointer",
            fontSize: "18px",
            fontWeight: "bold",
            padding: "0 4px",
          }}
        >
          ×
        </button>
      )}
    </div>
  );
}
