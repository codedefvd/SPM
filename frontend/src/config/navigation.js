export const roleMenus = {
  READER: [
    { key: "reader-books", title: "Book Discovery", hint: "列表、搜索、详情、借阅入口" },
    { key: "reader-records", title: "Borrow Records", hint: "借阅与归还记录" },
    { key: "reader-reservations", title: "Reservations", hint: "预约与状态跟踪" },
  ],
  LIBRARIAN: [
    { key: "librarian-catalog", title: "Catalog Workspace", hint: "图书、分类、库存、上架" },
    { key: "librarian-requests", title: "Request Processing", hint: "借阅与归还审批" },
    { key: "librarian-operations", title: "Reservations & Fines", hint: "预约、罚款、统计" },
  ],
  ADMIN: [
    { key: "admin-users", title: "Governance", hint: "用户、权限、日志" },
    { key: "admin-logs", title: "Operation Logs", hint: "操作日志查询与导出" },
    { key: "admin-monitoring", title: "Monitoring", hint: "报表、备份、恢复、系统状态" },
  ],
};

export const roleDescriptions = {
  READER: "Focus on user-facing journeys and interaction polish.",
  LIBRARIAN: "Focus on catalog correctness and transaction processing.",
  ADMIN: "Focus on governance, monitoring, and operational safety.",
};
