import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getMyProfile, getUser, logout, updateMyProfile } from "../api/client";

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
  fontSize: "14px",
  outline: "none",
  boxSizing: "border-box",
};

export default function ProfilePage() {
  const navigate = useNavigate();
  const [user] = useState(getUser());
  const [phone, setPhone] = useState(getUser()?.phone || "");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [form, setForm] = useState({
    username: "",
    patientAge: "",
    patientGender: "",
    residentCity: "",
    area: "",
    triagePrefer: "",
  });

  const set = (key, value) => setForm((prev) => ({ ...prev, [key]: value }));

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setMessage("");
      try {
        const profile = await getMyProfile();
        setPhone(profile?.phone || user?.phone || "");
        setForm({
          username: profile?.username || "",
          patientAge: profile?.patientAge ?? "",
          patientGender: profile?.patientGender || "",
          residentCity: profile?.residentCity || "",
          area: profile?.area || "",
          triagePrefer: profile?.triagePrefer || "",
        });
      } catch (e) {
        setMessage(`加载个人信息失败：${e.message}`);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setMessage("");
    try {
      await updateMyProfile({
        ...form,
        phone,
        patientAge: form.patientAge === "" ? null : Number(form.patientAge),
      });
      setMessage("个人信息已保存");
    } catch (e) {
      setMessage(`保存失败：${e.message}`);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ minHeight: "100vh", background: "#f5f6fa" }}>
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
        <h1 style={{ margin: 0, fontSize: 18, color: "#1e3a5f" }}>个人信息</h1>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <span style={{ fontSize: 14, color: "#4b5563" }}>{user?.username || "用户"}</span>
          <button
            onClick={() => navigate("/")}
            style={{
              padding: "6px 14px",
              border: "1px solid #d1d5db",
              borderRadius: 8,
              background: "#fff",
              cursor: "pointer",
              fontSize: 13,
            }}
          >
            返回首页
          </button>
          <button
            onClick={logout}
            style={{
              padding: "6px 14px",
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

      <main style={{ maxWidth: 760, margin: "24px auto", padding: "0 24px" }}>
        <div
          style={{
            background: "#fff",
            borderRadius: 12,
            border: "1px solid #e5e7eb",
            padding: 20,
          }}
        >
          <form onSubmit={handleSubmit}>
            <div style={{ marginBottom: 14 }}>
              <label style={{ display: "block", marginBottom: 6, fontSize: 13 }}>手机号</label>
              <div
                style={{
                  ...inputStyle,
                  background: "#f9fafb",
                  color: "#111827",
                  minHeight: 42,
                  display: "flex",
                  alignItems: "center",
                }}
              >
                {phone || "-"}
              </div>
            </div>

            <div style={{ marginBottom: 14 }}>
              <label style={{ display: "block", marginBottom: 6, fontSize: 13 }}>用户名</label>
              <input
                style={inputStyle}
                value={form.username}
                onChange={(e) => set("username", e.target.value)}
                placeholder="请输入用户名"
              />
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 14 }}>
              <div>
                <label style={{ display: "block", marginBottom: 6, fontSize: 13 }}>年龄</label>
                <input
                  style={inputStyle}
                  type="number"
                  value={form.patientAge}
                  onChange={(e) => set("patientAge", e.target.value)}
                  placeholder="请输入年龄"
                />
              </div>
              <div>
                <label style={{ display: "block", marginBottom: 6, fontSize: 13 }}>性别</label>
                <select style={inputStyle} value={form.patientGender} onChange={(e) => set("patientGender", e.target.value)}>
                  {GENDER_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 14 }}>
              <div>
                <label style={{ display: "block", marginBottom: 6, fontSize: 13 }}>城市</label>
                <input
                  style={inputStyle}
                  value={form.residentCity}
                  onChange={(e) => set("residentCity", e.target.value)}
                  placeholder="请输入常驻城市"
                />
              </div>
              <div>
                <label style={{ display: "block", marginBottom: 6, fontSize: 13 }}>区域</label>
                <input
                  style={inputStyle}
                  value={form.area}
                  onChange={(e) => set("area", e.target.value)}
                  placeholder="请输入区域"
                />
              </div>
            </div>

            <div style={{ marginBottom: 14 }}>
              <label style={{ display: "block", marginBottom: 6, fontSize: 13 }}>就诊偏好</label>
              <select style={inputStyle} value={form.triagePrefer} onChange={(e) => set("triagePrefer", e.target.value)}>
                {PREFER_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </div>

            <button
              type="submit"
              disabled={loading || saving}
              style={{
                padding: "10px 20px",
                background: "#2563eb",
                color: "#fff",
                border: "none",
                borderRadius: 8,
                cursor: "pointer",
                fontSize: 14,
                opacity: loading || saving ? 0.6 : 1,
              }}
            >
              {saving ? "保存中..." : "保存"}
            </button>
          </form>

          {message && <p style={{ marginTop: 12, fontSize: 13, color: "#374151" }}>{message}</p>}
        </div>
      </main>
    </div>
  );
}
