package company.aura.erp.service;

import company.aura.erp.constants.BucketFolders;
import company.aura.erp.dto.payable.PayableRequest;
import company.aura.erp.dto.payable.PayableResponse;
import company.aura.erp.dto.payable.PayableSummaryResponse;
import company.aura.erp.dto.payable.PayableTransactionRequest;
import company.aura.erp.entity.CommissionEntryEntity;
import company.aura.erp.entity.CommissionStatementEntity;
import company.aura.erp.entity.PayableEntity;
import company.aura.erp.entity.PayableTransactionEntity;
import company.aura.erp.enums.*;
import company.aura.erp.mapper.PayableMapper;
import company.aura.erp.mapper.PayableTransactionMapper;
import company.aura.erp.modules.bucket.service.CloudflareR2Service;
import company.aura.erp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayableService {

    private final PayableRepository payableRepository;
    private final PayableTransactionRepository payableTransactionRepository;
    private final PartnerRepository partnerRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final CommissionStatementRepository commissionStatementRepository;
    private final CommissionEntryRepository commissionEntryRepository;
    private final CloudflareR2Service r2Service;

    @Transactional
    public PayableResponse createPayable(Long companyId, PayableRequest request) {
        log.info("createPayable() - START - companyId={}, amount={}, partnerId={}, collaboratorId={}",
                companyId, request.getAmount(), request.getPartnerId(), request.getCollaboratorId());

        validateBeneficiary(companyId, request.getPartnerId(), request.getCollaboratorId());

        boolean isPaid = Boolean.TRUE.equals(request.getPaid());
        if (isPaid && request.getPaymentMethod() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A forma de pagamento é obrigatória quando a conta já foi paga.");
        }

        PayableEntity savedPayable = payableRepository.save(PayableMapper.toEntity(companyId, request));

        List<PayableTransactionEntity> transactions = isPaid
                ? List.of(payableTransactionRepository.save(PayableTransactionMapper.toEntity(companyId, savedPayable, request)))
                : Collections.emptyList();

        PayableResponse response = PayableMapper.fromEntity(savedPayable, transactions);
        log.info("createPayable() - END - payableId={}, status={}", response.getId(), response.getStatus());
        return response;
    }

    @Transactional
    public PayableResponse payPayable(Long companyId, Long payableId, PayableTransactionRequest request) {
        log.info("payPayable() - START - companyId={}, payableId={}, amountPaid={}",
                companyId, payableId, request != null ? request.getAmountPaid() : null);

        PayableEntity payable = findPayable(companyId, payableId);

        if (payable.getStatus() == PayableStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não é possível registrar movimentação para uma conta a pagar cancelada.");
        }

        PayableTransactionType transactionType = request.getTransactionType() != null
                ? request.getTransactionType()
                : PayableTransactionType.PAYMENT;

        List<PayableTransactionEntity> existingTransactions = payableTransactionRepository.findByPayableId(payableId);
        BigDecimal currentPaid = retrieveCurrentPaymentAmount(existingTransactions);
        BigDecimal totalAmount = payable.getAmount() != null ? payable.getAmount() : BigDecimal.ZERO;

        BigDecimal newTotalPaid;
        if (transactionType == PayableTransactionType.PAYMENT) {
            if (payable.getStatus() == PayableStatus.PAID) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Não é possível registrar pagamento para uma conta já paga.");
            }
            validateRemainingValue(request.getAmountPaid(), totalAmount, currentPaid);
            newTotalPaid = currentPaid.add(request.getAmountPaid());
        } else {
            validateReversalValue(request.getAmountPaid(), currentPaid);
            newTotalPaid = currentPaid.subtract(request.getAmountPaid());
        }

        payable.setStatus(resolveStatusAfterTransaction(newTotalPaid, totalAmount));
        payable.setPendingAmount(totalAmount.subtract(newTotalPaid).setScale(2, RoundingMode.HALF_UP));
        if (payable.getPaymentMethod() == null) {
            payable.setPaymentMethod(request.getPaymentMethod());
        }

        PayableEntity savedPayable = payableRepository.save(payable);
        PayableTransactionEntity savedTransaction = payableTransactionRepository.save(
                PayableTransactionMapper.toEntity(companyId, savedPayable.getId(), transactionType, request)
        );

        syncCommissionStatementPaymentStatus(companyId, savedPayable, savedTransaction.getPaidAt());

        PayableResponse response = PayableMapper.fromEntity(savedPayable, existingTransactions, savedTransaction);
        log.info("payPayable() - END - companyId={}, payableId={}, status={}", companyId, payableId, response.getStatus());
        return response;
    }

    @Transactional
    public PayableResponse cancelPayable(Long companyId, Long payableId) {
        log.info("cancelPayable() - START - companyId={}, payableId={}", companyId, payableId);

        PayableEntity payable = findPayable(companyId, payableId);

        if (payable.getStatus() == PayableStatus.PAID || payable.getStatus() == PayableStatus.PARTIALLY_PAID) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não é possível cancelar uma conta a pagar que já possui pagamentos.");
        }

        List<PayableTransactionEntity> existingTransactions = payableTransactionRepository.findByPayableId(payableId);
        if (!existingTransactions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não é possível cancelar uma conta a pagar que possui transações.");
        }

        payable.setStatus(PayableStatus.CANCELED);
        payable.setPendingAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        PayableEntity saved = payableRepository.save(payable);

        PayableResponse response = PayableMapper.fromEntity(saved);
        log.info("cancelPayable() - END - companyId={}, payableId={}, status={}", companyId, payableId, response.getStatus());
        return response;
    }

    @Transactional
    public PayableResponse uploadPaymentFile(Long companyId, Long payableId, MultipartFile file, Long transactionId) {
        log.info("uploadPaymentFile() - START - companyId={}, payableId={}, transactionId={}, filename={}",
                companyId, payableId, transactionId, file != null ? file.getOriginalFilename() : null);

        PayableEntity payable = findPayable(companyId, payableId);

        if (payable.getStatus() == PayableStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não é possível anexar comprovante a uma conta a pagar cancelada.");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo do comprovante é obrigatório.");
        }

        PayableTransactionEntity transaction = resolveTargetTransaction(companyId, payableId, transactionId);

        if (transaction.getReceiptUrl() != null && !transaction.getReceiptUrl().isBlank()) {
            deleteOldReceiptFile(transaction.getReceiptUrl());
        }

        String folder = BucketFolders.PAYABLE_FILE + companyId + "/" + payableId;
        String fileKey = r2Service.upload(file, folder);
        String fileUrl = r2Service.getFileUrl(fileKey);

        transaction.setReceiptUrl(fileUrl);
        payableTransactionRepository.save(transaction);

        List<PayableTransactionEntity> transactions = payableTransactionRepository.findByPayableId(payableId);
        PayableResponse response = PayableMapper.fromEntity(payable, transactions);

        log.info("uploadPaymentFile() - END - companyId={}, payableId={}, receiptUrl={}", companyId, payableId, fileUrl);
        return response;
    }

    @Transactional(readOnly = true)
    public String getPaymentFileUrl(Long companyId, Long payableId, Long transactionId) {
        log.info("getPaymentFileUrl() - START - companyId={}, payableId={}, transactionId={}", companyId, payableId, transactionId);

        PayableEntity payable = findPayable(companyId, payableId);
        if (!Objects.equals(payable.getCompanyId(), companyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A conta a pagar não pertence à empresa.");
        }

        PayableTransactionEntity transaction = resolveTargetTransaction(companyId, payableId, transactionId);
        String receiptUrl = transaction.getReceiptUrl();
        if (receiptUrl == null || receiptUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprovante não encontrado para esta conta a pagar.");
        }

        String temporaryUrl = r2Service.generateTemporaryUrl(receiptUrl, Duration.ofMinutes(5));
        log.info("getPaymentFileUrl() - END - companyId={}, payableId={}", companyId, payableId);
        return temporaryUrl;
    }

    @Transactional
    public void deletePaymentFile(Long companyId, Long payableId, Long transactionId) {
        log.info("deletePaymentFile() - START - companyId={}, payableId={}, transactionId={}", companyId, payableId, transactionId);

        PayableEntity payable = findPayable(companyId, payableId);
        if (payable.getStatus() == PayableStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não é possível remover comprovante de uma conta a pagar cancelada.");
        }

        PayableTransactionEntity transaction = resolveTargetTransaction(companyId, payableId, transactionId);
        if (transaction.getReceiptUrl() == null || transaction.getReceiptUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprovante não encontrado para esta conta a pagar.");
        }

        deleteOldReceiptFile(transaction.getReceiptUrl());
        transaction.setReceiptUrl(null);
        payableTransactionRepository.save(transaction);

        log.info("deletePaymentFile() - END - companyId={}, payableId={}", companyId, payableId);
    }

    @Transactional(readOnly = true)
    public Page<PayableSummaryResponse> listPayables(
            String search,
            LocalDate startDate,
            LocalDate endDate,
            List<PayableStatus> statuses,
            List<PaymentMethod> paymentMethods,
            Long companyId,
            Pageable pageable
    ) {
        log.info("listPayables() - START - companyId={}, search={}, startDate={}, endDate={}, statuses={}",
                companyId, search, startDate, endDate, statuses);

        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        boolean hasDateRange = (startDate != null && endDate != null);
        boolean hasStartDateOnly = (startDate != null && endDate == null);
        boolean hasEndDateOnly = (startDate == null && endDate != null);

        if (hasDateRange && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser posterior à data final.");
        }

        boolean hasStatuses = (statuses != null && !statuses.isEmpty());
        boolean hasPaymentMethods = (paymentMethods != null && !paymentMethods.isEmpty());
        List<PayableStatus> effectiveStatuses = hasStatuses ? statuses : List.of(PayableStatus.PENDING);

        Page<PayableSummaryResponse> page = payableRepository.searchSummarized(
                companyId,
                searchTerm,
                hasDateRange,
                hasStartDateOnly,
                hasEndDateOnly,
                startDate,
                endDate,
                hasStatuses,
                effectiveStatuses,
                hasPaymentMethods,
                paymentMethods,
                pageable
        );

        log.info("listPayables() - END - companyId={}, totalElements={}", companyId, page.getTotalElements());
        return page;
    }

    @Transactional(readOnly = true)
    public PayableResponse findById(Long companyId, Long payableId) {
        log.info("findById() - START - companyId={}, payableId={}", companyId, payableId);

        PayableEntity payable = findPayable(companyId, payableId);
        List<PayableTransactionEntity> transactions = payableTransactionRepository.findByPayableId(payableId);
        PayableResponse response = PayableMapper.fromEntity(payable, transactions);

        log.info("findById() - END - companyId={}, payableId={}, status={}", companyId, payableId, response.getStatus());
        return response;
    }

    private PayableEntity findPayable(Long companyId, Long payableId) {
        return payableRepository.findByIdAndCompanyId(payableId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta a pagar não encontrada"));
    }

    private void validateBeneficiary(Long companyId, Long partnerId, Long collaboratorId) {
        if (partnerId == null && collaboratorId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "É necessário informar o parceiro ou o colaborador beneficiário.");
        }
        if (partnerId != null) {
            partnerRepository.findByIdAndCompanyIdAndStatusNot(partnerId, companyId, PartnerStatus.DELETED.name())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parceiro não encontrado com ID: " + partnerId));
        }
        if (collaboratorId != null) {
            collaboratorRepository.findByIdAndCompanyIdAndStatusNot(collaboratorId, companyId, CollaboratorStatus.DELETED.name())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado com ID: " + collaboratorId));
        }
    }

    private BigDecimal retrieveCurrentPaymentAmount(List<PayableTransactionEntity> existingTransactions) {
        return existingTransactions.stream()
                .filter(Objects::nonNull)
                .map(tx -> {
                    BigDecimal amount = tx.getAmountPaid() != null ? tx.getAmountPaid() : BigDecimal.ZERO;
                    return tx.getTransactionType() == PayableTransactionType.REVERSAL ? amount.negate() : amount;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateRemainingValue(BigDecimal amountPaid, BigDecimal payableAmount, BigDecimal currentPaid) {
        BigDecimal remaining = payableAmount.subtract(currentPaid);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "A conta já está totalmente paga.");
        }
        if (amountPaid.compareTo(remaining) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("O valor do pagamento (R$ %s) excede o saldo restante da conta (R$ %s).",
                            amountPaid.setScale(2, RoundingMode.HALF_UP),
                            remaining.setScale(2, RoundingMode.HALF_UP))
            );
        }
    }

    private void validateReversalValue(BigDecimal reversalAmount, BigDecimal currentPaid) {
        if (currentPaid.compareTo(BigDecimal.ZERO) <= 0 || reversalAmount.compareTo(currentPaid) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("O valor do estorno (R$ %s) excede o valor pago até o momento (R$ %s).",
                            reversalAmount.setScale(2, RoundingMode.HALF_UP),
                            currentPaid.setScale(2, RoundingMode.HALF_UP))
            );
        }
    }

    private PayableStatus resolveStatusAfterTransaction(BigDecimal newTotalPaid, BigDecimal totalAmount) {
        if (newTotalPaid.compareTo(totalAmount) >= 0) {
            return PayableStatus.PAID;
        }
        if (newTotalPaid.compareTo(BigDecimal.ZERO) > 0) {
            return PayableStatus.PARTIALLY_PAID;
        }
        return PayableStatus.PENDING;
    }

    private PayableTransactionEntity resolveTargetTransaction(Long companyId, Long payableId, Long transactionId) {
        if (transactionId != null) {
            return payableTransactionRepository.findByIdAndPayableIdAndCompanyId(transactionId, payableId, companyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Transação de pagamento não encontrada com ID: " + transactionId));
        }
        return payableTransactionRepository.findTopByPayableIdAndCompanyIdOrderByPaidAtDescIdDesc(payableId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Nenhuma transação de pagamento encontrada para esta conta a pagar."));
    }

    private void deleteOldReceiptFile(String receiptUrl) {
        try {
            String oldKey = r2Service.extractKeyFromUrl(receiptUrl);
            r2Service.delete(oldKey);
        } catch (Exception ex) {
            log.warn("deleteOldReceiptFile() - WARN - Falha ao remover comprovante anterior no bucket: {}", ex.getMessage());
        }
    }

    private void syncCommissionStatementPaymentStatus(Long companyId, PayableEntity payable, LocalDateTime paidAt) {
        if (payable.getOriginType() != PayableOriginType.COMMISSION_STATEMENT) {
            return;
        }

        CommissionStatementEntity statement = commissionStatementRepository.findByPayableIdAndCompanyId(payable.getId(), companyId)
                .orElse(null);

        if (statement == null) {
            return;
        }

        List<CommissionEntryEntity> entries = commissionEntryRepository
                .findByStatementIdAndCompanyIdOrderByOccurredAtDescIdDesc(statement.getId(), companyId);

        if (payable.getStatus() == PayableStatus.PAID && statement.getStatus() != CommissionStatementStatus.PAID) {
            statement.setStatus(CommissionStatementStatus.PAID);
            statement.setPaidAt(paidAt != null ? paidAt : LocalDateTime.now());
            commissionStatementRepository.save(statement);

            entries.forEach(entry -> entry.setStatus(CommissionEntryStatus.PAID));
            commissionEntryRepository.saveAll(entries);
        } else if (payable.getStatus() != PayableStatus.PAID && statement.getStatus() == CommissionStatementStatus.PAID) {
            statement.setStatus(CommissionStatementStatus.APPROVED);
            statement.setPaidAt(null);
            commissionStatementRepository.save(statement);

            entries.forEach(entry -> entry.setStatus(CommissionEntryStatus.APPROVED));
            commissionEntryRepository.saveAll(entries);
        }
    }
}
