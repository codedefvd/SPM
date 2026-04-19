const API_BASE = import.meta.env.VITE_API_BASE_URL || "/api";

export async function request(path, options = {}, token, method = "GET", responseType = "json") {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {}),
    };

    if (token) {
        headers.Authorization = token.startsWith("Bearer ") ? token : `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE}${path}`, {
        method: method,
        ...options,
        headers,
    });

    if (responseType === "blob") {
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        return { data: await response.blob() };
    }

    const payload = await response.json().catch(() => null);
    if (!response.ok) {
        throw new Error(payload?.message || `HTTP ${response.status}`);
    }
    if (payload?.code && payload.code !== 200) {
        throw new Error(payload.message || "request failed");
    }
    return payload?.data ?? payload;
}
