package com.example.docserver.controller;

import com.example.docserver.dto.FillTemplateFromUrlRequest;
import com.example.docserver.dto.WordToPdfFromUrlRequest;
import com.example.docserver.service.DocumentResult;
import com.example.docserver.service.DocumentService;
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
import java.io.IOException;
import org.jodconverter.core.office.OfficeException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文档处理", description = "Word 模板填充与 PDF 转换")
@Validated
@RestController
@RequestMapping("/api/docs")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(
        summary = "模板填充（本地上传）",
        description = "上传 DOCX 模板和 variables JSON，替换模板中的占位符。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "填充成功。convertToPdf 为 false 时返回 DOCX，为 true 时返回 PDF。",
            content = {
                @Content(mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                @Content(mediaType = "application/pdf")
            }
        ),
        @ApiResponse(responseCode = "400", description = "请求参数无效或 variables JSON 格式错误"),
        @ApiResponse(responseCode = "502", description = "PDF 转换失败")
    })
    @PostMapping(
        value = "/fillTemplate",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            MediaType.APPLICATION_PDF_VALUE
        }
    )
    public ResponseEntity<byte[]> fillTemplate(
        @Parameter(description = "Word 模板文件（.docx）", required = true, schema = @Schema(type = "string", format = "binary"))
        @RequestPart("template") @NotNull MultipartFile template,
        @Parameter(description = "占位符变量 JSON 对象", required = true, example = "{\"customerName\":\"示例客户\",\"amount\":\"1000\"}")
        @RequestPart("variables") @NotBlank String variablesJson,
        @Parameter(description = "为 true 时返回 PDF，否则返回填充后的 Word 文件", example = "false")
        @RequestParam(value = "convertToPdf", defaultValue = "false") boolean convertToPdf
    ) throws IOException, OfficeException {
        return toResponse(documentService.fillTemplate(template, variablesJson, convertToPdf));
    }

    @Operation(
        summary = "模板填充（HTTP 直链）",
        description = "通过 HTTP 或 HTTPS 下载 DOCX 模板，并使用 variables 替换模板中的占位符。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "模板填充成功"),
        @ApiResponse(responseCode = "400", description = "模板地址或请求参数无效"),
        @ApiResponse(responseCode = "502", description = "模板下载或 PDF 转换失败")
    })
    @PostMapping(
        value = "/fillTemplateFromUrl",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            MediaType.APPLICATION_PDF_VALUE
        }
    )
    public ResponseEntity<byte[]> fillTemplateFromUrl(@Valid @RequestBody FillTemplateFromUrlRequest body)
        throws IOException, OfficeException {
        return toResponse(documentService.fillTemplateFromUrl(body));
    }

    @Operation(
        summary = "Word 转 PDF",
        description = "上传 Word 文件，通过 LibreOffice 转换为 PDF 并返回。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "转换成功", content = @Content(mediaType = "application/pdf")),
        @ApiResponse(responseCode = "400", description = "文件格式不支持或转换失败")
    })
    @PostMapping(
        value = "/wordToPdf",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> wordToPdf(
        @Parameter(description = "待转换的 Word 文件", required = true, schema = @Schema(type = "string", format = "binary"))
        @RequestPart("file") @NotNull MultipartFile file
    ) throws IOException, OfficeException {
        return toResponse(documentService.wordToPdf(file));
    }

    @Operation(
        summary = "Word 模板填充并转 PDF（HTTP 直链）",
        description = "通过 HTTP 或 HTTPS 下载 Word 文件；传入 variables 时先填充 DOCX 模板，再转换为 PDF。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "转换成功", content = @Content(mediaType = "application/pdf")),
        @ApiResponse(responseCode = "400", description = "Word 地址、模板或转换参数无效"),
        @ApiResponse(responseCode = "502", description = "Word 文件下载失败")
    })
    @PostMapping(
        value = "/wordToPdfFromUrl",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> wordToPdfFromUrl(@Valid @RequestBody WordToPdfFromUrlRequest body)
        throws IOException, OfficeException {
        return toResponse(documentService.wordToPdfFromUrl(body));
    }

    private static ResponseEntity<byte[]> toResponse(DocumentResult result) {
        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(result.filename()).build().toString()
            )
            .contentType(result.mediaType())
            .body(result.content());
    }
}
