export const roleMenus = {
  READER: [
    { key: "reader-books", title: "Books", hint: "Search and browse" },
    { key: "reader-records", title: "Borrow Records", hint: "Current and history" },
    { key: "reader-reservations", title: "Reservations", hint: "Reservation status" },
    { key: "reader-feedback", title: "Feedback", hint: "Chat with librarians" },
  ],
  LIBRARIAN: [
    { key: "librarian-catalog", title: "Catalog", hint: "Books and categories" },
    { key: "librarian-requests", title: "Borrow Requests", hint: "Approve or reject" },
    { key: "librarian-operations", title: "Operations", hint: "Returns, reservations, fines" },
    { key: "librarian-feedback", title: "Reader Feedback", hint: "Reply to reader questions" },
  ],
  ADMIN: [
    { key: "admin-users", title: "User Management", hint: "Accounts and permissions" },
    { key: "admin-monitoring", title: "Monitoring", hint: "Reports and system status" },
  ],
};

export const roleDescriptions = {
  READER: "Reader workspace",
  LIBRARIAN: "Librarian workspace",
  ADMIN: "Administrator workspace",
};
