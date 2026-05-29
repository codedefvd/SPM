import { request } from "./base";

export const readerApi = {
  listBooks(token, q = "") {
    const query = q ? `?q=${encodeURIComponent(q)}` : "";
    return request(`/reader/books${query}`, {}, token);
  },
  listFavoriteBooks(token) {
    return request("/reader/books/favorites", {}, token);
  },
  getBookDetail(token, bookId) {
    return request(`/reader/books/${bookId}`, {}, token);
  },
  toggleFavorite(token, bookId) {
    return request(`/reader/books/${bookId}/favorite`, { method: "POST" }, token);
  },
  submitBorrowRequest(token, payload) {
    return request("/reader/books/borrow-requests", { method: "POST", body: JSON.stringify(payload) }, token);
  },
  submitReview(token, bookId, payload) {
    return request(`/reader/books/${bookId}/reviews`, { method: "POST", body: JSON.stringify(payload) }, token);
  },
  listBorrowRecords(token) {
    return request("/reader/books/records", {}, token);
  },
  listFines(token) {
    return request("/reader/books/fines", {}, token);
  },
  createFinePaymentOrder(token, fineId) {
    return request(`/reader/books/fines/${fineId}/alipay-order`, { method: "POST" }, token);
  },
  confirmFinePayment(token, fineId) {
    return request(`/reader/books/fines/${fineId}/pay-confirm`, { method: "POST" }, token);
  },
  submitReturnRequest(token, recordId) {
    return request(`/reader/books/records/${recordId}/return-request`, { method: "POST" }, token);
  },
  listReservations(token) {
    return request("/reader/books/reservations", {}, token);
  },
  createReservation(token, payload) {
    return request("/reader/books/reservations", { method: "POST", body: JSON.stringify(payload) }, token);
  },
};
