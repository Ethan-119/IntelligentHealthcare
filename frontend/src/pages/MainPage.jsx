import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { getMyProfile, updateMyProfile, aiAnalyze, logout, getUser } from "../api/client";

const LABEL_MAP = {
  nearby: "就近就医",
  authority: "权威优先",
  emergency: "紧急优先",
};
const GENDER_MAP = { male: "男", female: "女", unknown: "未知" };

const inputStyle = {
  width: "100%",
  padding: "8px 10px",
  border: "1px solid #d1d5db",
  borderRadius: "6px",
  fontSize: "14px",
  outline: "none",
  boxSizing: "border-box",
};

function ProfileCard({ user, onRefresh }) {
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (user) {
      setForm({
        username: user.username || "",
        phone: "",
        patientAge: user.patientAge ?? "",
        patientGender: user.patientGender || "",
        residentCity: user.residentCity || "",
        area: user.area || "",
        triagePrefer: user.triagePrefer || "",
      });
    }
  }, [user]);

  const set = (key, value) => setForm((f) => ({ ...f, [key]: value }));

  const handleSave = async () => {
    setSaving(true);
    try {
      await updateMyProfile({
        ...form,
        patientAge: form.patientAge ? parseInt(form.patientAge, 10) : null,
      });
      setEditing(false);
      onRefresh();
    } catch (e) {
      alert(e.message);
    } finally {
      setSaving(false);
    }
  };

  if (!user) return null;

  if (editing) {
    return (
      <div>
        <h3 style={{ margin: "0 0 16px", fontSize: 16 }}>编辑个人资料</h3>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
          <div>
            <label style={{ fontSize: 13, color: "#6b7280" }}>用户名</label>
            <input
              style={inputStyle}
              value={form.username}
              onChange={(e) => set("username", e.target.value)}
            />
          </div>
          <div>
            <label style={{ fontSize: 13, color: "#6b7280" }}>手机号</label>
            <input
              style={inputStyle}
              value={form.phone}
              onChange={(e) => set("phone", e.target.value)}
            />
          </div>
          <div>
            <label style={{ fontSize: 13, color: "#6b7280" }}>年龄</label>
            <input
              style={inputStyle}
              type="number"
              value={form.patientAge}
              onChange={(e) => set("patientAge", e.target.value)}
            />
          </div>
          <div>
            <label style={{ fontSize: 13, color: "#6b7280" }}>性别</label>
            <select
              style={inputStyle}
              value={form.patientGender}
              onChange={(e) => set("patientGender", e.target.value)}
            >
              <option value="">请选择</option>
              <option value="male">男</option>
              <option value="female">女</option>
              <option value="unknown">未知</option>
            </select>
          </div>
          <div>
            <label style={{ fontSize: 13, color: "#6b7280" }}>城市</label>
            <input
              style={inputStyle}
              value={form.residentCity}
              onChange={(e) => set("residentCity", e.target.value)}
            />
          </div>
          <div>
            <label style={{ fontSize: 13, color: "#6b7280" }}>区域</label>
            <input
              style={inputStyle}
              value={form.area}
              onChange={(e) => set("area", e.target.value)}
            />
          </div>
          <div style={{ gridColumn: "1 / -1" }}>
            <label style={{ fontSize: 13, color: "#6b7280" }}>就诊偏好</label>
            <select
              style={inputStyle}
              value={form.triagePrefer}
              onChange={(e) => set("triagePrefer", e.target.value)}
            >
              <option value="">请选择</option>
              <option value="nearby">就近就医</option>
              <option value="authority">权威优先</option>
              <option value="emergency">紧急优先</option>
            </select>
          </div>
        </div>
        <div style={{ marginTop: 12, display: "flex", gap: 8 }}>
          <button
            onClick={handleSave}
            disabled={saving}
            style={{
              padding: "8px 16px",
              background: "#2563eb",
              color: "#fff",
              border: "none",
              borderRadius: 6,
              cursor: "pointer",
            }}
          >
            {saving ? "保存中..." : "保存"}
          </button>
          <button
            onClick={() => setEditing(false)}
            style={{
              padding: "8px 16px",
              background: "#f3f4f6",
              border: "1px solid #d1d5db",
              borderRadius: 6,
              cursor: "pointer",
            }}
          >
            取消
          </button>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 12,
        }}
      >
        <h3 style={{ margin: 0, fontSize: 16 }}>
          {user.username || "未设置用户名"}
        </h3>
        <button
          onClick={() => setEditing(true)}
          style={{
            padding: "4px 12px",
            background: "none",
            border: "1px solid #2563eb",
            borderRadius: 6,
            color: "#2563eb",
            cursor: "pointer",
            fontSize: 13,
          }}
        >
          编辑
        </button>
      </div>
      <div style={{ fontSize: 14, color: "#4b5563", lineHeight: 1.8 }}>
        {user.patientAge != null && <div>年龄：{user.patientAge} 岁</div>}
        {user.patientGender && (
          <div>性别：{GENDER_MAP[user.patientGender] || user.patientGender}</div>
        )}
        {user.residentCity && <div>城市：{user.residentCity}</div>}
        {user.area && <div>区域：{user.area}</div>}
        {user.triagePrefer && (
          <div>就诊偏好：{LABEL_MAP[user.triagePrefer] || user.triagePrefer}</div>
        )}
      </div>
    </div>
  );
}

function SymptomChat() {
  const [messages, setMessages] = useState([
    {
      role: "assistant",
      text: "您好，我是智能医疗助手。请描述您的症状或健康问题，我将为您提供初步分析和就医建议。",
    },
  ]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const endRef = useRef(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSend = async () => {
    const content = input.trim();
    if (!content || loading) return;

    setInput("");
    setMessages((prev) => [...prev, { role: "user", text: content }]);
    setLoading(true);

    try {
      const data = await aiAnalyze(content);
      setMessages((prev) => [
        ...prev,
        { role: "assistant", text: data.result || "暂无分析结果" },
      ]);
    } catch (e) {
      setMessages((prev) => [
        ...prev,
        { role: "assistant", text: `分析失败：${e.message}` },
      ]);
    } finally {
      setLoading(false);
    }
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
          padding: "0 0 16px",
          minHeight: 200,
          maxHeight: 500,
        }}
      >
        {messages.map((m, i) => (
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
                maxWidth: "80%",
                padding: "10px 14px",
                borderRadius: 12,
                fontSize: 14,
                lineHeight: 1.6,
                whiteSpace: "pre-wrap",
                background: m.role === "user" ? "#2563eb" : "#f3f4f6",
                color: m.role === "user" ? "#fff" : "#1f2937",
              }}
            >
              {m.text}
            </div>
          </div>
        ))}
        {loading && (
          <div style={{ color: "#9ca3af", fontSize: 13, padding: "0 4px" }}>
            AI 正在分析...
          </div>
        )}
        <div ref={endRef} />
      </div>

      <div style={{ display: "flex", gap: 8 }}>
        <textarea
          style={{
            ...inputStyle,
            resize: "none",
            minHeight: 44,
            fontFamily: "inherit",
          }}
          placeholder="请描述您的症状、不适或健康问题..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          rows={2}
        />
        <button
          onClick={handleSend}
          disabled={loading || !input.trim()}
          style={{
            padding: "8px 20px",
            background: "#2563eb",
            color: "#fff",
            border: "none",
            borderRadius: 8,
            fontSize: 14,
            fontWeight: 600,
            cursor: "pointer",
            whiteSpace: "nowrap",
            opacity: loading || !input.trim() ? 0.5 : 1,
            alignSelf: "flex-end",
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
  const navigate = useNavigate();

  const refreshProfile = async () => {
    try {
      const data = await getMyProfile();
      setUser(data);
    } catch (e) {
      // ignore
    }
  };

  useEffect(() => {
    refreshProfile();
  }, []);

  const handleLogout = () => {
    logout();
    navigate("/auth");
  };

  return (
    <div style={{ minHeight: "100vh", background: "#f9fafb" }}>
      {/* Navbar */}
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
        <h1
          style={{
            fontSize: 18,
            fontWeight: 700,
            color: "#1e3a5f",
            margin: 0,
          }}
        >
          智能医疗
        </h1>
        <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
          <span style={{ fontSize: 14, color: "#4b5563" }}>
            {user?.username || "用户"}
          </span>
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

      {/* Content */}
      <main
        style={{
          maxWidth: 960,
          margin: "24px auto",
          padding: "0 24px",
          display: "grid",
          gridTemplateColumns: "300px 1fr",
          gap: 24,
        }}
      >
        {/* Left: Profile */}
        <aside>
          <div
            style={{
              background: "#fff",
              borderRadius: 12,
              border: "1px solid #e5e7eb",
              padding: 20,
            }}
          >
            <ProfileCard user={user} onRefresh={refreshProfile} />
          </div>
        </aside>

        {/* Right: AI Symptom Chat */}
        <section>
          <div
            style={{
              background: "#fff",
              borderRadius: 12,
              border: "1px solid #e5e7eb",
              padding: 20,
              height: "fit-content",
            }}
          >
            <h2 style={{ margin: "0 0 16px", fontSize: 16 }}>
              AI 症状分析
            </h2>
            <SymptomChat />
          </div>
        </section>
      </main>
    </div>
  );
}
