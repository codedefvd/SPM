const BASE_URL = "/api";

export async function request(path, options = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
    ...options,
  });

  const contentType = response.headers.get("content-type") || "";

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }

  if (contentType.includes("application/json")) {
    const data = await response.json();
    if (data.code !== 200) {
      throw new Error(data.message || `HTTP ${response.status}`);
    }
    return data.data ?? data;
  }

  const text = await response.text();
  throw new Error(text || "Unexpected response");
}