package com.example.docserver.controller;

import java.util.Map;
import org.jodconverter.core.office.OfficeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OfficeException.class)
    public ResponseEntity<Map<String, Object>> handleOfficeException(OfficeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "code", "DOC_CONVERT_FAILED",
                "message", "Word 转 PDF 失败，请检查文档格式或 LibreOffice 环境。",
                "detail", ex.getMessage()
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "code", "INTERNAL_ERROR",
                "message", "服务处理失败。",
                "detail", ex.getMessage()
            ));
    }
}
