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

        if (attachments == null || attachments.isEmpty()) {
            return Collections.emptyList();
        }

        return attachments.stream().map(this::downloadAttachment).filter(Objects::nonNull).toList();
    }

    private DownloadedAttachment downloadAttachment(Attachment attachment) {
        return switch (attachment.getSource()){
            case BUCKET -> downloadedAttachmentInBucket(attachment);
        };
    }

    private DownloadedAttachment downloadedAttachmentInBucket(Attachment attachment) {

        if (attachment == null || StringUtils.isBlank(attachment.getUrl())) {
            return null;
        }

        try {
            String key = cloudflareR2Service.extractKeyFromUrl(attachment.getUrl());

            byte[] data = cloudflareR2Service.download(key);

            if (data != null && data.length > 0) {
                String fileName = extractFileName(key, attachment.getUrl());
                String contentType = determineContentType(fileName);

                DownloadedAttachment downloadedAttachment = new DownloadedAttachment();
                downloadedAttachment.setFileName(fileName);
                downloadedAttachment.setData(data);
                downloadedAttachment.setContentType(contentType);

                log.info("downloadAttachments() - Successfully downloaded attachment: fileName={}, size={}", fileName, data.length);
                return downloadedAttachment;
            } else {
                log.warn("downloadAttachments() - Downloaded file is empty for url: {}", attachment.getUrl());
            }
        } catch (Exception ex) {
            log.warn("downloadAttachments() - Could not download attachment from url: {}, error: {}", attachment.getUrl(), ex.getMessage());
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
