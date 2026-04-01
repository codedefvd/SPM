import { useMemo, useState } from "react";
import { AppShell } from "./layouts/AppShell";
import { roleMenus } from "./config/navigation";
import { AuthPage } from "./pages/auth/AuthPage";
import { ReaderRegisterPage } from "./pages/reader/ReaderRegisterPage";
import { ReaderBooksPage } from "./pages/reader/ReaderBooksPage";
import { ReaderRecordsPage } from "./pages/reader/ReaderRecordsPage";
import { ReaderReservationsPage } from "./pages/reader/ReaderReservationsPage";
import { LibrarianCatalogPage } from "./pages/librarian/LibrarianCatalogPage";
import { LibrarianRequestsPage } from "./pages/librarian/LibrarianRequestsPage";
import { LibrarianOperationsPage } from "./pages/librarian/LibrarianOperationsPage";
import { AdminUsersPage } from "./pages/admin/AdminUsersPage";
import { AdminMonitoringPage } from "./pages/admin/AdminMonitoringPage";

const STORAGE_KEY = "lms_frontend_workspace";

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
  const initialWorkspace = loadWorkspace();
  const [workspace, setWorkspace] = useState(initialWorkspace);
  const [activeKey, setActiveKey] = useState(initialWorkspace?.activeKey || getDefaultPage("READER"));
  const [authMode, setAuthMode] = useState("login");

  const menus = useMemo(() => {
    if (!workspace) {
      return [];
    }
    return roleMenus[workspace.role];
  }, [workspace]);

  function handleLogin(nextWorkspace) {
    const createdWorkspace = {
      ...nextWorkspace,
      activeKey: getDefaultPage(nextWorkspace.role),
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(createdWorkspace));
    setWorkspace(createdWorkspace);
    setActiveKey(createdWorkspace.activeKey);
  }

  function handleNavigate(nextKey) {
    setActiveKey(nextKey);
    if (workspace) {
      const nextWorkspace = { ...workspace, activeKey: nextKey };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(nextWorkspace));
      setWorkspace(nextWorkspace);
    }
  }

  function handleLogout() {
    localStorage.removeItem(STORAGE_KEY);
    setWorkspace(null);
    setActiveKey(getDefaultPage("READER"));
    setAuthMode("login");
  }

  function renderPage() {
    switch (activeKey) {
      case "reader-books":
        return <ReaderBooksPage workspace={workspace} />;
      case "reader-records":
        return <ReaderRecordsPage workspace={workspace} />;
      case "reader-reservations":
        return <ReaderReservationsPage workspace={workspace} />;
      case "librarian-catalog":
        return <LibrarianCatalogPage workspace={workspace} />;
      case "librarian-requests":
        return <LibrarianRequestsPage workspace={workspace} />;
      case "librarian-operations":
        return <LibrarianOperationsPage workspace={workspace} />;
      case "admin-users":
        return <AdminUsersPage workspace={workspace} />;
      case "admin-monitoring":
        return <AdminMonitoringPage workspace={workspace} />;
      default:
        return <ReaderBooksPage workspace={workspace} />;
    }
  }

  if (!workspace) {
    if (authMode === "register") {
      return <ReaderRegisterPage onGoLogin={() => setAuthMode("login")} />;
    }

    return (
      <AuthPage
        onLogin={handleLogin}
        onGoRegister={() => setAuthMode("register")}
      />
    );
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
      {renderPage()}
    </AppShell>
  );
}