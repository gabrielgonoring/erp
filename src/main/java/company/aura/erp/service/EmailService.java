package company.aura.erp.service;

import company.aura.erp.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    /**
     * Envia e-mail de recuperação de senha contendo a URL de redefinição.
     * Implementação pendente: atualmente registra apenas em log a URL gerada.
     *
     * @param user  Endereço de e-mail do destinatário
     * @param resetUrl URL completa para redefinição de senha
     */
    public void sendPasswordResetEmail(UserEntity user, String resetUrl) {
        // TODO: Implementar envio real de e-mail (ex: JavaMailSender, SendGrid, Amazon SES, etc.)
        log.info("Envio de e-mail de recuperação de senha para '{}': {}", user.getEmail(), resetUrl);
    }
}
