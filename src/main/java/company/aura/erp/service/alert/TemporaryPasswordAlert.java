package company.aura.erp.service.alert;

import company.aura.erp.dto.dashboard.SystemAlertResponse;
import company.aura.erp.enums.SystemAlertCode;
import company.aura.erp.security.UserPrincipal;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Order(0)
@Service
public class TemporaryPasswordAlert implements AlertService {

    @Override
    public SystemAlertResponse getAlert(UserPrincipal userPrincipal) {
        if (userPrincipal != null && Boolean.TRUE.equals(userPrincipal.getTemporaryPassword())) {
            return SystemAlertResponse.builder()
                    .message("Por favor, altere sua senha temporária.")
                    .code(SystemAlertCode.NEED_CHANGE_PASSWORD.getCode())
                    .build();
        }
        return null;
    }
}
