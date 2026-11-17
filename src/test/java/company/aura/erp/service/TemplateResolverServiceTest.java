package company.aura.erp.service;

import company.aura.erp.modules.email.domain.template.EmailTemplate;
import company.aura.erp.modules.email.enumerate.NotificationType;
import company.aura.erp.modules.email.exception.NotificationException;
import company.aura.erp.modules.email.service.TemplateResolverService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateResolverServiceTest {

    private final TemplateResolverService templateResolverService = new TemplateResolverService();

    @Test
    @DisplayName("Deve resolver template e mensagem de texto para todos os tipos de notificação enviáveis")
    void shouldResolveTemplateAndTextForAllNotificationTypes() {
        for (NotificationType type : NotificationType.values()) {
            if (type == NotificationType.DEFAULT) {
                assertThrows(NotificationException.class, () -> templateResolverService.getEmailTemplate(type));
                assertThrows(NotificationException.class, () -> templateResolverService.getTextMessage(type));
                continue;
            }

            EmailTemplate template = templateResolverService.getEmailTemplate(type);
            String textMessage = templateResolverService.getTextMessage(type);

            assertNotNull(template);
            assertNotNull(template.getSubject());
            assertFalse(template.getSubject().isBlank());
            assertNotNull(template.getHtml());
            assertFalse(template.getHtml().isBlank());
            assertNotNull(textMessage);
            assertFalse(textMessage.isBlank());
        }
    }

    @Test
    @DisplayName("Deve conter os placeholders esperados no template INVOICE_LINKED")
    void shouldContainExpectedPlaceholdersInInvoiceLinkedTemplate() {
        EmailTemplate template = templateResolverService.getEmailTemplate(NotificationType.INVOICE_LINKED);
        String textMessage = templateResolverService.getTextMessage(NotificationType.INVOICE_LINKED);

        assertTrue(template.getSubject().contains("${orderId}"));
        assertTrue(template.getHtml().contains("${customerName}"));
        assertTrue(template.getHtml().contains("${companyName}"));
        assertTrue(template.getHtml().contains("${orderId}"));
        assertTrue(template.getHtml().contains("${documentType}"));
        assertTrue(template.getHtml().contains("${invoiceNumber}"));
        assertTrue(template.getHtml().contains("${amount}"));

        assertTrue(textMessage.contains("${customerName}"));
        assertTrue(textMessage.contains("${companyName}"));
        assertTrue(textMessage.contains("${orderId}"));
        assertTrue(textMessage.contains("${documentType}"));
        assertTrue(textMessage.contains("${invoiceNumber}"));
        assertTrue(textMessage.contains("${amount}"));
    }
}
