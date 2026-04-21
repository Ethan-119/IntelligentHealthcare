const containerStyle = {
  maxWidth: "780px",
  margin: "48px auto",
  fontFamily: "Arial, sans-serif",
  lineHeight: 1.7,
  color: "#1f2937"
};

const cardStyle = {
  border: "1px solid #e5e7eb",
  borderRadius: "10px",
  padding: "20px",
  background: "#ffffff"
};

function App() {
  return (
    <main style={containerStyle}>
      <h1>Intelligent Healthcare</h1>
      <p>前端基础页面（React）已完成，当前为前后端分离初期框架。</p>
      <section style={cardStyle}>
        <h3>当前模块</h3>
        <ul>
          <li>前端技术栈：React + Vite</li>
          <li>后端接口：Spring Boot（/api/*）</li>
          <li>连接方式：Nginx 反向代理</li>
        </ul>
      </section>
    </main>
  );
}

export default App;
