package com.example.docserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

@Schema(description = "通过模板直链填充 Word：提供 http(s) 模板地址与占位符键值（键对应模板中的 {{键}}，兼容 ${键}）")
public record FillTemplateFromUrlRequest(
    @Schema(description = "Word 模板文件的 http 或 https 直链", example = "https://example.com/files/template.docx", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板链接不能为空")
    @Pattern(regexp = "(?i)^https?://\\S+", message = "仅支持以 http:// 或 https:// 开头的链接")
    String templateUrl,

    @Schema(
        description = "占位符映射：key 会替换模板中的 {{key}}（并兼容 ${key}）；值为 null 时按空字符串处理",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "{\"plateNum\":\"云A12345\",\"plateColor\":\"黄色\",\"ownerName\":\"张三\"}"
    )
    @NotNull(message = "variables 不能为空")
    Map<String, String> variables
) {}
