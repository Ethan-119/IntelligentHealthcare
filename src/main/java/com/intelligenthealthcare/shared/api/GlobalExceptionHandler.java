package com.intelligenthealthcare.shared.api;

import com.intelligenthealthcare.importjob.domain.exception.ImportJobNotFoundException;
import com.intelligenthealthcare.importjob.domain.exception.ImportReviewItemNotFoundException;
import com.intelligenthealthcare.patient.domain.exception.PatientNotFoundException;
import com.intelligenthealthcare.patient.domain.exception.PatientPhoneAlreadyUsedException;
import com.intelligenthealthcare.patient.domain.exception.PatientPhoneRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 将校验失败与带 HTTP 状态的业务异常统一为 JSON 体，便于前后端联调；安全框架自身的 401 仍由 {@link com.intelligenthealthcare.shared.config.SecurityConfig} 写出。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        StringBuilder sb = new StringBuilder();
        var fieldErrors = ex.getBindingResult().getFieldErrors();
        for (int i = 0; i < fieldErrors.size(); i++) {
            FieldError fieldError = fieldErrors.get(i);
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(fieldError.getField());
            sb.append(": ");
            sb.append(fieldError.getDefaultMessage());
        }
        String message = sb.toString();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode().value())
                .body(Map.of("message", ex.getReason() != null ? ex.getReason() : "请求无法完成"));
    }

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePatientNotFound(PatientNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(PatientPhoneAlreadyUsedException.class)
    public ResponseEntity<Map<String, String>> handlePatientPhoneConflict(PatientPhoneAlreadyUsedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(PatientPhoneRequiredException.class)
    public ResponseEntity<Map<String, String>> handlePatientPhoneRequired(PatientPhoneRequiredException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ImportJobNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleImportJobNotFound(ImportJobNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ImportReviewItemNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleImportReviewNotFound(ImportReviewItemNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "请求参数不合法"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("message", "上传文件大小超出限制（最大 50MB）"));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, String>> handleMultipart(MultipartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "文件上传失败，请检查文件格式和大小"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = "请求体格式错误，请检查参数类型";
        Throwable cause = ex.getCause();
        if (cause != null && cause.getMessage() != null) {
            // 提取 Jackson 枚举反序列化失败的友好提示
            String detail = cause.getMessage();
            if (detail.contains("not one of the values accepted")) {
                message = "请求参数值不合法: " + detail.substring(0, Math.min(detail.length(), 200));
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    // 兜底：所有未预期异常返回 JSON 500，避免穿透到 Tomcat 输出 HTML 错误页并泄露堆栈。
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleFallback(Exception ex, HttpServletRequest request) {
        String uri = request == null ? "unknown" : request.getRequestURI();
        log.error("Unhandled exception on uri={}", uri, ex);
        if (isSseRequest(request)) {
            // SSE 请求不能回写 Map(JSON) 到 text/event-stream，否则会触发 HttpMessageNotWritableException。
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body("event: error\ndata: 服务器内部错误，请稍后重试\n\n");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "服务器内部错误，请稍后重试"));
    }

    private boolean isSseRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        if (uri != null && uri.contains("/api/ai/analyze/stream")) {
            return true;
        }
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
