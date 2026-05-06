package com.example.docserver.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Service;

@Service
public class DocTemplateService {

    public byte[] fillTemplate(InputStream templateInput, Map<String, String> variables) throws IOException {
        try (XWPFDocument document = new XWPFDocument(templateInput);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            replaceInParagraphs(document.getParagraphs(), variables);
            for (XWPFTable table : document.getTables()) {
                table.getRows().forEach(row ->
                    row.getTableCells().forEach(cell ->
                        replaceInParagraphs(cell.getParagraphs(), variables))
                );
            }

            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void replaceInParagraphs(List<XWPFParagraph> paragraphs, Map<String, String> variables) {
        for (XWPFParagraph paragraph : paragraphs) {
            String text = paragraph.getText();
            if (text == null || text.isBlank()) {
                continue;
            }

            String replaced = text;
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String key = "${" + entry.getKey() + "}";
                replaced = replaced.replace(key, entry.getValue() == null ? "" : entry.getValue());
            }

            if (!text.equals(replaced)) {
                int runCount = paragraph.getRuns().size();
                for (int i = runCount - 1; i >= 0; i--) {
                    paragraph.removeRun(i);
                }
                XWPFRun newRun = paragraph.createRun();
                newRun.setText(replaced);
            }
        }
    }
}
