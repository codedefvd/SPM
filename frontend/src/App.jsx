import { useEffect, useMemo, useState } from "react";
import { matchPath, Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { AppShell } from "./layouts/AppShell";
import { roleMenus } from "./config/navigation";
import { permissionApi } from "./api/permissions";
import { AuthPage } from "./pages/auth/AuthPage";
import { ReaderBooksPage } from "./pages/reader/ReaderBooksPage";
import { ReaderBookDetailPage } from "./pages/reader/ReaderBookDetailPage";
import { ReaderRecordsPage } from "./pages/reader/ReaderRecordsPage";
import { ReaderFinesPage } from "./pages/reader/ReaderFinesPage";
import { ReaderFinePaymentPage } from "./pages/reader/ReaderFinePaymentPage";
import { ReaderRegisterPage } from "./pages/reader/ReaderRegisterPage";
import { ReaderReservationsPage } from "./pages/reader/ReaderReservationsPage";
import { ReaderFeedbackPage } from "./pages/reader/ReaderFeedbackPage";
import { LibrarianCatalogPage } from "./pages/librarian/LibrarianCatalogPage";
import { LibrarianRequestsPage } from "./pages/librarian/LibrarianRequestsPage";
import { LibrarianOperationsPage } from "./pages/librarian/LibrarianOperationsPage";
import { LibrarianNotificationsPage } from "./pages/librarian/LibrarianNotificationsPage";
import { LibrarianFeedbackPage } from "./pages/librarian/LibrarianFeedbackPage";

import { AdminUsersPage } from "./pages/admin/AdminUsersPage";
import { AdminMonitoringPage } from "./pages/admin/AdminMonitoringPage";

const STORAGE_KEY = "lms_frontend_workspace";
const roleHomePaths = {
  READER: "/reader/books",
  LIBRARIAN: "/librarian/catalog",
  ADMIN: "/admin/users",
};

const pageRoutes = [
  { key: "reader-books", role: "READER", path: "/reader/books" },
  { key: "reader-records", role: "READER", path: "/reader/records" },
  { key: "reader-fines", role: "READER", path: "/reader/fines" },
  { key: "reader-reservations", role: "READER", path: "/reader/reservations" },
  { key: "reader-feedback", role: "READER", path: "/reader/feedback" },
  { key: "librarian-catalog", role: "LIBRARIAN", path: "/librarian/catalog" },
  { key: "librarian-requests", role: "LIBRARIAN", path: "/librarian/requests" },
  { key: "librarian-notifications", role: "LIBRARIAN", path: "/librarian/notifications" },
  { key: "librarian-operations", role: "LIBRARIAN", path: "/librarian/operations" },
    { key: "librarian-feedback", role: "LIBRARIAN", path: "/librarian/feedback" },
  { key: "admin-users", role: "ADMIN", path: "/admin/users" },
  { key: "admin-monitoring", role: "ADMIN", path: "/admin/monitoring" },
];

const routeByKey = Object.fromEntries(pageRoutes.map((item) => [item.key, item]));

function getDefaultPage(role) {
  return roleMenus[role]?.[0]?.key || null;
}

function hasAnyPermission(workspace, requiredPermissions) {
  if (!workspace || workspace.role === "ADMIN") {
    return true;
  }
  if (!Array.isArray(workspace.permissions)) {
    return true;
  }
  return requiredPermissions.some((item) => workspace.permissions.includes(item));
}

function filterMenusByPermissions(workspace) {
  if (!workspace) {
    return [];
  }
  const menus = roleMenus[workspace.role] || [];
  if (workspace.role === "ADMIN" || !Array.isArray(workspace.permissions)) {
    return menus;
  }

  return menus.filter((item) => {
    if (item.key === "reader-books") {
      return hasAnyPermission(workspace, ["BOOK_SEARCH", "BOOK_VIEW", "BORROW_REQUEST"]);
    }
    if (item.key === "reader-records") {
      return hasAnyPermission(workspace, ["RETURN_REQUEST", "BORROW_REQUEST", "BOOK_SEARCH", "BOOK_VIEW"]);
    }
    if (item.key === "reader-fines") {
      return hasAnyPermission(workspace, ["RETURN_REQUEST", "BORROW_REQUEST", "BOOK_SEARCH", "BOOK_VIEW"]);
    }
    if (item.key === "reader-reservations") {
      return hasAnyPermission(workspace, ["RESERVATION"]);
    }
    if (item.key === "reader-feedback") {
      return hasAnyPermission(workspace, ["FEEDBACK"]);
    }
    if (item.key === "librarian-catalog") {
      return hasAnyPermission(workspace, ["BOOK_MANAGE", "INVENTORY_MANAGE"]);
    }
    if (item.key === "librarian-requests") {
      return hasAnyPermission(workspace, ["REQUEST_PROCESS"]);
    }
    if (item.key === "librarian-operations") {
      return hasAnyPermission(workspace, ["REQUEST_PROCESS", "RESERVATION_PROCESS", "FINE_MANAGE"]);
    }
      if (item.key === "librarian-notifications") {  // ← 添加这3行
          return hasAnyPermission(workspace, ["REQUEST_PROCESS", "FINE_MANAGE"]);
      }
    if (item.key === "librarian-feedback") {
      return hasAnyPermission(workspace, ["FEEDBACK_MANAGE"]);
    }
    return true;
  });
}

function loadWorkspace() {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch (error) {
    localStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

export default function App() {
  const navigate = useNavigate();
  const location = useLocation();
  const [workspace, setWorkspace] = useState(loadWorkspace());

  const menus = useMemo(() => filterMenusByPermissions(workspace), [workspace]);

  useEffect(() => {
    if (!workspace || workspace.role === "ADMIN" || !workspace.token || Array.isArray(workspace.permissions)) {
      return;
    }

    let ignore = false;
    permissionApi
      .getCurrentPermissions(workspace.token)
      .then((permissions) => {
        if (ignore) {
          return;
        }
        const nextWorkspace = { ...workspace, permissions };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(nextWorkspace));
        setWorkspace(nextWorkspace);
      })
      .catch(() => {
        if (ignore) {
          return;
        }
        const nextWorkspace = { ...workspace, permissions: [] };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(nextWorkspace));
        setWorkspace(nextWorkspace);
      });

    return () => {
      ignore = true;
    };
  }, [workspace]);

  function handleLogin(nextWorkspace) {
    const defaultKey = getDefaultPage(nextWorkspace.role);
    const createdWorkspace = {
      ...nextWorkspace,
      activeKey: defaultKey,
      permissions: nextWorkspace.role === "ADMIN" ? null : null,
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(createdWorkspace));
    setWorkspace(createdWorkspace);
    navigate(routeByKey[defaultKey].path, { replace: true });
  }

  function handleNavigate(nextKey) {
    if (!workspace) {
      return;
    }
    const nextRoute = routeByKey[nextKey];
    if (!nextRoute || nextRoute.role !== workspace.role) {
      return;
    }
    const nextWorkspace = { ...workspace, activeKey: nextKey };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(nextWorkspace));
    setWorkspace(nextWorkspace);
    navigate(nextRoute.path);
  }

  function handleLogout() {
    localStorage.removeItem(STORAGE_KEY);
    setWorkspace(null);
    navigate("/auth", { replace: true });
  }

  function handleGoRegister() {
    navigate("/reader/register");
  }

  function handleGoLogin() {
    navigate("/auth", { replace: true });
  }

  const activeRoute = pageRoutes.find((item) => item.path === location.pathname)
    || (matchPath("/reader/books/:bookId", location.pathname) ? { key: "reader-books", role: "READER" } : null)
    || (matchPath("/reader/fines/pay/:fineId", location.pathname) ? { key: "reader-fines", role: "READER" } : null);

  function updateWorkspaceActiveKey(nextKey) {
    if (!workspace || workspace.activeKey === nextKey) {
      return;
    }
    const nextWorkspace = { ...workspace, activeKey: nextKey };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(nextWorkspace));
    setWorkspace(nextWorkspace);
  }

  if (!workspace) {
    if (location.pathname !== "/auth" && location.pathname !== "/reader/register") {
      return <Navigate to="/auth" replace />;
    }
  } else {
    if (location.pathname === "/auth" || location.pathname === "/reader/register") {
      const startPath = routeByKey[workspace.activeKey]?.path || roleHomePaths[workspace.role];
      return <Navigate to={startPath} replace />;
    }
    if (!activeRoute || activeRoute.role !== workspace.role) {
      return <Navigate to={roleHomePaths[workspace.role]} replace />;
    }
    updateWorkspaceActiveKey(activeRoute.key);

    if (workspace.role !== "ADMIN" && Array.isArray(workspace.permissions)) {
      const accessibleMenuKeys = menus.map((item) => item.key);
      if (!accessibleMenuKeys.includes(activeRoute.key)) {
        const fallbackKey = accessibleMenuKeys[0];
        if (!fallbackKey) {
          return <Navigate to="/auth" replace />;
        }
        return <Navigate to={routeByKey[fallbackKey].path} replace />;
      }
    }
  }

  function renderProtectedPage(role, activeKey, page) {
    if (!workspace) {
      return <Navigate to="/auth" replace />;
    }
    if (workspace.role !== role) {
      return <Navigate to={roleHomePaths[workspace.role]} replace />;
    }
    return (
      <AppShell
        role={workspace.role}
        username={workspace.username}
        menus={menus}
        activeKey={activeKey}
        onNavigate={handleNavigate}
        onLogout={handleLogout}
      >
        {page}
      </AppShell>
    );
  }

  const fallbackPath = workspace ? routeByKey[workspace.activeKey]?.path || roleHomePaths[workspace.role] : "/auth";

  return (
    <Routes>
      <Route path="/auth" element={<AuthPage onLogin={handleLogin} onGoRegister={handleGoRegister} />} />
      <Route path="/reader/register" element={<ReaderRegisterPage onGoLogin={handleGoLogin} />} />
      <Route
        path="/reader/books"
        element={renderProtectedPage("READER", "reader-books", <ReaderBooksPage workspace={workspace} />)}
      />
      <Route
        path="/reader/books/:bookId"
        element={renderProtectedPage("READER", "reader-books", <ReaderBookDetailPage workspace={workspace} />)}
      />
      <Route
        path="/reader/records"
        element={renderProtectedPage("READER", "reader-records", <ReaderRecordsPage workspace={workspace} />)}
      />
      <Route
        path="/reader/fines"
        element={renderProtectedPage("READER", "reader-fines", <ReaderFinesPage workspace={workspace} />)}
      />
      <Route
        path="/reader/fines/pay/:fineId"
        element={renderProtectedPage("READER", "reader-fines", <ReaderFinePaymentPage workspace={workspace} />)}
      />
      <Route
        path="/reader/reservations"
        element={renderProtectedPage("READER", "reader-reservations", <ReaderReservationsPage workspace={workspace} />)}
      />
      <Route
        path="/reader/feedback"
        element={renderProtectedPage("READER", "reader-feedback", <ReaderFeedbackPage workspace={workspace} />)}
      />
      <Route
        path="/librarian/catalog"
        element={renderProtectedPage("LIBRARIAN", "librarian-catalog", <LibrarianCatalogPage workspace={workspace} />)}
      />
      <Route
        path="/librarian/requests"
        element={renderProtectedPage("LIBRARIAN", "librarian-requests", <LibrarianRequestsPage workspace={workspace} />)}
      />
      <Route
        path="/librarian/operations"
        element={renderProtectedPage("LIBRARIAN", "librarian-operations", <LibrarianOperationsPage workspace={workspace} />)}
      />
        <Route
            path="/librarian/notifications"
            element={renderProtectedPage("LIBRARIAN", "librarian-notifications", <LibrarianNotificationsPage workspace={workspace} />)}
        />
      <Route
        path="/librarian/feedback"
        element={renderProtectedPage("LIBRARIAN", "librarian-feedback", <LibrarianFeedbackPage workspace={workspace} />)}
      />
      <Route
        path="/admin/users"
        element={renderProtectedPage("ADMIN", "admin-users", <AdminUsersPage workspace={workspace} />)}
      />
      <Route
        path="/admin/monitoring"
        element={renderProtectedPage("ADMIN", "admin-monitoring", <AdminMonitoringPage workspace={workspace} />)}
      />
      <Route path="*" element={<Navigate to={fallbackPath} replace />} />
    </Routes>
  );
}
