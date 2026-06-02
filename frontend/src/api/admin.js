import { request } from "./base";

export const adminApi = {
  getCurrentPermissions(token) {
    return request("/admin/users/me/permissions", {}, token);
  },
  listUsers(token) {
    return request("/admin/users", {}, token);
  },
  getUser(userId, token) {
    return request(`/admin/users/${userId}`, {}, token);
  },
  createUser(payload, token) {
    return request("/admin/users", { method: "POST", body: JSON.stringify(payload) }, token);
  },
  updateUser(userId, payload, token) {
    return request(`/admin/users/${userId}`, { method: "PUT", body: JSON.stringify(payload) }, token);
  },
  updateUserEnabled(userId, enabled, token) {
    return request(`/admin/users/${userId}/enabled`, {
      method: "PATCH",
      body: JSON.stringify({ enabled }),
    }, token);
  },
  listRolePermissions(token) {
    return request("/admin/users/roles/permissions", {}, token);
  },
  updateRolePermission(role, permissionScope, token) {
    return request(`/admin/users/roles/${role}/permissions`, {
      method: "PUT",
      body: JSON.stringify({ permissionScope }),
    }, token);
  },
  listSystemConfigs(token) {
    return request("/admin/users/system-parameters", {}, token);
  },
  updateSystemConfig(configKey, payload, token) {
    return request(`/admin/users/system-parameters/${configKey}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    }, token);
  },
  listCategoryCodes(token) {
    return request("/admin/rules/category-codes", {}, token);
  },
  createCategoryCode(payload, token) {
    return request("/admin/rules/category-codes", { method: "POST", body: JSON.stringify(payload) }, token);
  },
  updateCategoryCode(categoryId, payload, token) {
    return request(`/admin/rules/category-codes/${categoryId}`, { method: "PUT", body: JSON.stringify(payload) }, token);
  },
  deleteCategoryCode(categoryId, force, token) {
    return request(`/admin/rules/category-codes/${categoryId}`, { method: "DELETE", body: JSON.stringify({ force }) }, token);
  },
  listStatusCodes(token) {
    return request("/admin/rules/status-codes", {}, token);
  },
  createStatusCode(payload, token) {
    return request("/admin/rules/status-codes", { method: "POST", body: JSON.stringify(payload) }, token);
  },
  updateStatusCode(statusCodeId, payload, token) {
    return request(`/admin/rules/status-codes/${statusCodeId}`, { method: "PUT", body: JSON.stringify(payload) }, token);
  },
  getMonitoringOverview(token) {
    return request("/admin/monitoring/overview", {}, token);
  },
  getBusinessReport(token) {
    return request("/admin/monitoring/reports", {}, token);
  },
  getFineStatistics(token) {
    return request("/admin/monitoring/fine-statistics", {}, token);
  },
  getRuntimeStatus(token) {
    return request("/admin/monitoring/runtime-status", {}, token);
  },
  listOperationLogs(token) {
    return request("/admin/monitoring/operation-logs", {}, token);
  },
  listAbnormalBehaviors(token) {
    return request("/admin/monitoring/abnormal-behaviors", {}, token);
  },
  executeBackup(token) {
    return request("/admin/monitoring/backups", { method: "POST" }, token);
  },
  listBackupRecords(token) {
    return request("/admin/monitoring/backups", {}, token);
  },
  restoreBackup(backupId, token) {
    return request("/admin/monitoring/backups/restore", {
      method: "POST",
      body: JSON.stringify({ backupId }),
    }, token);
  },
};
