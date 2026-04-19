import { useMemo, useState } from "react";
import { Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { AppShell } from "./layouts/AppShell";
import { roleMenus } from "./config/navigation";
import { AuthPage } from "./pages/auth/AuthPage";
import { ReaderBooksPage } from "./pages/reader/ReaderBooksPage";
import { ReaderRecordsPage } from "./pages/reader/ReaderRecordsPage";
import { ReaderReservationsPage } from "./pages/reader/ReaderReservationsPage";
import { LibrarianCatalogPage } from "./pages/librarian/LibrarianCatalogPage";
import { LibrarianRequestsPage } from "./pages/librarian/LibrarianRequestsPage";
import { LibrarianOperationsPage } from "./pages/librarian/LibrarianOperationsPage";
import { AdminUsersPage } from "./pages/admin/AdminUsersPage";
import { AdminDictionariesPage } from "./pages/admin/AdminDictionariesPage";
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
    { key: "reader-reservations", role: "READER", path: "/reader/reservations" },
    { key: "librarian-catalog", role: "LIBRARIAN", path: "/librarian/catalog" },
    { key: "librarian-requests", role: "LIBRARIAN", path: "/librarian/requests" },
    { key: "librarian-operations", role: "LIBRARIAN", path: "/librarian/operations" },
    { key: "admin-users", role: "ADMIN", path: "/admin/users" },
    { key: "admin-dictionaries", role: "ADMIN", path: "/admin/dictionaries" },
    { key: "admin-monitoring", role: "ADMIN", path: "/admin/monitoring" },
];

const routeByKey = Object.fromEntries(pageRoutes.map((item) => [item.key, item]));
const routeByPath = Object.fromEntries(pageRoutes.map((item) => [item.path, item]));

function getDefaultPage(role) {
    return roleMenus[role][0].key;
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
    const initialWorkspace = loadWorkspace();
    const [workspace, setWorkspace] = useState(initialWorkspace);

    const menus = useMemo(() => {
        if (!workspace) {
            return [];
        }
        return roleMenus[workspace.role];
    }, [workspace]);

    function handleLogin(nextWorkspace) {
        const defaultKey = getDefaultPage(nextWorkspace.role);
        const createdWorkspace = {
            ...nextWorkspace,
            activeKey: defaultKey,
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

    const activeRoute = routeByPath[location.pathname];

    function updateWorkspaceActiveKey(nextKey) {
        if (!workspace || workspace.activeKey === nextKey) {
            return;
        }
        const nextWorkspace = { ...workspace, activeKey: nextKey };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(nextWorkspace));
        setWorkspace(nextWorkspace);
    }

    if (!workspace) {
        if (location.pathname !== "/auth") {
            return <Navigate to="/auth" replace />;
        }
    } else {
        if (location.pathname === "/auth") {
            const startPath = routeByKey[workspace.activeKey]?.path || roleHomePaths[workspace.role];
            return <Navigate to={startPath} replace />;
        }
        if (!activeRoute || activeRoute.role !== workspace.role) {
            return <Navigate to={roleHomePaths[workspace.role]} replace />;
        }
        updateWorkspaceActiveKey(activeRoute.key);
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
            <Route path="/auth" element={<AuthPage onLogin={handleLogin} />} />
            <Route
                path="/reader/books"
                element={renderProtectedPage("READER", "reader-books", <ReaderBooksPage workspace={workspace} />)}
            />
            <Route
                path="/reader/records"
                element={renderProtectedPage("READER", "reader-records", <ReaderRecordsPage workspace={workspace} />)}
            />
            <Route
                path="/reader/reservations"
                element={renderProtectedPage("READER", "reader-reservations", <ReaderReservationsPage workspace={workspace} />)}
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
                element={
                    renderProtectedPage("LIBRARIAN", "librarian-operations", <LibrarianOperationsPage workspace={workspace} />)
                }
            />
            <Route
                path="/admin/users"
                element={renderProtectedPage("ADMIN", "admin-users", <AdminUsersPage workspace={workspace} />)}
            />
            <Route
                path="/admin/dictionaries"
                element={renderProtectedPage("ADMIN", "admin-dictionaries", <AdminDictionariesPage workspace={workspace} />)}
            />
            <Route
                path="/admin/monitoring"
                element={renderProtectedPage("ADMIN", "admin-monitoring", <AdminMonitoringPage workspace={workspace} />)}
            />
            <Route path="*" element={<Navigate to={fallbackPath} replace />} />
        </Routes>
    );
}
