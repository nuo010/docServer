package com.example.docserver.controller;

import java.util.Map;
import org.jodconverter.core.office.OfficeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus resolved = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex.getReason() != null ? ex.getReason() : "请求处理失败";
        log.warn("Request failed with status={}, message={}", resolved.value(), message, ex);
        return ResponseEntity.status(resolved)
            .body(Map.of(
                "code", "HTTP_" + resolved.value(),
                "message", message
            ));
    }

    @ExceptionHandler(OfficeException.class)
    public ResponseEntity<Map<String, Object>> handleOfficeException(OfficeException ex) {
        log.error("Word conversion failed", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "code", "DOC_CONVERT_FAILED",
                "message", "Word 转 PDF 失败，请检查文档格式或 LibreOffice 环境。",
                "detail", ex.getMessage()
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unhandled exception caught by global handler", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "code", "INTERNAL_ERROR",
                "message", "服务处理失败。",
                "detail", ex.getMessage()
            ));
    }
}
