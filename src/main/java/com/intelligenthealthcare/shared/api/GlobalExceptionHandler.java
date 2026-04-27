package com.intelligenthealthcare.shared.api;

import com.intelligenthealthcare.importjob.domain.exception.ImportJobNotFoundException;
import com.intelligenthealthcare.importjob.domain.exception.ImportReviewItemNotFoundException;
import com.intelligenthealthcare.patient.domain.exception.PatientNotFoundException;
import com.intelligenthealthcare.patient.domain.exception.PatientPhoneAlreadyUsedException;
import com.intelligenthealthcare.patient.domain.exception.PatientPhoneRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 将校验失败与带 HTTP 状态的业务异常统一为 JSON 体，便于前后端联调；安全框架自身的 401 仍由 {@link com.intelligenthealthcare.shared.config.SecurityConfig} 写出。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
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
}
