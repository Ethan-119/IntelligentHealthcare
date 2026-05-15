package com.intelligenthealthcare.importjob.domain.model;

import java.util.Map;
import lombok.Value;

/**
 * 从 Excel/CSV 解析出的一行：文件行号 + 小写列名到单元格文本。
 */
@Value
public class ImportTableRow {

    int fileLineNumber;
    Map<String, String> values;

    /**
     * 用于失败日志、待审核项中的原文快照。
     */
    public String toRawText() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : values.entrySet()) {
            if (!first) {
                sb.append(" | ");
            }
            first = false;
            sb.append(e.getKey());
            sb.append("=");
            if (e.getValue() != null) {
                sb.append(e.getValue());
            }
        }
        return sb.toString();
    }
}
