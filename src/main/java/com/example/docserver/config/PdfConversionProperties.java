package com.example.docserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Word → PDF（LibreOffice writer_pdf_Export）可调参数，对应 {@code application.yml} 中 {@code doc.conversion.pdf.*}。
 */
@ConfigurationProperties(prefix = "doc.conversion.pdf")
public class PdfConversionProperties {

    /**
     * LibreOffice FilterData SelectPdfVersion；0 常为 PDF 1.4，具体取值见所用 LO 版本说明。
     */
    private int selectPdfVersion = 2;

    /**
     * 打开源文档时是否使用 UpdateDocMode.FULL_UPDATE（完整重算，略慢，有时能改善分页）。
     */
    private boolean fullUpdateOnLoad = true;

    private boolean embedStandardFonts = true;
    private boolean useTaggedPdf = false;
    private boolean exportBookmarks = true;
    private boolean exportFormFields = true;
    private int quality = 95;
    private boolean useLosslessCompression = false;

    public int getSelectPdfVersion() {
        return selectPdfVersion;
    }

    public void setSelectPdfVersion(int selectPdfVersion) {
        this.selectPdfVersion = selectPdfVersion;
    }

    public boolean isFullUpdateOnLoad() {
        return fullUpdateOnLoad;
    }

    public void setFullUpdateOnLoad(boolean fullUpdateOnLoad) {
        this.fullUpdateOnLoad = fullUpdateOnLoad;
    }

    public boolean isEmbedStandardFonts() {
        return embedStandardFonts;
    }

    public void setEmbedStandardFonts(boolean embedStandardFonts) {
        this.embedStandardFonts = embedStandardFonts;
    }

    public boolean isUseTaggedPdf() {
        return useTaggedPdf;
    }

    public void setUseTaggedPdf(boolean useTaggedPdf) {
        this.useTaggedPdf = useTaggedPdf;
    }

    public boolean isExportBookmarks() {
        return exportBookmarks;
    }

    public void setExportBookmarks(boolean exportBookmarks) {
        this.exportBookmarks = exportBookmarks;
    }

    public boolean isExportFormFields() {
        return exportFormFields;
    }

    public void setExportFormFields(boolean exportFormFields) {
        this.exportFormFields = exportFormFields;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public boolean isUseLosslessCompression() {
        return useLosslessCompression;
    }

    public void setUseLosslessCompression(boolean useLosslessCompression) {
        this.useLosslessCompression = useLosslessCompression;
    }
}
