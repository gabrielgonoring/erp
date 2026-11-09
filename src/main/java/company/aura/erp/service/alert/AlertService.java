package company.aura.erp.service.alert;

import company.aura.erp.dto.dashboard.SystemAlertResponse;
import company.aura.erp.security.UserPrincipal;

public interface AlertService {
    SystemAlertResponse getAlert(UserPrincipal userPrincipal);
}
