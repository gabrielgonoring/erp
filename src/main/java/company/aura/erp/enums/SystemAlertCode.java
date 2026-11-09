package company.aura.erp.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SystemAlertCode {

    NEED_CHANGE_PASSWORD("0001");

    private String code;
}
