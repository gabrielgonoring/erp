package company.aura.erp.service;

import company.aura.erp.config.AwsSesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AmazonSesServiceTest {

    @Mock
    private SesClient sesClient;

    @Mock
    private AwsSesProperties properties;

    @InjectMocks
    private AmazonSesService sesService;

    private static final String FROM_EMAIL = "contato@aurasistemas.com.br";
    private static final String FROM_NAME = "Aura ERP";

    @BeforeEach
    void setUp() {
        lenient().when(properties.getFormattedFrom()).thenReturn(FROM_NAME + " <" + FROM_EMAIL + ">");
        lenient().when(properties.getEnabled()).thenReturn(true);
    }

    @Test
    @DisplayName("Deve enviar e-mail com sucesso para um único destinatário com corpo HTML e texto")
    void sendEmailSingleRecipientSuccess() {
        SendEmailResponse mockResponse = SendEmailResponse.builder()
                .messageId("msg-12345")
                .build();
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(mockResponse);

        String messageId = sesService.sendEmail("cliente@dominio.com", "Bem-vindo", "<h1>Olá</h1>", "Olá");

        assertEquals("msg-12345", messageId);

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient, times(1)).sendEmail(captor.capture());

        SendEmailRequest sentRequest = captor.getValue();
        assertEquals("Aura ERP <contato@aurasistemas.com.br>", sentRequest.source());
        assertEquals(List.of("cliente@dominio.com"), sentRequest.destination().toAddresses());
        assertEquals("Bem-vindo", sentRequest.message().subject().data());
        assertEquals("<h1>Olá</h1>", sentRequest.message().body().html().data());
        assertEquals("Olá", sentRequest.message().body().text().data());
    }

    @Test
    @DisplayName("Deve enviar e-mail com CC, BCC, ConfigurationSet e Reply-To")
    void sendEmailWithCcBccAndReplyTo() {
        when(properties.getConfigurationSet()).thenReturn("erp-metric-set");

        SendEmailResponse mockResponse = SendEmailResponse.builder()
                .messageId("msg-complex-6789")
                .build();
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(mockResponse);

        String messageId = sesService.sendEmail(
                List.of("to@teste.com"),
                List.of("cc@teste.com"),
                List.of("bcc@teste.com"),
                "Assunto",
                "<p>Corpo</p>",
                "Corpo",
                List.of("reply@teste.com")
        );

        assertEquals("msg-complex-6789", messageId);

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(captor.capture());

        SendEmailRequest req = captor.getValue();
        assertEquals(List.of("to@teste.com"), req.destination().toAddresses());
        assertEquals(List.of("cc@teste.com"), req.destination().ccAddresses());
        assertEquals(List.of("bcc@teste.com"), req.destination().bccAddresses());
        assertEquals(List.of("reply@teste.com"), req.replyToAddresses());
        assertEquals("erp-metric-set", req.configurationSetName());
    }

    @Test
    @DisplayName("Quando o envio estiver desabilitado (enabled=false), não chama o SES e retorna ID simulado")
    void sendEmailWhenDisabled() {
        when(properties.getEnabled()).thenReturn(false);

        String messageId = sesService.sendEmail("destinatario@teste.com", "Teste", "Conteudo");

        assertNotNull(messageId);
        assertTrue(messageId.startsWith("mock-ses-"));
        verifyNoInteractions(sesClient);
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException BAD_REQUEST se destinatários forem vazios")
    void sendEmailWithoutRecipients() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sesService.sendEmail("", "Assunto", "Corpo")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(sesClient);
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException BAD_REQUEST se assunto for vazio")
    void sendEmailWithoutSubject() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sesService.sendEmail("dest@teste.com", "   ", "Corpo")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(sesClient);
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException BAD_REQUEST se corpo (HTML e texto) forem nulos")
    void sendEmailWithoutBody() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sesService.sendEmail("dest@teste.com", "Assunto", null, null)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(sesClient);
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException se remetente não estiver configurado")
    void sendEmailWithoutFromAddress() {
        when(properties.getFormattedFrom()).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                sesService.sendEmail("dest@teste.com", "Assunto", "Corpo")
        );
        assertTrue(ex.getMessage().contains("O e-mail do remetente não foi configurado"));
        verifyNoInteractions(sesClient);
    }

    @Test
    @DisplayName("Deve capturar SesException e relançar ResponseStatusException INTERNAL_SERVER_ERROR")
    void sendEmailSesException() {
        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorMessage("Email address not verified")
                .errorCode("MessageRejected")
                .build();
        SesException sesException = (SesException) SesException.builder()
                .awsErrorDetails(errorDetails)
                .message("Message rejected")
                .build();

        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenThrow(sesException);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sesService.sendEmail("unverified@teste.com", "Assunto", "Corpo")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Email address not verified"));
    }
}
