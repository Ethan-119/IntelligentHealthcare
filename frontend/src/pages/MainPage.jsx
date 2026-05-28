import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  aiAnalyze,
  aiAnalyzeStream,
  getAiSessionTurns,
  getMyProfile,
  getUser,
  isAdmin,
  listAiSessions,
  logout,
} from "../api/client";

const ROLE_MAP = { patient: "普通用户", admin: "管理员" };
const MAX_UPLOAD_IMAGES = 5;
const QUICK_IMAGE_ANALYZE_LIMIT = 2;
const promptedLocationUsers = new Set();

const inputStyle = {
  width: "100%",
  padding: "10px 12px",
  border: "1px solid #d1d5db",
  borderRadius: "10px",
  fontSize: "14px",
  outline: "none",
  boxSizing: "border-box",
};

function formatBytes(size) {
  if (!size && size !== 0) return "";
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDateTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return `${date.getMonth() + 1}-${date.getDate()} ${String(date.getHours()).padStart(2, "0")}:${String(
    date.getMinutes()
  ).padStart(2, "0")}`;
}

function fileToDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(new Error("图片读取失败，请重试"));
    reader.readAsDataURL(file);
  });
}

function mapTurnsToMessages(turns = []) {
  const result = [];
  for (let i = 0; i < turns.length; i++) {
    const turn = turns[i];
    if (turn.userMessage) {
      result.push({ role: "user", text: turn.userMessage });
    }
    if (turn.replyText) {
      result.push({ role: "assistant", text: turn.replyText });
    }
  }
  return result;
}

function cleanInlineMarkdown(text) {
  if (!text) return "";
  return text
    .replace(/\*\*/g, "")
    .replace(/`/g, "")
    .replace(/[✅⚠️❌📌➡️▪️•◆■□★☆]/g, "")
    .replace(/<br\s*\/?>/gi, "；")
    .replace(/\uFFFD/g, "")
    .replace(/\s+/g, " ")
    .trim();
}

function formatAssistantText(text) {
  if (!text) return "";
  const lines = String(text).replace(/\r\n/g, "\n").split("\n");
  const formatted = [];
  for (const raw of lines) {
    const line = raw.trim();
    if (!line) {
      formatted.push("");
      continue;
    }
    // 过滤内部链路/推理过程描述，避免对用户暴露实现细节。
    if (
      /MCP视觉服务|MCP\s*图片|ReAct推理|ReAct过程|ReAct Trace|结构化医疗辅助分析报告/.test(line)
    ) {
      continue;
    }
    if (/^-{3,}$/.test(line)) {
      continue;
    }
    if (line.startsWith("|")) {
      // 过滤 markdown 表头分隔符行
      if (/^\|?[\s:\-|]+\|?$/.test(line)) {
        continue;
      }
      const cells = line
        .split("|")
        .map((v) => cleanInlineMarkdown(v))
        .filter(Boolean);
      if (cells.length === 0) continue;
      const joined = cells.join("");
      // 过滤表头行
      if (/检查类型|具体建议|目的说明/.test(joined)) {
        continue;
      }
      if (cells.length >= 3) {
        formatted.push(`- ${cells[0]}：${cells[1]}（${cells[2]}）`);
      } else if (cells.length === 2) {
        formatted.push(`- ${cells[0]}：${cells[1]}`);
      } else {
        formatted.push(`- ${cells[0]}`);
      }
      continue;
    }
    const normalizedLine = cleanInlineMarkdown(raw)
      .replace(/^[-*+]\s*\*\*/g, "- ")
      .replace(/^[-*+]\s*/, "- ");
    formatted.push(normalizedLine);
  }
  const merged = formatted.join("\n").replace(/\n{3,}/g, "\n\n").trim();
  return merged;
}

function renderAssistantText(text) {
  const normalized = formatAssistantText(text);
  if (!normalized) return null;
  const lines = normalized.split("\n");
  return lines.map((raw, idx) => {
    const line = raw.trim();
    if (!line) {
      return <div key={`assistant-line-${idx}`} style={{ height: 8 }} />;
    }
    // 兼容 "- ### 标题" / "* ## 标题" / "###标题" 等写法
    const normalizedLine = line.replace(/^[-*+]\s+/, "");
    const headingMatch = normalizedLine.match(/^(#{1,6})\s*(.+)$/);
    if (headingMatch) {
      const level = headingMatch[1].length;
      const titleText = headingMatch[2].replace(/\*\*/g, "").trim();
      const styleByLevel =
        level === 1
          ? { fontSize: 26, fontWeight: 700, margin: "12px 0 8px", lineHeight: 1.35 }
          : level === 2
          ? { fontSize: 23, fontWeight: 700, margin: "10px 0 6px", lineHeight: 1.35 }
          : level === 3
          ? { fontSize: 20, fontWeight: 700, margin: "8px 0 4px", lineHeight: 1.35 }
          : { fontSize: 18, fontWeight: 700, margin: "8px 0 4px", lineHeight: 1.35 };
      return (
        <div key={`assistant-line-${idx}`} style={styleByLevel}>
          {titleText}
        </div>
      );
    }
    return (
      <div key={`assistant-line-${idx}`} style={{ whiteSpace: "pre-wrap" }}>
        {raw}
      </div>
    );
  });
}

function SymptomChat({
  activeSessionId,
  initialTurns,
  loadingHistory,
  onSessionResolved,
  onConversationUpdated,
  currentUserId,
  isMobile,
}) {
  const initialAssistantMessage = useMemo(
    () => ({
      role: "assistant",
      text: "您好，我是智能医疗助手。请描述您的症状或健康问题，我将为您提供初步分析和就医建议。",
    }),
    []
  );
  const [messages, setMessages] = useState([initialAssistantMessage]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [selectedImages, setSelectedImages] = useState([]);
  const [deepImageAnalysis, setDeepImageAnalysis] = useState(false);
  const [sessionId, setSessionId] = useState(activeSessionId || "");
  const [location, setLocation] = useState(null);
  const [locationStatus, setLocationStatus] = useState("未获取定位");
  const endRef = useRef(null);
  const fileInputRef = useRef(null);
  const locationRequestedRef = useRef(false);
  const abortRef = useRef(null);
  const messagesRef = useRef(messages);

  useEffect(() => {
    messagesRef.current = messages;
  }, [messages]);

  useEffect(() => {
    // 切换会话时中止未完成的流
    if (abortRef.current) {
      abortRef.current.abort();
      abortRef.current = null;
    }
    const incomingSessionId = activeSessionId || "";
    const previousSessionId = sessionId || "";
    const switchingSession = incomingSessionId !== previousSessionId;
    setSessionId(incomingSessionId);
    setInput("");
    setSelectedImages([]);
    setDeepImageAnalysis(false);
    if (loadingHistory) {
      return;
    }
    const loaded = mapTurnsToMessages(initialTurns || []);
    if (loaded.length > 0) {
      setMessages(loaded);
      return;
    }
    if (!incomingSessionId) {
      setMessages([initialAssistantMessage]);
      return;
    }
    // 新会话刚创建后 activeSessionId 会从空变为新值，但 initialTurns 仍为空。
    // 这时保留当前已显示的流式消息，避免“答案消失，刷新后才看到”。
    const creatingSessionFromCurrentChat = !previousSessionId && incomingSessionId && messagesRef.current.length > 1;
    if (switchingSession && !creatingSessionFromCurrentChat) {
      setMessages([initialAssistantMessage]);
    }
  }, [activeSessionId, initialTurns, initialAssistantMessage, loadingHistory]);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, loading]);

  // 通过浏览器 W3C Geolocation API 获取用户位置。
  // 移动端底层调用设备 GPS 芯片，桌面端通过 Wi-Fi 三角定位 / IP 粗略定位。
  // enableHighAccuracy: false 降低电量消耗；
  // timeout 5 秒超时静默降级；
  // maximumAge 5 分钟复用近期缓存坐标，避免频繁请求定位服务。
  const fetchGeolocation = () =>
    new Promise((resolve) => {
      if (!navigator?.geolocation) {
        setLocationStatus("当前浏览器不支持定位");
        resolve(null);
        return;
      }
      setLocationStatus("正在获取定位...");
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const next = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          };
          setLocation(next);
          setLocationStatus("定位已获取");
          resolve(next);
        },
        () => {
          setLocationStatus("定位获取失败或被拒绝");
          resolve(null);
        },
        { enableHighAccuracy: false, timeout: 4000, maximumAge: 5 * 60 * 1000 }
      );
    });

  useEffect(() => {
    setLocation(null);
    setLocationStatus("未获取定位");
    locationRequestedRef.current = false;
  }, [currentUserId]);

  useEffect(() => {
    if (!currentUserId) return;
    const promptedKey = `geoPrompted:${currentUserId}`;
    const alreadyPrompted =
      locationRequestedRef.current ||
      promptedLocationUsers.has(currentUserId) ||
      window.sessionStorage.getItem(promptedKey) === "1";
    if (alreadyPrompted) return;
    locationRequestedRef.current = true;
    promptedLocationUsers.add(currentUserId);
    window.sessionStorage.setItem(promptedKey, "1");
    const confirmed = window.confirm("是否授权当前位置用于“就近医院推荐”？");
    if (!confirmed) {
      setLocationStatus("用户未授权定位");
      return;
    }
    void fetchGeolocation();
  }, [currentUserId]);

  // 仅展示必要且友好的定位状态，避免失败细节暴露给用户。
  const locationStatusText =
    locationStatus === "正在获取定位..."
      ? "正在获取附近位置..."
      : locationStatus === "定位已获取"
      ? "已获取附近位置"
      : "";

  const handleSend = async () => {
    const content = input.trim();
    if (!content || loading || uploading || loadingHistory) return;

    // 用户连续发送时，用 AbortController 中止上一个未完成的流，
    // 避免旧流的 onChunk / onDone 回调污染新消息
    if (abortRef.current) {
      abortRef.current.abort();
    }
    const controller = new AbortController();
    abortRef.current = controller;

    const outgoingImages = selectedImages.map((item) => item.dataUrl);
    const useDeepImageAnalysis = deepImageAnalysis;
    setInput("");
    setSelectedImages([]);
    setDeepImageAnalysis(false);

    // 先插入一个空的 assistant 占位消息（__streaming: true 标记），
    // 后续 onChunk 回调将 text 逐片段追加到此消息上，实现逐字出现的效果
    setMessages((prev) => [
      ...prev,
      {
        role: "user",
        text: content,
        images: selectedImages.map((item) => ({
          name: item.name,
          sizeText: formatBytes(item.size),
        })),
      },
      { role: "assistant", text: "", imageAnalysis: "", __streaming: true },
    ]);
    setLoading(true);

    try {
      let submitLocation = location;
      const canRetryLocation =
        locationStatus !== "用户未授权定位" &&
        locationStatus !== "定位获取失败或被拒绝" &&
        locationStatus !== "当前浏览器不支持定位";
      if (!submitLocation && canRetryLocation) {
        submitLocation = await fetchGeolocation();
      }
      await aiAnalyzeStream(
        content,
        sessionId || null,
        outgoingImages,
        submitLocation?.latitude ?? null,
        submitLocation?.longitude ?? null,
        useDeepImageAnalysis,
        {
          // 每个 token 追加到尾部 assistant 消息的 text 上
          onChunk: (chunk) => {
            setMessages((prev) => {
              const updated = [...prev];
              const last = updated[updated.length - 1];
              if (last.role === "assistant") {
                updated[updated.length - 1] = {
                  ...last,
                  text: last.text + chunk,
                };
              }
              return updated;
            });
          },
          // 推理状态文本（可扩展为 toast 提示）
          onStatus: (_status) => {},
          // 流正常结束时，锁定消息、绑定 sessionId、刷新侧边栏
          onDone: ({ sessionId: sid, imageAnalysis: ia }) => {
            if (sid) {
              setSessionId(sid);
              onSessionResolved?.(sid);
            }
            setMessages((prev) => {
              const updated = [...prev];
              const last = updated[updated.length - 1];
              if (last.role === "assistant") {
                updated[updated.length - 1] = {
                  ...last,
                  // 如果流没有产生任何 chunk（极端情况），用兜底文本
                  text: last.text || "暂无分析结果",
                  imageAnalysis: ia || "",
                  __streaming: false, // 解除 streaming 标记
                };
              }
              return updated;
            });
            onConversationUpdated?.(sid || sessionId || "");
            abortRef.current = null;
          },
          // 流异常时，将已收到的碎片保留，追加错误提示
          onError: (errorText) => {
            setMessages((prev) => {
              const updated = [...prev];
              const last = updated[updated.length - 1];
              if (last.role === "assistant") {
                updated[updated.length - 1] = {
                  ...last,
                  text: last.text || `分析失败：${errorText}`,
                  __streaming: false,
                };
              }
              return updated;
            });
            abortRef.current = null;
          },
          signal: controller.signal,
        }
      );
    } catch (e) {
      if (e.name !== "AbortError") {
        setMessages((prev) => {
          const updated = [...prev];
          const last = updated[updated.length - 1];
          if (last.role === "assistant") {
            updated[updated.length - 1] = {
              ...last,
              text: last.text || `分析失败：${e.message}`,
              __streaming: false,
            };
          }
          return updated;
        });
      }
      abortRef.current = null;
    } finally {
      setLoading(false);
    }
  };

  const handleImagePick = async (e) => {
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;
    if (selectedImages.length >= MAX_UPLOAD_IMAGES) {
      alert(`最多上传 ${MAX_UPLOAD_IMAGES} 张图片`);
      e.target.value = "";
      return;
    }
    const availableCount = MAX_UPLOAD_IMAGES - selectedImages.length;
    const picked = files.slice(0, availableCount);
    const invalid = picked.find((file) => !file.type.startsWith("image/"));
    if (invalid) {
      alert(`文件 ${invalid.name} 不是图片格式`);
      e.target.value = "";
      return;
    }
    setUploading(true);
    try {
      const converted = await Promise.all(
        picked.map(async (file) => ({
          id: `${file.name}-${file.size}-${file.lastModified}`,
          name: file.name,
          size: file.size,
          dataUrl: await fileToDataUrl(file),
        }))
      );
      setSelectedImages((prev) => [...prev, ...converted]);
    } catch (error) {
      alert(error.message || "上传失败，请重试");
    } finally {
      setUploading(false);
      e.target.value = "";
    }
  };

  const removeSelectedImage = (id) => {
    setSelectedImages((prev) => prev.filter((item) => item.id !== id));
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%" }}>
      <div
        style={{
          flex: 1,
          overflowY: "auto",
          padding: "4px 0 16px",
          minHeight: isMobile ? 240 : 360,
          maxHeight: isMobile ? "52vh" : "calc(100vh - 280px)",
        }}
      >
        {loadingHistory ? (
          <div style={{ color: "#9ca3af", fontSize: 13, padding: "20px 8px" }}>正在加载历史对话...</div>
        ) : (
          messages.map((m, i) => (
            <div
              key={i}
              style={{
                marginBottom: 12,
                display: "flex",
                justifyContent: m.role === "user" ? "flex-end" : "flex-start",
              }}
            >
              <div
                style={{
                  maxWidth: isMobile ? "94%" : "82%",
                  padding: "10px 14px",
                  borderRadius: 12,
                  fontSize: 14,
                  lineHeight: 1.6,
                  whiteSpace: m.role === "assistant" ? "normal" : "pre-wrap",
                  background: m.role === "user" ? "#2563eb" : "#f3f4f6",
                  color: m.role === "user" ? "#fff" : "#1f2937",
                }}
              >
                {m.role === "assistant" ? renderAssistantText(m.text) : m.text}
                {Array.isArray(m.images) && m.images.length > 0 && (
                  <div
                    style={{
                      marginTop: 10,
                      paddingTop: 8,
                      borderTop: "1px dashed rgba(107,114,128,0.35)",
                      fontSize: 12,
                      lineHeight: 1.8,
                    }}
                  >
                    {m.images.map((img, idx) => (
                      <div key={`${img.name}-${idx}`}>
                        附图{idx + 1}：{img.name}（{img.sizeText}）
                      </div>
                    ))}
                  </div>
                )}
                {m.role === "assistant" && m.imageAnalysis && (
                  <div
                    style={{
                      marginTop: 10,
                      paddingTop: 8,
                      borderTop: "1px dashed rgba(107,114,128,0.35)",
                      fontSize: 12,
                      color: "#374151",
                      whiteSpace: "pre-wrap",
                    }}
                  >
                    <strong>图片辅助分析：</strong>
                    <div>{m.imageAnalysis}</div>
                  </div>
                )}
              </div>
            </div>
          ))
        )}
        {loading && (
          <div style={{ color: "#9ca3af", fontSize: 13, padding: "0 4px" }}>
            AI 正在分析{messages[messages.length - 1]?.__streaming ? " · 逐字生成中" : "..."}
          </div>
        )}
        <div ref={endRef} />
      </div>

      {selectedImages.length > 0 && (
        <div
          style={{
            marginBottom: 10,
            padding: 10,
            border: "1px solid #e5e7eb",
            borderRadius: 10,
            background: "#f9fafb",
          }}
        >
          <div style={{ fontSize: 13, color: "#4b5563", marginBottom: 8 }}>
            待发送图片（{selectedImages.length}/{MAX_UPLOAD_IMAGES}）
          </div>
          <label
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: 6,
              marginBottom: 8,
              fontSize: 12,
              color: "#374151",
            }}
          >
            <input
              type="checkbox"
              checked={deepImageAnalysis}
              onChange={(e) => setDeepImageAnalysis(e.target.checked)}
              disabled={loading || uploading || loadingHistory}
            />
            深度分析图片（更慢，最多分析 {MAX_UPLOAD_IMAGES} 张）
          </label>
          <div style={{ fontSize: 12, color: "#6b7280", marginBottom: 8 }}>
            默认快速模式：最多分析 {QUICK_IMAGE_ANALYZE_LIMIT} 张图片
          </div>
          <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
            {selectedImages.map((item) => (
              <div
                key={item.id}
                style={{
                  width: 96,
                  border: "1px solid #e5e7eb",
                  borderRadius: 8,
                  background: "#fff",
                  overflow: "hidden",
                }}
              >
                <img
                  src={item.dataUrl}
                  alt={item.name}
                  style={{ width: "100%", height: 64, objectFit: "cover", display: "block" }}
                />
                <div style={{ padding: 6, fontSize: 11, lineHeight: 1.4 }}>
                  <div title={item.name} style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                    {item.name}
                  </div>
                  <div style={{ color: "#6b7280" }}>{formatBytes(item.size)}</div>
                  <button
                    onClick={() => removeSelectedImage(item.id)}
                    style={{
                      marginTop: 4,
                      width: "100%",
                      border: "none",
                      background: "#fee2e2",
                      color: "#b91c1c",
                      borderRadius: 4,
                      fontSize: 11,
                      cursor: "pointer",
                      padding: "4px 0",
                    }}
                  >
                    移除
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div style={{ display: "flex", gap: 8, alignItems: isMobile ? "stretch" : "flex-end", flexDirection: isMobile ? "column" : "row" }}>
        <div style={{ fontSize: 12, color: "#9ca3af", marginBottom: 4, minWidth: isMobile ? "auto" : 110 }}>{locationStatusText}</div>
        <button
          onClick={() => fileInputRef.current?.click()}
          disabled={loading || uploading || loadingHistory || selectedImages.length >= MAX_UPLOAD_IMAGES}
          style={{
            padding: "10px 14px",
            border: "1px solid #bfdbfe",
            borderRadius: 10,
            background: "#eff6ff",
            fontSize: 13,
            cursor: "pointer",
            whiteSpace: "nowrap",
            color: "#1d4ed8",
            width: isMobile ? "100%" : "auto",
          }}
        >
          {uploading ? "上传中..." : "上传图片"}
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          multiple
          style={{ display: "none" }}
          onChange={handleImagePick}
        />
        <textarea
          style={{
            ...inputStyle,
            resize: "none",
            minHeight: 48,
            fontFamily: "inherit",
            width: "100%",
          }}
          placeholder="请描述您的症状、不适或健康问题（可配合上传图片）..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          rows={isMobile ? 3 : 2}
        />
        <button
          onClick={handleSend}
          disabled={loading || uploading || loadingHistory || !input.trim()}
          style={{
            padding: "10px 22px",
            background: "#2563eb",
            color: "#fff",
            border: "none",
            borderRadius: 10,
            fontSize: 14,
            fontWeight: 600,
            cursor: "pointer",
            whiteSpace: "nowrap",
            opacity: loading || uploading || loadingHistory || !input.trim() ? 0.5 : 1,
            width: isMobile ? "100%" : "auto",
          }}
        >
          发送
        </button>
      </div>
    </div>
  );
}

export default function MainPage() {
  const [user, setUser] = useState(getUser());
  const [sessions, setSessions] = useState([]);
  const [loadingSessions, setLoadingSessions] = useState(false);
  const [loadingTurns, setLoadingTurns] = useState(false);
  const [activeSessionId, setActiveSessionId] = useState("");
  const [activeTurns, setActiveTurns] = useState([]);
  const [isMobile, setIsMobile] = useState(() =>
    typeof window !== "undefined" ? window.innerWidth <= 900 : false
  );
  const navigate = useNavigate();
  const admin = isAdmin(user);

  const refreshProfile = async () => {
    try {
      const data = await getMyProfile();
      setUser(data);
    } catch {
      // ignore
    }
  };

  const refreshSessions = async (keepActive = true, preferredActiveSessionId = "") => {
    setLoadingSessions(true);
    try {
      const data = await listAiSessions();
      const list = Array.isArray(data) ? data : [];
      const targetActiveSessionId = preferredActiveSessionId || activeSessionId;
      setSessions(list);
      if (keepActive) {
        if (targetActiveSessionId) {
          const exists = list.some((item) => item.sessionId === targetActiveSessionId);
          if (!exists) {
            setActiveSessionId("");
            setActiveTurns([]);
          } else if (targetActiveSessionId !== activeSessionId) {
            // 避免 setState 异步导致的会话 ID 竞态，把目标会话固定住。
            setActiveSessionId(targetActiveSessionId);
          }
        } else if (activeSessionId) {
          setActiveSessionId("");
          setActiveTurns([]);
        }
      }
      if (!keepActive && list.length > 0) {
        setActiveSessionId(list[0].sessionId);
      }
    } finally {
      setLoadingSessions(false);
    }
  };

  const loadSessionTurns = async (sessionId) => {
    if (!sessionId) {
      setActiveSessionId("");
      setActiveTurns([]);
      return;
    }
    setLoadingTurns(true);
    try {
      const data = await getAiSessionTurns(sessionId);
      setActiveSessionId(sessionId);
      setActiveTurns(Array.isArray(data?.turns) ? data.turns : []);
    } catch (e) {
      await refreshSessions(true);
      const msg = e?.message || "";
      if (msg.includes("会话不存在") || msg.includes("404")) {
        setActiveSessionId("");
        setActiveTurns([]);
        alert("该会话已被删除，列表已自动刷新");
        return;
      }
      alert(msg || "加载历史会话失败");
    } finally {
      setLoadingTurns(false);
    }
  };

  useEffect(() => {
    refreshProfile();
    refreshSessions(false);
  }, []);

  useEffect(() => {
    const onResize = () => {
      setIsMobile(window.innerWidth <= 900);
    };
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  useEffect(() => {
    const onFocus = () => {
      refreshSessions(true);
    };
    window.addEventListener("focus", onFocus);
    const timerId = window.setInterval(() => {
      refreshSessions(true);
    }, 30000);
    return () => {
      window.removeEventListener("focus", onFocus);
      window.clearInterval(timerId);
    };
  }, [activeSessionId]);

  const handleSessionResolved = async (sessionId) => {
    if (!sessionId) return;
    setActiveSessionId(sessionId);
    await refreshSessions(true, sessionId);
  };

  const handleConversationUpdated = async (sessionId) => {
    await refreshSessions(true, sessionId || "");
    if (sessionId) {
      setActiveSessionId(sessionId);
    }
  };

  const handleNewConversation = () => {
    setActiveSessionId("");
    setActiveTurns([]);
  };

  const handleLogout = () => {
    logout();
    navigate("/auth");
  };

  return (
    <div style={{ minHeight: "100vh", background: "#f5f6fa" }}>
      <header
        style={{
          background: "#fff",
          borderBottom: "1px solid #e5e7eb",
          padding: isMobile ? "10px 12px" : "0 20px",
          minHeight: 56,
          display: "flex",
          alignItems: isMobile ? "flex-start" : "center",
          justifyContent: "space-between",
          gap: 10,
          flexWrap: isMobile ? "wrap" : "nowrap",
        }}
      >
        <h1 style={{ fontSize: isMobile ? 16 : 18, fontWeight: 700, color: "#1e3a5f", margin: 0 }}>智能医疗</h1>
        <div style={{ display: "flex", alignItems: "center", gap: isMobile ? 8 : 14, flexWrap: "wrap", justifyContent: isMobile ? "flex-start" : "flex-end" }}>
          <span style={{ fontSize: 14, color: "#4b5563", maxWidth: isMobile ? "100%" : "none" }}>
            {(user?.username || "用户") + "（" + (ROLE_MAP[user?.role] || "未知角色") + "）"}
          </span>
          <button
            onClick={() => navigate("/profile")}
            style={{
              padding: "6px 12px",
              border: "1px solid #d1d5db",
              borderRadius: 8,
              background: "#fff",
              cursor: "pointer",
              fontSize: 13,
            }}
          >
            我的
          </button>
          {admin && (
            <button
              onClick={() => navigate("/admin")}
              style={{
                padding: "6px 12px",
                border: "none",
                borderRadius: 8,
                background: "#2563eb",
                color: "#fff",
                cursor: "pointer",
                fontSize: 13,
              }}
            >
              管理员页面
            </button>
          )}
          <button
            onClick={handleLogout}
            style={{
              padding: "6px 12px",
              border: "1px solid #d1d5db",
              borderRadius: 8,
              background: "#fff",
              cursor: "pointer",
              fontSize: 13,
            }}
          >
            退出
          </button>
        </div>
      </header>

      <main
        style={{
          display: "grid",
          gridTemplateColumns: isMobile ? "1fr" : "290px 1fr",
          gap: 16,
          maxWidth: 1360,
          margin: "16px auto",
          padding: isMobile ? "0 10px 12px" : "0 16px",
        }}
      >
        <aside
          style={{
            background: "#f3f4f6",
            borderRadius: 12,
            padding: "14px 12px",
            border: "1px solid #e5e7eb",
            height: isMobile ? "auto" : "calc(100vh - 108px)",
            maxHeight: isMobile ? "36vh" : "none",
            overflow: "hidden",
            display: "flex",
            flexDirection: "column",
          }}
        >
          <div style={{ fontSize: 13, color: "#6b7280", marginBottom: 8, paddingLeft: 4 }}>对话分组</div>
          <button
            onClick={handleNewConversation}
            style={{
              border: "none",
              background: activeSessionId ? "#e5e7eb" : "#dbeafe",
              color: activeSessionId ? "#111827" : "#1d4ed8",
              borderRadius: 10,
              padding: "10px 12px",
              display: "flex",
              alignItems: "center",
              gap: 8,
              cursor: "pointer",
              marginBottom: 18,
              boxShadow: activeSessionId ? "none" : "inset 0 0 0 1px #93c5fd",
            }}
          >
            <span style={{ fontSize: 24, lineHeight: 1 }}>+</span>
            <span style={{ fontSize: 20, lineHeight: 1 }}>新分组</span>
          </button>

          <div style={{ fontSize: 13, color: "#9ca3af", marginBottom: 10, paddingLeft: 4 }}>最近对话</div>
          <div style={{ overflowY: "auto", paddingRight: 2 }}>
            {loadingSessions ? (
              <div style={{ fontSize: 13, color: "#9ca3af", padding: "10px 8px" }}>加载中...</div>
            ) : sessions.length === 0 ? (
              <div style={{ fontSize: 13, color: "#9ca3af", padding: "10px 8px" }}>暂无历史对话</div>
            ) : (
              sessions.map((item) => (
                <button
                  key={item.sessionId}
                  onClick={() => loadSessionTurns(item.sessionId)}
                  style={{
                    width: "100%",
                    textAlign: "left",
                    border: "none",
                    borderRadius: 10,
                    background: item.sessionId === activeSessionId ? "#e0ecff" : "#ffffff",
                    padding: "10px 10px",
                    marginBottom: 8,
                    cursor: "pointer",
                    boxShadow: item.sessionId === activeSessionId ? "inset 0 0 0 1px #93c5fd" : "none",
                    borderLeft: item.sessionId === activeSessionId ? "3px solid #2563eb" : "3px solid transparent",
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
                    <div
                      style={{
                        fontSize: 14,
                        color: "#111827",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                        marginBottom: 4,
                      }}
                      title={item.title || "新会话"}
                    >
                      {item.title || "新会话"}
                    </div>
                    {item.sessionId === activeSessionId && (
                      <span
                        style={{
                          flexShrink: 0,
                          fontSize: 11,
                          color: "#1d4ed8",
                          background: "#dbeafe",
                          borderRadius: 999,
                          padding: "2px 6px",
                          lineHeight: 1.2,
                        }}
                      >
                        当前
                      </span>
                    )}
                  </div>
                  <div style={{ fontSize: 12, color: "#9ca3af" }}>
                    {formatDateTime(item.updateTime)} · 第{item.askRound || 0}轮
                  </div>
                </button>
              ))
            )}
          </div>
        </aside>

        <section
          style={{
            background: "#fff",
            borderRadius: 12,
            border: "1px solid #e5e7eb",
            padding: isMobile ? 12 : 20,
            minHeight: isMobile ? "60vh" : "calc(100vh - 108px)",
            display: "flex",
            flexDirection: "column",
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 12, alignItems: "center" }}>
            <h2 style={{ margin: 0, fontSize: 16 }}>AI 症状分析</h2>
            <span style={{ fontSize: 12, color: "#9ca3af" }}>{activeSessionId ? "当前会话" : "新会话"}</span>
          </div>
          <SymptomChat
            activeSessionId={activeSessionId}
            initialTurns={activeTurns}
            loadingHistory={loadingTurns}
            onSessionResolved={handleSessionResolved}
            onConversationUpdated={handleConversationUpdated}
            currentUserId={String(user?.id || "")}
            isMobile={isMobile}
          />
        </section>
      </main>
    </div>
  );
}
