package com.example.docserver;

import com.example.docserver.config.HttpTemplateProperties;
import com.example.docserver.config.PdfConversionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({PdfConversionProperties.class, HttpTemplateProperties.class})
public class DocServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocServerApplication.class, args);
    }
}
