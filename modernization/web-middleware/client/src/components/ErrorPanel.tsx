/** ERRMAP equivalent: error code (ERRCOUT) + details (ERRDOUT), BMS RED -> theme error. */
export default function ErrorPanel({ code, details }: { code: string | number; details: string }) {
  return (
    <section className="error-panel" role="alert">
      <h2>System Error</h2>
      <div className="error-row">
        <span className="label">Error Code:</span>
        <span className="error-value">{code}</span>
      </div>
      <div className="error-row">
        <span className="label">Details:</span>
        <span className="error-value">{details}</span>
      </div>
    </section>
  );
}
