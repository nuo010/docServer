package com.example.docserver.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.springframework.stereotype.Service;

@Service
public class WordConvertService {

    private final DocumentConverter converter;

    public WordConvertService(OfficeManager officeManager) {
        this.converter = LocalConverter.builder().officeManager(officeManager).build();
    }

    public byte[] convertToPdf(InputStream sourceInputStream, String sourceSuffix) throws IOException, OfficeException {
        Path sourceFile = Files.createTempFile("word-src-", sourceSuffix);
        Path targetFile = Files.createTempFile("word-pdf-", ".pdf");
        try {
            Files.copy(sourceInputStream, sourceFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            converter.convert(sourceFile.toFile()).to(targetFile.toFile()).execute();
            return Files.readAllBytes(targetFile);
        } finally {
            Files.deleteIfExists(sourceFile);
            Files.deleteIfExists(targetFile);
        }
    }
}
