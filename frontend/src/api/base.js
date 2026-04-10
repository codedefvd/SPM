const API_BASE = import.meta.env.VITE_API_BASE_URL || "/api";

export async function request(path, options = {}, token) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {}),
    };

    if (token) {
        headers.Authorization = token.startsWith("Bearer ") ? token : `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers,
    });

    const payload = await response.json().catch(() => null);
    if (!response.ok) {
        throw new Error(payload?.message || `HTTP ${response.status}`);
    }
    if (payload?.code && payload.code !== 200) {
        throw new Error(payload.message || "request failed");
    }
    return payload?.data ?? payload;
}
