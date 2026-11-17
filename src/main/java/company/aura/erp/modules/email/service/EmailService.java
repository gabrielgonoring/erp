package company.aura.erp.modules.email.service;

import company.aura.erp.modules.email.domain.DownloadedAttachment;
import company.aura.erp.modules.email.domain.template.NotificationRequest;
import company.aura.erp.modules.email.domain.template.EmailTemplate;
import company.aura.erp.modules.email.enumerate.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final AmazonSesService amazonSesService;
    private final TemplateResolverService templateResolverService;
    private final AttachmentService attachmentService;

    public void sendNotification(NotificationType type, NotificationRequest request) {
        log.info("sendNotification() - START - type: {}, request: {}", type, request);

        if (request == null || StringUtils.isBlank(request.getReceiveEmail())) {
            log.warn("sendNotification() - Invalid recipientEmail cancelling notification.");
            return;
        }

        Map<String, String> replacers = getReplacersByNotificationType(type, request);

        EmailTemplate emailTemplate = templateResolverService.getEmailTemplate(type);

        String subject = StringSubstitutor.replace(emailTemplate.getSubject(), replacers);
        String textBody = StringSubstitutor.replace(templateResolverService.getTextMessage(type), replacers);
        String htmlBody = StringSubstitutor.replace(emailTemplate.getHtml(), replacers);

        List<DownloadedAttachment> downloadedAttachments = attachmentService.downloadAttachments(request.getAttachment());

        String resp;
        if (!downloadedAttachments.isEmpty()) {
            resp = amazonSesService.sendEmail(request.getReceiveEmail(), subject, htmlBody, textBody, downloadedAttachments);
        } else {
            resp = amazonSesService.sendEmail(request.getReceiveEmail(), subject, htmlBody, textBody);
        }

        log.info("sendNotification() - END - request: {}, code: {}", request, resp);
    }


    private Map<String, String> getReplacersByNotificationType(NotificationType type, NotificationRequest request) {
        return type.buildReplacer(request);
    }
}
