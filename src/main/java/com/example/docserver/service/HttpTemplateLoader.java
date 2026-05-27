package com.example.docserver.service;

import com.example.docserver.config.HttpTemplateProperties;
import java.net.URI;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HttpTemplateLoader {

    private static final Logger log = LoggerFactory.getLogger(HttpTemplateLoader.class);
    private static final int MAX_TEMPLATE_BYTES = 20 * 1024 * 1024;

    private final RestClient restClient;
    private final HttpTemplateProperties httpTemplateProperties;

    public HttpTemplateLoader(HttpTemplateProperties httpTemplateProperties) {
        this.httpTemplateProperties = httpTemplateProperties;
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(
            org.springframework.boot.web.client.ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(httpTemplateProperties.getConnectTimeout())
                .withReadTimeout(httpTemplateProperties.getReadTimeout())
        );
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public byte[] fetchAsBytes(String templateUrl) {
        URI uri = parseAndValidateUri(templateUrl);
        long start = System.nanoTime();
        Duration connectTimeout = httpTemplateProperties.getConnectTimeout();
        Duration readTimeout = httpTemplateProperties.getReadTimeout();
        log.info(
            "Start downloading template from url={}, connectTimeout={}ms, readTimeout={}ms",
            uri,
            connectTimeout.toMillis(),
            readTimeout.toMillis()
        );
        try {
            byte[] body = restClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "模板链接返回 HTTP " + response.getStatusCode().value()
                    );
                })
                .body(byte[].class);
            if (body == null || body.length == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "下载的模板内容为空");
            }
            if (body.length > MAX_TEMPLATE_BYTES) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "模板文件超过 " + (MAX_TEMPLATE_BYTES / (1024 * 1024)) + "MB 限制"
                );
            }
            log.info(
                "Template download succeeded, url={}, bytes={}, elapsedMs={}",
                uri,
                body.length,
                elapsedMillis(start)
            );
            return body;
        } catch (ResponseStatusException e) {
            log.warn(
                "Template download failed with business error, url={}, status={}, message={}, elapsedMs={}",
                uri,
                e.getStatusCode().value(),
                e.getReason(),
                elapsedMillis(start)
            );
            throw e;
        } catch (ResourceAccessException e) {
            log.error(
                "Template download failed with network error, url={}, elapsedMs={}",
                uri,
                elapsedMillis(start),
                e
            );
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "无法下载模板（网络或超时）: " + e.getMessage(),
                e
            );
        } catch (RestClientException e) {
            log.error(
                "Template download failed with RestClientException, url={}, elapsedMs={}",
                uri,
                elapsedMillis(start),
                e
            );
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "下载模板失败: " + e.getMessage(),
                e
            );
        }
    }

    private static URI parseAndValidateUri(String templateUrl) {
        URI uri;
        try {
            uri = URI.create(templateUrl.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "模板链接格式无效");
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 http 或 https 链接");
        }
        return uri;
    }

    private static long elapsedMillis(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
