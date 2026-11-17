package company.aura.erp.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethod {
    PIX("Pix"),
    CREDIT_CARD("Cartão de Crédito"),
    DEBIT_CARD("Cartão de Débito"),
    CASH("Dinheiro físico"),
    BOLETO("Boleto"),
    ON_ACCOUNT("Depósito em conta"),
    OTHER("Outros");

    private String humanDescription;

}
