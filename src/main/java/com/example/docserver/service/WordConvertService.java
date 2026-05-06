package com.example.docserver.service;

import com.example.docserver.config.PdfConversionProperties;
import com.sun.star.document.UpdateDocMode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.springframework.stereotype.Service;

@Service
public class WordConvertService {

    private final DocumentConverter converter;

    public WordConvertService(OfficeManager officeManager, PdfConversionProperties pdf) {
        LocalConverter.Builder builder = LocalConverter.builder().officeManager(officeManager);
        if (pdf.isFullUpdateOnLoad()) {
            builder.loadProperty("UpdateDocMode", UpdateDocMode.FULL_UPDATE);
        }
        builder.storeProperties(pdfWriterFilterData(pdf));
        this.converter = builder.build();
    }

    /**
     * LibreOffice writer_pdf_Export 的 FilterData；字段由 {@link PdfConversionProperties} 驱动。
     */
    private static Map<String, Object> pdfWriterFilterData(PdfConversionProperties pdf) {
        Map<String, Object> filterData = new LinkedHashMap<>();
        filterData.put("EmbedStandardFonts", pdf.isEmbedStandardFonts());
        filterData.put("SelectPdfVersion", pdf.getSelectPdfVersion());
        filterData.put("UseTaggedPDF", pdf.isUseTaggedPdf());
        filterData.put("ExportBookmarks", pdf.isExportBookmarks());
        filterData.put("ExportFormFields", pdf.isExportFormFields());
        filterData.put("Quality", clamp(pdf.getQuality(), 1, 100));
        filterData.put("UseLosslessCompression", pdf.isUseLosslessCompression());

        Map<String, Object> store = new LinkedHashMap<>();
        store.put("FilterData", filterData);
        return store;
    }

    private static int clamp(int v, int min, int max) {
        return Math.min(max, Math.max(min, v));
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
