package company.aura.erp.email.service;

import company.aura.erp.email.domain.template.EmailTemplate;
import company.aura.erp.email.enumerate.NotificationType;
import company.aura.erp.email.exception.NotificationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class TemplateResolverService {


    public String getTextMessage(NotificationType type){
        return switch (type){
            case INSTALLMENT_NOTIFY -> getInstallmentMessage();
            case FORGOT_PASSWORD -> getChangePassWordMessage();
            default -> throw new NotificationException("Invalid notification type");
        };
    }

    public EmailTemplate getEmailTemplate(NotificationType type){
        return switch (type){
            case INSTALLMENT_NOTIFY -> getInstallmentEmailTemplate();
            case FORGOT_PASSWORD -> getChangePassEmailTemplate();
            default -> throw new NotificationException("Invalid notification type");
        };
    }












    // ----------------------------------------- INSTALLMENT -----------------------------------

    private String getInstallmentMessage(){
        return """
        Olá, ${customerName}!

        Lembramos que a parcela ${installmentNumber} do pedido #${orderId} vencerá (${installmentDueDate}).

        Valor Pendente: R$ ${amount}
        Forma de Pagamento: ${paymentMethod}
        
        Caso já tenha realizado o pagamento, por favor desconsidere este aviso.

        Atenciosamente,
        Equipe Ajeite""";
    }

    private EmailTemplate getInstallmentEmailTemplate(){
        return EmailTemplate.builder()
                .subject("Aviso de Vencimento - Pedido ${orderId} - Parcela ${installmentNumber}")
                .html(getInstallmentEmailHtml())
                .build();
    }

    private String getInstallmentEmailHtml(){
        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
        </head>
        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
          <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
            <h2 style="color: #1a73e8; margin-top: 0;">Lembrete de Vencimento de Parcela</h2>
            <p>Olá, <strong>${customerName}</strong>!</p>
            <p>Informamos que a parcela <strong>${installmentNumber}</strong> referente ao pedido <strong>#${orderId}</strong> vencerá em breve:</p>
            <div style="background-color: #f8f9fa; padding: 15px; border-left: 4px solid #1a73e8; margin: 15px 0;">
              <p style="margin: 5px 0;"><strong>Data de Vencimento:</strong> ${installmentDueDate}</p>
              <p style="margin: 5px 0;"><strong>Valor a Pagar:</strong> R$ ${amount}</p>
              <p style="margin: 5px 0;"><strong>Forma de Pagamento:</strong> ${paymentMethod}</p>
              ${bankSlipCodeBlock}
            </div>
            ${bankSlipUrlBlock}
            <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;" />
            <p style="font-size: 12px; color: #777;">
              Caso já tenha efetuado o pagamento, por favor desconsidere este e-mail.<br />
              Esta é uma mensagem automática de cobrança do sistema Aura ERP.
            </p>
          </div>
        </body>
        </html>""";
    }










    // ----------------------------------------- CHANGE PASSWORD -----------------------------------

    private String getChangePassWordMessage(){
        return """
        Olá, ${customerName}!

        Recebemos uma solicitação para redefinir a sua senha no Aura ERP.
        Para redefinir sua senha, acesse o link abaixo:

        ${changePasswordUrl}

        Se você não solicitou a redefinição de senha, ignore esta mensagem.
        Caso precise de suporte, basta responder a este e-mail.

        Atenciosamente,
        Equipe Ajeite""";
    }

    private EmailTemplate getChangePassEmailTemplate(){
        return EmailTemplate.builder()
                .subject("Redefinição de Senha - Ajeite")
                .html(getChangePassEmailHtml())
                .build();
    }

    private String getChangePassEmailHtml(){
        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"></head>
        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
          <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
            <h2 style="color: #1a73e8;">Recuperação de Senha</h2>
            <p>Olá, <strong>${customerName}</strong>!</p>
            <p>Recebemos uma solicitação para redefinir a sua senha no sistema <strong>Aura ERP</strong>.</p>
            <p style="margin: 24px 0;">
              <a href="${changePasswordUrl}" style="background-color: #1a73e8; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block; font-weight: bold;">
                Redefinir Minha Senha
              </a>
            </p>
            <p>Ou copie e cole o link a seguir no seu navegador:</p>
            <p style="word-break: break-all; color: #1a73e8;">${changePasswordUrl}</p>
            <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;" />
            <p style="font-size: 12px; color: #777;">
              Se você não realizou essa solicitação, nenhuma ação é necessária e seu acesso permanece seguro.<br />
              Esta é uma mensagem automática. Caso tenha alguma dúvida ou precise de ajuda, basta responder a este e-mail.
            </p>
          </div>
        </body>
        </html>""";
    }
}
