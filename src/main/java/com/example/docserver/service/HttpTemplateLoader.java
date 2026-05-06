package com.example.docserver.service;

import java.net.URI;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HttpTemplateLoader {

    private static final int MAX_TEMPLATE_BYTES = 20 * 1024 * 1024;

    private final RestClient restClient = RestClient.create();

    public byte[] fetchAsBytes(String templateUrl) {
        URI uri;
        try {
            uri = URI.create(templateUrl.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "模板链接格式无效");
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "仅支持 http 或 https 链接");
        }
        try {
            byte[] body = restClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_GATEWAY,
                        "模板链接返回 HTTP " + response.getStatusCode().value()
                    );
                })
                .body(byte[].class);
            if (body == null || body.length == 0) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "下载的模板内容为空");
            }
            if (body.length > MAX_TEMPLATE_BYTES) {
                throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "模板文件超过 " + (MAX_TEMPLATE_BYTES / (1024 * 1024)) + "MB 限制"
                );
            }
            return body;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                "无法下载模板（网络或超时）: " + e.getMessage(),
                e
            );
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                "下载模板失败: " + e.getMessage(),
                e
            );
        }
    }
}
