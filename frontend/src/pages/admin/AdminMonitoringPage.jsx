import { useEffect, useMemo, useState } from "react";
import { adminApi } from "../../api/admin";
import { StatCard } from "../../components/StatCard";

export function AdminMonitoringPage({ workspace }) {
  const token = workspace?.token;
  const [overview, setOverview] = useState(null);
  const [report, setReport] = useState(null);
  const [runtimeStatus, setRuntimeStatus] = useState(null);
  const [logs, setLogs] = useState([]);
  const [abnormalBehaviors, setAbnormalBehaviors] = useState([]);
  const [backups, setBackups] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("success");
  const [restoringBackupId, setRestoringBackupId] = useState(null);

  async function loadMonitoringData() {
    if (!token) {
      return;
    }
    setLoading(true);
    setMessage("");
    try {
      const [overviewResult, reportResult, runtimeResult, logRows, abnormalRows, backupRows] = await Promise.all([
        adminApi.getMonitoringOverview(token),
        adminApi.getBusinessReport(token),
        adminApi.getRuntimeStatus(token),
        adminApi.listOperationLogs(token),
        adminApi.listAbnormalBehaviors(token),
        adminApi.listBackupRecords(token),
      ]);
      setOverview(overviewResult || {});
      setReport(reportResult || {});
      setRuntimeStatus(runtimeResult || {});
      setLogs(logRows || []);
      setAbnormalBehaviors(abnormalRows || []);
      setBackups(backupRows || []);
    } catch (requestError) {
      showMessage("error", requestError.message || "Failed to load monitoring data.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadMonitoringData();
  }, [token]);

  function showMessage(type, text) {
    setMessageType(type);
    setMessage(text);
  }

  async function handleCreateBackup() {
    try {
      setLoading(true);
      showMessage("success", "");
      const result = await adminApi.executeBackup(token);
      showMessage("success", `Backup ${result.backupName} created successfully.`);
      await loadMonitoringData();
    } catch (requestError) {
      showMessage("error", requestError.message || "Failed to create backup.");
      setLoading(false);
    }
  }

  async function handleRestoreBackup(backup) {
    const confirmMessage = `Restore backup ${backup.backupName}? This will replace current business data with the snapshot content.`;
    if (!window.confirm(confirmMessage)) {
      return;
    }
    try {
      setRestoringBackupId(backup.backupId);
      setLoading(true);
      showMessage("success", "");
      const result = await adminApi.restoreBackup(backup.backupId, token);
      const restoredTables = (result.restoredTables || []).map((item) => `${item.label}: ${item.value}`).join(", ");
      showMessage("success", `Backup ${result.backupName} restored successfully. ${restoredTables}`);
      await loadMonitoringData();
    } catch (requestError) {
      showMessage("error", requestError.message || "Failed to restore backup.");
      setLoading(false);
    } finally {
      setRestoringBackupId(null);
    }
  }

  const summaryCards = useMemo(() => ([
    { label: "Total Users", value: overview?.totalUsers ?? 0 },
    { label: "Total Books", value: overview?.totalBooks ?? 0 },
    { label: "Active Borrows", value: overview?.activeBorrows ?? 0 },
    { label: "Latest Backup", value: overview?.latestBackup || "No backup yet" },
  ]), [overview]);

  const reportBreakdowns = [
    { title: "Borrow Status Breakdown", items: report?.borrowStatusBreakdown || [] },
    { title: "Borrow Request Breakdown", items: report?.requestStatusBreakdown || [] },
    { title: "Reservation Breakdown", items: report?.reservationStatusBreakdown || [] },
  ];

  return (
    <div className="page-stack">
      <section className="page-card">
        <div className="section-head compact">
          <div>
            <span className="eyebrow">Admin</span>
            <h2 className="section-title">Monitoring And Recovery</h2>
          </div>
          <div className="inline-actions">
            <button className="primary-button" type="button" onClick={handleCreateBackup} disabled={loading}>
              {loading ? "Processing..." : "Create Backup"}
            </button>
            <button className="secondary-button" type="button" onClick={loadMonitoringData} disabled={loading}>
              Refresh
            </button>
          </div>
        </div>

        <div className="stats-grid stats-grid-4">
          {summaryCards.map((item) => (
            <StatCard key={item.label} label={item.label} value={item.value} />
          ))}
        </div>

        {message ? (
          <p className="page-note" style={{ color: messageType === "success" ? "#15803d" : "#b91c1c" }}>
            {message}
          </p>
        ) : null}
      </section>

      <section className="page-card split-grid">
        <div>
          <h3 className="section-title">Runtime Status</h3>
          <div className="monitor-grid">
            <div className="monitor-card">
              <small>System Status</small>
              <strong>{runtimeStatus?.systemStatus || overview?.systemStatus || "Unknown"}</strong>
            </div>
            <div className="monitor-card">
              <small>Database</small>
              <strong>{runtimeStatus?.databaseStatus || "Unknown"}</strong>
            </div>
            <div className="monitor-card">
              <small>Backup Status</small>
              <strong>{runtimeStatus?.backupStatus || "Unknown"}</strong>
            </div>
          </div>

          <div className="monitor-grid" style={{ marginTop: "14px" }}>
            <div className="monitor-card">
              <small>Recent Failure Logs</small>
              <strong>{runtimeStatus?.recentFailureLogs ?? 0}</strong>
            </div>
            <div className="monitor-card">
              <small>Disabled Users</small>
              <strong>{runtimeStatus?.disabledUsers ?? 0}</strong>
            </div>
            <div className="monitor-card">
              <small>Books Out Of Stock</small>
              <strong>{runtimeStatus?.booksWithoutAvailableCopies ?? 0}</strong>
            </div>
          </div>
        </div>

        <div>
          <h3 className="section-title">Recovery Readiness</h3>
          <div className="empty-state">
            <strong>Operational Guidance</strong>
            <p className="page-note">
              Backups created from this version are restorable snapshots. Restoring a snapshot rewinds core business data and keeps audit logs for the restore action itself.
            </p>
          </div>
          <div className="metric-list">
            {(runtimeStatus?.alerts || []).map((item) => (
              <div key={item.label} className="metric-row">
                <span>{item.label}</span>
                <strong>{item.value}</strong>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="page-card">
        <div className="section-head compact">
          <div>
            <h3 className="section-title">Business Reports</h3>
            <p className="page-note">Usage, backlog, fines, and inventory coverage in one place.</p>
          </div>
        </div>
        <div className="stats-grid stats-grid-4">
          <StatCard label="Readers" value={report?.totalReaders ?? 0} />
          <StatCard label="Borrow Requests" value={report?.totalBorrowRequests ?? 0} />
          <StatCard label="Pending Requests" value={report?.pendingBorrowRequests ?? 0} />
          <StatCard label="Overdue Borrows" value={report?.overdueBorrows ?? 0} />
          <StatCard label="Active Reservations" value={report?.activeReservations ?? 0} />
          <StatCard label="Unavailable Books" value={report?.unavailableBooks ?? 0} />
          <StatCard label="Available Copies" value={report?.availableInventoryCopies ?? 0} />
          <StatCard label="Unpaid Fine Amount" value={formatCurrency(report?.unpaidFineAmount)} />
        </div>

        <div className="split-grid" style={{ marginTop: "18px" }}>
          {reportBreakdowns.map((group) => (
            <div key={group.title} className="monitor-card">
              <strong>{group.title}</strong>
              <div className="metric-list">
                {group.items.length > 0 ? group.items.map((item) => (
                  <div key={`${group.title}-${item.label}`} className="metric-row">
                    <span>{item.label}</span>
                    <strong>{item.value}</strong>
                  </div>
                )) : <span className="page-note">No data.</span>}
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="page-card">
        <h3 className="section-title">Backup Records</h3>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Backup Name</th>
                <th>Status</th>
                <th>Restorable</th>
                <th>Summary</th>
                <th>File Path</th>
                <th>Created At</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {backups.map((item) => (
                <tr key={item.backupId}>
                  <td>{item.backupId}</td>
                  <td>{item.backupName}</td>
                  <td>{item.status}</td>
                  <td>{item.restorable ? "Yes" : "No"}</td>
                  <td>{item.summary || "-"}</td>
                  <td>{item.filePath}</td>
                  <td>{item.createdAt || "-"}</td>
                  <td>
                    <button
                      className="secondary-button"
                      type="button"
                      onClick={() => handleRestoreBackup(item)}
                      disabled={!item.restorable || loading || restoringBackupId === item.backupId}
                    >
                      {restoringBackupId === item.backupId ? "Restoring..." : "Restore"}
                    </button>
                  </td>
                </tr>
              ))}
              {!loading && backups.length === 0 ? <tr><td colSpan="8">No backup records.</td></tr> : null}
            </tbody>
          </table>
        </div>
      </section>

      <section className="page-card split-grid">
        <div>
          <h3 className="section-title">Key Operation Logs</h3>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Module</th>
                  <th>Action</th>
                  <th>Operator</th>
                  <th>Result</th>
                  <th>Created At</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((item) => (
                  <tr key={item.logId}>
                    <td>{item.logId}</td>
                    <td>{item.moduleName}</td>
                    <td>{item.actionName}</td>
                    <td>{item.operatorName}</td>
                    <td>{item.resultMessage || "-"}</td>
                    <td>{item.createdAt || "-"}</td>
                  </tr>
                ))}
                {!loading && logs.length === 0 ? <tr><td colSpan="6">No operation logs.</td></tr> : null}
              </tbody>
            </table>
          </div>
        </div>

        <div>
          <h3 className="section-title">Abnormal Behaviors</h3>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Module</th>
                  <th>Action</th>
                  <th>Operator</th>
                  <th>Detail</th>
                </tr>
              </thead>
              <tbody>
                {abnormalBehaviors.map((item) => (
                  <tr key={item.logId}>
                    <td>{item.logId}</td>
                    <td>{item.moduleName}</td>
                    <td>{item.actionName}</td>
                    <td>{item.operatorName}</td>
                    <td>{item.resultMessage || "-"}</td>
                  </tr>
                ))}
                {!loading && abnormalBehaviors.length === 0 ? <tr><td colSpan="5">No abnormal behaviors.</td></tr> : null}
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>
  );
}

function formatCurrency(value) {
  const numberValue = Number(value ?? 0);
  if (Number.isNaN(numberValue)) {
    return value ?? "0";
  }
  return numberValue.toFixed(2);
}
