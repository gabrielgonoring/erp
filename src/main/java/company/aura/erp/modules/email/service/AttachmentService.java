package company.aura.erp.modules.email.service;

import company.aura.erp.modules.bucket.service.CloudflareR2Service;
import company.aura.erp.modules.email.domain.DownloadedAttachment;
import company.aura.erp.modules.email.domain.template.Attachment;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@AllArgsConstructor
public class AttachmentService {

    private CloudflareR2Service cloudflareR2Service;

    public List<DownloadedAttachment> downloadAttachments(List<Attachment> attachments) {
        int count = (attachments != null) ? attachments.size() : 0;
        log.info("downloadAttachments() - START - totalAttachments={}", count);

        if (attachments == null || attachments.isEmpty()) {
            log.info("downloadAttachments() - END - attachments is empty, returning empty list");
            return Collections.emptyList();
        }

        List<DownloadedAttachment> downloadedAttachments = attachments.stream()
                .map(this::downloadAttachment)
                .filter(Objects::nonNull)
                .toList();

        log.info("downloadAttachments() - END - requestedAttachments={}, downloadedAttachments={}", count, downloadedAttachments.size());
        return downloadedAttachments;
    }

    private DownloadedAttachment downloadAttachment(Attachment attachment) {
        log.info("downloadAttachment() - START - source={}", attachment != null ? attachment.getSource() : null);

        DownloadedAttachment downloaded = switch (attachment.getSource()) {
            case BUCKET -> downloadedAttachmentInBucket(attachment);
        };

        log.info("downloadAttachment() - END - source={}, downloaded={}", attachment.getSource(), downloaded != null);
        return downloaded;
    }

    private DownloadedAttachment downloadedAttachmentInBucket(Attachment attachment) {
        String url = (attachment != null) ? attachment.getUrl() : null;
        log.info("downloadedAttachmentInBucket() - START - url={}", url);

        if (attachment == null || StringUtils.isBlank(url)) {
            log.warn("downloadedAttachmentInBucket() - WARN - Attachment or URL is null or blank");
            return null;
        }

        try {
            String key = cloudflareR2Service.extractKeyFromUrl(url);

            byte[] data = cloudflareR2Service.download(key);

            if (data != null && data.length > 0) {
                String fileName = extractFileName(key, url);
                String contentType = determineContentType(fileName);

                DownloadedAttachment downloadedAttachment = new DownloadedAttachment();
                downloadedAttachment.setFileName(fileName);
                downloadedAttachment.setData(data);
                downloadedAttachment.setContentType(contentType);

                log.info("downloadedAttachmentInBucket() - END - fileName={}, contentType={}, size={}", fileName, contentType, data.length);
                return downloadedAttachment;
            } else {
                log.warn("downloadedAttachmentInBucket() - WARN - Downloaded file is empty for url: {}", url);
            }
        } catch (Exception ex) {
            log.error("downloadedAttachmentInBucket() - ERROR - Could not download attachment from bucket - url: {}, error: {}", url, ex.getMessage());
        }

        return null;
    }

    private String extractFileName(String key, String url) {
        String path = (key != null && !key.isBlank()) ? key : url;
        if (path == null || path.isBlank()) {
            return "attachment";
        }
        int slashIdx = path.lastIndexOf('/');
        String name = (slashIdx >= 0) ? path.substring(slashIdx + 1) : path;
        int queryIdx = name.indexOf('?');
        if (queryIdx >= 0) {
            name = name.substring(0, queryIdx);
        }
        if (name.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}-.+")) {
            name = name.substring(37);
        }
        return StringUtils.isNotBlank(name) ? name : "attachment";
    }

    private String determineContentType(String fileName) {
        if (fileName == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF_VALUE;
        } else if (lower.endsWith(".xml")) {
            return MediaType.APPLICATION_XML_VALUE;
        } else if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        } else if (lower.endsWith(".txt")) {
            return MediaType.TEXT_PLAIN_VALUE;
        } else if (lower.endsWith(".html")) {
            return MediaType.TEXT_HTML_VALUE;
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
