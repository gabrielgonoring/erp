package company.aura.erp.service;

import company.aura.erp.config.AwsSesProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmazonSesService {

    private final SesClient sesClient;
    private final AwsSesProperties properties;

    /**
     * Envia um e-mail simples (apenas destinatário, assunto e corpo HTML).
     *
     * @param to Destinatário do e-mail
     * @param subject Assunto do e-mail
     * @param htmlBody Conteúdo HTML do e-mail
     * @return Message ID retornado pelo SES
     */
    public String sendEmail(String to, String subject, String htmlBody) {
        return sendEmail(to, subject, htmlBody, null);
    }

    /**
     * Envia um e-mail com versões HTML e texto simples para um único destinatário.
     *
     * @param to Destinatário do e-mail
     * @param subject Assunto do e-mail
     * @param htmlBody Conteúdo HTML do e-mail
     * @param textBody Conteúdo em texto puro (fallback)
     * @return Message ID retornado pelo SES
     */
    public String sendEmail(String to, String subject, String htmlBody, String textBody) {
        List<String> recipients = (to != null && !to.isBlank()) ? List.of(to.trim()) : Collections.emptyList();
        return sendEmail(recipients, Collections.emptyList(), Collections.emptyList(), subject, htmlBody, textBody, Collections.emptyList());
    }

    /**
     * Envia um e-mail com versões HTML e texto simples para um único destinatário com Reply-To customizado.
     *
     * @param to Destinatário do e-mail
     * @param subject Assunto do e-mail
     * @param htmlBody Conteúdo HTML do e-mail
     * @param textBody Conteúdo em texto puro (fallback)
     * @param replyTo Endereço para resposta (Reply-To)
     * @return Message ID retornado pelo SES
     */
    public String sendEmail(String to, String subject, String htmlBody, String textBody, String replyTo) {
        List<String> recipients = (to != null && !to.isBlank()) ? List.of(to.trim()) : Collections.emptyList();
        List<String> replyToList = (replyTo != null && !replyTo.isBlank()) ? List.of(replyTo.trim()) : Collections.emptyList();
        return sendEmail(recipients, Collections.emptyList(), Collections.emptyList(), subject, htmlBody, textBody, replyToList);
    }

    /**
     * Envia um e-mail para múltiplos destinatários com corpo HTML.
     *
     * @param to Lista de destinatários (TO)
     * @param subject Assunto do e-mail
     * @param htmlBody Conteúdo HTML do e-mail
     * @return Message ID retornado pelo SES
     */
    public String sendEmail(List<String> to, String subject, String htmlBody) {
        return sendEmail(to, Collections.emptyList(), Collections.emptyList(), subject, htmlBody, null, Collections.emptyList());
    }

    /**
     * Envia um e-mail para múltiplos destinatários com versão HTML e texto simples.
     *
     * @param to Lista de destinatários (TO)
     * @param subject Assunto do e-mail
     * @param htmlBody Conteúdo HTML do e-mail
     * @param textBody Conteúdo texto puro (fallback)
     * @return Message ID retornado pelo SES
     */
    public String sendEmail(List<String> to, String subject, String htmlBody, String textBody) {
        return sendEmail(to, Collections.emptyList(), Collections.emptyList(), subject, htmlBody, textBody, Collections.emptyList());
    }

    /**
     * Envia um e-mail com listas completas de TO, CC e BCC.
     */
    public String sendEmail(List<String> to, List<String> cc, List<String> bcc, String subject, String htmlBody, String textBody) {
        return sendEmail(to, cc, bcc, subject, htmlBody, textBody, Collections.emptyList());
    }

    /**
     * Método principal para envio de e-mails via Amazon SES.
     *
     * @param to Lista de destinatários principais
     * @param cc Lista de destinatários em cópia
     * @param bcc Lista de destinatários em cópia oculta
     * @param subject Assunto do e-mail
     * @param htmlBody Conteúdo HTML
     * @param textBody Conteúdo texto puro
     * @param replyTo Lista opcional de endereços para resposta (Reply-To)
     * @return Message ID retornado pelo SES
     */
    public String sendEmail(
            List<String> to,
            List<String> cc,
            List<String> bcc,
            String subject,
            String htmlBody,
            String textBody,
            List<String> replyTo
    ) {
        log.info("sendEmail() - START - to={}, cc={}, bcc={}, subject={}", to, cc, bcc, subject);

        List<String> sanitizedTo = filterValidEmails(to);
        List<String> sanitizedCc = filterValidEmails(cc);
        List<String> sanitizedBcc = filterValidEmails(bcc);

        if (sanitizedTo.isEmpty() && sanitizedCc.isEmpty() && sanitizedBcc.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pelo menos um destinatário válido (TO, CC ou BCC) deve ser informado.");
        }

        if (subject == null || subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O assunto do e-mail não pode ser nulo ou vazio.");
        }

        boolean hasHtml = htmlBody != null && !htmlBody.isBlank();
        boolean hasText = textBody != null && !textBody.isBlank();

        if (!hasHtml && !hasText) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O corpo do e-mail (HTML ou texto) deve ser fornecido.");
        }

        String fromAddress = properties.getFormattedFrom();
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("O e-mail do remetente não foi configurado (aws.ses.from-email / AWS_SES_FROM_EMAIL).");
        }

        // Se o envio estiver desabilitado no ambiente (ex: dev local), apenas simula e registra em log
        if (Boolean.FALSE.equals(properties.getEnabled())) {
            String mockMessageId = "mock-ses-" + UUID.randomUUID();
            log.warn("sendEmail() - SES DESABILITADO (aws.ses.enabled=false). Simulação de envio para: {}, subject: {}, mockMessageId={}",
                    sanitizedTo, subject, mockMessageId);
            return mockMessageId;
        }

        try {
            Destination destination = Destination.builder()
                    .toAddresses(sanitizedTo)
                    .ccAddresses(sanitizedCc)
                    .bccAddresses(sanitizedBcc)
                    .build();

            Body.Builder bodyBuilder = Body.builder();
            if (hasHtml) {
                bodyBuilder.html(Content.builder()
                        .data(htmlBody)
                        .charset(StandardCharsets.UTF_8.name())
                        .build());
            }
            if (hasText) {
                bodyBuilder.text(Content.builder()
                        .data(textBody)
                        .charset(StandardCharsets.UTF_8.name())
                        .build());
            }

            Message message = Message.builder()
                    .subject(Content.builder()
                            .data(subject)
                            .charset(StandardCharsets.UTF_8.name())
                            .build())
                    .body(bodyBuilder.build())
                    .build();

            SendEmailRequest.Builder requestBuilder = SendEmailRequest.builder()
                    .source(fromAddress)
                    .destination(destination)
                    .message(message);

            if (properties.getConfigurationSet() != null && !properties.getConfigurationSet().isBlank()) {
                requestBuilder.configurationSetName(properties.getConfigurationSet().trim());
            }

            // Define os endereços de Reply-To (prioriza o informado no método; caso vazio, usa o configurado em aws.ses.reply-to)
            List<String> sanitizedReplyTo = filterValidEmails(replyTo);
            if (sanitizedReplyTo.isEmpty() && properties.getReplyTo() != null && !properties.getReplyTo().isBlank()) {
                sanitizedReplyTo = filterValidEmails(List.of(properties.getReplyTo().trim()));
            }

            if (!sanitizedReplyTo.isEmpty()) {
                requestBuilder.replyToAddresses(sanitizedReplyTo);
                log.debug("sendEmail() - Reply-To configurado: {}", sanitizedReplyTo);
            }

            SendEmailResponse response = sesClient.sendEmail(requestBuilder.build());
            String messageId = response.messageId();

            log.info("sendEmail() - END - messageId={}, to={}", messageId, sanitizedTo);
            return messageId;

        } catch (SesException e) {
            String errorMsg = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            log.error("sendEmail() - ERROR - Falha ao enviar e-mail via Amazon SES - to: {}, subject: {}, error: {}",
                    sanitizedTo, subject, errorMsg);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao enviar e-mail via Amazon SES: " + errorMsg, e);
        }
    }

    private List<String> filterValidEmails(List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return Collections.emptyList();
        }
        return emails.stream()
                .filter(email -> email != null && !email.trim().isBlank())
                .map(String::trim)
                .toList();
    }
}
