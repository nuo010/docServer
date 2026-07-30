package com.example.docserver.util;

import com.aspose.words.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Description: 文档操作
 * Author: fgw
 * CreateDate: 2022/10/11
 */
@Slf4j
@UtilityClass
public class FileUtil {
    private static boolean license = false;

    private final int size = 1024;

    public static void wordToPdf(String pdfPath,String wordPath) {
        FileOutputStream os = null;
        try {
            //凭证 不然切换后有水印
            InputStream is = new ClassPathResource("/license.xml").getInputStream();
            License aposeLic = new License();
            aposeLic.setLicense(is);
            license = true;
            if (!license) {
                System.out.println("License验证不通过...");
                log.error("License验证不通过...");
            }
            //生成一个空的PDF文件
            File file = new File(pdfPath);
            os = new FileOutputStream(file);
            //要转换的word文件
            Document doc = new Document(wordPath);
//            doc.save(os, SaveFormat.PDF);
            Document document = new Document();
            document.removeAllChildren();
            document.appendDocument(doc, ImportFormatMode.USE_DESTINATION_STYLES);
            document.save(os, SaveFormat.PDF);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * File url
     *
     * @param urlAddress url address
     * @param localFileName local file name
     * @param destinationDir destination dir
     */
    public static void fileUrl(String urlAddress, String localFileName, String destinationDir) {
        OutputStream outputStream = null;
        URLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            URL url = new URL(urlAddress);
            outputStream = new BufferedOutputStream(Files.newOutputStream(Paths.get(destinationDir + "/" + localFileName)));
            urlConnection = url.openConnection();
            inputStream = urlConnection.getInputStream();

            byte[] buf = new byte[size];
            int byteRead, byteWritten = 0;
            while ((byteRead = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, byteRead);
                outputStream.flush();
                byteWritten += byteRead;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            try {
                inputStream.close();
                outputStream.close();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * File download
     *
     * @param urlAddress url address
     * @param destinationDir destination dir
     */
    public static void fileDownload(String urlAddress, String fileName, String destinationDir) {
        fileUrl(urlAddress, fileName, destinationDir);
    }


    public static void main(String[] args) {
        wordToPdf("C:\\Users\\nuo01\\Desktop\\666.pdf","C:\\Users\\nuo01\\Desktop\\111.docx");
    }
}
