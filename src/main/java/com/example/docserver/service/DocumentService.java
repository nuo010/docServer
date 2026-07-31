package com.example.docserver.service;

import com.example.docserver.dto.FillTemplateFromUrlRequest;
import com.example.docserver.dto.WordToPdfFromUrlRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import org.jodconverter.core.office.OfficeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final MediaType DOCX_MEDIA = MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final DocTemplateService docTemplateService;
    private final WordConvertService wordConvertService;
    private final ObjectMapper objectMapper;
    private final HttpTemplateLoader httpTemplateLoader;

    public DocumentService(
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

    public DocumentResult fillTemplate(MultipartFile template, String variablesJson, boolean convertToPdf)
        throws IOException, OfficeException {
        Map<String, Object> variables = parseVariables(variablesJson);
        byte[] docx = docTemplateService.fillTemplate(template.getInputStream(), variables);
        return asDocxOrPdf(docx, convertToPdf);
    }

    public DocumentResult fillTemplateFromUrl(FillTemplateFromUrlRequest request)
        throws IOException, OfficeException {
        long start = System.nanoTime();
        log.info(
            "Received fillTemplateFromUrl request, templateUrl={}, variableCount={}, convertToPdf={}",
            request.templateUrl(),
            request.variables().size(),
            Boolean.TRUE.equals(request.convertToPdf())
        );

        byte[] templateBytes = httpTemplateLoader.fetchAsBytes(request.templateUrl());
        byte[] docx;
        try (ByteArrayInputStream input = new ByteArrayInputStream(templateBytes)) {
            docx = docTemplateService.fillTemplate(input, request.variables());
        }

        boolean convertToPdf = Boolean.TRUE.equals(request.convertToPdf());
        DocumentResult result = asDocxOrPdf(docx, convertToPdf);
        log.info(
            "fillTemplateFromUrl finished, templateUrl={}, templateBytes={}, resultBytes={}, convertToPdf={}, elapsedMs={}",
            request.templateUrl(),
            templateBytes.length,
            result.content().length,
            convertToPdf,
            elapsedMillis(start)
        );
        return result;
    }

    public DocumentResult wordToPdf(MultipartFile file) throws IOException, OfficeException {
        String suffix = resolveSuffix(file.getOriginalFilename());
        byte[] output = wordConvertService.convertToPdf(file.getInputStream(), suffix);
        return pdfResult(output);
    }

    public DocumentResult wordToPdfFromUrl(WordToPdfFromUrlRequest request)
        throws IOException, OfficeException {
        long start = System.nanoTime();
        byte[] wordBytes = httpTemplateLoader.fetchAsBytes(request.templateUrl());
        String originalSuffix = resolveUrlSuffix(request.templateUrl());
        boolean fillVariables = request.variables() != null && !request.variables().isEmpty();
        if (fillVariables && !".docx".equalsIgnoreCase(originalSuffix)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "字段填充仅支持 .docx 模板");
        }

        byte[] sourceBytes = wordBytes;
        String sourceSuffix = originalSuffix;
        if (fillVariables) {
            try (ByteArrayInputStream input = new ByteArrayInputStream(wordBytes)) {
                sourceBytes = docTemplateService.fillTemplate(input, request.variables());
            }
        }

        byte[] output = wordConvertService.convertToPdf(new ByteArrayInputStream(sourceBytes), sourceSuffix);
        log.info(
            "wordToPdfFromUrl finished, templateUrl={}, wordBytes={}, filled={}, pdfBytes={}, elapsedMs={}",
            request.templateUrl(),
            wordBytes.length,
            fillVariables,
            output.length,
            elapsedMillis(start)
        );
        return pdfResult(output);
    }

    private DocumentResult asDocxOrPdf(byte[] docxBytes, boolean convertToPdf)
        throws IOException, OfficeException {
        if (!convertToPdf) {
            return new DocumentResult(docxBytes, "filled-template.docx", DOCX_MEDIA);
        }
        return new DocumentResult(
            wordConvertService.convertToPdf(new ByteArrayInputStream(docxBytes), ".docx"),
            "filled-template.pdf",
            MediaType.APPLICATION_PDF
        );
    }

    private Map<String, Object> parseVariables(String variablesJson) {
        try {
            return objectMapper.readValue(variablesJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "variables JSON 格式无效", e);
        }
    }

    private static DocumentResult pdfResult(byte[] content) {
        return new DocumentResult(content, "converted.pdf", MediaType.APPLICATION_PDF);
    }

    private static String resolveSuffix(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".docx";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private static String resolveUrlSuffix(String fileUrl) {
        String path = URI.create(fileUrl).getPath();
        int slashIndex = path.lastIndexOf('/');
        String filename = slashIndex >= 0 ? path.substring(slashIndex + 1) : path;
        return resolveSuffix(filename);
    }

    private static long elapsedMillis(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
