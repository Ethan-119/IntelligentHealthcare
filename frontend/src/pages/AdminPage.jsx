import { useState, useRef, useCallback, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  addKnowledgeHotspots,
  evictKnowledgeCache,
  refreshKnowledgeCache,
  uploadDocument,
  listDocuments,
  activateDocument,
  deactivateDocument,
  listKnowledgeDataByType,
  toggleKnowledgeData,
  uploadImportJob,
  listImportJobs,
  logout,
  getUser,
} from "../api/client";

const HOTSPOT_SCOPE_OPTIONS = [
  { value: "disease", label: "疾病" },
  { value: "hospital", label: "医院" },
  { value: "department", label: "科室" },
  { value: "doctor", label: "医生" },
  { value: "capability", label: "能力" },
];

const ALLOWED_EXTENSIONS = ".pdf,.doc,.docx,.ppt,.pptx,.csv,.txt";

function CacheManagementPanel() {
  const [running, setRunning] = useState(false);
  const [message, setMessage] = useState("");
  const [selectedHotspotScopes, setSelectedHotspotScopes] = useState([
    "disease",
    "hospital",
  ]);

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
      const scopesText = Array.isArray(data?.scopes)
        ? data.scopes.join("、")
        : "";
      const totalText =
        data?.totalEntries != null ? `，共 ${data.totalEntries} 条` : "";
      const costText = data?.costMs != null ? `，耗时 ${data.costMs} ms` : "";
      setMessage(
        (data?.message || "热点缓存新增预热完成") +
          `（${scopesText}${totalText}${costText}）`
      );
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

  return (
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
        <p style={{ margin: "12px 0 0", fontSize: 13, color: "#374151" }}>
          {message}
        </p>
      )}
    </div>
  );
}

function DocumentManagementPanel() {
  const fileInputRef = useRef(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("info");
  const [documents, setDocuments] = useState([]);
  const [loadingList, setLoadingList] = useState(false);
  const [togglingDoc, setTogglingDoc] = useState(null);

  const loadDocuments = useCallback(async () => {
    setLoadingList(true);
    try {
      const data = await listDocuments();
      setDocuments(Array.isArray(data) ? data : []);
    } catch (e) {
      setMessage("加载文档列表失败：" + e.message);
      setMessageType("error");
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => {
    loadDocuments();
  }, [loadDocuments]);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setSelectedFile(file);
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setMessage("请先选择文件");
      setMessageType("error");
      return;
    }
    setUploading(true);
    setMessage("");
    try {
      const data = await uploadDocument(selectedFile);
      setMessage(data?.message || "上传成功");
      setMessageType("success");
      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
      loadDocuments();
    } catch (e) {
      setMessage("上传失败：" + e.message);
      setMessageType("error");
    } finally {
      setUploading(false);
    }
  };

  const handleCancel = () => {
    setSelectedFile(null);
    setMessage("");
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  // 切换文档上架/下架状态
  const handleToggleActive = async function (docName, currentActive) {
    setTogglingDoc(docName);
    try {
      if (currentActive) {
        await deactivateDocument(docName);
      } else {
        await activateDocument(docName);
      }
      loadDocuments();
    } catch (e) {
      setMessage("操作失败：" + e.message);
      setMessageType("error");
    } finally {
      setTogglingDoc(null);
    }
  };

  return (
    <div>
      {/* 上传区域 */}
      <div
        style={{
          background: "#fff",
          borderRadius: 12,
          border: "1px solid #e5e7eb",
          padding: 20,
          marginBottom: 16,
        }}
      >
        <h2 style={{ margin: "0 0 16px", fontSize: 16 }}>上传文档</h2>
        <p style={{ fontSize: 13, color: "#6b7280", margin: "0 0 12px" }}>
          支持格式：PDF、DOC、DOCX、PPT、PPTX、CSV、TXT（仅支持包含文本层的 PDF）
        </p>

        <div style={{ display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap" }}>
          <button
            onClick={() => fileInputRef.current && fileInputRef.current.click()}
            disabled={uploading}
            style={{
              padding: "8px 16px",
              border: "1px solid #d1d5db",
              borderRadius: 6,
              background: "#fff",
              cursor: "pointer",
              fontSize: 13,
            }}
          >
            选择文件
          </button>
          <input
            type="file"
            ref={fileInputRef}
            accept={ALLOWED_EXTENSIONS}
            onChange={handleFileChange}
            style={{ display: "none" }}
          />

          {selectedFile && (
            <>
              <span style={{ fontSize: 13, color: "#374151" }}>
                {selectedFile.name}
              </span>
              <button
                onClick={handleUpload}
                disabled={uploading}
                style={{
                  padding: "8px 16px",
                  border: "none",
                  borderRadius: 6,
                  background: "#2563eb",
                  color: "#fff",
                  cursor: "pointer",
                  fontSize: 13,
                }}
              >
                {uploading ? "上传中..." : "上传"}
              </button>
              <button
                onClick={handleCancel}
                disabled={uploading}
                style={{
                  padding: "8px 16px",
                  border: "1px solid #d1d5db",
                  borderRadius: 6,
                  background: "#fff",
                  cursor: "pointer",
                  fontSize: 13,
                }}
              >
                取消
              </button>
            </>
          )}
        </div>

        {message && (
          <p
            style={{
              margin: "12px 0 0",
              fontSize: 13,
              color: messageType === "success" ? "#16a34a" : messageType === "error" ? "#dc2626" : "#374151",
            }}
          >
            {message}
          </p>
        )}
      </div>

      {/* 文档列表区域 */}
      <div
        style={{
          background: "#fff",
          borderRadius: 12,
          border: "1px solid #e5e7eb",
          padding: 20,
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            marginBottom: 16,
          }}
        >
          <h2 style={{ margin: 0, fontSize: 16 }}>已上传文档</h2>
          <button
            onClick={loadDocuments}
            disabled={loadingList}
            style={{
              padding: "6px 14px",
              border: "1px solid #d1d5db",
              borderRadius: 6,
              background: "#fff",
              cursor: "pointer",
              fontSize: 13,
            }}
          >
            {loadingList ? "刷新中..." : "刷新列表"}
          </button>
        </div>

        {documents.length === 0 ? (
          <p style={{ fontSize: 13, color: "#9ca3af", margin: 0 }}>
            暂无已上传文档
          </p>
        ) : (
          <table
            style={{
              width: "100%",
              borderCollapse: "collapse",
              fontSize: 13,
            }}
          >
            <thead>
              <tr style={{ borderBottom: "2px solid #e5e7eb", textAlign: "left" }}>
                <th style={{ padding: "8px 12px", color: "#4b5563", fontWeight: 600 }}>
                  文件名
                </th>
                <th
                  style={{
                    padding: "8px 12px",
                    color: "#4b5563",
                    fontWeight: 600,
                    width: 100,
                    textAlign: "center",
                  }}
                >
                  文本块数
                </th>
                <th
                  style={{
                    padding: "8px 12px",
                    color: "#4b5563",
                    fontWeight: 600,
                    width: 70,
                    textAlign: "center",
                  }}
                >
                  状态
                </th>
                <th
                  style={{
                    padding: "8px 12px",
                    color: "#4b5563",
                    fontWeight: 600,
                    width: 80,
                    textAlign: "center",
                  }}
                >
                  操作
                </th>
              </tr>
            </thead>
            <tbody>
              {documents.map(function (doc) {
                var isActive = doc.active !== false; // 后端返回 active 字段
                return (
                  <tr
                    key={doc.documentName || doc.document_name}
                    style={{ borderBottom: "1px solid #f3f4f6" }}
                  >
                    <td style={{ padding: "8px 12px", color: "#1f2937" }}>
                      {doc.documentName || doc.document_name || "-"}
                    </td>
                    <td style={{ padding: "8px 12px", color: "#1f2937", textAlign: "center" }}>
                      {doc.chunkCount ?? doc.chunk_count ?? 0}
                    </td>
                    <td style={{ padding: "8px 12px", textAlign: "center" }}>
                      <span
                        style={{
                          fontSize: 12,
                          padding: "2px 8px",
                          borderRadius: 10,
                          background: isActive ? "#dcfce7" : "#fee2e2",
                          color: isActive ? "#16a34a" : "#dc2626",
                        }}
                      >
                        {isActive ? "上架" : "下架"}
                      </span>
                    </td>
                    <td style={{ padding: "8px 12px", textAlign: "center" }}>
                      <button
                        onClick={function () {
                          var docName = doc.documentName || doc.document_name;
                          handleToggleActive(docName, isActive);
                        }}
                        disabled={togglingDoc === (doc.documentName || doc.document_name)}
                        style={{
                          padding: "4px 10px",
                          border: "1px solid #d1d5db",
                          borderRadius: 4,
                          background: "#fff",
                          cursor: "pointer",
                          fontSize: 12,
                          color: isActive ? "#dc2626" : "#16a34a",
                        }}
                      >
                        {togglingDoc === (doc.documentName || doc.document_name)
                          ? "..."
                          : isActive
                          ? "下架"
                          : "上架"}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

// 数据集类型选项
const DATASET_TYPE_OPTIONS = [
  { value: "DISEASE_MASTER", label: "疾病主数据" },
  { value: "DISEASE_ALIAS", label: "疾病别名" },
];

const IMPORT_ALLOWED_EXTENSIONS = ".csv,.xlsx,.xls";

function DataImportPanel() {
  const fileInputRef = useRef(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const [datasetType, setDatasetType] = useState("DISEASE_MASTER");
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("info");
  const [jobs, setJobs] = useState([]);
  const [loadingList, setLoadingList] = useState(false);

  const loadJobs = useCallback(async () => {
    setLoadingList(true);
    try {
      const data = await listImportJobs();
      setJobs(Array.isArray(data) ? data : []);
    } catch (e) {
      setMessage("加载导入任务列表失败：" + e.message);
      setMessageType("error");
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => {
    loadJobs();
  }, [loadJobs]);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setSelectedFile(file);
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setMessage("请先选择文件");
      setMessageType("error");
      return;
    }
    setUploading(true);
    setMessage("");
    try {
      const data = await uploadImportJob(selectedFile, datasetType);
      // 构造一条成功消息
      var successCount = data.successCount ?? data.success_count ?? 0;
      var failureCount = data.failureCount ?? data.failure_count ?? 0;
      var reviewCount = data.reviewCount ?? data.review_count ?? 0;
      setMessage(
        "导入完成" +
          "（成功 " + successCount +
          "，失败 " + failureCount +
          "，待复核 " + reviewCount + "）"
      );
      setMessageType("success");
      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
      loadJobs();
    } catch (e) {
      setMessage("导入失败：" + e.message);
      setMessageType("error");
    } finally {
      setUploading(false);
    }
  };

  const handleCancel = () => {
    setSelectedFile(null);
    setMessage("");
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  // 中文映射数据集类型
  var datasetTypeLabels = {
    DISEASE_MASTER: "疾病主数据",
    DISEASE_ALIAS: "疾病别名",
  };

  // 中文映射状态
  var statusLabels = {
    RUNNING: "进行中",
    COMPLETED: "已完成",
    FAILED: "失败",
    REVIEWING: "待复核",
  };

  return (
    <div>
      {/* 上传区域 */}
      <div
        style={{
          background: "#fff",
          borderRadius: 12,
          border: "1px solid #e5e7eb",
          padding: 20,
          marginBottom: 16,
        }}
      >
        <h2 style={{ margin: "0 0 4px", fontSize: 16 }}>
          结构化数据导入
        </h2>
        <p style={{ fontSize: 13, color: "#6b7280", margin: "0 0 16px" }}>
          上传 Excel 或 CSV 文件，写入 PostgreSQL 业务表（仅支持 .csv、.xlsx、.xls）
        </p>

        {/* 数据集类型选择 */}
        <div
          style={{
            marginBottom: 12,
            display: "flex",
            gap: 16,
            alignItems: "center",
          }}
        >
          <span style={{ fontSize: 13, color: "#4b5563" }}>数据类型：</span>
          {DATASET_TYPE_OPTIONS.map(function (opt) {
            var isActive = datasetType === opt.value;
            return (
              <button
                key={opt.value}
                onClick={function () {
                  setDatasetType(opt.value);
                }}
                disabled={uploading}
                style={{
                  padding: "6px 14px",
                  border: isActive
                    ? "2px solid #2563eb"
                    : "1px solid #d1d5db",
                  borderRadius: 6,
                  background: isActive ? "#eff6ff" : "#fff",
                  color: isActive ? "#1d4ed8" : "#4b5563",
                  cursor: "pointer",
                  fontSize: 13,
                  fontWeight: isActive ? 600 : 400,
                }}
              >
                {opt.label}
              </button>
            );
          })}
        </div>

        {/* 文件选择与上传 */}
        <div
          style={{
            display: "flex",
            gap: 12,
            alignItems: "center",
            flexWrap: "wrap",
          }}
        >
          <button
            onClick={function () {
              if (fileInputRef.current) fileInputRef.current.click();
            }}
            disabled={uploading}
            style={{
              padding: "8px 16px",
              border: "1px solid #d1d5db",
              borderRadius: 6,
              background: "#fff",
              cursor: "pointer",
              fontSize: 13,
            }}
          >
            选择文件
          </button>
          <input
            type="file"
            ref={fileInputRef}
            accept={IMPORT_ALLOWED_EXTENSIONS}
            onChange={handleFileChange}
            style={{ display: "none" }}
          />

          {selectedFile && (
            <span style={{ fontSize: 13, color: "#374151" }}>
              {selectedFile.name}
            </span>
          )}
          {selectedFile && (
            <button
              onClick={handleUpload}
              disabled={uploading}
              style={{
                padding: "8px 16px",
                border: "none",
                borderRadius: 6,
                background: "#2563eb",
                color: "#fff",
                cursor: "pointer",
                fontSize: 13,
              }}
            >
              {uploading ? "导入中..." : "开始导入"}
            </button>
          )}
          {selectedFile && (
            <button
              onClick={handleCancel}
              disabled={uploading}
              style={{
                padding: "8px 16px",
                border: "1px solid #d1d5db",
                borderRadius: 6,
                background: "#fff",
                cursor: "pointer",
                fontSize: 13,
              }}
            >
              取消
            </button>
          )}
        </div>

        {message && (
          <p
            style={{
              margin: "12px 0 0",
              fontSize: 13,
              color:
                messageType === "success"
                  ? "#16a34a"
                  : messageType === "error"
                  ? "#dc2626"
                  : "#374151",
            }}
          >
            {message}
          </p>
        )}
      </div>

      {/* 导入任务列表 */}
      <div
        style={{
          background: "#fff",
          borderRadius: 12,
          border: "1px solid #e5e7eb",
          padding: 20,
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            marginBottom: 16,
          }}
        >
          <h2 style={{ margin: 0, fontSize: 16 }}>最近导入任务</h2>
          <button
            onClick={loadJobs}
            disabled={loadingList}
            style={{
              padding: "6px 14px",
              border: "1px solid #d1d5db",
              borderRadius: 6,
              background: "#fff",
              cursor: "pointer",
              fontSize: 13,
            }}
          >
            {loadingList ? "刷新中..." : "刷新列表"}
          </button>
        </div>

        {jobs.length === 0 ? (
          <p style={{ fontSize: 13, color: "#9ca3af", margin: 0 }}>
            暂无导入记录
          </p>
        ) : (
          <table
            style={{
              width: "100%",
              borderCollapse: "collapse",
              fontSize: 13,
            }}
          >
            <thead>
              <tr
                style={{
                  borderBottom: "2px solid #e5e7eb",
                  textAlign: "left",
                }}
              >
                <th
                  style={{
                    padding: "8px 12px",
                    color: "#4b5563",
                    fontWeight: 600,
                    width: 60,
                  }}
                >
                  ID
                </th>
                <th
                  style={{ padding: "8px 12px", color: "#4b5563", fontWeight: 600 }}
                >
                  文件名
                </th>
                <th
                  style={{
                    padding: "8px 12px",
                    color: "#4b5563",
                    fontWeight: 600,
                    width: 100,
                  }}
                >
                  类型
                </th>
                <th
                  style={{
                    padding: "8px 12px",
                    color: "#4b5563",
                    fontWeight: 600,
                    width: 70,
                  }}
                >
                  状态
                </th>
                <th
                  style={{
                    padding: "8px 12px",
                    color: "#4b5563",
                    fontWeight: 600,
                    width: 50,
                    textAlign: "center",
                  }}
                >
                  成功
                </th>
                <th
                  style={{
                    padding: "8px 12px",
                    color: "#4b5563",
                    fontWeight: 600,
                    width: 50,
                    textAlign: "center",
                  }}
                >
                  失败
                </th>
                <th
                  style={{
                    padding: "8px 12px",
                    color: "#4b5563",
                    fontWeight: 600,
                    width: 50,
                    textAlign: "center",
                  }}
                >
                  待审
                </th>
                <th
                  style={{
                    padding: "8px 12px",
                    color: "#4b5563",
                    fontWeight: 600,
                    width: 140,
                  }}
                >
                  时间
                </th>
              </tr>
            </thead>
            <tbody>
              {jobs.map(function (job) {
                var typeLabel =
                  datasetTypeLabels[job.datasetType] || job.datasetType || "-";
                var statusLabel =
                  statusLabels[job.status] || job.status || "-";
                var statusColor = "#374151";
                if (job.status === "COMPLETED") statusColor = "#16a34a";
                else if (job.status === "FAILED") statusColor = "#dc2626";
                else if (job.status === "RUNNING") statusColor = "#2563eb";

                return (
                  <tr
                    key={job.id}
                    style={{ borderBottom: "1px solid #f3f4f6" }}
                  >
                    <td style={{ padding: "8px 12px", color: "#9ca3af" }}>
                      {job.id}
                    </td>
                    <td style={{ padding: "8px 12px", color: "#1f2937" }}>
                      {job.fileName || job.file_name || "-"}
                    </td>
                    <td style={{ padding: "8px 12px", color: "#4b5563" }}>
                      {typeLabel}
                    </td>
                    <td
                      style={{
                        padding: "8px 12px",
                        color: statusColor,
                        fontWeight: 500,
                      }}
                    >
                      {statusLabel}
                    </td>
                    <td
                      style={{
                        padding: "8px 12px",
                        color: "#16a34a",
                        textAlign: "center",
                      }}
                    >
                      {job.successCount ?? job.success_count ?? 0}
                    </td>
                    <td
                      style={{
                        padding: "8px 12px",
                        color:
                          (job.failureCount ?? job.failure_count ?? 0) > 0
                            ? "#dc2626"
                            : "#9ca3af",
                        textAlign: "center",
                      }}
                    >
                      {job.failureCount ?? job.failure_count ?? 0}
                    </td>
                    <td
                      style={{
                        padding: "8px 12px",
                        color:
                          (job.reviewCount ?? job.review_count ?? 0) > 0
                            ? "#d97706"
                            : "#9ca3af",
                        textAlign: "center",
                      }}
                    >
                      {job.reviewCount ?? job.review_count ?? 0}
                    </td>
                    <td style={{ padding: "8px 12px", color: "#6b7280" }}>
                      {job.createTime || job.create_time || "-"}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

// 主库数据类型的中文标签
var KNOWLEDGE_TYPE_LABELS = {
  disease: "疾病",
  hospital: "医院",
  department: "科室",
  doctor: "医生",
  capability: "能力",
};

// 每类数据单独的分页状态
var INITIAL_PAGE_STATE = { records: [], total: 0, page: 1, size: 20, pages: 0, loading: false };

// 热点管理面板内的子 Tab
var HOTSPOT_SUB_TABS = [
  { key: "main", label: "主库 (PostgreSQL)" },
  { key: "vector", label: "向量库 (MongoDB)" },
];

// 主库数据类型子 Tab
var MAIN_TYPE_TABS = [
  { key: "disease", label: "疾病" },
  { key: "hospital", label: "医院" },
  { key: "department", label: "科室" },
  { key: "doctor", label: "医生" },
  { key: "capability", label: "能力" },
];

function HotspotManagementPanel() {
  var [subTab, setSubTab] = useState("main");
  var [mainTypeTab, setMainTypeTab] = useState("disease");

  var [message, setMessage] = useState("");
  var [messageType, setMessageType] = useState("info");
  var [togglingKey, setTogglingKey] = useState(null);

  // 主库每类分页数据
  var [diseasePage, setDiseasePage] = useState(INITIAL_PAGE_STATE);
  var [hospitalPage, setHospitalPage] = useState(INITIAL_PAGE_STATE);
  var [deptPage, setDeptPage] = useState(INITIAL_PAGE_STATE);
  var [doctorPage, setDoctorPage] = useState(INITIAL_PAGE_STATE);
  var [capPage, setCapPage] = useState(INITIAL_PAGE_STATE);

  // 向量库
  var [documents, setDocuments] = useState([]);
  var [togglingDoc, setTogglingDoc] = useState(null);

  var getPageState = function (type) {
    switch (type) {
      case "disease": return diseasePage;
      case "hospital": return hospitalPage;
      case "department": return deptPage;
      case "doctor": return doctorPage;
      case "capability": return capPage;
      default: return INITIAL_PAGE_STATE;
    }
  };

  var getPageSetter = function (type) {
    switch (type) {
      case "disease": return setDiseasePage;
      case "hospital": return setHospitalPage;
      case "department": return setDeptPage;
      case "doctor": return setDoctorPage;
      case "capability": return setCapPage;
    }
  };

  var loadPageByType = useCallback(async function (type, page, setter) {
    setter(function (prev) {
      return Object.assign({}, prev, { loading: true });
    });
    try {
      var result = await listKnowledgeDataByType(type, page, 20, null);
      setter({
        records: result.records || [],
        total: result.total || 0,
        page: result.page || page,
        size: result.size || 20,
        pages: result.pages || 0,
        loading: false,
      });
    } catch (e) {
      setMessage("加载失败：" + e.message);
      setMessageType("error");
      setter(function (prev) {
        return Object.assign({}, prev, { loading: false });
      });
    }
  }, []);

  // 首次进入主库时加载所有类型第一页
  useEffect(function () {
    var types = ["disease", "hospital", "department", "doctor", "capability"];
    for (var i = 0; i < types.length; i++) {
      loadPageByType(types[i], 1, getPageSetter(types[i]));
    }
  }, [loadPageByType]);

  var loadDocuments = useCallback(async function () {
    try {
      var docs = await listDocuments();
      setDocuments(Array.isArray(docs) ? docs : []);
    } catch (e) {}
  }, []);

  useEffect(function () {
    loadDocuments();
  }, [loadDocuments]);

  var handleToggle = async function (type, id) {
    var key = type + ":" + id;
    setTogglingKey(key);
    try {
      var result = await toggleKnowledgeData(type, id);
      var setter = getPageSetter(type);
      setter(function (prev) {
        var newRecords = [];
        for (var i = 0; i < prev.records.length; i++) {
          var item = Object.assign({}, prev.records[i]);
          if (String(item.id) === String(id)) {
            item.active = result.active;
          }
          newRecords.push(item);
        }
        return Object.assign({}, prev, { records: newRecords });
      });
    } catch (e) {
      setMessage("操作失败：" + e.message);
      setMessageType("error");
    } finally {
      setTogglingKey(null);
    }
  };

  var handleDocToggle = async function (docName, currentActive) {
    setTogglingDoc(docName);
    try {
      if (currentActive) {
        await deactivateDocument(docName);
      } else {
        await activateDocument(docName);
      }
      loadDocuments();
    } catch (e) {
      setMessage("操作失败：" + e.message);
      setMessageType("error");
    } finally {
      setTogglingDoc(null);
    }
  };

  var renderPagination = function (type, pageState) {
    if (pageState.pages <= 1) return null;
    return (
      <div style={{ display: "flex", gap: 8, alignItems: "center", justifyContent: "center", marginTop: 8 }}>
        <button
          onClick={function () {
            if (pageState.page > 1) {
              loadPageByType(type, pageState.page - 1, getPageSetter(type));
            }
          }}
          disabled={pageState.page <= 1 || pageState.loading}
          style={{ padding: "3px 10px", border: "1px solid #d1d5db", borderRadius: 4, background: "#fff", cursor: "pointer", fontSize: 12 }}
        >
          上一页
        </button>
        <span style={{ fontSize: 12, color: "#6b7280" }}>{pageState.page} / {pageState.pages}</span>
        <button
          onClick={function () {
            if (pageState.page < pageState.pages) {
              loadPageByType(type, pageState.page + 1, getPageSetter(type));
            }
          }}
          disabled={pageState.page >= pageState.pages || pageState.loading}
          style={{ padding: "3px 10px", border: "1px solid #d1d5db", borderRadius: 4, background: "#fff", cursor: "pointer", fontSize: 12 }}
        >
          下一页
        </button>
      </div>
    );
  };

  // 生成子 Tab 按钮样式
  var makeSubTabStyle = function (key) {
    var isActive = subTab === key;
    return {
      padding: "8px 18px",
      cursor: "pointer",
      fontSize: 13,
      fontWeight: isActive ? 600 : 400,
      color: isActive ? "#2563eb" : "#6b7280",
      borderBottom: isActive ? "2px solid #2563eb" : "2px solid transparent",
      background: "transparent",
      borderTop: "none",
      borderLeft: "none",
      borderRight: "none",
    };
  };

  // 主库类型子 Tab 样式
  var makeMainTypeTabStyle = function (key) {
    var isActive = mainTypeTab === key;
    return {
      padding: "6px 14px",
      cursor: "pointer",
      fontSize: 13,
      fontWeight: isActive ? 600 : 400,
      color: isActive ? "#2563eb" : "#6b7280",
      borderBottom: isActive ? "2px solid #2563eb" : "2px solid transparent",
      background: "transparent",
      borderTop: "none",
      borderLeft: "none",
      borderRight: "none",
    };
  };

  // 渲染主库某类型的数据表格
  var renderMainTypeTable = function (type) {
    var pageState = getPageState(type);
    var records = pageState.records;

    return (
      <div>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 8 }}>
          <span style={{ fontSize: 13, color: "#6b7280" }}>
            共 {pageState.total} 条
          </span>
        </div>

        {pageState.loading ? (
          <p style={{ fontSize: 13, color: "#9ca3af" }}>加载中...</p>
        ) : records.length === 0 ? (
          <p style={{ fontSize: 13, color: "#9ca3af" }}>暂无数据</p>
        ) : (
          <>
            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: "1px solid #e5e7eb", textAlign: "left" }}>
                  <th style={{ padding: "6px 8px", color: "#6b7280", fontWeight: 500, width: 50 }}>#</th>
                  <th style={{ padding: "6px 8px", color: "#6b7280", fontWeight: 500 }}>名称</th>
                  <th style={{ padding: "6px 8px", color: "#6b7280", fontWeight: 500, width: 100, textAlign: "center" }}>状态</th>
                  <th style={{ padding: "6px 8px", color: "#6b7280", fontWeight: 500, width: 80, textAlign: "center" }}>操作</th>
                </tr>
              </thead>
              <tbody>
                {records.map(function (item, idx) {
                  var isActive = item.active !== false;
                  var key = type + ":" + item.id;
                  var isToggling = togglingKey === key;
                  return (
                    <tr key={key} style={{ borderBottom: "1px solid #f9fafb" }}>
                      <td style={{ padding: "6px 8px", color: "#d1d5db" }}>{(pageState.page - 1) * pageState.size + idx + 1}</td>
                      <td style={{ padding: "6px 8px", color: "#1f2937" }}>{item.name || item.id || "-"}</td>
                      <td style={{ padding: "6px 8px", textAlign: "center" }}>
                        <span style={{
                          fontSize: 11, padding: "1px 6px", borderRadius: 8,
                          background: isActive ? "#dcfce7" : "#fee2e2",
                          color: isActive ? "#16a34a" : "#dc2626",
                        }}>
                          {isActive ? "热点" : "非热点"}
                        </span>
                      </td>
                      <td style={{ padding: "6px 8px", textAlign: "center" }}>
                        <button
                          onClick={function () { handleToggle(type, item.id); }}
                          disabled={isToggling}
                          style={{
                            padding: "3px 10px",
                            border: "1px solid " + (isActive ? "#fca5a5" : "#86efac"),
                            borderRadius: 4, background: "#fff", cursor: "pointer",
                            fontSize: 12, color: isActive ? "#dc2626" : "#16a34a",
                          }}
                        >
                          {isToggling ? "..." : isActive ? "取消热点" : "设为热点"}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            {renderPagination(type, pageState)}
          </>
        )}
      </div>
    );
  };

  return (
    <div>
      {message && (
        <div style={{
          background: messageType === "success" ? "#dcfce7" : "#fee2e2",
          color: messageType === "success" ? "#16a34a" : "#dc2626",
          padding: "8px 16px", borderRadius: 6, marginBottom: 16, fontSize: 13,
        }}>
          {message}
        </div>
      )}

      {/* 子 Tab：主库 / 向量库 */}
      <div style={{ display: "flex", gap: 0, marginBottom: 20, borderBottom: "2px solid #e5e7eb" }}>
        {HOTSPOT_SUB_TABS.map(function (tab) {
          return (
            <button key={tab.key} style={makeSubTabStyle(tab.key)} onClick={function () { setSubTab(tab.key); }}>
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* ===== 主库 ===== */}
      {subTab === "main" && (
        <div style={{ background: "#fff", borderRadius: 12, border: "1px solid #e5e7eb", padding: 20 }}>
          <h2 style={{ margin: "0 0 4px", fontSize: 16, color: "#1e3a5f" }}>主库（PostgreSQL）</h2>
          <p style={{ fontSize: 12, color: "#9ca3af", margin: "0 0 12px" }}>
            控制哪些数据参与缓存：切换后需刷新热点缓存才能生效
          </p>

          {/* 主库类型子 Tab */}
          <div style={{ display: "flex", gap: 0, marginBottom: 16, borderBottom: "1px solid #e5e7eb" }}>
            {MAIN_TYPE_TABS.map(function (tab) {
              var pageState = getPageState(tab.key);
              return (
                <button
                  key={tab.key}
                  style={makeMainTypeTabStyle(tab.key)}
                  onClick={function () { setMainTypeTab(tab.key); }}
                >
                  {tab.label}
                  <span style={{ fontSize: 11, color: "#9ca3af", marginLeft: 4 }}>
                    ({pageState.total})
                  </span>
                </button>
              );
            })}
          </div>

          {renderMainTypeTable(mainTypeTab)}
        </div>
      )}

      {/* ===== 向量库 ===== */}
      {subTab === "vector" && (
        <div style={{ background: "#fff", borderRadius: 12, border: "1px solid #e5e7eb", padding: 20 }}>
          <h2 style={{ margin: "0 0 4px", fontSize: 16, color: "#1e3a5f" }}>向量库（MongoDB）</h2>
          <p style={{ fontSize: 12, color: "#9ca3af", margin: "0 0 12px" }}>
            控制文档是否参与向量检索：开关即时生效
          </p>
          <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: 8 }}>
            <button onClick={loadDocuments} style={{ padding: "6px 14px", border: "1px solid #d1d5db", borderRadius: 6, background: "#fff", cursor: "pointer", fontSize: 13 }}>
              刷新
            </button>
          </div>
          {documents.length === 0 ? (
            <p style={{ fontSize: 13, color: "#9ca3af" }}>暂无文档</p>
          ) : (
            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: "1px solid #e5e7eb", textAlign: "left" }}>
                  <th style={{ padding: "6px 8px", color: "#6b7280", fontWeight: 500 }}>文件名</th>
                  <th style={{ padding: "6px 8px", color: "#6b7280", fontWeight: 500, width: 100, textAlign: "center" }}>状态</th>
                  <th style={{ padding: "6px 8px", color: "#6b7280", fontWeight: 500, width: 80, textAlign: "center" }}>操作</th>
                </tr>
              </thead>
              <tbody>
                {documents.map(function (doc) {
                  var docName = doc.documentName || doc.document_name || "-";
                  var isActive = doc.active !== false;
                  return (
                    <tr key={docName} style={{ borderBottom: "1px solid #f9fafb" }}>
                      <td style={{ padding: "6px 8px", color: "#1f2937" }}>{docName}</td>
                      <td style={{ padding: "6px 8px", textAlign: "center" }}>
                        <span style={{ fontSize: 11, padding: "1px 6px", borderRadius: 8, background: isActive ? "#dcfce7" : "#fee2e2", color: isActive ? "#16a34a" : "#dc2626" }}>
                          {isActive ? "热点" : "非热点"}
                        </span>
                      </td>
                      <td style={{ padding: "6px 8px", textAlign: "center" }}>
                        <button
                          onClick={function () { handleDocToggle(docName, isActive); }}
                          disabled={togglingDoc === docName}
                          style={{ padding: "3px 10px", border: "1px solid " + (isActive ? "#fca5a5" : "#86efac"), borderRadius: 4, background: "#fff", cursor: "pointer", fontSize: 12, color: isActive ? "#dc2626" : "#16a34a" }}
                        >
                          {togglingDoc === docName ? "..." : isActive ? "取消热点" : "设为热点"}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}

export default function AdminPage() {
  const navigate = useNavigate();
  const user = getUser();
  const [activeTab, setActiveTab] = useState("cache");

  const handleLogout = () => {
    logout();
  };

  const tabStyle = {
    cache: {
      padding: "10px 20px",
      cursor: "pointer",
      fontSize: 14,
      fontWeight: activeTab === "cache" ? 600 : 400,
      color: activeTab === "cache" ? "#2563eb" : "#6b7280",
      borderBottom:
        activeTab === "cache" ? "2px solid #2563eb" : "2px solid transparent",
      background: "transparent",
      borderTop: "none",
      borderLeft: "none",
      borderRight: "none",
    },
    import: {
      padding: "10px 20px",
      cursor: "pointer",
      fontSize: 14,
      fontWeight: activeTab === "import" ? 600 : 400,
      color: activeTab === "import" ? "#2563eb" : "#6b7280",
      borderBottom:
        activeTab === "import" ? "2px solid #2563eb" : "2px solid transparent",
      background: "transparent",
      borderTop: "none",
      borderLeft: "none",
      borderRight: "none",
    },
    docs: {
      padding: "10px 20px",
      cursor: "pointer",
      fontSize: 14,
      fontWeight: activeTab === "docs" ? 600 : 400,
      color: activeTab === "docs" ? "#2563eb" : "#6b7280",
      borderBottom:
        activeTab === "docs" ? "2px solid #2563eb" : "2px solid transparent",
      background: "transparent",
      borderTop: "none",
      borderLeft: "none",
      borderRight: "none",
    },
    hotspot: {
      padding: "10px 20px",
      cursor: "pointer",
      fontSize: 14,
      fontWeight: activeTab === "hotspot" ? 600 : 400,
      color: activeTab === "hotspot" ? "#2563eb" : "#6b7280",
      borderBottom:
        activeTab === "hotspot" ? "2px solid #2563eb" : "2px solid transparent",
      background: "transparent",
      borderTop: "none",
      borderLeft: "none",
      borderRight: "none",
    },
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
          <span style={{ fontSize: 14, color: "#4b5563" }}>
            {user?.username || "管理员"}
          </span>
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
        {/* Tab 导航 */}
        <div style={{ display: "flex", gap: 0, marginBottom: 24, borderBottom: "2px solid #e5e7eb" }}>
          <button style={tabStyle.cache} onClick={() => setActiveTab("cache")}>
            缓存管理
          </button>
          <button style={tabStyle.import} onClick={() => setActiveTab("import")}>
            数据导入
          </button>
          <button style={tabStyle.docs} onClick={() => setActiveTab("docs")}>
            文档管理
          </button>
          <button style={tabStyle.hotspot} onClick={() => setActiveTab("hotspot")}>
            热点管理
          </button>
        </div>

        {/* 面板切换 */}
        {activeTab === "cache" && <CacheManagementPanel />}
        {activeTab === "import" && <DataImportPanel />}
        {activeTab === "docs" && <DocumentManagementPanel />}
        {activeTab === "hotspot" && <HotspotManagementPanel />}
      </main>
    </div>
  );
}
