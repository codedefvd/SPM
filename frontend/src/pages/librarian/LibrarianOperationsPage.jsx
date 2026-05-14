import { useEffect, useMemo, useState } from "react";
import { librarianApi } from "../../api/client";
import { StatCard } from "../../components/StatCard";
import Barcode from "../../components/Barcode";

function formatMoney(value) {
  return Number(value || 0).toFixed(2);
}

export function LibrarianOperationsPage({ workspace }) {
  const permissionsLoaded = Array.isArray(workspace?.permissions);
  const permissions = permissionsLoaded ? workspace.permissions : [];
  const canProcessReturns =
    !permissionsLoaded || permissions.includes("REQUEST_PROCESS");
  const canProcessReservations =
    !permissionsLoaded || permissions.includes("RESERVATION_PROCESS");
  const canManageFines =
    !permissionsLoaded || permissions.includes("FINE_MANAGE");
  const hasOperationsAccess =
    canProcessReturns || canProcessReservations || canManageFines;

  const [stats, setStats] = useState(null);
  const [details, setDetails] = useState(null);
  const [currentBorrowings, setCurrentBorrowings] = useState([]);
  const [overdueBorrowings, setOverdueBorrowings] = useState([]);
  const [returns, setReturns] = useState([]);
  const [reservations, setReservations] = useState([]);
  const [fines, setFines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [barcodeInput, setBarcodeInput] = useState("");
  const [scannedRecord, setScannedRecord] = useState(null);
  const [scanning, setScanning] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [fineFilterForm, setFineFilterForm] = useState({
    status: "ALL",
    keyword: "",
  });
  const [appliedFineFilters, setAppliedFineFilters] = useState({
    status: "ALL",
    keyword: "",
  });
  const [fineDrafts, setFineDrafts] = useState({});
  const [fineSavingId, setFineSavingId] = useState(null);

  async function loadData(activeFineFilters = appliedFineFilters) {
    if (!hasOperationsAccess) {
      setStats(null);
      setDetails(null);
      setCurrentBorrowings([]);
      setOverdueBorrowings([]);
      setReturns([]);
      setReservations([]);
      setFines([]);
      setFineDrafts({});
      setLoading(false);
      return;
    }

    setLoading(true);
    setError("");
    try {
      const [
        statistics,
        statisticsDetail,
        currentRows,
        overdueRows,
        returnRows,
        reservationRows,
        fineRows,
      ] = await Promise.all([
        librarianApi.getStatistics(workspace?.token),
        librarianApi.getDetailedStatistics(workspace?.token, "month"),
        canProcessReturns
          ? librarianApi.listCurrentBorrowings(workspace?.token)
          : Promise.resolve([]),
        canManageFines
          ? librarianApi.listOverdueBorrowings(workspace?.token)
          : Promise.resolve([]),
        canProcessReturns
          ? librarianApi.listReturnRequests(workspace?.token)
          : Promise.resolve([]),
        canProcessReservations
          ? librarianApi.listReservations(workspace?.token)
          : Promise.resolve([]),
        canManageFines
          ? librarianApi.listFines(workspace?.token, activeFineFilters)
          : Promise.resolve([]),
      ]);
      setStats(statistics || {});
      setDetails(statisticsDetail || null);
      setCurrentBorrowings(currentRows || []);
      setOverdueBorrowings(overdueRows || []);
      setReturns(returnRows || []);
      setReservations(reservationRows || []);
      setFines(fineRows || []);
      setFineDrafts(
        Object.fromEntries(
          (fineRows || []).map((item) => [
            item.fineId,
            { amount: item.amount ?? 0, status: item.status || "UNPAID" },
          ]),
        ),
      );
    } catch (requestError) {
      setError(requestError.message || "Failed to load operations data");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, [
    workspace?.token,
    hasOperationsAccess,
    canProcessReturns,
    canProcessReservations,
    canManageFines,
    appliedFineFilters.status,
    appliedFineFilters.keyword,
  ]);

  async function handleBarcodeLookup() {
    if (!canProcessReturns) {
      setError("Current librarian role cannot process returns.");
      return;
    }
    if (!barcodeInput.trim()) {
      setError("Please enter a barcode.");
      return;
    }
    setScanning(true);
    setMessage("");
    setError("");
    setScannedRecord(null);
    try {
      const result = await librarianApi.lookupByBarcode(
        workspace?.token,
        barcodeInput.trim(),
      );
      setScannedRecord(result);
      setMessage("Active borrow record found.");
    } catch (requestError) {
      setError(requestError.message || "Failed to lookup barcode");
    } finally {
      setScanning(false);
    }
  }

  async function handleBarcodeReturn(recordId, approve) {
    await handleProcessReturn(recordId, approve);
    setScannedRecord(null);
    setBarcodeInput("");
  }

  async function handleProcessReturn(recordId, approve) {
    if (!canProcessReturns) {
      setError("Current librarian role cannot process return requests.");
      return;
    }
    setMessage("");
    setError("");
    try {
      await librarianApi.processReturnRequest(workspace?.token, recordId, {
        approve,
        fineAmount: approve ? 0 : undefined,
        rejectReason: approve
          ? undefined
          : "return request rejected by librarian",
      });
      setMessage(`Return #${recordId} ${approve ? "approved" : "rejected"}.`);
      await loadData();
    } catch (requestError) {
      setError(requestError.message || "Failed to process return");
    }
  }

  async function handleProcessReservation(reservationId, action) {
    if (!canProcessReservations) {
      setError("Current librarian role cannot process reservations.");
      return;
    }
    setMessage("");
    setError("");
    try {
      await librarianApi.processReservation(workspace?.token, reservationId, {
        action,
      });
      setMessage(
        `Reservation #${reservationId} ${action.toLowerCase()} completed.`,
      );
      await loadData();
    } catch (requestError) {
      setError(requestError.message || "Failed to process reservation");
    }
  }

  function handleFineDraftChange(fineId, field, value) {
    setFineDrafts((current) => ({
      ...current,
      [fineId]: {
        amount: current[fineId]?.amount ?? 0,
        status: current[fineId]?.status ?? "UNPAID",
        [field]: value,
      },
    }));
  }

  function resetFineDraft(item) {
    setFineDrafts((current) => ({
      ...current,
      [item.fineId]: {
        amount: item.amount ?? 0,
        status: item.status || "UNPAID",
      },
    }));
  }

  async function handleFineUpdate(fineId, overrides = {}) {
    if (!canManageFines) {
      setError("Current librarian role cannot manage fines.");
      return;
    }
    const draft = {
      amount: fineDrafts[fineId]?.amount ?? 0,
      status: fineDrafts[fineId]?.status ?? "UNPAID",
      ...overrides,
    };
    const amount = Number.parseFloat(draft.amount);
    if (Number.isNaN(amount) || amount < 0) {
      setError("Fine amount must be a number greater than or equal to 0.");
      return;
    }
    setMessage("");
    setError("");
    setFineSavingId(fineId);
    try {
      await librarianApi.updateFine(workspace?.token, fineId, {
        status: draft.status,
        amount: Number(amount.toFixed(2)),
      });
      setMessage(`Fine #${fineId} updated.`);
      await loadData();
    } catch (requestError) {
      setError(requestError.message || "Failed to update fine");
    } finally {
      setFineSavingId(null);
    }
  }

  function applyFineFilters() {
    setAppliedFineFilters({
      status: fineFilterForm.status,
      keyword: fineFilterForm.keyword.trim(),
    });
  }

  function resetFineFilters() {
    const resetFilters = { status: "ALL", keyword: "" };
    setFineFilterForm(resetFilters);
    setAppliedFineFilters(resetFilters);
  }

  const fineOverview = useMemo(() => {
    const unpaidRows = fines.filter((item) => item.status === "UNPAID");
    const waivedRows = fines.filter((item) => item.status === "WAIVED");
    const unpaidAmount = unpaidRows.reduce(
      (sum, item) => sum + Number(item.amount || 0),
      0,
    );
    return {
      total: fines.length,
      unpaidCount: unpaidRows.length,
      waivedCount: waivedRows.length,
      unpaidAmount,
    };
  }, [fines]);

  return (
    <div className="page-stack">
      <section className="page-card">
        <div className="section-head compact">
          <div>
            <span className="eyebrow">Librarian</span>
            <h2 className="section-title">Operations</h2>
          </div>
        </div>

        <div className="stats-grid">
          <StatCard
            label="Current Borrowings"
            value={stats?.activeBorrows ?? 0}
          />
          <StatCard
            label="Pending Reservations"
            value={stats?.pendingReservations ?? 0}
          />
          <StatCard label="Unpaid Fines" value={stats?.unpaidFines ?? 0} />
        </div>
      </section>

      {canProcessReturns ? (
        <section className="page-card">
          <h3 className="section-title">Return by Barcode</h3>
          <div className="field-inline">
            <input
              placeholder="Scan or enter book copy barcode"
              value={barcodeInput}
              onChange={(e) => setBarcodeInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") handleBarcodeLookup();
              }}
            />
            <button
              className="primary-button"
              type="button"
              onClick={handleBarcodeLookup}
              disabled={scanning}
            >
              {scanning ? "Looking Up..." : "Lookup"}
            </button>
          </div>
          {scannedRecord ? (
            <div className="feature-banner" style={{ marginTop: "12px" }}>
              <strong>
                Record #{scannedRecord.recordId} — {scannedRecord.bookTitle}
              </strong>
              <p>
                Reader: {scannedRecord.readerUsername} | Status:{" "}
                {scannedRecord.status} | Due: {scannedRecord.dueDate || "-"}
              </p>
              <div className="inline-actions" style={{ marginBottom: 0 }}>
                <button
                  className="primary-button"
                  type="button"
                  onClick={() =>
                    handleBarcodeReturn(scannedRecord.recordId, true)
                  }
                >
                  Approve Return
                </button>
                <button
                  className="secondary-button"
                  type="button"
                  onClick={() =>
                    handleBarcodeReturn(scannedRecord.recordId, false)
                  }
                >
                  Reject Return
                </button>
              </div>
            </div>
          ) : null}
        </section>
      ) : null}

      {!hasOperationsAccess ? (
        <section className="page-card">
          <p className="page-note">No operations access.</p>
        </section>
      ) : (
        <>
          <section className="page-card">
            {message ? <p className="page-note">{message}</p> : null}
            {error ? <p className="page-note">{error}</p> : null}
            <div className="inline-actions">
              <button
                className="secondary-button"
                type="button"
                onClick={loadData}
                disabled={loading}
              >
                {loading ? "Loading..." : "Refresh"}
              </button>
            </div>
            <div className="monitor-grid">
              <div className="monitor-card">
                <small>Total Books</small>
                <strong>{stats?.totalBooks ?? 0}</strong>
              </div>
              <div className="monitor-card">
                <small>Pending Returns</small>
                <strong>{stats?.pendingReturnRequests ?? 0}</strong>
              </div>
              <div className="monitor-card">
                <small>Overdue Records</small>
                <strong>{overdueBorrowings.length}</strong>
              </div>
            </div>
          </section>

          {canProcessReturns ? (
            <section className="page-card">
              <h3 className="section-title">Current Borrowing Records</h3>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Reader</th>
                      <th>Book</th>
                      <th>Copy Barcode</th>
                      <th>Status</th>
                      <th>Borrow Date</th>
                      <th>Due Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {currentBorrowings.map((item) => (
                      <tr key={item.recordId}>
                        <td>{item.recordId}</td>
                        <td>{item.readerUsername}</td>
                        <td>{item.bookTitle}</td>
                        <td>
                          <Barcode
                            value={item.copyBarcode}
                            height={25}
                            fontSize={9}
                            displayValue={true}
                          />
                        </td>
                        <td>{item.status}</td>
                        <td>{item.borrowDate || "-"}</td>
                        <td>{item.dueDate || "-"}</td>
                      </tr>
                    ))}
                    {!loading && currentBorrowings.length === 0 ? (
                      <tr>
                        <td colSpan="7">No active borrowing records.</td>
                      </tr>
                    ) : null}
                  </tbody>
                </table>
              </div>
            </section>
          ) : null}

          {canManageFines ? (
            <section className="page-card">
              <h3 className="section-title">Overdue Borrowing Records</h3>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Reader</th>
                      <th>Book</th>
                      <th>Copy Barcode</th>
                      <th>Due Date</th>
                      <th>Overdue Days</th>
                      <th>Fine</th>
                      <th>Fine Status</th>
                      <th>Reminder</th>
                    </tr>
                  </thead>
                  <tbody>
                    {overdueBorrowings.map((item) => (
                      <tr key={item.recordId}>
                        <td>{item.recordId}</td>
                        <td>{item.readerUsername}</td>
                        <td>{item.bookTitle}</td>
                        <td>
                          <Barcode
                            value={item.copyBarcode}
                            height={25}
                            fontSize={9}
                            displayValue={true}
                          />
                        </td>
                        <td>{item.dueDate || "-"}</td>
                        <td>{item.overdueDays ?? 0}</td>
                        <td>{item.fineAmount ?? 0}</td>
                        <td>{item.fineStatus || "-"}</td>
                        <td>{item.reminderInfo || "-"}</td>
                      </tr>
                    ))}
                    {!loading && overdueBorrowings.length === 0 ? (
                      <tr>
                        <td colSpan="9">No overdue borrowing records.</td>
                      </tr>
                    ) : null}
                  </tbody>
                </table>
              </div>
            </section>
          ) : null}

          {canProcessReturns ? (
            <section className="page-card">
              <h3 className="section-title">Return Requests</h3>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Reader</th>
                      <th>Book</th>
                      <th>Copy Barcode</th>
                      <th>Status</th>
                      <th>Due Date</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {returns.map((item) => (
                      <tr key={item.recordId}>
                        <td>{item.recordId}</td>
                        <td>{item.readerUsername}</td>
                        <td>{item.bookTitle}</td>
                        <td>
                          <Barcode
                            value={item.copyBarcode}
                            height={25}
                            fontSize={9}
                            displayValue={true}
                          />
                        </td>
                        <td>{item.status}</td>
                        <td>{item.dueDate || "-"}</td>
                        <td>
                          <div className="table-actions">
                            <button
                              className="primary-button"
                              type="button"
                              onClick={() =>
                                handleProcessReturn(item.recordId, true)
                              }
                            >
                              Approve
                            </button>
                            <button
                              className="secondary-button"
                              type="button"
                              onClick={() =>
                                handleProcessReturn(item.recordId, false)
                              }
                            >
                              Reject
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                    {!loading && returns.length === 0 ? (
                      <tr>
                        <td colSpan="7">No return requests.</td>
                      </tr>
                    ) : null}
                  </tbody>
                </table>
              </div>
            </section>
          ) : null}

          {canProcessReservations ? (
            <section className="page-card">
              <h3 className="section-title">Reservations</h3>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Reader</th>
                      <th>Book</th>
                      <th>Status</th>
                      <th>Queue No</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reservations.map((item) => (
                      <tr key={item.reservationId}>
                        <td>{item.reservationId}</td>
                        <td>{item.readerUsername}</td>
                        <td>{item.bookTitle}</td>
                        <td>{item.status}</td>
                        <td>{item.queueNo}</td>
                        <td>
                          {item.status === "PENDING" ? (
                            <div className="table-actions">
                              <button
                                className="primary-button"
                                type="button"
                                onClick={() =>
                                  handleProcessReservation(
                                    item.reservationId,
                                    "FULFILL",
                                  )
                                }
                              >
                                Fulfill
                              </button>
                              <button
                                className="secondary-button"
                                type="button"
                                onClick={() =>
                                  handleProcessReservation(
                                    item.reservationId,
                                    "CANCEL",
                                  )
                                }
                              >
                                Cancel
                              </button>
                            </div>
                          ) : (
                            item.message || "-"
                          )}
                        </td>
                      </tr>
                    ))}
                    {!loading && reservations.length === 0 ? (
                      <tr>
                        <td colSpan="6">No reservations.</td>
                      </tr>
                    ) : null}
                  </tbody>
                </table>
              </div>
            </section>
          ) : null}

          {canManageFines ? (
            <section className="page-card">
              <h3 className="section-title">Fines</h3>
              <div className="field-inline" style={{ marginBottom: "12px" }}>
                <select
                  value={fineFilterForm.status}
                  onChange={(e) =>
                    setFineFilterForm((current) => ({
                      ...current,
                      status: e.target.value,
                    }))
                  }
                >
                  <option value="ALL">All Statuses</option>
                  <option value="UNPAID">Unpaid</option>
                  <option value="PAID">Paid</option>
                  <option value="WAIVED">Waived</option>
                </select>
                <input
                  placeholder="Search by reader, book, barcode, or record ID"
                  value={fineFilterForm.keyword}
                  onChange={(e) =>
                    setFineFilterForm((current) => ({
                      ...current,
                      keyword: e.target.value,
                    }))
                  }
                  onKeyDown={(e) => {
                    if (e.key === "Enter") applyFineFilters();
                  }}
                />
                <button
                  className="secondary-button"
                  type="button"
                  onClick={applyFineFilters}
                  disabled={loading}
                >
                  Apply
                </button>
                <button
                  className="ghost-button"
                  type="button"
                  onClick={resetFineFilters}
                  disabled={loading}
                >
                  Reset
                </button>
              </div>
              <div className="monitor-grid">
                <div className="monitor-card">
                  <small>Loaded Fines</small>
                  <strong>{fineOverview.total}</strong>
                </div>
                <div className="monitor-card">
                  <small>Unpaid</small>
                  <strong>{fineOverview.unpaidCount}</strong>
                </div>
                <div className="monitor-card">
                  <small>Waived</small>
                  <strong>{fineOverview.waivedCount}</strong>
                </div>
                <div className="monitor-card">
                  <small>Unpaid Amount</small>
                  <strong>${formatMoney(fineOverview.unpaidAmount)}</strong>
                </div>
              </div>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Record</th>
                      <th>Book</th>
                      <th>Copy Barcode</th>
                      <th>Reader</th>
                      <th>Due Date</th>
                      <th>Overdue Days</th>
                      <th>Amount</th>
                      <th>Status</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {fines.map((item) => (
                      <tr key={item.fineId}>
                        <td>{item.fineId}</td>
                        <td>{item.recordId || "-"}</td>
                        <td>{item.bookTitle || "-"}</td>
                        <td>
                          <Barcode
                            value={item.copyBarcode}
                            height={25}
                            fontSize={9}
                            displayValue={true}
                          />
                        </td>
                        <td>{item.readerUsername}</td>
                        <td>{item.dueDate || "-"}</td>
                        <td>{item.overdueDays ?? 0}</td>
                        <td>
                          <input
                            type="number"
                            min="0"
                            step="0.01"
                            value={
                              fineDrafts[item.fineId]?.amount ??
                              item.amount ??
                              0
                            }
                            onChange={(e) =>
                              handleFineDraftChange(
                                item.fineId,
                                "amount",
                                e.target.value,
                              )
                            }
                            disabled={fineSavingId === item.fineId}
                          />
                        </td>
                        <td>
                          <select
                            value={
                              fineDrafts[item.fineId]?.status ??
                              item.status ??
                              "UNPAID"
                            }
                            onChange={(e) =>
                              handleFineDraftChange(
                                item.fineId,
                                "status",
                                e.target.value,
                              )
                            }
                            disabled={fineSavingId === item.fineId}
                          >
                            <option value="UNPAID">UNPAID</option>
                            <option value="PAID">PAID</option>
                            <option value="WAIVED">WAIVED</option>
                          </select>
                        </td>
                        <td>
                          <div className="table-actions">
                            <button
                              className="primary-button"
                              type="button"
                              onClick={() => handleFineUpdate(item.fineId)}
                              disabled={fineSavingId === item.fineId}
                            >
                              {fineSavingId === item.fineId
                                ? "Saving..."
                                : "Save"}
                            </button>
                            <button
                              className="secondary-button"
                              type="button"
                              onClick={() =>
                                handleFineUpdate(item.fineId, {
                                  status: "PAID",
                                })
                              }
                              disabled={fineSavingId === item.fineId}
                            >
                              Mark Paid
                            </button>
                            <button
                              className="ghost-button"
                              type="button"
                              onClick={() =>
                                handleFineUpdate(item.fineId, {
                                  status: "WAIVED",
                                })
                              }
                              disabled={fineSavingId === item.fineId}
                            >
                              Waive
                            </button>
                            <button
                              className="ghost-button"
                              type="button"
                              onClick={() => resetFineDraft(item)}
                              disabled={fineSavingId === item.fineId}
                            >
                              Reset Draft
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                    {!loading && fines.length === 0 ? (
                      <tr>
                        <td colSpan="10">No fines.</td>
                      </tr>
                    ) : null}
                  </tbody>
                </table>
              </div>
            </section>
          ) : null}

          <section className="page-card">
            <h3 className="section-title">Borrowing Statistics</h3>
            <div className="split-grid">
              <div>
                <h4 className="section-title">Popular Books</h4>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Book</th>
                        <th>Author</th>
                        <th>Category</th>
                        <th>Borrow Count</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(details?.popularBooks || []).map((item) => (
                        <tr key={item.bookId}>
                          <td>{item.title}</td>
                          <td>{item.author}</td>
                          <td>{item.categoryName || "-"}</td>
                          <td>{item.borrowCount}</td>
                        </tr>
                      ))}
                      {!loading &&
                      (!details?.popularBooks ||
                        details.popularBooks.length === 0) ? (
                        <tr>
                          <td colSpan="4">No popular book data.</td>
                        </tr>
                      ) : null}
                    </tbody>
                  </table>
                </div>
              </div>
              <div>
                <h4 className="section-title">Borrowing Trend</h4>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Period</th>
                        <th>Borrow Count</th>
                        <th>Return Count</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(details?.borrowTrend || []).map((item) => (
                        <tr key={item.period}>
                          <td>{item.period}</td>
                          <td>{item.borrowCount}</td>
                          <td>{item.returnCount}</td>
                        </tr>
                      ))}
                      {!loading &&
                      (!details?.borrowTrend ||
                        details.borrowTrend.length === 0) ? (
                        <tr>
                          <td colSpan="3">No borrowing trend data.</td>
                        </tr>
                      ) : null}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
