package company.aura.erp.modules.invoice.enumerate;

import company.aura.erp.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SefazPaymentCode {
    DINHEIRO("01", "Dinheiro", PaymentMethod.CASH),
    CHEQUE("02", "Cheque", PaymentMethod.OTHER),
    CARTAO_CREDITO("03", "Cartão de Crédito", PaymentMethod.CREDIT_CARD),
    CARTAO_DEBITO("04", "Cartão de Débito", PaymentMethod.DEBIT_CARD),
    CREDITO_LOJA("05", "Crédito Loja", PaymentMethod.OTHER),
    VALE_ALIMENTACAO("10", "Vale Alimentação", PaymentMethod.OTHER),
    VALE_REFEICAO("11", "Vale Refeição", PaymentMethod.OTHER),
    VALE_PRESENTE("12", "Vale Presente", PaymentMethod.OTHER),
    VALE_COMBUSTIVEL("13", "Vale Combustível", PaymentMethod.OTHER),
    DUPLICATA_MERCANTIL("14", "Duplicata Mercantil", PaymentMethod.BOLETO),
    BOLETO_BANCARIO("15", "Boleto Bancário", PaymentMethod.BOLETO),
    DEPOSITO_BANCARIO("16", "Depósito Bancário", PaymentMethod.ON_ACCOUNT),
    PIX_DINAMICO("17", "Pagamento Instantâneo (PIX) - Dinâmico", PaymentMethod.PIX),
    TRANSFERENCIA_BANCARIA("18", "Transferência bancária / Carteira Digital", PaymentMethod.ON_ACCOUNT),
    PROGRAMA_FIDELIDADE("19", "Programa de fidelidade / Cashback / Crédito Virtual", PaymentMethod.OTHER),
    PIX_ESTATICO("20", "Pagamento Instantâneo (PIX) - Estático", PaymentMethod.PIX),
    CREDITO_EM_LOJA("21", "Crédito em Loja", PaymentMethod.OTHER),
    PAGAMENTO_ELETRONICO("22", "Pagamento Eletrônico não Informado", PaymentMethod.OTHER),
    SEM_PAGAMENTO("90", "Sem Pagamento", PaymentMethod.OTHER),
    OUTROS("99", "Outros", PaymentMethod.OTHER);

    private final String code;
    private final String description;
    private final PaymentMethod erpPaymentMethod;

    public static SefazPaymentCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            return OUTROS;
        }
        String normalized = code.trim();
        return Arrays.stream(values())
                .filter(item -> item.code.equals(normalized))
                .findFirst()
                .orElse(OUTROS);
    }
}
