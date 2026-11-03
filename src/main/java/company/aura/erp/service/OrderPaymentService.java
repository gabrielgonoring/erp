package company.aura.erp.service;

import company.aura.erp.dto.order.*;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderInstallmentEntity;
import company.aura.erp.entity.PaymentTransactionEntity;
import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.repository.OrderInstallmentRepository;
import company.aura.erp.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPaymentService {

    private final OrderInstallmentRepository orderInstallmentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public List<OrderInstallmentEntity> processInstallments(OrderCloseRequest request, OrderEntity order) {

        validateOrderPayment(request, order);

        return request.installments().stream()
                .map(installmentRequest -> processSingleInstallment(order, installmentRequest))
                .toList();
        
    }

    private void validateOrderPayment(OrderCloseRequest request, OrderEntity order) {

        BigDecimal totalInstallments = request.installments().stream()
                .map(OrderInstallmentRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalInstallments.compareTo(order.getTotalAmount()) != 0) {
            BigDecimal installmentsFormatted = totalInstallments.setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalOrderFormatted = order.getTotalAmount() != null
                    ? order.getTotalAmount().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("A soma das parcelas (R$ %s) não confere com o valor total do pedido (R$ %s).",
                            installmentsFormatted, totalOrderFormatted)
            );
        }
    }


    private OrderInstallmentEntity processSingleInstallment(OrderEntity order, OrderInstallmentRequest installmentRequest) {

        OrderInstallmentEntity savedInstallment = saveInstallment(order, installmentRequest);

        if (installmentRequest.paid()) {
            savePaymentTransaction(order, installmentRequest, savedInstallment);
        }

        return savedInstallment;
    }

    private OrderInstallmentEntity saveInstallment(OrderEntity order, OrderInstallmentRequest installmentRequest) {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .orderId(order.getId())
                .companyId(order.getCompanyId())
                .partnerId(order.getPartnerId())
                .installmentNumber(installmentRequest.installmentNumber())
                .dueDate(installmentRequest.dueDate())
                .amount(installmentRequest.amount())
                .paymentMethod(installmentRequest.paymentMethod())
                .status(installmentRequest.paid() ? InstallmentStatus.PAID : InstallmentStatus.PENDING)
                .bankSlipCode(installmentRequest.bankSlipCode())
                .bankSlipUrl(installmentRequest.bankSlipUrl())
                .notes(installmentRequest.notes())
                .build();

        return orderInstallmentRepository.save(installment);
    }

    private void savePaymentTransaction(OrderEntity order, OrderInstallmentRequest installmentRequest, OrderInstallmentEntity savedInstallment) {
        PaymentTransactionEntity transaction = PaymentTransactionEntity.builder()
                .installmentId(savedInstallment.getId())
                .companyId(order.getCompanyId())
                .paidAt(LocalDateTime.now())
                .amountPaid(savedInstallment.getAmount())
                .paymentMethod(savedInstallment.getPaymentMethod())
                .cardBrand(installmentRequest.cardBrand())
                .nsuAuthorization(installmentRequest.nsuAuthorization())
                .notes(installmentRequest.notes())
                .build();

        paymentTransactionRepository.save(transaction);
    }

}
