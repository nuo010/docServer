package com.example.docserver.controller;

import com.example.docserver.dto.FillTemplateFromUrlRequest;
import com.example.docserver.service.DocTemplateService;
import com.example.docserver.service.HttpTemplateLoader;
import com.example.docserver.service.WordConvertService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import org.jodconverter.core.office.OfficeException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文档处理", description = "Word 模板填充、Word 转 PDF")
@Validated
@RestController
@RequestMapping("/api/docs")
public class DocumentController {

    private static final MediaType DOCX_MEDIA = MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final DocTemplateService docTemplateService;
    private final WordConvertService wordConvertService;
    private final ObjectMapper objectMapper;
    private final HttpTemplateLoader httpTemplateLoader;

    public DocumentController(
        DocTemplateService docTemplateService,
        WordConvertService wordConvertService,
        ObjectMapper objectMapper,
        HttpTemplateLoader httpTemplateLoader
    ) {
        this.docTemplateService = docTemplateService;
        this.wordConvertService = wordConvertService;
        this.objectMapper = objectMapper;
        this.httpTemplateLoader = httpTemplateLoader;
    }

    @Operation(
        summary = "模板填充（本地上传）",
        description = "上传 .docx 模板与 variables JSON。模板内占位符推荐写法为 {{键名}}（与 JSON 字段名一致）；仍兼容旧版 ${键名}。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "填充成功，响应体为 Word 二进制流",
            content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        ),
        @ApiResponse(responseCode = "400", description = "参数校验失败或 JSON 无法解析")
    })
    @PostMapping(
        value = "/fill-template",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )
    public ResponseEntity<byte[]> fillTemplate(
        @Parameter(description = "Word 模板文件（.docx）", required = true, schema = @Schema(type = "string", format = "binary"))
        @RequestPart("template") @NotNull MultipartFile template,
        @Parameter(
            description = "占位符 JSON 对象，如 {\"plateNum\":\"云A12345\"}，对应模板中的 {{plateNum}}（亦兼容 ${plateNum}）",
            required = true,
            example = "{\"customerName\":\"示例客户\",\"amount\":\"1000\"}"
        )
        @RequestPart("variables") @NotBlank String variablesJson
    ) throws IOException {
        Map<String, String> variables = objectMapper.readValue(variablesJson, new TypeReference<>() {});
        byte[] output = docTemplateService.fillTemplate(template.getInputStream(), variables);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("filled-template.docx").build().toString())
            .contentType(DOCX_MEDIA)
            .body(output);
    }

    @Operation(
        summary = "模板填充（模板 http 直链）",
        description = "通过 http(s) 下载 .docx 模板，使用请求体中的 variables 映射替换 {{键}}（并兼容 ${键}），返回填充后的 Word 文件。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "填充成功，响应体为 Word 二进制流",
            content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        ),
        @ApiResponse(responseCode = "400", description = "链接非法、模板过大或校验失败"),
        @ApiResponse(responseCode = "502", description = "模板链接不可达或返回非 2xx")
    })
    @PostMapping(
        value = "/fill-template-from-url",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )
    public ResponseEntity<byte[]> fillTemplateFromUrl(@Valid @RequestBody FillTemplateFromUrlRequest body) throws IOException {
        byte[] templateBytes = httpTemplateLoader.fetchAsBytes(body.templateUrl());
        byte[] output;
        try (ByteArrayInputStream in = new ByteArrayInputStream(templateBytes)) {
            output = docTemplateService.fillTemplate(in, body.variables());
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("filled-template.docx").build().toString())
            .contentType(DOCX_MEDIA)
            .body(output);
    }

    @Operation(
        summary = "Word 转 PDF",
        description = "上传 .doc 或 .docx，服务端通过 LibreOffice 转为 PDF 并返回。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "转换成功", content = @Content(mediaType = "application/pdf")),
        @ApiResponse(responseCode = "400", description = "文档格式不支持或转换失败")
    })
    @PostMapping(
        value = "/word-to-pdf",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> wordToPdf(
        @Parameter(description = "待转换的 Word 文件", required = true, schema = @Schema(type = "string", format = "binary"))
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
