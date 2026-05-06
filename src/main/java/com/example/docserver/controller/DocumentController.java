package com.example.docserver.controller;

import com.example.docserver.service.DocTemplateService;
import com.example.docserver.service.WordConvertService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Map;
import org.jodconverter.core.office.OfficeException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/docs")
public class DocumentController {

    private final DocTemplateService docTemplateService;
    private final WordConvertService wordConvertService;
    private final ObjectMapper objectMapper;

    public DocumentController(
        DocTemplateService docTemplateService,
        WordConvertService wordConvertService,
        ObjectMapper objectMapper
    ) {
        this.docTemplateService = docTemplateService;
        this.wordConvertService = wordConvertService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(
        value = "/fill-template",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )
    public ResponseEntity<byte[]> fillTemplate(
        @RequestPart("template") @NotNull MultipartFile template,
        @RequestPart("variables") @NotBlank String variablesJson
    ) throws IOException {
        Map<String, String> variables = objectMapper.readValue(variablesJson, new TypeReference<>() {});
        byte[] output = docTemplateService.fillTemplate(template.getInputStream(), variables);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("filled-template.docx").build().toString())
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .body(output);
    }

    @PostMapping(
        value = "/word-to-pdf",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> wordToPdf(
        @RequestPart("file") @NotNull MultipartFile file
    ) throws IOException, OfficeException {
        String suffix = resolveSuffix(file.getOriginalFilename());
        byte[] output = wordConvertService.convertToPdf(file.getInputStream(), suffix);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("converted.pdf").build().toString())
            .contentType(MediaType.APPLICATION_PDF)
            .body(output);
    }

    private String resolveSuffix(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".docx";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
