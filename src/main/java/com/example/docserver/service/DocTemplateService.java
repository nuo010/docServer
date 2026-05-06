package com.example.docserver.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

@Service
public class DocTemplateService {

    /**
     * 将模板中的占位符替换为 map 中的值。
     * 支持 {@code {{键名}}}（推荐）与历史兼容的 {@code ${键名}}；map 的 key 不要包含花括号或 {@code $}。
     */
    public byte[] fillTemplate(InputStream templateInput, Map<String, String> variables) throws IOException {
        try (XWPFDocument document = new XWPFDocument(templateInput);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            replaceInDocumentBody(document, variables);
            for (XWPFHeader header : document.getHeaderList()) {
                replaceInHeaderFooter(header.getBodyElements(), variables);
            }
            for (XWPFFooter footer : document.getFooterList()) {
                replaceInHeaderFooter(footer.getBodyElements(), variables);
            }

            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void replaceInDocumentBody(XWPFDocument document, Map<String, String> variables) {
        for (IBodyElement element : document.getBodyElements()) {
            if (element instanceof XWPFParagraph p) {
                replaceInParagraph(p, variables);
            } else if (element instanceof XWPFTable t) {
                replaceInTable(t, variables);
            }
        }
    }

    private void replaceInHeaderFooter(List<IBodyElement> elements, Map<String, String> variables) {
        for (IBodyElement element : elements) {
            if (element instanceof XWPFParagraph p) {
                replaceInParagraph(p, variables);
            } else if (element instanceof XWPFTable t) {
                replaceInTable(t, variables);
            }
        }
    }

    private void replaceInTable(XWPFTable table, Map<String, String> variables) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (IBodyElement element : cell.getBodyElements()) {
                    if (element instanceof XWPFParagraph p) {
                        replaceInParagraph(p, variables);
                    } else if (element instanceof XWPFTable nested) {
                        replaceInTable(nested, variables);
                    }
                }
            }
        }
    }

    private void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> variables) {
        String text = paragraph.getText();
        if (text == null || text.isBlank()) {
            return;
        }

        String replaced = applyReplacements(text, variables);
        if (text.equals(replaced)) {
            return;
        }

        int runCount = paragraph.getRuns().size();
        for (int i = runCount - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }
        XWPFRun newRun = paragraph.createRun();
        newRun.setText(replaced);
    }

    private static String applyReplacements(String source, Map<String, String> variables) {
        String replaced = source;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            replaced = replaced.replace("{{" + entry.getKey() + "}}", value);
            replaced = replaced.replace("${" + entry.getKey() + "}", value);
        }
        return replaced;
    }
}
