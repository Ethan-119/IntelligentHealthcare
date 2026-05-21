import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { login, register } from "../api/client";

const GENDER_OPTIONS = [
  { value: "", label: "请选择" },
  { value: "male", label: "男" },
  { value: "female", label: "女" },
  { value: "unknown", label: "未知" },
];

const PREFER_OPTIONS = [
  { value: "", label: "请选择" },
  { value: "nearby", label: "就近就医" },
  { value: "authority", label: "权威优先" },
  { value: "emergency", label: "紧急优先" },
];

const inputStyle = {
  width: "100%",
  padding: "10px 12px",
  border: "1px solid #d1d5db",
  borderRadius: "8px",
  fontSize: "15px",
  outline: "none",
  boxSizing: "border-box",
};

const btnStyle = {
  width: "100%",
  padding: "12px",
  background: "#2563eb",
  color: "#fff",
  border: "none",
  borderRadius: "8px",
  fontSize: "16px",
  cursor: "pointer",
  fontWeight: "600",
};

function LoginForm({ onSuccess }) {
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await login(phone, password);
      onSuccess();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {error && (
        <p style={{ color: "#dc2626", marginBottom: 12, fontSize: 14 }}>{error}</p>
      )}
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: "block", marginBottom: 6, fontWeight: 500 }}>
          手机号
        </label>
        <input
          style={inputStyle}
          placeholder="请输入11位手机号"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          pattern="1[3-9]\d{9}"
          title="请输入有效的11位手机号"
          required
        />
      </div>
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: "block", marginBottom: 6, fontWeight: 500 }}>
          密码
        </label>
        <input
          style={inputStyle}
          type="password"
          placeholder="请输入密码"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
      </div>
      <button style={btnStyle} disabled={loading} type="submit">
        {loading ? "登录中..." : "登录"}
      </button>
    </form>
  );
}

function RegisterForm({ onSuccess }) {
  const [form, setForm] = useState({
    phone: "",
    password: "",
    username: "",
    patientAge: "",
    patientGender: "",
    residentCity: "",
    area: "",
    triagePrefer: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const set = (key, value) => setForm((f) => ({ ...f, [key]: value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await register({
        ...form,
        patientAge: form.patientAge ? parseInt(form.patientAge, 10) : null,
        patientGender: form.patientGender || null,
        triagePrefer: form.triagePrefer || null,
      });
      onSuccess();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {error && (
        <p style={{ color: "#dc2626", marginBottom: 12, fontSize: 14 }}>{error}</p>
      )}
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: "block", marginBottom: 6, fontWeight: 500 }}>
          手机号 <span style={{ color: "#dc2626" }}>*</span>
        </label>
        <input
          style={inputStyle}
          placeholder="请输入11位手机号"
          value={form.phone}
          onChange={(e) => set("phone", e.target.value)}
          pattern="1[3-9]\d{9}"
          title="请输入有效的11位手机号"
          required
        />
      </div>
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: "block", marginBottom: 6, fontWeight: 500 }}>
          密码 <span style={{ color: "#dc2626" }}>*</span>
        </label>
        <input
          style={inputStyle}
          type="password"
          placeholder="至少 8 位密码"
          value={form.password}
          onChange={(e) => set("password", e.target.value)}
          minLength={8}
          required
        />
      </div>
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: "block", marginBottom: 6, fontWeight: 500 }}>
          用户名
        </label>
        <input
          style={inputStyle}
          placeholder="请输入用户名"
          value={form.username}
          onChange={(e) => set("username", e.target.value)}
        />
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: 12,
          marginBottom: 16,
        }}
      >
        <div>
          <label style={{ display: "block", marginBottom: 6, fontWeight: 500 }}>
            年龄
          </label>
          <input
            style={inputStyle}
            type="number"
            placeholder="年龄"
            value={form.patientAge}
            onChange={(e) => set("patientAge", e.target.value)}
          />
        </div>
        <div>
          <label style={{ display: "block", marginBottom: 6, fontWeight: 500 }}>
            性别
          </label>
          <select
            style={inputStyle}
            value={form.patientGender}
            onChange={(e) => set("patientGender", e.target.value)}
          >
            {GENDER_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: 12,
          marginBottom: 16,
        }}
      >
        <div>
          <label style={{ display: "block", marginBottom: 6, fontWeight: 500 }}>
            城市
          </label>
          <input
            style={inputStyle}
            placeholder="如 北京"
            value={form.residentCity}
            onChange={(e) => set("residentCity", e.target.value)}
          />
        </div>
        <div>
          <label style={{ display: "block", marginBottom: 6, fontWeight: 500 }}>
            区域
          </label>
          <input
            style={inputStyle}
            placeholder="如 海淀区"
            value={form.area}
            onChange={(e) => set("area", e.target.value)}
          />
        </div>
      </div>

      <div style={{ marginBottom: 16 }}>
        <label style={{ display: "block", marginBottom: 6, fontWeight: 500 }}>
          就诊偏好
        </label>
        <select
          style={inputStyle}
          value={form.triagePrefer}
          onChange={(e) => set("triagePrefer", e.target.value)}
        >
          {PREFER_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>

      <button style={btnStyle} disabled={loading} type="submit">
        {loading ? "注册中..." : "注册"}
      </button>
    </form>
  );
}

export default function AuthPage() {
  const [tab, setTab] = useState("login");
  const navigate = useNavigate();

  const handleSuccess = () => navigate("/");

  return (
    <div
      style={{
        minHeight: "100vh",
        background: "linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: 24,
      }}
    >
      <div
        style={{
          width: "100%",
          maxWidth: 440,
          background: "#fff",
          borderRadius: 16,
          boxShadow: "0 4px 24px rgba(0,0,0,0.08)",
          overflow: "hidden",
        }}
      >
        <div style={{ padding: "32px 32px 0" }}>
          <h1
            style={{
              textAlign: "center",
              margin: "0 0 8px",
              fontSize: 24,
              color: "#1e3a5f",
            }}
          >
            智能医疗
          </h1>
          <p
            style={{
              textAlign: "center",
              margin: "0 0 24px",
              color: "#6b7280",
              fontSize: 14,
            }}
          >
            AI 驱动的症状分析与就医建议
          </p>

          <div
            style={{
              display: "flex",
              borderBottom: "2px solid #e5e7eb",
              marginBottom: 24,
            }}
          >
            <button
              onClick={() => setTab("login")}
              style={{
                flex: 1,
                padding: "10px 0",
                border: "none",
                background: "none",
                fontSize: 15,
                fontWeight: tab === "login" ? 600 : 400,
                color: tab === "login" ? "#2563eb" : "#9ca3af",
                borderBottom: tab === "login" ? "2px solid #2563eb" : "2px solid transparent",
                marginBottom: -2,
                cursor: "pointer",
              }}
            >
              登录
            </button>
            <button
              onClick={() => setTab("register")}
              style={{
                flex: 1,
                padding: "10px 0",
                border: "none",
                background: "none",
                fontSize: 15,
                fontWeight: tab === "register" ? 600 : 400,
                color: tab === "register" ? "#2563eb" : "#9ca3af",
                borderBottom:
                  tab === "register" ? "2px solid #2563eb" : "2px solid transparent",
                marginBottom: -2,
                cursor: "pointer",
              }}
            >
              注册
            </button>
          </div>
        </div>

        <div style={{ padding: "0 32px 32px" }}>
          {tab === "login" ? (
            <LoginForm onSuccess={handleSuccess} />
          ) : (
            <RegisterForm onSuccess={handleSuccess} />
          )}
        </div>
      </div>
    </div>
  );
}
