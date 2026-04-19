import { useState, useEffect } from "react";
import { adminApi } from "../../api/admin";

export function AdminLogsPage() {
  const [logs, setLogs] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({
    operatorName: "",
    startTime: "",
    endTime: "",
    actionName: "",
    page: 1,
    size: 10,
  });
  const [token] = useState(localStorage.getItem("token") || "");

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const response = await adminApi.getLogs(filters, token);
      setLogs(response.data.list || []);
      setTotal(response.data.total || 0);
    } catch (error) {
      console.error("Failed to fetch logs:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [filters.page]);

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters(prev => ({ ...prev, [name]: value, page: 1 }));
  };

  const handleSearch = () => {
    fetchLogs();
  };

  const handleExport = async () => {
    try {
      const response = await adminApi.exportLogs(filters, token);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", "logs.xlsx");
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error("Failed to export logs:", error);
    }
  };

  const handlePageChange = (newPage) => {
    setFilters(prev => ({ ...prev, page: newPage }));
  };

  return (
    <div className="page-stack">
      <section className="page-card">
        <div className="section-head">
          <div>
            <span className="eyebrow">Admin Scope</span>
            <h2 className="section-title">Operation Logs</h2>
          </div>
          <p className="page-note">
            View and manage system operation logs for auditing and troubleshooting.
          </p>
        </div>

        <div className="filter-section">
          <div className="filter-row">
            <input
              type="text"
              name="operatorName"
              placeholder="Operator Name"
              value={filters.operatorName}
              onChange={handleFilterChange}
              className="filter-input"
            />
            <input
              type="text"
              name="actionName"
              placeholder="Action Name"
              value={filters.actionName}
              onChange={handleFilterChange}
              className="filter-input"
            />
            <input
              type="datetime-local"
              name="startTime"
              placeholder="Start Time"
              value={filters.startTime}
              onChange={handleFilterChange}
              className="filter-input"
            />
            <input
              type="datetime-local"
              name="endTime"
              placeholder="End Time"
              value={filters.endTime}
              onChange={handleFilterChange}
              className="filter-input"
            />
            <button className="primary-button" onClick={handleSearch}>
              Search
            </button>
            <button className="secondary-button" onClick={handleExport}>
              Export
            </button>
          </div>
        </div>

        <div className="logs-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Operator</th>
                <th>Action</th>
                <th>Module</th>
                <th>Result</th>
                <th>Time</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="6">Loading...</td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan="6">No logs found</td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id}>
                    <td>{log.id}</td>
                    <td>{log.operatorName}</td>
                    <td>{log.actionName}</td>
                    <td>{log.moduleName}</td>
                    <td>{log.resultMessage}</td>
                    <td>{new Date(log.createdAt).toLocaleString()}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="pagination">
          <button
            className="secondary-button"
            disabled={filters.page === 1}
            onClick={() => handlePageChange(filters.page - 1)}
          >
            Previous
          </button>
          <span>
            Page {filters.page} of {Math.ceil(total / filters.size)}
          </span>
          <button
            className="secondary-button"
            disabled={filters.page >= Math.ceil(total / filters.size)}
            onClick={() => handlePageChange(filters.page + 1)}
          >
            Next
          </button>
        </div>
      </section>
    </div>
  );
}