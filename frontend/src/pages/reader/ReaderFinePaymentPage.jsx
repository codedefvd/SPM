import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import QRCode from "qrcode";
import { readerApi } from "../../api/client";

function formatMoney(value) {
  return `$${Number(value || 0).toFixed(2)}`;
}

export function ReaderFinePaymentPage({ workspace }) {
  const { fineId } = useParams();
  const navigate = useNavigate();
  const [fines, setFines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [creatingOrder, setCreatingOrder] = useState(false);
  const [paymentOrder, setPaymentOrder] = useState(null);
  const [qrImageUrl, setQrImageUrl] = useState("");
  const [checkingStatus, setCheckingStatus] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setError("");
    readerApi
      .listFines(workspace?.token)
      .then((result) => {
        if (!ignore) {
          setFines(result || []);
        }
      })
      .catch((requestError) => {
        if (!ignore) {
          setError(requestError.message || "Failed to load fine");
        }
      })
      .finally(() => {
        if (!ignore) {
          setLoading(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, [workspace?.token]);

  const fine = useMemo(
    () => fines.find((item) => String(item.fineId) === String(fineId)),
    [fines, fineId]
  );

  useEffect(() => {
    let ignore = false;

    if (!paymentOrder?.qrCode) {
      setQrImageUrl("");
      return () => {
        ignore = true;
      };
    }

    QRCode.toDataURL(paymentOrder.qrCode, {
      errorCorrectionLevel: "M",
      margin: 1,
      width: 280,
    })
      .then((dataUrl) => {
        if (!ignore) {
          setQrImageUrl(dataUrl);
        }
      })
      .catch(() => {
        if (!ignore) {
          setQrImageUrl("");
          setError("Unable to render the Alipay QR code.");
        }
      });

    return () => {
      ignore = true;
    };
  }, [paymentOrder?.qrCode]);

  useEffect(() => {
    if (!paymentOrder?.outTradeNo || fine?.status !== "UNPAID") {
      return undefined;
    }

    let ignore = false;
    let redirectTimer;

    async function refreshPaymentStatus() {
      setCheckingStatus(true);
      try {
        const latestFines = await readerApi.listFines(workspace?.token);
        if (ignore) {
          return;
        }

        setFines(latestFines || []);
        const latestFine = (latestFines || []).find((item) => String(item.fineId) === String(fineId));
        if (latestFine?.status === "PAID") {
          setMessage("Payment received. Fine status has been updated.");
          setPaymentOrder(null);
          redirectTimer = window.setTimeout(() => {
            if (!ignore) {
              navigate("/reader/fines", { replace: true });
            }
          }, 900);
        }
      } catch {
        // Keep the QR visible if one polling request fails; the next interval may recover.
      } finally {
        if (!ignore) {
          setCheckingStatus(false);
        }
      }
    }

    const intervalId = window.setInterval(refreshPaymentStatus, 4000);
    refreshPaymentStatus();

    return () => {
      ignore = true;
      window.clearInterval(intervalId);
      if (redirectTimer) {
        window.clearTimeout(redirectTimer);
      }
    };
  }, [fine?.status, fineId, navigate, paymentOrder?.outTradeNo, workspace?.token]);

  async function handleConfirmPayment() {
    setSubmitting(true);
    setError("");
    setMessage("");
    try {
      await readerApi.confirmFinePayment(workspace?.token, fineId);
      setMessage("Payment confirmed. Fine status has been updated.");
      navigate("/reader/fines", { replace: true });
    } catch (requestError) {
      setError(requestError.message || "Failed to confirm payment");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleStartAlipayPayment() {
    setCreatingOrder(true);
    setError("");
    setMessage("");
    try {
      const order = await readerApi.createFinePaymentOrder(workspace?.token, fineId);
      if (!order?.qrCode) {
        throw new Error("Alipay sandbox did not return a QR code.");
      }
      setPaymentOrder(order);
      setMessage("Alipay sandbox order created.");
    } catch (requestError) {
      setError(requestError.message || "Failed to start Alipay sandbox payment");
    } finally {
      setCreatingOrder(false);
    }
  }

  return (
    <div className="page-stack">
      <section className="page-card">
        <div className="section-head compact">
          <div>
            <span className="eyebrow">Reader</span>
            <h2 className="section-title">Fine Payment</h2>
          </div>
          <button className="secondary-button" type="button" onClick={() => navigate("/reader/fines")}>
            Back to Fines
          </button>
        </div>

        {loading ? <p className="page-note">Loading fine...</p> : null}
        {error ? <p className="page-note error-note">{error}</p> : null}
        {message ? <p className="page-note success-note">{message}</p> : null}

        {!loading && !fine ? (
          <p className="page-note">Fine not found.</p>
        ) : null}

        {fine ? (
          <div className="payment-layout">
            <div className="payment-summary">
              <div className="metric-row">
                <span>Fine ID</span>
                <strong>{fine.fineId}</strong>
              </div>
              <div className="metric-row">
                <span>Book</span>
                <strong>{fine.bookTitle || "-"}</strong>
              </div>
              <div className="metric-row">
                <span>Amount</span>
                <strong>{formatMoney(fine.amount)}</strong>
              </div>
              <div className="metric-row">
                <span>Status</span>
                <strong>{fine.status || "-"}</strong>
              </div>
            </div>

            <div className="payment-panel">
              <div className="payment-status-panel">
                {paymentOrder?.qrCode ? (
                  <>
                    <strong>Alipay QR</strong>
                    {qrImageUrl ? (
                      <img className="payment-qr" src={qrImageUrl} alt="Alipay sandbox payment QR code" />
                    ) : (
                      <div className="payment-qr payment-qr-placeholder">Preparing QR...</div>
                    )}
                    <span className="payment-order-code">{paymentOrder.outTradeNo}</span>
                    <span>{checkingStatus ? "Checking payment status..." : "Waiting for payment callback..."}</span>
                  </>
                ) : (
                  <>
                    <strong>Alipay Sandbox</strong>
                    <span>Create a sandbox QR order and scan it with the Alipay sandbox buyer app.</span>
                  </>
                )}
              </div>
              <div className="payment-actions">
                <button
                  className="primary-button"
                  type="button"
                  disabled={creatingOrder || fine.status !== "UNPAID"}
                  onClick={handleStartAlipayPayment}
                >
                  {creatingOrder ? "Creating..." : paymentOrder?.qrCode ? "Refresh QR" : "Create QR"}
                </button>
                <button
                  className="secondary-button"
                  type="button"
                  disabled={submitting || fine.status !== "UNPAID"}
                  onClick={handleConfirmPayment}
                >
                  {submitting ? "Confirming..." : "I Have Paid"}
                </button>
              </div>
              <button className="ghost-button payment-cancel-button" type="button" onClick={() => navigate("/reader/fines")}>
                Cancel
              </button>
            </div>
          </div>
        ) : null}
      </section>
    </div>
  );
}
