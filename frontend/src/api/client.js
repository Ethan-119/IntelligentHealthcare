const BASE_URL = "/api";

function getToken() {
  return localStorage.getItem("accessToken");
}

function setToken(token) {
  localStorage.setItem("accessToken", token);
}

function clearToken() {
  localStorage.removeItem("accessToken");
}

function getUser() {
  const raw = localStorage.getItem("user");
  return raw ? JSON.parse(raw) : null;
}

function setUser(user) {
  localStorage.setItem("user", JSON.stringify(user));
}

function clearUser() {
  localStorage.removeItem("user");
}

async function request(path, options = {}) {
  const token = getToken();
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (res.status === 401) {
    clearToken();
    clearUser();
    window.location.href = "/auth";
    return null;
  }

  if (!res.ok) {
    const body = await res.text();
    let message;
    try {
      const json = JSON.parse(body);
      message = json.message || json.error || body;
    } catch {
      message = body;
    }
    throw new Error(message || `请求失败 (${res.status})`);
  }

  return res.json();
}

/* ====== Auth ====== */

export async function login(phone, password) {
  const data = await request("/auth/login", {
    method: "POST",
    body: JSON.stringify({ phone, password }),
  });
  setToken(data.accessToken);
  setUser(data.user);
  return data;
}

export async function register({
  phone,
  password,
  username,
  patientAge,
  patientGender,
  residentCity,
  area,
  triagePrefer,
}) {
  const data = await request("/auth/register", {
    method: "POST",
    body: JSON.stringify({
      phone,
      password,
      username,
      patientAge,
      patientGender,
      residentCity,
      area,
      triagePrefer,
    }),
  });
  setToken(data.accessToken);
  setUser(data.user);
  return data;
}

export function logout() {
  clearToken();
  clearUser();
  window.location.href = "/auth";
}

/* ====== Patient Profile ====== */

export async function getMyProfile() {
  const data = await request("/patient/me");
  setUser(data);
  return data;
}

export async function updateMyProfile(profile) {
  const data = await request("/patient/me", {
    method: "PUT",
    body: JSON.stringify(profile),
  });
  setUser(data);
  return data;
}

/* ====== AI Analysis ====== */

export async function aiAnalyze(content) {
  const data = await request("/ai/analyze", {
    method: "POST",
    body: JSON.stringify({ content }),
  });
  return data;
}

export { getToken, getUser };
