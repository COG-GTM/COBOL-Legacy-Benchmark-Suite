import { useEffect, useState } from 'react';
import type { AuditRecord } from '../types/audit';
import { subscribeToAudit } from '../services/auditService';

/**
 * Administrator-only screen. Demonstrates role-based gating (read-only users
 * are redirected away by ProtectedRoute) and previews the security audit trail
 * that SECMGR writes to AUDITLOG.
 */
export function AdminPage() {
  const [records, setRecords] = useState<readonly AuditRecord[]>([]);

  useEffect(() => subscribeToAudit(setRecords), []);

  return (
    <section className="page">
      <h1>Administration</h1>
      <p>Security audit trail (login / logout events).</p>
      <table className="audit-table">
        <thead>
          <tr>
            <th>Timestamp</th>
            <th>User</th>
            <th>Action</th>
            <th>Status</th>
            <th>Message</th>
          </tr>
        </thead>
        <tbody>
          {records.length === 0 ? (
            <tr>
              <td colSpan={5} className="muted">
                No audit records yet.
              </td>
            </tr>
          ) : (
            records
              .slice()
              .reverse()
              .map((record, index) => (
                <tr key={`${record.timestamp}-${index}`}>
                  <td>{new Date(record.timestamp).toLocaleString()}</td>
                  <td>{record.userId}</td>
                  <td>{record.action}</td>
                  <td>
                    <span className={`status status-${record.status.toLowerCase()}`}>
                      {record.status}
                    </span>
                  </td>
                  <td>{record.message}</td>
                </tr>
              ))
          )}
        </tbody>
      </table>
    </section>
  );
}
