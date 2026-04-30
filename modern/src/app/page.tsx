import Link from "next/link";

export default function Home() {
  return (
    <main style={{ padding: "2rem", fontFamily: "system-ui" }}>
      <h1>CLBS Portfolio Manager</h1>
      <p>Modernized Investment Portfolio Management System</p>
      <nav>
        <ul>
          <li><Link href="/api/health">Health Check</Link></li>
          <li><Link href="/api/portfolios">Portfolios API</Link></li>
          <li><Link href="/api/batch">Batch Jobs API</Link></li>
        </ul>
      </nav>
    </main>
  );
}
