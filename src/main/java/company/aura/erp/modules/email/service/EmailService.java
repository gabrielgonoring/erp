package company.aura.erp.modules.email.service;

import company.aura.erp.entity.CompanyEmailConfigEntity;
import company.aura.erp.modules.email.domain.DownloadedAttachment;
import company.aura.erp.modules.email.domain.template.EmailTemplate;
import company.aura.erp.modules.email.domain.template.NotificationRequest;
import company.aura.erp.modules.email.enumerate.NotificationType;
import company.aura.erp.service.CompanyEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final AmazonSesService amazonSesService;
    private final TemplateResolverService templateResolverService;
    private final AttachmentService attachmentService;
    private final CompanyEmailService companyEmailService;

    public void sendNotification(NotificationType type, NotificationRequest request) {
        String recipientEmail = (request != null) ? request.getReceiveEmail() : null;

        log.info("sendNotification() - START - type={}, recipientEmail={}", type, recipientEmail);

        if (request == null || StringUtils.isBlank(request.getReceiveEmail())) {
            log.warn("sendNotification() - WARN - Invalid recipientEmail cancelling notification - type={}, request={}", type, request);
            return;
        }

        try {
            Map<String, String> replacers = getReplacersByNotificationType(type, request);

            EmailTemplate emailTemplate = templateResolverService.getEmailTemplate(type);

            String subject = StringSubstitutor.replace(emailTemplate.getSubject(), replacers);
            String textBody = StringSubstitutor.replace(templateResolverService.getTextMessage(type), replacers);
            String htmlBody = StringSubstitutor.replace(emailTemplate.getHtml(), replacers);

            List<DownloadedAttachment> downloadedAttachments = attachmentService.downloadAttachments(request.getAttachment());

            Long companyId = (request.getCompany() != null) ? request.getCompany().getId() : null;
            Optional<CompanyEmailConfigEntity> companyEmailConfigOpt = companyEmailService.resolveActiveConfig(companyId, type);

            log.info("sendNotification() - Sending email - type={}, recipientEmail={}, companyId={}, customSender={}, subject={}, attachmentsCount={}", type, recipientEmail, companyId, companyEmailConfigOpt.isPresent(), subject, downloadedAttachments != null ? downloadedAttachments.size() : 0);

            String resp;

            if (companyEmailConfigOpt.isPresent()) {
                CompanyEmailConfigEntity emailConfig = companyEmailConfigOpt.get();
                resp = amazonSesService.sendEmail(emailConfig.getFromEmail(), emailConfig.getFromName(), request.getReceiveEmail(), subject, htmlBody, textBody, emailConfig.getReplyTo(), downloadedAttachments);
            } else {
                resp = amazonSesService.sendEmail(request.getReceiveEmail(), subject, htmlBody, textBody, downloadedAttachments);
            }

            log.info("sendNotification() - END - type={}, recipientEmail={}, messageId={}", type, recipientEmail, resp);
        } catch (Exception ex) {
            log.error("sendNotification() - ERROR - Error when sending notification - type={}, recipientEmail={}, error: {}", type, recipientEmail, ex.getMessage());
            throw ex;
        }
    }

    private Map<String, String> getReplacersByNotificationType(NotificationType type, NotificationRequest request) {
        log.info("getReplacersByNotificationType() - START - type={}", type);
        Map<String, String> replacers = type.buildReplacer(request);
        log.info("getReplacersByNotificationType() - END - type={}, totalReplacers={}", type, replacers != null ? replacers.size() : 0);
        return replacers;
    }
}
