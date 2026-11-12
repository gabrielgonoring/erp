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
import org.jspecify.annotations.NonNull;
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
        OrderInstallmentEntity installment = findInstallment(order, installmentId);

        validateOrderInstallmentToPay(order, installment);

        List<PaymentTransactionEntity> existingTransactions = paymentTransactionRepository.findByInstallmentId(installmentId);

        BigDecimal currentPaymentAmount = retrieveCurrentPaymentAmount(existingTransactions);

        BigDecimal totalAmount = installment.getAmount() != null ? installment.getAmount() : BigDecimal.ZERO;

        validateRemainingValue(request, totalAmount, currentPaymentAmount);

        BigDecimal newTotalPaid = currentPaymentAmount.add(request.amountPaid());

        installment.setStatus(getInstallmentStatusAfterPayment(newTotalPaid, totalAmount));
        installment.setPendingAmount(totalAmount.subtract(newTotalPaid));

        OrderInstallmentEntity savedInstallment = orderInstallmentRepository.save(installment);

        List<PaymentTransactionEntity> allTransactions = new ArrayList<>(existingTransactions);
        PaymentTransactionEntity savedTransaction = savePaymentTransaction(order, request, installment);
        allTransactions.add(savedTransaction);

        return OrderInstallmentMapper.fromEntity(savedInstallment, allTransactions);
    }

    private OrderInstallmentEntity findInstallment(OrderEntity order, Long installmentId) {
        return orderInstallmentRepository.findByIdAndCompanyId(installmentId, order.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parcela não encontrada com ID: " + installmentId));
    }

    private InstallmentStatus getInstallmentStatusAfterPayment(BigDecimal newTotalPaid, BigDecimal installmentAmount) {
        if (newTotalPaid.compareTo(installmentAmount) >= 0) {
            return InstallmentStatus.PAID;
        } else {
            return InstallmentStatus.PARTIALLY_PAID;
        }
    }

    private PaymentTransactionEntity savePaymentTransaction(OrderEntity order, PaymentTransactionRequest request, OrderInstallmentEntity installment) {

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

        return paymentTransactionRepository.save(transaction);
    }

    private void validateRemainingValue(PaymentTransactionRequest request, BigDecimal installmentAmount, BigDecimal currentPaid) {
        BigDecimal remaining = installmentAmount.subtract(currentPaid);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0 ) {
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
    }

    private BigDecimal retrieveCurrentPaymentAmount(List<PaymentTransactionEntity> existingTransactions) {
        return existingTransactions.stream()
                .map(PaymentTransactionEntity::getAmountPaid)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateOrderInstallmentToPay(OrderEntity order, OrderInstallmentEntity installment) {
        if (!installment.getOrderId().equals(order.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A parcela não pertence a este pedido.");
        }

        if (installment.getStatus() == InstallmentStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível registrar pagamento para uma parcela cancelada.");
        }

        if (installment.getStatus() == InstallmentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível registrar pagamento para uma parcela já paga.");
        }


    }

    public OrderInstallmentResponse addInstallment(OrderEntity order, OrderInstallmentCreateRequest request) {

        int installmentNumber = getInstallmentNumber(order, request);

        boolean isPaid = Boolean.TRUE.equals(request.paid());

        BigDecimal amount = request.amount() != null ? request.amount() : BigDecimal.ZERO;
        BigDecimal pendingAmount = isPaid ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amount.setScale(2, RoundingMode.HALF_UP);

        OrderInstallmentEntity saved = saveNewInstallment(order, request, installmentNumber, amount, pendingAmount, isPaid);

        if (isPaid) {
            PaymentTransactionEntity savedTx = savePaymentTransactionFromNewInstallment(order, request, saved);
            return OrderInstallmentMapper.fromEntity(saved, List.of(savedTx));
        }

        return OrderInstallmentMapper.fromEntity(saved, Collections.emptyList());
    }

    private PaymentTransactionEntity savePaymentTransactionFromNewInstallment(OrderEntity order, OrderInstallmentCreateRequest request, OrderInstallmentEntity saved) {
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
        return paymentTransactionRepository.save(transaction);
    }

    private OrderInstallmentEntity saveNewInstallment(OrderEntity order, OrderInstallmentCreateRequest request, int installmentNumber, BigDecimal amount, BigDecimal pendingAmount, boolean isPaid) {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .orderId(order.getId())
                .companyId(order.getCompanyId())
                .partnerId(order.getPartnerId())
                .installmentNumber(installmentNumber)
                .dueDate(request.dueDate())
                .amount(amount)
                .pendingAmount(pendingAmount)
                .paymentMethod(request.paymentMethod())
                .status(isPaid ? InstallmentStatus.PAID : InstallmentStatus.PENDING)
                .bankSlipCode(request.bankSlipCode())
                .bankSlipUrl(request.bankSlipUrl())
                .notes(request.notes())
                .build();

        return orderInstallmentRepository.save(installment);
    }

    private int getInstallmentNumber(OrderEntity order, OrderInstallmentCreateRequest request) {
        int installmentNumber;

        if (request.installmentNumber() != null && request.installmentNumber() > 0) {
            installmentNumber = request.installmentNumber();
        } else {
            Integer maxCurrentInstallmentNumber = orderInstallmentRepository.findMaxInstallmentNumberByOrderId(order.getId());
            installmentNumber = maxCurrentInstallmentNumber + 1;
        }
        return installmentNumber;
    }

    public OrderInstallmentResponse cancelInstallment(OrderEntity order, Long installmentId) {
        OrderInstallmentEntity installment = findInstallment(order, installmentId);

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
        installment.setPendingAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
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
        boolean isPaid = installmentRequest.paid();
        BigDecimal amount = installmentRequest.amount() != null ? installmentRequest.amount() : BigDecimal.ZERO;
        BigDecimal pendingAmount = isPaid ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amount.setScale(2, RoundingMode.HALF_UP);

        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .orderId(order.getId())
                .companyId(order.getCompanyId())
                .partnerId(order.getPartnerId())
                .installmentNumber(installmentRequest.installmentNumber())
                .dueDate(installmentRequest.dueDate())
                .amount(amount)
                .pendingAmount(pendingAmount)
                .paymentMethod(installmentRequest.paymentMethod())
                .status(isPaid ? InstallmentStatus.PAID : InstallmentStatus.PENDING)
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
