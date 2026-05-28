const BASE_URL = "/api";
const TOKEN_KEY = "accessToken";
const USER_KEY = "user";

function getAuthStorage() {
  if (typeof window !== "undefined" && window.sessionStorage) {
    return window.sessionStorage;
  }
  return localStorage;
}

class ApiError extends Error {
  constructor(message, status, details = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

function getToken() {
  return getAuthStorage().getItem(TOKEN_KEY);
}

function setToken(token) {
  const storage = getAuthStorage();
  storage.setItem(TOKEN_KEY, token);
  // 清理历史全局登录态，避免多标签串号
  localStorage.removeItem(TOKEN_KEY);
}

function clearToken() {
  getAuthStorage().removeItem(TOKEN_KEY);
  localStorage.removeItem(TOKEN_KEY);
}

function getUser() {
  const raw = getAuthStorage().getItem(USER_KEY);
  return raw ? JSON.parse(raw) : null;
}

function setUser(user) {
  const storage = getAuthStorage();
  storage.setItem(USER_KEY, JSON.stringify(user));
  // 清理历史全局登录态，避免多标签串号
  localStorage.removeItem(USER_KEY);
}

function clearUser() {
  getAuthStorage().removeItem(USER_KEY);
  localStorage.removeItem(USER_KEY);
}

function isUnusableMessage(message) {
  if (!message) return true;
  const trimmed = String(message).trim();
  if (!trimmed || trimmed === "{}") return true;
  if (/^\?+$/.test(trimmed)) return true;
  return false;
}

function containsSensitiveDetails(message) {
  if (!message) return false;
  const text = String(message).trim();
  if (!text) return false;
  const lower = text.toLowerCase();
  return (
    lower.includes("exception") ||
    lower.includes("stacktrace") ||
    lower.includes("org.springframework") ||
    lower.includes("java.") ||
    lower.includes("sql") ||
    lower.includes("at ") ||
    lower.includes("<html") ||
    lower.includes("<!doctype html")
  );
}

function normalizeBackendMessage(path, status, backendMessage) {
  if (isUnusableMessage(backendMessage)) return "";
  const trimmed = String(backendMessage).trim();

  // 登录/注册页面上返回“未登录或凭证无效”会误导用户，优先转成对应语义。
  if (path === "/auth/login" && status === 401 && trimmed.includes("未登录")) {
    return "手机号或密码错误，请重新输入。";
  }
  if (path === "/auth/register" && status === 401 && trimmed.includes("未登录")) {
    return "注册失败，请检查输入信息后重试。";
  }
  if (containsSensitiveDetails(trimmed)) {
    return "";
  }
  return trimmed;
}

function buildFriendlyErrorMessage(path, status, backendMessage) {
  const normalized = normalizeBackendMessage(path, status, backendMessage);
  if (normalized) {
    return normalized;
  }
  if (status === 400) return "请求参数不正确，请检查输入后重试。";
  if (status === 401 && path === "/auth/login") return "手机号或密码错误，请重新输入。";
  if (status === 401 && path === "/auth/register") return "注册失败，请检查输入信息后重试。";
  if (status === 403 && path === "/auth/login") return "账号已禁用或无权限登录，请联系管理员。";
  if (status === 403 && path === "/auth/register") return "当前无法完成注册，请稍后重试。";
  if (status === 401) return "登录状态已失效，请重新登录。";
  if (status === 403) return "当前账号没有权限执行该操作。";
  if (status === 404) return "请求的接口不存在，请稍后重试。";
  if (status === 409) return "当前操作与现有数据冲突，请检查后重试。";
  if (status >= 500) return "服务器暂时不可用，请稍后重试。";
  return `请求失败（HTTP ${status}）`;
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

  if (res.status === 401 && !path.startsWith("/auth/")) {
    clearToken();
    clearUser();
    window.location.href = "/auth";
    throw new ApiError("登录状态已失效，请重新登录。", 401, { path });
  }

  if (!res.ok) {
    const body = await res.text();
    let backendMessage = "";
    try {
      const json = JSON.parse(body);
      backendMessage = json.message || json.error || json.detail || body;
    } catch {
      backendMessage = body;
    }
    const message = buildFriendlyErrorMessage(path, res.status, backendMessage);
    throw new ApiError(message, res.status, { path, backendMessage });
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

export async function aiAnalyze(
  content,
  sessionId,
  images = [],
  latitude = null,
  longitude = null,
  deepImageAnalysis = false
) {
  const data = await request("/ai/analyze", {
    method: "POST",
    body: JSON.stringify({ content, sessionId, images, latitude, longitude, deepImageAnalysis }),
  });
  return data;
}

/**
 * 流式 AI 分析：通过 SSE (text/event-stream) 逐 token 接收答复。
 * <p>
 * 使用 fetch + ReadableStream（而非 EventSource），因为：
 * <ul>
 * <li>EventSource 只支持 GET，我们需要 POST 发送请求体</li>
 * <li>ReadableStream 可搭配 AbortController 取消正在进行的流</li>
 * </ul>
 * <p>
 * SSE 解析：字节流 → TextDecoder 解码 → 按 \n\n 分割事件 → 按行解析 event/data 字段。
 * TextDecoder 的 stream: true 保证跨 chunk 的多字节 UTF-8 字符不被截断。
 *
 * @param {Object} callbacks - { onStatus, onChunk, onDone, onError }
 * @param {AbortSignal} [callbacks.signal] - 取消信号
 */
export async function aiAnalyzeStream(
  content,
  sessionId,
  images,
  latitude,
  longitude,
  deepImageAnalysis = false,
  callbacks = {}
) {
  const { onStatus, onChunk, onDone, onError, signal } = callbacks;
  const token = getToken();
  const headers = {
    "Content-Type": "application/json",
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const res = await fetch("/api/ai/analyze/stream", {
    method: "POST",
    headers,
    body: JSON.stringify({ content, sessionId, images, latitude, longitude, deepImageAnalysis }),
    signal,
  });

  // 401 时立即跳转登录页，不尝试读取响应体（此时可能已是登录页 HTML）
  if (res.status === 401) {
    clearToken();
    clearUser();
    window.location.href = "/auth";
    throw new ApiError("登录状态已失效，请重新登录。", 401, { path: "/ai/analyze/stream" });
  }

  if (!res.ok) {
    const body = await res.text();
    let msg = "";
    try {
      msg = JSON.parse(body).message || body;
    } catch {
      msg = body;
    }
    throw new ApiError(msg, res.status, { path: "/ai/analyze/stream", backendMessage: body });
  }

  // 以 ReadableStream 逐块读取 SSE 响应体
  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let doneReceived = false;

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    // stream: true 让 TextDecoder 保留跨 chunk 的不完整多字节序列
    buffer += decoder.decode(value, { stream: true });
    // 兼容 CRLF（\r\n）与 LF（\n）两种分隔风格，避免事件无法被识别
    buffer = buffer.replace(/\r\n/g, "\n");

    // SSE 协议以 \n\n 为事件分隔符；一个 TCP 帧可能包含多个事件
    while (true) {
      const idx = buffer.indexOf("\n\n");
      if (idx === -1) break; // 未收到完整事件，等待下一帧
      const raw = buffer.slice(0, idx);
      buffer = buffer.slice(idx + 2);

      let eventType = "";
      const dataLines = [];
      const lines = raw.split("\n");
      for (const line of lines) {
        if (!line || line.startsWith(":")) {
          // 空行或注释行（SSE 心跳）直接忽略
          continue;
        }
        // 兼容 "event:chunk" 和 "event: chunk" 两种格式
        if (line.startsWith("event:")) {
          eventType = line.slice(6).trim();
        } else if (line.startsWith("data:")) {
          // SSE 标准允许多行 data，客户端需用换行拼接
          dataLines.push(line.slice(5).trimStart());
        }
      }
      const data = dataLines.join("\n");

      if (eventType === "status") {
        onStatus?.(data);
      } else if (eventType === "chunk") {
        // 过滤 UTF-8 解码失败产生的替换字符 (U+FFFD)，避免前端显示乱码
        onChunk?.(data.replace(/\uFFFD/g, ""));
        // 给浏览器一次绘制机会，避免大量 chunk 在同一任务中被批处理成“看起来阻塞”
        if (typeof requestAnimationFrame === "function") {
          await new Promise((resolve) => requestAnimationFrame(() => resolve()));
        } else {
          await new Promise((resolve) => setTimeout(resolve, 0));
        }
      } else if (eventType === "done") {
        let parsed = {};
        try {
          parsed = JSON.parse(data);
        } catch {
          // JSON 解析失败时传空对象，前端用默认值兜底
        }
        doneReceived = true;
        onDone?.(parsed);
        return; // done 事件后流结束，退出循环
      } else if (eventType === "error") {
        doneReceived = true;
        onError?.(data);
        return;
      }
    }
  }

  // 某些网关/代理会在 EOF 前吞掉最后一个空行，导致最后一个事件没有 \n\n 分隔。
  // 这里对剩余缓冲再做一次兜底解析，避免错过 done/error。
  const rest = buffer.trim();
  if (rest) {
    let eventType = "";
    const dataLines = [];
    const lines = rest.split("\n");
    for (const line of lines) {
      if (!line || line.startsWith(":")) continue;
      if (line.startsWith("event:")) {
        eventType = line.slice(6).trim();
      } else if (line.startsWith("data:")) {
        dataLines.push(line.slice(5).trimStart());
      }
    }
    const data = dataLines.join("\n");
    if (eventType === "done") {
      let parsed = {};
      try {
        parsed = JSON.parse(data);
      } catch {
        // ignore
      }
      doneReceived = true;
      onDone?.(parsed);
      return;
    }
    if (eventType === "error") {
      doneReceived = true;
      onError?.(data || "流式分析失败");
      return;
    }
  }

  // 服务端异常断开但未发送 done/error 时，主动兜底结束，避免前端停留在“加载中”
  if (!doneReceived) {
    onError?.("流式连接已结束，未收到完成事件");
  }
}

export async function listAiSessions() {
  return request("/ai/sessions");
}

export async function getAiSessionTurns(sessionId) {
  return request(`/ai/sessions/${encodeURIComponent(sessionId)}/turns`);
}

/* ====== Admin Cache ====== */

export async function evictKnowledgeCache() {
  return request("/admin/knowledge/cache/evict", {
    method: "POST",
  });
}

export async function refreshKnowledgeCache() {
  return request("/admin/knowledge/cache/refresh", {
    method: "POST",
  });
}

export async function addKnowledgeHotspots(scopes = []) {
  return request("/admin/knowledge/cache/hotspots/add", {
    method: "POST",
    body: JSON.stringify({ scopes }),
  });
}

/* ====== Admin Documents ====== */

export async function uploadDocument(file) {
  const token = getToken();
  const headers = {};
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  const formData = new FormData();
  formData.append("file", file);

  const res = await fetch(`${BASE_URL}/admin/documents/upload`, {
    method: "POST",
    headers,
    body: formData,
  });

  if (res.status === 401) {
    clearToken();
    clearUser();
    window.location.href = "/auth";
    throw new ApiError("登录状态已失效，请重新登录。", 401, {
      path: "/admin/documents/upload",
    });
  }

  if (!res.ok) {
    const body = await res.text();
    let backendMessage = "";
    try {
      const json = JSON.parse(body);
      backendMessage = json.message || json.error || body;
    } catch (_e) {
      backendMessage = body;
    }
    const message = buildFriendlyErrorMessage(
      "/admin/documents/upload",
      res.status,
      backendMessage
    );
    throw new ApiError(message, res.status, {
      path: "/admin/documents/upload",
      backendMessage,
    });
  }

  return res.json();
}

export async function listDocuments() {
  return request("/admin/documents");
}

// 下架文档：所有块不再参与检索
export async function deactivateDocument(documentName) {
  return request(`/admin/documents/${encodeURIComponent(documentName)}/deactivate`, {
    method: "PUT",
  });
}

// 上架文档：恢复检索
export async function activateDocument(documentName) {
  return request(`/admin/documents/${encodeURIComponent(documentName)}/activate`, {
    method: "PUT",
  });
}

/* ====== Admin Import Jobs ====== */

// 上传结构化数据文件（Excel/CSV）导入 PostgreSQL 业务表
export async function uploadImportJob(file, datasetType) {
  const token = getToken();
  const headers = {};
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  const formData = new FormData();
  formData.append("file", file);
  formData.append("datasetType", datasetType);

  const res = await fetch(`${BASE_URL}/admin/import/jobs`, {
    method: "POST",
    headers,
    body: formData,
  });

  if (res.status === 401) {
    clearToken();
    clearUser();
    window.location.href = "/auth";
    throw new ApiError("登录状态已失效，请重新登录。", 401, {
      path: "/admin/import/jobs",
    });
  }

  if (!res.ok) {
    const body = await res.text();
    let backendMessage = "";
    try {
      const json = JSON.parse(body);
      backendMessage = json.message || json.error || body;
    } catch (_e) {
      backendMessage = body;
    }
    const message = buildFriendlyErrorMessage(
      "/admin/import/jobs",
      res.status,
      backendMessage
    );
    throw new ApiError(message, res.status, {
      path: "/admin/import/jobs",
      backendMessage,
    });
  }

  return res.json();
}

// 获取最近的导入任务列表
export async function listImportJobs() {
  return request("/admin/import/jobs");
}

/* ====== Admin Knowledge Data ====== */

// 按类型分页查询 Knowledge 数据
export async function listKnowledgeDataByType(type, page, size, keyword) {
  var params = "page=" + (page || 1) + "&size=" + (size || 20);
  if (keyword) {
    params += "&keyword=" + encodeURIComponent(keyword);
  }
  return request(`/admin/knowledge/data/${encodeURIComponent(type)}?${params}`);
}

// 切换单条 Knowledge 数据的热点状态
export async function toggleKnowledgeData(type, id) {
  return request(`/admin/knowledge/data/${encodeURIComponent(type)}/${encodeURIComponent(id)}/toggle`, {
    method: "PUT",
  });
}

export function isAdmin(user) {
  return !!user && user.role === "admin";
}

export { getToken, getUser };
