package com.intelligenthealthcare.rag.infrastructure.parsing;

import com.opencsv.CSVReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.opencsv.exceptions.CsvValidationException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 从上传文件中提取纯文本，支持 PDF / DOC / DOCX / PPT / PPTX / CSV / TXT。
 */
@Component
public class DocumentTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(DocumentTextExtractor.class);

    /**
     * 根据文件名后缀提取文本。
     *
     * @param filename    原始文件名，用于判断格式
     * @param inputStream 文件输入流
     * @return 提取到的文本
     */
    public String extract(String filename, InputStream inputStream) {
        String lowerName = filename.toLowerCase(Locale.ROOT);
        try {
            if (lowerName.endsWith(".pdf")) {
                return extractPdf(inputStream);
            }
            if (lowerName.endsWith(".docx")) {
                return extractDocx(inputStream);
            }
            if (lowerName.endsWith(".doc")) {
                return extractDoc(inputStream);
            }
            if (lowerName.endsWith(".pptx")) {
                return extractPptx(inputStream);
            }
            if (lowerName.endsWith(".ppt")) {
                return extractPpt(inputStream);
            }
            if (lowerName.endsWith(".csv")) {
                return extractCsv(inputStream);
            }
            if (lowerName.endsWith(".txt")) {
                return extractTxt(inputStream);
            }
            throw new IllegalArgumentException("不支持的文件格式: " + filename);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            log.error("文件解析失败: {}", filename, e);
            throw new RuntimeException("文件解析失败: " + filename + " - " + e.getMessage(), e);
        }
    }

    private String extractPdf(InputStream in) throws IOException {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String extractDocx(InputStream in) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(in)) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
            return extractor.getText();
        }
    }

    private String extractDoc(InputStream in) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(in)) {
            WordExtractor extractor = new WordExtractor(doc);
            return extractor.getText();
        }
    }

    private String extractPptx(InputStream in) throws IOException {
        try (XMLSlideShow ppt = new XMLSlideShow(in)) {
            StringBuilder sb = new StringBuilder();
            java.util.List<XSLFSlide> slides = ppt.getSlides();
            for (int i = 0; i < slides.size(); i++) {
                XSLFSlide slide = slides.get(i);
                java.util.List<XSLFShape> shapes = slide.getShapes();
                for (int j = 0; j < shapes.size(); j++) {
                    XSLFShape shape = shapes.get(j);
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        String text = textShape.getText();
                        if (text != null && !text.isBlank()) {
                            sb.append(text).append("\n");
                        }
                    }
                }
            }
            return sb.toString();
        }
    }

    private String extractPpt(InputStream in) throws IOException {
        try (HSLFSlideShow ppt = new HSLFSlideShow(in)) {
            StringBuilder sb = new StringBuilder();
            java.util.List<HSLFSlide> slides = ppt.getSlides();
            for (int i = 0; i < slides.size(); i++) {
                HSLFSlide slide = slides.get(i);
                java.util.List<HSLFShape> shapes = slide.getShapes();
                for (int j = 0; j < shapes.size(); j++) {
                    if (shapes.get(j) instanceof HSLFTextShape) {
                        HSLFTextShape shape = (HSLFTextShape) shapes.get(j);
                        String text = shape.getText();
                        if (text != null && !text.isBlank()) {
                            sb.append(text).append("\n");
                        }
                    }
                }
            }
            return sb.toString();
        }
    }

    private String extractCsv(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        try (CSVReader reader = new CSVReader(new StringReader(content))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                for (int i = 0; i < line.length; i++) {
                    String cell = line[i];
                    if (cell != null && !cell.isBlank()) {
                        sb.append(cell.trim());
                        if (i < line.length - 1) {
                            sb.append(" ");
                        }
                    }
                }
                sb.append("\n");
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    private String extractTxt(InputStream in) throws IOException {
        byte[] bytes = in.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
