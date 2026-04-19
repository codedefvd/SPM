import { request } from "./base";

export const adminApi = {
  listUsers(token) {
    return request("/admin/users", {}, token);
  },
  getLogs(params, token) {
    return request("/admin/logs/list", params, token, "POST");
  },
  exportLogs(params, token) {
    return request("/admin/logs/export", params, token, "GET", "blob");
  },
};
