package com.example.docserver.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 远程模板下载配置，对应 {@code application.yml} 中 {@code doc.template.http.*}。
 */
@ConfigurationProperties(prefix = "doc.template.http")
public class HttpTemplateProperties {

    /**
     * 建连超时；避免目标地址不可达时请求长期挂起。
     */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * 读超时；避免对端建立连接后长期不返回内容。
     */
    private Duration readTimeout = Duration.ofSeconds(20);

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}
