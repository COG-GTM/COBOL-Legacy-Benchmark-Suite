export default function Home() {
  return (
    <main>
      <h1>Investment Portfolio Management System</h1>
      <p>Modernized from COBOL Legacy Benchmark Suite</p>
      <h2>API Endpoints</h2>
      <ul>
        <li><a href="/api/health">GET /api/health</a> - Database health check</li>
        <li><a href="/api/reports/positions">GET /api/reports/positions</a> - Position report</li>
        <li><a href="/api/reports/audit">GET /api/reports/audit</a> - Audit report</li>
        <li><a href="/api/reports/statistics">GET /api/reports/statistics</a> - Statistics report</li>
      </ul>
    </main>
  );
}
