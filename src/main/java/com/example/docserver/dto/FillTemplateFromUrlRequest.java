package com.example.docserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

@Schema(description = "通过模板直链填充 Word 文档")
public record FillTemplateFromUrlRequest(
    @Schema(
        description = "Word 模板文件的 HTTP 或 HTTPS 地址",
        example = "https://example.com/files/template.docx",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "模板链接不能为空")
    @Pattern(regexp = "(?i)^https?://\\S+", message = "仅支持以 http:// 或 https:// 开头的链接")
    String templateUrl,

    @Schema(
        description = "占位符变量映射。key 对应模板中的 {{key}}，value 支持字符串、数字、布尔值和数组。",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "{\"plateNum\":\"粤A12345\",\"plateColor\":\"黄色\",\"ownerName\":\"张三\"}"
    )
    @NotNull(message = "variables 不能为空")
    Map<String, Object> variables,

    @Schema(
        description = "为 true 时填充后直接返回 PDF；省略或为 false 时返回填充后的 Word（.docx）文件。",
        defaultValue = "false",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    Boolean convertToPdf
) {}
