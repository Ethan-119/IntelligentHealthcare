package com.intelligenthealthcare.importjob.infrastructure.file;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import com.intelligenthealthcare.importjob.domain.model.ImportTableRow;
import org.springframework.web.multipart.MultipartFile;

/**
 * 从 Excel/CSV 第一行表头、后续为数据，解析为领域行。列名统一为小写，便于与模板对照。
 */
public final class ImportFileTableReader {

    private ImportFileTableReader() {}

    public static List<ImportTableRow> read(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }
        String name = file.getOriginalFilename();
        if (name == null) {
            throw new IllegalArgumentException("文件名为空");
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv")) {
            return readCsv(file);
        }
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return readExcel(file);
        }
        throw new IllegalArgumentException("仅支持 .csv、.xlsx、.xls");
    }

    private static List<ImportTableRow> readCsv(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream();
                InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
                CSVReader reader = new CSVReaderBuilder(isr).build()) {
            List<String[]> all;
            try {
                all = reader.readAll();
            } catch (CsvException e) {
                throw new IOException("CSV 解析失败", e);
            }
            if (all.isEmpty()) {
                return new ArrayList<>();
            }
            String[] headerRaw = all.get(0);
            if (headerRaw.length == 0) {
                throw new IllegalArgumentException("CSV 表头为空");
            }
            int colCount = headerRaw.length;
            String[] headerNorm = new String[colCount];
            for (int c = 0; c < colCount; c++) {
                headerNorm[c] = normalizeKey(headerRaw[c]);
            }
            List<ImportTableRow> out = new ArrayList<>();
            for (int i = 1; i < all.size(); i++) {
                int fileLine = i + 1;
                String[] line = all.get(i);
                if (isBlankRow(line)) {
                    continue;
                }
                Map<String, String> map = new HashMap<>();
                for (int c = 0; c < colCount; c++) {
                    String val = c < line.length ? line[c] : "";
                    if (val != null) {
                        val = val.trim();
                    } else {
                        val = "";
                    }
                    if (!headerNorm[c].isEmpty()) {
                        map.put(headerNorm[c], val);
                    }
                }
                if (mapIsEmptyOrWhitespace(map)) {
                    continue;
                }
                out.add(new ImportTableRow(fileLine, map));
            }
            return out;
        }
    }

    private static List<ImportTableRow> readExcel(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream();
                Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            int first = sheet.getFirstRowNum();
            int last = sheet.getLastRowNum();
            if (last < first) {
                return new ArrayList<>();
            }
            Row headerRow = sheet.getRow(first);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel 表头为空");
            }
            int colCount = headerRow.getLastCellNum();
            if (colCount <= 0) {
                throw new IllegalArgumentException("Excel 表头列为空");
            }
            String[] headerNorm = new String[colCount];
            DataFormatter fmt = new DataFormatter();
            for (int c = 0; c < colCount; c++) {
                Cell cell = headerRow.getCell(c);
                String raw = cell == null ? "" : fmt.formatCellValue(cell);
                headerNorm[c] = normalizeKey(raw);
            }
            List<ImportTableRow> out = new ArrayList<>();
            for (int r = first + 1; r <= last; r++) {
                int fileLine = r + 1;
                Row dataRow = sheet.getRow(r);
                if (dataRow == null) {
                    continue;
                }
                Map<String, String> map = new HashMap<>();
                boolean any = false;
                for (int c = 0; c < colCount; c++) {
                    Cell cell = dataRow.getCell(c);
                    String val = cell == null ? "" : fmt.formatCellValue(cell);
                    if (val == null) {
                        val = "";
                    } else {
                        val = val.trim();
                    }
                    if (val != null && !val.isEmpty()) {
                        any = true;
                    }
                    if (c < headerNorm.length && !headerNorm[c].isEmpty()) {
                        map.put(headerNorm[c], val);
                    }
                }
                if (!any) {
                    continue;
                }
                if (mapIsEmptyOrWhitespace(map)) {
                    continue;
                }
                out.add(new ImportTableRow(fileLine, map));
            }
            return out;
        }
    }

    private static boolean isBlankRow(String[] line) {
        if (line == null || line.length == 0) {
            return true;
        }
        for (int i = 0; i < line.length; i++) {
            if (line[i] != null && !line[i].trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean mapIsEmptyOrWhitespace(Map<String, String> map) {
        for (String v : map.values()) {
            if (v != null && !v.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeKey(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase(Locale.ROOT);
    }
}
