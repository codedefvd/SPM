import { request } from "./base";

export const librarianApi = {
  lookupBookByIsbn(token, isbn) {
    return request(`/librarian/books/isbn-lookup?isbn=${encodeURIComponent(isbn)}`, {}, token);
  },
  listBooks(token) {
    return request("/librarian/books", {}, token);
  },
  getBookBarcode(token, bookId) {
    return request(`/librarian/books/${bookId}/barcode`, {}, token);
  },
  listBookCopies(token, bookId) {
    return request(`/librarian/books/${bookId}/copies`, {}, token);
  },
  createBook(token, payload) {
    return request("/librarian/books", { method: "POST", body: JSON.stringify(payload) }, token);
  },
  updateBook(token, bookId, payload) {
    return request(`/librarian/books/${bookId}`, { method: "PUT", body: JSON.stringify(payload) }, token);
  },
  updateInventory(token, bookId, payload) {
    return request(`/librarian/books/${bookId}/inventory`, { method: "PATCH", body: JSON.stringify(payload) }, token);
  },
  updateShelfStatus(token, bookId, payload) {
    return request(`/librarian/books/${bookId}/shelf-status`, { method: "PATCH", body: JSON.stringify(payload) }, token);
  },
  deleteBook(token, bookId) {
    return request(`/librarian/books/${bookId}`, { method: "DELETE" }, token);
  },
  listCategories(token) {
    return request("/librarian/categories", {}, token);
  },
  createCategory(token, payload) {
    return request("/librarian/categories", { method: "POST", body: JSON.stringify(payload) }, token);
  },
  updateCategory(token, categoryId, payload) {
    return request(`/librarian/categories/${categoryId}`, { method: "PUT", body: JSON.stringify(payload) }, token);
  },
  deleteCategory(token, categoryId, payload) {
    return request(`/librarian/categories/${categoryId}`, {
      method: "DELETE",
      body: payload ? JSON.stringify(payload) : undefined,
    }, token);
  },
  listBorrowRequests(token, statusFilter = "PENDING") {
    const query = statusFilter ? `?status_filter=${encodeURIComponent(statusFilter)}` : "";
    return request(`/librarian/borrow-requests${query}`, {}, token);
  },
  processBorrowRequest(token, requestId, payload) {
    return request(`/librarian/borrow-requests/${requestId}/process`, { method: "POST", body: JSON.stringify(payload) }, token);
  },
  listReturnRequests(token) {
    return request("/librarian/return-requests", {}, token);
  },
  lookupByBarcode(token, barcode) {
    return request(`/librarian/borrow-records/by-barcode?barcode=${encodeURIComponent(barcode)}`, {}, token);
  },
  listCurrentBorrowings(token) {
    return request("/librarian/borrow-records/current", {}, token);
  },
  listOverdueBorrowings(token) {
    return request("/librarian/borrow-records/overdue", {}, token);
  },
  processReturnRequest(token, recordId, payload) {
    return request(`/librarian/return-requests/${recordId}/process`, { method: "POST", body: JSON.stringify(payload) }, token);
  },
  listReservations(token) {
    return request("/librarian/reservations", {}, token);
  },
  processReservation(token, reservationId, payload) {
    return request(`/librarian/reservations/${reservationId}/process`, { method: "POST", body: JSON.stringify(payload) }, token);
  },
  listFines(token, filters = {}) {
    const params = new URLSearchParams();
    if (filters.status && filters.status !== "ALL") {
      params.set("status", filters.status);
    }
    if (filters.keyword) {
      params.set("keyword", filters.keyword);
    }
    const query = params.toString();
    return request(`/librarian/fines${query ? `?${query}` : ""}`, {}, token);
  },
  updateFine(token, fineId, payload) {
    return request(`/librarian/fines/${fineId}`, { method: "PATCH", body: JSON.stringify(payload) }, token);
  },
  getStatistics(token) {
    return request("/librarian/statistics", {}, token);
  },
  getDetailedStatistics(token, periodType = "month") {
    return request(`/librarian/statistics/detailed?periodType=${encodeURIComponent(periodType)}`, {}, token);
  },
};
