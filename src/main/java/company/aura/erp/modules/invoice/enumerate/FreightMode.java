package company.aura.erp.modules.invoice.enumerate;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum FreightMode {
    SENDER(0, "Contratação do Frete por conta do Remetente (CIF)"),
    RECIPIENT(1, "Contratação do Frete por conta do Destinatário (FOB)"),
    THIRD_PARTY(2, "Contratação do Frete por conta de Terceiros"),
    OWN_BY_SENDER(3, "Transporte Próprio por conta do Remetente"),
    OWN_BY_RECIPIENT(4, "Transporte Próprio por conta do Destinatário"),
    NO_FREIGHT(9, "Sem Ocorrência de Transporte");

    private final Integer code;
    private final String description;

    public static FreightMode fromCode(Integer code) {
        if (code == null) {
            return NO_FREIGHT;
        }
        return Arrays.stream(values())
                .filter(mode -> mode.code.equals(code))
                .findFirst()
                .orElse(NO_FREIGHT);
    }
}
