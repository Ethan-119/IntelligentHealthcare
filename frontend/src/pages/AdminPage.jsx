import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { addKnowledgeHotspots, evictKnowledgeCache, refreshKnowledgeCache, logout, getUser } from "../api/client";

const HOTSPOT_SCOPE_OPTIONS = [
  { value: "disease", label: "疾病" },
  { value: "hospital", label: "医院" },
  { value: "department", label: "科室" },
  { value: "doctor", label: "医生" },
  { value: "capability", label: "能力" },
];

export default function AdminPage() {
  const navigate = useNavigate();
  const user = getUser();
  const [running, setRunning] = useState(false);
  const [message, setMessage] = useState("");
  const [selectedHotspotScopes, setSelectedHotspotScopes] = useState(["disease", "hospital"]);

  const handleEvict = async () => {
    setRunning(true);
    setMessage("");
    try {
      const data = await evictKnowledgeCache();
      setMessage(data?.message || "缓存已清理");
    } catch (e) {
      setMessage(`清理失败：${e.message}`);
    } finally {
      setRunning(false);
    }
  };

  const handleRefresh = async () => {
    setRunning(true);
    setMessage("");
    try {
      const data = await refreshKnowledgeCache();
      const costText = data?.costMs != null ? `，耗时 ${data.costMs} ms` : "";
      setMessage((data?.message || "热点缓存已刷新") + costText);
    } catch (e) {
      setMessage(`刷新失败：${e.message}`);
    } finally {
      setRunning(false);
    }
  };

  const handleAddHotspots = async () => {
    if (selectedHotspotScopes.length === 0) {
      setMessage("请至少选择一个热点范围");
      return;
    }
    setRunning(true);
    setMessage("");
    try {
      const data = await addKnowledgeHotspots(selectedHotspotScopes);
      const scopesText = Array.isArray(data?.scopes) ? data.scopes.join("、") : "";
      const totalText = data?.totalEntries != null ? `，共 ${data.totalEntries} 条` : "";
      const costText = data?.costMs != null ? `，耗时 ${data.costMs} ms` : "";
      setMessage((data?.message || "热点缓存新增预热完成") + `（${scopesText}${totalText}${costText}）`);
    } catch (e) {
      setMessage(`新增热点缓存失败：${e.message}`);
    } finally {
      setRunning(false);
    }
  };

  const toggleHotspotScope = (scope) => {
    setSelectedHotspotScopes((prev) => {
      if (prev.includes(scope)) {
        return prev.filter((item) => item !== scope);
      }
      return [...prev, scope];
    });
  };

  const handleLogout = () => {
    logout();
  };

  return (
    <div style={{ minHeight: "100vh", background: "#f9fafb" }}>
      <header
        style={{
          background: "#fff",
          borderBottom: "1px solid #e5e7eb",
          padding: "0 24px",
          height: 56,
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
        }}
      >
        <h1 style={{ fontSize: 18, fontWeight: 700, color: "#1e3a5f", margin: 0 }}>
          管理员后台
        </h1>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <span style={{ fontSize: 14, color: "#4b5563" }}>{user?.username || "管理员"}</span>
          <button
            onClick={() => navigate("/")}
            style={{
              padding: "6px 14px",
              border: "1px solid #d1d5db",
              borderRadius: 6,
              background: "#fff",
              cursor: "pointer",
              fontSize: 13,
            }}
          >
            返回首页
          </button>
          <button
            onClick={handleLogout}
            style={{
              padding: "6px 14px",
              border: "1px solid #d1d5db",
              borderRadius: 6,
              background: "#fff",
              cursor: "pointer",
              fontSize: 13,
            }}
          >
            退出
          </button>
        </div>
      </header>

      <main style={{ maxWidth: 960, margin: "24px auto", padding: "0 24px" }}>
        <div
          style={{
            background: "#fff",
            borderRadius: 12,
            border: "1px solid #e5e7eb",
            padding: 20,
          }}
        >
          <h2 style={{ margin: "0 0 16px", fontSize: 16 }}>知识库缓存管理</h2>
          <div
            style={{
              marginBottom: 12,
              display: "flex",
              gap: 12,
              flexWrap: "wrap",
              alignItems: "center",
            }}
          >
            <div style={{ fontSize: 13, color: "#4b5563" }}>新增热点范围：</div>
            {HOTSPOT_SCOPE_OPTIONS.map((item) => (
              <label
                key={item.value}
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: 6,
                  fontSize: 13,
                  color: "#374151",
                }}
              >
                <input
                  type="checkbox"
                  checked={selectedHotspotScopes.includes(item.value)}
                  disabled={running}
                  onChange={() => toggleHotspotScope(item.value)}
                />
                {item.label}
              </label>
            ))}
          </div>
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
            <button
              onClick={handleEvict}
              disabled={running}
              style={{
                padding: "8px 16px",
                border: "1px solid #d1d5db",
                borderRadius: 6,
                background: "#fff",
                cursor: "pointer",
              }}
            >
              清理缓存
            </button>
            <button
              onClick={handleRefresh}
              disabled={running}
              style={{
                padding: "8px 16px",
                border: "none",
                borderRadius: 6,
                background: "#2563eb",
                color: "#fff",
                cursor: "pointer",
              }}
            >
              刷新热点缓存
            </button>
            <button
              onClick={handleAddHotspots}
              disabled={running}
              style={{
                padding: "8px 16px",
                border: "1px solid #2563eb",
                borderRadius: 6,
                background: "#eff6ff",
                color: "#1d4ed8",
                cursor: "pointer",
              }}
            >
              新增热点缓存
            </button>
          </div>
          {message && (
            <p style={{ margin: "12px 0 0", fontSize: 13, color: "#374151" }}>{message}</p>
          )}
        </div>
      </main>
    </div>
  );
}
