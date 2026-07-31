package com.example.docserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

@Schema(description = "通过 Word 文件直链填充并转换为 PDF")
public record WordToPdfFromUrlRequest(
    @Schema(
        description = "Word 文件的 HTTP 或 HTTPS 地址；传入 variables 时必须是 .docx 模板。",
        example = "https://example.com/files/sample.docx",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Word 文件链接不能为空")
    @Pattern(regexp = "(?i)^https?://\\S+", message = "仅支持以 http:// 或 https:// 开头的链接")
    String templateUrl,

    @Schema(
        description = "占位符变量映射。key 对应 Word 模板中的 {{key}}；不传或为空时仅执行 Word 转 PDF。",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "{\"plateNum\":\"粤A12345\",\"ownerName\":\"张三\"}"
    )
    Map<String, Object> variables
) {}
