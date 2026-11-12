package company.aura.erp.service;

import company.aura.erp.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final AmazonSesService amazonSesService;

    /**
     * Envia e-mail de recuperação de senha contendo a URL de redefinição.
     * Utiliza o Reply-To padrão configurado na aplicação.
     *
     * @param user     Usuário destinatário
     * @param resetUrl URL completa para redefinição de senha
     */
    public void sendPasswordResetEmail(UserEntity user, String resetUrl) {
        sendPasswordResetEmail(user, resetUrl, null);
    }

    /**
     * Envia e-mail de recuperação de senha contendo a URL de redefinição e Reply-To customizado.
     *
     * @param user     Usuário destinatário
     * @param resetUrl URL completa para redefinição de senha
     * @param replyTo  E-mail para onde o usuário pode responder com dúvidas (opcional, null usa o padrão)
     */
    public void sendPasswordResetEmail(UserEntity user, String resetUrl, String replyTo) {
        log.info("sendPasswordResetEmail() - START - email={}, replyTo={}", user != null ? user.getEmail() : null, replyTo);

        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("sendPasswordResetEmail() - Usuário ou e-mail inválido, abortando envio.");
            return;
        }

        String subject = "Redefinição de Senha - Aura ERP";
        String userName = user.getName() != null && !user.getName().isBlank() ? user.getName() : "Usuário";

        String textBody = String.format(
                "Olá, %s!\n\n" +
                "Recebemos uma solicitação para redefinir a sua senha no Aura ERP.\n" +
                "Para redefinir sua senha, acesse o link abaixo:\n\n" +
                "%s\n\n" +
                "Se você não solicitou a redefinição de senha, ignore esta mensagem.\n" +
                "Caso precise de suporte, basta responder a este e-mail.\n\n" +
                "Atenciosamente,\nEquipe Aura ERP",
                userName, resetUrl
        );

        String htmlBody = String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset=\"UTF-8\"></head>" +
                "<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">" +
                "  <div style=\"max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;\">" +
                "    <h2 style=\"color: #1a73e8;\">Recuperação de Senha</h2>" +
                "    <p>Olá, <strong>%s</strong>!</p>" +
                "    <p>Recebemos uma solicitação para redefinir a sua senha no sistema <strong>Aura ERP</strong>.</p>" +
                "    <p style=\"margin: 24px 0;\">" +
                "      <a href=\"%s\" style=\"background-color: #1a73e8; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block; font-weight: bold;\">" +
                "        Redefinir Minha Senha" +
                "      </a>" +
                "    </p>" +
                "    <p>Ou copie e cole o link a seguir no seu navegador:</p>" +
                "    <p style=\"word-break: break-all; color: #1a73e8;\">%s</p>" +
                "    <hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\" />" +
                "    <p style=\"font-size: 12px; color: #777;\">" +
                "      Se você não realizou essa solicitação, nenhuma ação é necessária e seu acesso permanece seguro.<br />" +
                "      Esta é uma mensagem automática. Caso tenha alguma dúvida ou precise de ajuda, basta responder a este e-mail." +
                "    </p>" +
                "  </div>" +
                "</body>" +
                "</html>",
                userName, resetUrl, resetUrl
        );

        var resp = amazonSesService.sendEmail(user.getEmail(), subject, htmlBody, textBody, replyTo);
        log.info("sendPasswordResetEmail() - END - E-mail enviado com sucesso para: {}, code: {}", user.getEmail(), resp);
    }
}
