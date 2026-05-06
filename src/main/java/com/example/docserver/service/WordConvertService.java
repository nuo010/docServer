package com.example.docserver.service;

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

    public WordConvertService(OfficeManager officeManager) {
        this.converter = LocalConverter.builder()
            .officeManager(officeManager)
            // 打开后做一次完整更新，让分页/域尽量按当前引擎重算，再导出 PDF（略增耗时）
            .loadProperty("UpdateDocMode", UpdateDocMode.FULL_UPDATE)
            .storeProperties(pdfWriterFilterData())
            .build();
    }

    /**
     * LibreOffice writer_pdf_Export 的 FilterData。
     * 无法做到与 Microsoft Word 逐页一致，但嵌入标准字体、选用较新 PDF 版本可减少「方框字」与部分换行差异。
     */
    private static Map<String, Object> pdfWriterFilterData() {
        Map<String, Object> filterData = new LinkedHashMap<>();
        filterData.put("EmbedStandardFonts", Boolean.TRUE);
        // 0=1.4；2 在常见 LO 版本中表示较新子集（具体以 LibreOffice 为准），利于嵌入字体相关能力
        filterData.put("SelectPdfVersion", 2);
        filterData.put("UseTaggedPDF", Boolean.FALSE);
        filterData.put("ExportBookmarks", Boolean.TRUE);
        filterData.put("ExportFormFields", Boolean.TRUE);
        // 表格密集文档：略提高 JPEG 质量，减轻因图片重压缩带来的版心漂移（对纯文字影响很小）
        filterData.put("Quality", 95);
        filterData.put("UseLosslessCompression", Boolean.FALSE);

        Map<String, Object> store = new LinkedHashMap<>();
        store.put("FilterData", filterData);
        return store;
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
