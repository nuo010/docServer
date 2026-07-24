package com.example.docserver.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.springframework.stereotype.Service;

@Service
public class DocTemplateService {

    /**
     * Replace placeholder in template with values from the map.
     * Placeholder format: {@code {{key}}} for scalar, {@code {{listKey.fieldName}}} for list items in tables.
     * <p>
     * For list fill: pass a key whose value is a {@code List<Map<String, Object>>}.
     * Create a template row in the table with placeholders like {@code {{listKey.columnName}}}.
     * The service detects this, duplicates the template row for each list item, and fills in the values.
     */
    public byte[] fillTemplate(InputStream templateInput, Map<String, Object> variables) throws IOException {
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

    // ---- Document body / Header-Footer traversal ----

    private void replaceInDocumentBody(XWPFDocument document, Map<String, Object> variables) {
        for (IBodyElement element : document.getBodyElements()) {
            if (element instanceof XWPFParagraph p) {
                replaceInParagraph(p, variables);
            } else if (element instanceof XWPFTable t) {
                replaceInTable(t, variables);
            }
        }
    }

    private void replaceInHeaderFooter(List<IBodyElement> elements, Map<String, Object> variables) {
        for (IBodyElement element : elements) {
            if (element instanceof XWPFParagraph p) {
                replaceInParagraph(p, variables);
            } else if (element instanceof XWPFTable t) {
                replaceInTable(t, variables);
            }
        }
    }

    // ---- Table processing: list expansion + scalar replacement ----

    private void replaceInTable(XWPFTable table, Map<String, Object> variables) {
        // Split into list-of-maps variables (table row cloning) vs scalar variables (simple replacement)
        Map<String, List<Map<String, Object>>> listVars = new LinkedHashMap<>();
        Map<String, Object> scalarVars = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (isListOfMaps(entry.getValue())) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> typedList = (List<Map<String, Object>>) entry.getValue();
                listVars.put(entry.getKey(), typedList);
            } else {
                scalarVars.put(entry.getKey(), entry.getValue());
            }
        }

        // First pass: expand list-based template rows
        for (Map.Entry<String, List<Map<String, Object>>> listEntry : listVars.entrySet()) {
            expandListInTable(table, listEntry.getKey(), listEntry.getValue());
        }

        // Second pass: scalar replacement in remaining rows (including cloned ones)
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (IBodyElement element : cell.getBodyElements()) {
                    if (element instanceof XWPFParagraph p) {
                        replaceInParagraph(p, scalarVars);
                    } else if (element instanceof XWPFTable nested) {
                        // Nested table: pass the full variables so it can have its own lists
                        replaceInTable(nested, variables);
                    }
                }
            }
        }
    }

    /**
     * Find template rows containing {@code {{listKey.xxx}}} and expand them.
     */
    private void expandListInTable(XWPFTable table, String listKey, List<Map<String, Object>> items) {
        String prefix = "{{" + listKey + ".";
        List<XWPFTableRow> rows = table.getRows();

        // Iterate backwards so row removal doesn't affect indices
        for (int i = rows.size() - 1; i >= 0; i--) {
            XWPFTableRow row = rows.get(i);
            if (!rowContainsPrefix(row, prefix)) {
                continue;
            }

            if (items.isEmpty()) {
                table.removeRow(i);
            } else {
                // Reuse the template row for the first item
                fillRowFromItem(row, items.get(0), listKey);
                // Clone for remaining items (insert after current row)
                for (int j = 1; j < items.size(); j++) {
                    insertClonedRow(table, row, i + j, items.get(j), listKey);
                }
            }
        }
    }

    /**
     * Clone the source row at CT level, insert after it, and fill with item data.
     */
    private void insertClonedRow(XWPFTable table, XWPFTableRow sourceRow, int insertPos,
                                 Map<String, Object> itemData, String listKey) {
        CTRow ctRow = (CTRow) sourceRow.getCtRow().copy();
        table.getCTTbl().getTrList().add(insertPos, ctRow);
        XWPFTableRow newRow = new XWPFTableRow(ctRow, table);
        fillRowFromItem(newRow, itemData, listKey);
    }

    /**
     * Replace {@code {{listKey.fieldName}}} in every cell of the row with values from itemData.
     */
    private void fillRowFromItem(XWPFTableRow row, Map<String, Object> itemData, String listKey) {
        String prefix = "{{" + listKey + ".";
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                String text = paragraph.getText();
                if (text == null || text.isBlank()) {
                    continue;
                }
                String replaced = text;
                for (Map.Entry<String, Object> entry : itemData.entrySet()) {
                    replaced = replaced.replace(prefix + entry.getKey() + "}}",
                        stringifyVariableValue(entry.getValue()));
                }
                if (text.equals(replaced)) {
                    continue;
                }
                // Clear runs and set new text (same pattern as replaceInParagraph)
                List<XWPFRun> runs = paragraph.getRuns();
                for (int r = runs.size() - 1; r >= 0; r--) {
                    paragraph.removeRun(r);
                }
                XWPFRun newRun = paragraph.createRun();
                newRun.setText(replaced);
            }
        }
    }

    private boolean rowContainsPrefix(XWPFTableRow row, String prefix) {
        for (XWPFTableCell cell : row.getTableCells()) {
            if (cell.getText().contains(prefix)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean isListOfMaps(Object value) {
        if (!(value instanceof List)) {
            return false;
        }
        List<?> list = (List<?>) value;
        return !list.isEmpty() && list.get(0) instanceof Map;
    }

    // ---- Paragraph simple replacement ----

    private void replaceInParagraph(XWPFParagraph paragraph, Map<String, Object> variables) {
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

    private static String applyReplacements(String source, Map<String, Object> variables) {
        String replaced = source;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String value = stringifyVariableValue(entry.getValue());
            replaced = replaced.replace("{{" + entry.getKey() + "}}", value);
        }
        return replaced;
    }

    private static String stringifyVariableValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                .map(DocTemplateService::stringifyVariableValue)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
        }
        if (value instanceof Object[] array) {
            return java.util.Arrays.stream(array)
                .map(DocTemplateService::stringifyVariableValue)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
        }
        return Objects.toString(value, "");
    }
}