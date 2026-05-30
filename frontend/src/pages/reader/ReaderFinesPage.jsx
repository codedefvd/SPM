import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { readerApi } from "../../api/client";
import { StatCard } from "../../components/StatCard";
import Barcode from "../../components/Barcode";

function formatMoney(value) {
  return `$${Number(value || 0).toFixed(2)}`;
}

export function ReaderFinesPage({ workspace }) {
  const navigate = useNavigate();
  const permissionsLoaded = Array.isArray(workspace?.permissions);
  const permissions = permissionsLoaded ? workspace.permissions : [];
  const canViewFines =
    !permissionsLoaded
    || permissions.includes("RETURN_REQUEST")
    || permissions.includes("BORROW_REQUEST")
    || permissions.includes("BOOK_VIEW")
    || permissions.includes("BOOK_SEARCH");

  const [fines, setFines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function loadFines() {
    if (!canViewFines) {
      setFines([]);
      setLoading(false);
      return;
    }

    setLoading(true);
    setError("");
    try {
      const result = await readerApi.listFines(workspace?.token);
      setFines(result || []);
    } catch (requestError) {
      setError(requestError.message || "Failed to load fines");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadFines();
  }, [workspace?.token, canViewFines]);

  const stats = useMemo(() => {
    const unpaidFines = fines.filter((item) => item.status === "UNPAID");
    const paidOrWaived = fines.filter((item) => item.status === "PAID" || item.status === "WAIVED");
    const unpaidAmount = unpaidFines.reduce((total, item) => total + Number(item.amount || 0), 0);
    return {
      unpaidCount: unpaidFines.length,
      unpaidAmount,
      resolvedCount: paidOrWaived.length,
    };
  }, [fines]);

  return (
    <div className="page-stack">
      <section className="page-card">
        <div className="section-head compact">
          <div>
            <span className="eyebrow">Reader</span>
            <h2 className="section-title">My Fines</h2>
          </div>
        </div>

        <div className="stats-grid">
          <StatCard label="Unpaid Fines" value={stats.unpaidCount} />
          <StatCard label="Unpaid Amount" value={formatMoney(stats.unpaidAmount)} />
          <StatCard label="Resolved" value={stats.resolvedCount} />
        </div>
      </section>

      <section className="page-card">
        {!canViewFines ? (
          <p className="page-note">Current reader role does not have permission to view fines.</p>
        ) : (
          <>
            <div className="inline-actions">
              <button className="secondary-button" type="button" onClick={loadFines} disabled={loading}>
                {loading ? "Loading..." : "Refresh Fines"}
              </button>
            </div>
            {error ? <p className="page-note">{error}</p> : null}

            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Fine ID</th>
                    <th>Record ID</th>
                    <th>Book</th>
                    <th>Copy Barcode</th>
                    <th>Due Date</th>
                    <th>Overdue Days</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {fines.map((fine) => (
                    <tr key={fine.fineId}>
                      <td>{fine.fineId}</td>
                      <td>{fine.recordId}</td>
                      <td>{fine.bookTitle || "-"}</td>
                      <td><Barcode value={fine.copyBarcode} height={25} fontSize={9} displayValue={true} /></td>
                      <td>{fine.dueDate || "-"}</td>
                      <td>{fine.overdueDays ?? 0}</td>
                      <td>{formatMoney(fine.amount)}</td>
                      <td>{fine.status || "-"}</td>
                      <td>
                        {fine.status === "UNPAID" ? (
                          <button
                            className="primary-button compact-button"
                            type="button"
                            onClick={() => navigate(`/reader/fines/pay/${fine.fineId}`)}
                          >
                            Pay
                          </button>
                        ) : (
                          "-"
                        )}
                      </td>
                    </tr>
                  ))}
                  {!loading && fines.length === 0 ? (
                    <tr>
                      <td colSpan="9">No fines.</td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </>
        )}
      </section>
    </div>
  );
}
