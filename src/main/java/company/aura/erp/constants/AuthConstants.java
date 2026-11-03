package company.aura.erp.constants;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Setter
@Getter
public class AuthConstants {

    @Value("${auth.reset-password.base-url}")
    private String resetPasswordBaseUrl;

    @Value("${auth.reset-password.expiration-minutes}")
    private long resetPasswordExpirationMinutes = 60;
}
