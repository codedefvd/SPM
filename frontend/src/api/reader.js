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
  toggleReviewLike(token, reviewId) {
    return request(`/reader/books/reviews/${reviewId}/like`, { method: "POST" }, token);
  },
  submitReviewReply(token, reviewId, payload) {
    return request(`/reader/books/reviews/${reviewId}/replies`, { method: "POST", body: JSON.stringify(payload) }, token);
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
  renewBorrowRecord(token, recordId) {
    return request(`/reader/books/records/${recordId}/renew`, { method: "POST" }, token);
  },
  listReservations(token) {
    return request("/reader/books/reservations", {}, token);
  },
  createReservation(token, payload) {
    return request("/reader/books/reservations", { method: "POST", body: JSON.stringify(payload) }, token);
  },
  listFeedbackConversations(token) {
    return request("/reader/feedback/conversations", {}, token);
  },
  createFeedbackConversation(token, payload) {
    return request("/reader/feedback/conversations", { method: "POST", body: JSON.stringify(payload) }, token);
  },
  listFeedbackMessages(token, conversationId) {
    return request(`/reader/feedback/conversations/${conversationId}/messages`, {}, token);
  },
  sendFeedbackMessage(token, conversationId, payload) {
    return request(`/reader/feedback/conversations/${conversationId}/messages`, { method: "POST", body: JSON.stringify(payload) }, token);
  },
};
