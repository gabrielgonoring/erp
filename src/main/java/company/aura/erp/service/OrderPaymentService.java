package company.aura.erp.service;

import company.aura.erp.dto.order.*;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderInstallmentEntity;
import company.aura.erp.entity.PaymentTransactionEntity;
import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.mapper.OrderInstallmentMapper;
import company.aura.erp.repository.OrderInstallmentRepository;
import company.aura.erp.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

    public List<OrderInstallmentEntity> findInstallments(Long orderId) {
        return orderInstallmentRepository.findByOrderId(orderId);
    }

    public List<OrderInstallmentResponse> findInstallmentResponses(Long orderId) {
        List<OrderInstallmentEntity> installments = orderInstallmentRepository.findByOrderId(orderId);
        return installments.stream()
                .map(installment -> {
                    List<PaymentTransactionEntity> transactions = paymentTransactionRepository.findByInstallmentId(installment.getId());
                    return OrderInstallmentMapper.fromEntity(installment, transactions);
                })
                .toList();
    }

    public OrderInstallmentResponse payInstallment(OrderEntity order, Long installmentId, PaymentTransactionRequest request) {
        OrderInstallmentEntity installment = orderInstallmentRepository.findByIdAndCompanyId(installmentId, order.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parcela não encontrada com ID: " + installmentId));

        if (!installment.getOrderId().equals(order.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A parcela não pertence a este pedido.");
        }

        if (installment.getStatus() == InstallmentStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível registrar pagamento para uma parcela cancelada.");
        }

        List<PaymentTransactionEntity> existingTransactions = paymentTransactionRepository.findByInstallmentId(installmentId);
        BigDecimal currentPaid = existingTransactions.stream()
                .map(PaymentTransactionEntity::getAmountPaid)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal installmentAmount = installment.getAmount() != null ? installment.getAmount() : BigDecimal.ZERO;
        BigDecimal remaining = installmentAmount.subtract(currentPaid);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0 || installment.getStatus() == InstallmentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "A parcela já está totalmente paga.");
        }

        if (request.amountPaid().compareTo(remaining) > 0) {
            BigDecimal requestAmountFormatted = request.amountPaid().setScale(2, RoundingMode.HALF_UP);
            BigDecimal remainingFormatted = remaining.setScale(2, RoundingMode.HALF_UP);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("O valor do pagamento (R$ %s) excede o saldo restante da parcela (R$ %s).",
                            requestAmountFormatted, remainingFormatted)
            );
        }

        LocalDateTime paidAt = request.paidAt() != null ? request.paidAt() : LocalDateTime.now();
        PaymentTransactionEntity transaction = PaymentTransactionEntity.builder()
                .installmentId(installment.getId())
                .companyId(order.getCompanyId())
                .paidAt(paidAt)
                .amountPaid(request.amountPaid())
                .paymentMethod(request.paymentMethod())
                .cardBrand(request.cardBrand())
                .nsuAuthorization(request.nsuAuthorization())
                .notes(request.notes())
                .build();
        PaymentTransactionEntity savedTransaction = paymentTransactionRepository.save(transaction);

        BigDecimal newTotalPaid = currentPaid.add(request.amountPaid());
        if (newTotalPaid.compareTo(installmentAmount) >= 0) {
            installment.setStatus(InstallmentStatus.PAID);
        } else {
            installment.setStatus(InstallmentStatus.PARTIALLY_PAID);
        }
        OrderInstallmentEntity savedInstallment = orderInstallmentRepository.save(installment);

        List<PaymentTransactionEntity> allTransactions = new ArrayList<>(existingTransactions);
        allTransactions.add(savedTransaction);

        return OrderInstallmentMapper.fromEntity(savedInstallment, allTransactions);
    }

    public OrderInstallmentResponse addInstallment(OrderEntity order, OrderInstallmentCreateRequest request) {
        List<OrderInstallmentEntity> existing = orderInstallmentRepository.findByOrderId(order.getId());
        int installmentNumber;
        if (request.installmentNumber() != null && request.installmentNumber() > 0) {
            installmentNumber = request.installmentNumber();
        } else {
            installmentNumber = existing.stream()
                    .mapToInt(OrderInstallmentEntity::getInstallmentNumber)
                    .max()
                    .orElse(0) + 1;
        }

        boolean isPaid = Boolean.TRUE.equals(request.paid());

        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .orderId(order.getId())
                .companyId(order.getCompanyId())
                .partnerId(order.getPartnerId())
                .installmentNumber(installmentNumber)
                .dueDate(request.dueDate())
                .amount(request.amount())
                .paymentMethod(request.paymentMethod())
                .status(isPaid ? InstallmentStatus.PAID : InstallmentStatus.PENDING)
                .bankSlipCode(request.bankSlipCode())
                .bankSlipUrl(request.bankSlipUrl())
                .notes(request.notes())
                .build();

        OrderInstallmentEntity saved = orderInstallmentRepository.save(installment);

        if (isPaid) {
            PaymentTransactionEntity transaction = PaymentTransactionEntity.builder()
                    .installmentId(saved.getId())
                    .companyId(order.getCompanyId())
                    .paidAt(LocalDateTime.now())
                    .amountPaid(saved.getAmount())
                    .paymentMethod(saved.getPaymentMethod())
                    .cardBrand(request.cardBrand())
                    .nsuAuthorization(request.nsuAuthorization())
                    .notes(request.notes())
                    .build();
            PaymentTransactionEntity savedTx = paymentTransactionRepository.save(transaction);
            return OrderInstallmentMapper.fromEntity(saved, List.of(savedTx));
        }

        return OrderInstallmentMapper.fromEntity(saved, Collections.emptyList());
    }

    public OrderInstallmentResponse cancelInstallment(OrderEntity order, Long installmentId) {
        OrderInstallmentEntity installment = orderInstallmentRepository.findByIdAndCompanyId(installmentId, order.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parcela não encontrada com ID: " + installmentId));

        if (!installment.getOrderId().equals(order.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A parcela não pertence a este pedido.");
        }

        if (installment.getStatus() == InstallmentStatus.PAID || installment.getStatus() == InstallmentStatus.PARTIALLY_PAID) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível cancelar uma parcela que já possui pagamentos.");
        }

        List<PaymentTransactionEntity> existingTransactions = paymentTransactionRepository.findByInstallmentId(installmentId);
        if (!existingTransactions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível cancelar uma parcela que possui transações de pagamento.");
        }

        installment.setStatus(InstallmentStatus.CANCELED);
        OrderInstallmentEntity saved = orderInstallmentRepository.save(installment);

        return OrderInstallmentMapper.fromEntity(saved, Collections.emptyList());
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
