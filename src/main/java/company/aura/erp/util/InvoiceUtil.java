package company.aura.erp.util;

import company.aura.erp.security.UserPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public class InvoiceUtil {

    public static String buildPdfContentType(MultipartFile danfePdf) {
        return (danfePdf.getContentType() != null && !danfePdf.getContentType().isBlank())
                ? danfePdf.getContentType()
                : "application/pdf";
    }

    public static String buildXmlContentType(MultipartFile xmlFile) {
        return (xmlFile.getContentType() != null && !xmlFile.getContentType().isBlank())
                ? xmlFile.getContentType()
                : "application/xml";
    }

    public static String buildPdfBucketKey(String baseFolder, String fileIdentifier, String pdfOriginalName) {
        return baseFolder + "/pdf/" + fileIdentifier + "-" + pdfOriginalName;
    }

    public static String buildPdfOriginalName(MultipartFile danfePdf) {
        return StringUtils.cleanPath(
                danfePdf.getOriginalFilename() != null ? danfePdf.getOriginalFilename() : "danfe.pdf"
        );
    }

    public static String buildXmlBucketKey(String baseFolder, String fileIdentifier, String xmlOriginalName) {
        return baseFolder + "/xml/" + fileIdentifier + "-" + xmlOriginalName;
    }

    public static String buildXmlOriginalFileName(MultipartFile xmlFile) {
        return StringUtils.cleanPath(
                xmlFile.getOriginalFilename() != null ? xmlFile.getOriginalFilename() : "nota.xml"
        );
    }

    public static String buildCompanyFolderName(UserPrincipal currentUser) {
        return "invoices/" + currentUser.getCompanyId();
    }

    public static String buildInvoiceIdentifier(String accessKey) {
        return (org.apache.commons.lang3.StringUtils.isNotBlank(accessKey))
                ? accessKey.trim()
                : UUID.randomUUID().toString();
    }
}
