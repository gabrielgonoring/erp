package company.aura.erp.service;

import company.aura.erp.constants.BucketFolders;
import company.aura.erp.dto.order.*;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderInstallmentEntity;
import company.aura.erp.entity.PaymentTransactionEntity;
import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.mapper.OrderInstallmentMapper;
import company.aura.erp.repository.OrderInstallmentRepository;
import company.aura.erp.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentService {

    private final OrderInstallmentRepository orderInstallmentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CloudflareR2Service r2Service;

    public List<OrderInstallmentEntity> processInstallments(OrderCloseRequest request, OrderEntity order) {
        Long orderId = order != null ? order.getId() : null;
        log.info("processInstallments() - START - orderId={}, totalInstallments={}", orderId, request != null && request.installments() != null ? request.installments().size() : 0);

        validateOrderPayment(request, order);

        List<OrderInstallmentEntity> list = request.installments().stream()
                .map(installmentRequest -> processSingleInstallment(order, installmentRequest))
                .toList();

        log.info("processInstallments() - END - orderId={}, processedCount={}", orderId, list.size());
        return list;
    }

    public List<OrderInstallmentEntity> findInstallments(Long orderId) {
        log.info("findInstallments() - START - orderId={}", orderId);
        List<OrderInstallmentEntity> installments = orderInstallmentRepository.findByOrderId(orderId);
        log.info("findInstallments() - END - orderId={}, totalInstallments={}", orderId, installments.size());
        return installments;
    }

    public List<OrderInstallmentResponse> findInstallmentResponses(Long orderId) {
        log.info("findInstallmentResponses() - START - orderId={}", orderId);
        List<OrderInstallmentEntity> installments = orderInstallmentRepository.findByOrderId(orderId);
        List<OrderInstallmentResponse> responses = installments.stream()
                .map(installment -> {
                    List<PaymentTransactionEntity> transactions = paymentTransactionRepository.findByInstallmentId(installment.getId());
                    return OrderInstallmentMapper.fromEntity(installment, transactions);
                })
                .toList();
        log.info("findInstallmentResponses() - END - orderId={}, totalResponses={}", orderId, responses.size());
        return responses;
    }

    public OrderInstallmentResponse payInstallment(OrderEntity order, Long installmentId, PaymentTransactionRequest request) {
        Long orderId = order != null ? order.getId() : null;
        log.info("payInstallment() - START - orderId={}, installmentId={}, amountPaid={}", orderId, installmentId, request != null ? request.amountPaid() : null);

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

        OrderInstallmentResponse response = OrderInstallmentMapper.fromEntity(savedInstallment, allTransactions);
        log.info("payInstallment() - END - orderId={}, installmentId={}, status={}", orderId, installmentId, response != null ? response.getStatus() : null);
        return response;
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
        Long orderId = order != null ? order.getId() : null;
        log.info("addInstallment() - START - orderId={}, amount={}, paid={}", orderId, request != null ? request.amount() : null, request != null ? request.paid() : null);

        int installmentNumber = getInstallmentNumber(order, request);

        boolean isPaid = Boolean.TRUE.equals(request.paid());

        BigDecimal amount = request.amount() != null ? request.amount() : BigDecimal.ZERO;
        BigDecimal pendingAmount = isPaid ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amount.setScale(2, RoundingMode.HALF_UP);

        OrderInstallmentEntity saved = saveNewInstallment(order, request, installmentNumber, amount, pendingAmount, isPaid);

        if (isPaid) {
            PaymentTransactionEntity savedTx = savePaymentTransactionFromNewInstallment(order, request, saved);
            OrderInstallmentResponse response = OrderInstallmentMapper.fromEntity(saved, List.of(savedTx));
            log.info("addInstallment() - END - orderId={}, installmentId={}, status={}", orderId, response != null ? response.getId() : null, response != null ? response.getStatus() : null);
            return response;
        }

        OrderInstallmentResponse response = OrderInstallmentMapper.fromEntity(saved, Collections.emptyList());
        log.info("addInstallment() - END - orderId={}, installmentId={}, status={}", orderId, response != null ? response.getId() : null, response != null ? response.getStatus() : null);
        return response;
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
        Long orderId = order != null ? order.getId() : null;
        log.info("cancelInstallment() - START - orderId={}, installmentId={}", orderId, installmentId);

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

        OrderInstallmentResponse response = OrderInstallmentMapper.fromEntity(saved, Collections.emptyList());
        log.info("cancelInstallment() - END - orderId={}, installmentId={}, status={}", orderId, installmentId, response != null ? response.getStatus() : null);
        return response;
    }

    public OrderInstallmentResponse uploadBankSlip(OrderEntity order, Long installmentId, MultipartFile file, String bankSlipCode) {
        log.info("uploadBankSlip() - START - orderId={}, installmentId={}, originalFilename={}", order.getId(), installmentId, file != null ? file.getOriginalFilename() : null);

        OrderInstallmentEntity installment = findInstallment(order, installmentId);

        validateUploadBankSlip(order, file, installment);

        if (isBackSlipAlreadyLinked(installment)) {
            deleteOldBankSlip(installment);
        }

        String folder = BucketFolders.BANK_SLIP + order.getCompanyId() + "/" + order.getId();
        String fileKey = r2Service.upload(file, folder);
        String fileUrl = r2Service.getFileUrl(fileKey);

        installment.setBankSlipUrl(fileUrl);

        if (bankSlipCode != null && !bankSlipCode.isBlank()) {
            installment.setBankSlipCode(bankSlipCode.trim());
        }

        OrderInstallmentEntity saved = orderInstallmentRepository.save(installment);
        List<PaymentTransactionEntity> transactions = paymentTransactionRepository.findByInstallmentId(installmentId);
        OrderInstallmentResponse response = OrderInstallmentMapper.fromEntity(saved, transactions);

        log.info("uploadBankSlip() - END - orderId={}, installmentId={}, bankSlipUrl={}", order.getId(), installmentId, fileUrl);
        return response;
    }

    private boolean isBackSlipAlreadyLinked(OrderInstallmentEntity installment) {
        return installment.getBankSlipUrl() != null && !installment.getBankSlipUrl().isBlank();
    }

    private void validateUploadBankSlip(OrderEntity order, MultipartFile file, OrderInstallmentEntity installment) {
        if (!installment.getOrderId().equals(order.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A parcela não pertence a este pedido.");
        }

        if (installment.getStatus() == InstallmentStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível anexar boleto a uma parcela cancelada.");
        }

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo do boleto é obrigatório.");
        }
    }

    private void deleteOldBankSlip(OrderInstallmentEntity installment) {
        try {
            String oldKey = r2Service.extractKeyFromUrl(installment.getBankSlipUrl());
            r2Service.delete(oldKey);
        } catch (Exception ex) {
            log.warn("uploadBankSlip() - WARN - Falha ao remover boleto anterior no bucket: {}", ex.getMessage());
        }
    }

    public String getBankSlipUrl(OrderEntity order, Long installmentId) {
        Long orderId = order != null ? order.getId() : null;
        log.info("getBankSlipUrl() - START - orderId={}, installmentId={}", orderId, installmentId);

        OrderInstallmentEntity installment = findInstallment(order, installmentId);

        if (!installment.getOrderId().equals(order.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A parcela não pertence a este pedido.");
        }

        String bankSlipUrl = installment.getBankSlipUrl();
        if (bankSlipUrl == null || bankSlipUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo do boleto não encontrado para esta parcela.");
        }

        String temporaryUrl = r2Service.generateTemporaryUrl(bankSlipUrl, Duration.ofMinutes(5));
        log.info("getBankSlipUrl() - END - orderId={}, installmentId={}, temporaryUrl={}", orderId, installmentId, temporaryUrl);
        return temporaryUrl;
    }

    public void deleteBankSlip(OrderEntity order, Long installmentId) {
        log.info("deleteBankSlip() - START - orderId={}, installmentId={}", order.getId(), installmentId);

        OrderInstallmentEntity installment = findInstallment(order, installmentId);

        validateDeleteBankSlip(order, installment);

        deleteOldBankSlip(installment);

        installment.setBankSlipUrl(null);
        installment.setBankSlipCode(null);

        orderInstallmentRepository.save(installment);

        log.info("deleteBankSlip() - END - orderId={}, installmentId={}", order.getId(), installmentId);
    }

    private void validateDeleteBankSlip(OrderEntity order, OrderInstallmentEntity installment) {
        if (!installment.getOrderId().equals(order.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A parcela não pertence a este pedido.");
        }

        if (installment.getStatus() == InstallmentStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível remover boleto de uma parcela cancelada.");
        }

        if (!isBackSlipAlreadyLinked(installment)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo do boleto não encontrado para esta parcela.");
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderInstallmentSummaryResponse> listInstallments(
            String search,
            LocalDate startDate,
            LocalDate endDate,
            List<InstallmentStatus> statuses,
            Long companyId,
            Pageable pageable
    ) {
        log.info("listInstallments() - START - companyId={}, search={}, startDate={}, endDate={}, statuses={}",
                companyId, search, startDate, endDate, statuses);

        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        boolean hasDateRange = (startDate != null && endDate != null);
        boolean hasStartDateOnly = (startDate != null && endDate == null);
        boolean hasEndDateOnly = (startDate == null && endDate != null);

        if (hasDateRange && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser posterior à data final.");
        }

        boolean hasStatuses = (statuses != null && !statuses.isEmpty());
        List<InstallmentStatus> effectiveStatuses = hasStatuses ? statuses : List.of(InstallmentStatus.PENDING);

        Page<OrderInstallmentSummaryResponse> page = orderInstallmentRepository.searchSummarized(
                companyId,
                searchTerm,
                hasDateRange,
                hasStartDateOnly,
                hasEndDateOnly,
                startDate,
                endDate,
                hasStatuses,
                effectiveStatuses,
                pageable
        );

        log.info("listInstallments() - END - companyId={}, totalElements={}, totalPages={}",
                companyId, page.getTotalElements(), page.getTotalPages());
        return page;
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
