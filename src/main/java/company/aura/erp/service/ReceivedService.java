package company.aura.erp.service;

import company.aura.erp.constants.BucketFolders;
import company.aura.erp.dto.order.*;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.PaymentTransactionEntity;
import company.aura.erp.entity.ReceivableEntity;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.PaymentTransactionType;
import company.aura.erp.enums.ReceivableOriginType;
import company.aura.erp.enums.ReceivableStatus;
import company.aura.erp.mapper.ReceivableMapper;
import company.aura.erp.modules.bucket.service.CloudflareR2Service;
import company.aura.erp.repository.PaymentTransactionRepository;
import company.aura.erp.repository.ReceivableRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivedService {

    private final ReceivableRepository receivableRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CloudflareR2Service r2Service;


    public List<ReceivableResponse> findReceivableResponses(Long orderId) {
        log.info("findReceivableResponses() - START - orderId={}", orderId);
        List<ReceivableEntity> receivables = receivableRepository.findByOrderId(orderId);
        List<ReceivableResponse> responses = receivables.stream()
                .map(receivable -> {
                    List<PaymentTransactionEntity> transactions = paymentTransactionRepository.findByReceivableId(receivable.getId());
                    return ReceivableMapper.fromEntity(receivable, transactions);
                })
                .toList();
        log.info("findReceivableResponses() - END - orderId={}, totalResponses={}", orderId, responses.size());
        return responses;
    }

    private ReceivableEntity findReceivable(Long companyId, Long receivableId) {
        return receivableRepository.findByIdAndCompanyId(receivableId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recebível não encontrado"));
    }

    public void processReceivables(List<ReceivableRequest> receivables, Long companyId) {
        log.info("processReceivables() - START - companyId: {}, receivables: {}", companyId, receivables.size());

        receivables.forEach(receivableRequest -> processSingleReceivable(companyId, receivableRequest));

        log.info("processReceivables() - END - companyId={}, processedCount={}", companyId, receivables.size());
    }


    public ReceivableResponse payReceivable(Long companyId, Long receivableId, PaymentTransactionRequest request) {
        log.info("payReceivable() - START - companyId: {}, receivableId={}, amountPaid={}", companyId, receivableId, request != null ? request.getAmountPaid() : null);

        ReceivableEntity receivable = findReceivable(companyId, receivableId);

        validateReceivableToPay(receivable);

        List<PaymentTransactionEntity> existingTransactions = paymentTransactionRepository.findByReceivableId(receivableId);

        BigDecimal currentPaymentAmount = retrieveCurrentPaymentAmount(existingTransactions);

        BigDecimal totalAmount = receivable.getAmount() != null ? receivable.getAmount() : BigDecimal.ZERO;

        validateRemainingValue(request, totalAmount, currentPaymentAmount);

        BigDecimal newTotalPaid = currentPaymentAmount.add(request.getAmountPaid());

        receivable.setStatus(getReceivableStatusAfterPayment(newTotalPaid, totalAmount));
        receivable.setPendingAmount(totalAmount.subtract(newTotalPaid));

        ReceivableEntity savedReceivable = receivableRepository.save(receivable);

        List<PaymentTransactionEntity> allTransactions = new ArrayList<>(existingTransactions);
        PaymentTransactionEntity savedTransaction = savePaymentTransaction(companyId, request, receivable);
        allTransactions.add(savedTransaction);

        ReceivableResponse response = ReceivableMapper.fromEntity(savedReceivable, allTransactions);
        log.info("payReceivable() - END - companyId={}, receivableId={}, status={}", companyId, receivableId, response != null ? response.getStatus() : null);
        return response;
    }

    private ReceivableStatus getReceivableStatusAfterPayment(BigDecimal newTotalPaid, BigDecimal receivableAmount) {
        if (newTotalPaid.compareTo(receivableAmount) >= 0) {
            return ReceivableStatus.PAID;
        } else {
            return ReceivableStatus.PARTIALLY_PAID;
        }
    }



    private void validateRemainingValue(PaymentTransactionRequest request, BigDecimal receivableAmount, BigDecimal currentPaid) {
        BigDecimal remaining = receivableAmount.subtract(currentPaid);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "A parcela já está totalmente paga.");
        }

        if (request.getAmountPaid().compareTo(remaining) > 0) {
            BigDecimal requestAmountFormatted = request.getAmountPaid().setScale(2, RoundingMode.HALF_UP);
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
                .filter(Objects::nonNull)
                .map(tx -> {
                    BigDecimal amount = tx.getAmountPaid() != null ? tx.getAmountPaid() : BigDecimal.ZERO;
                    return tx.getTransactionType() == PaymentTransactionType.REFUND ? amount.negate() : amount;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateReceivableToPay(ReceivableEntity receivable) {

        if (receivable.getStatus() == ReceivableStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível registrar pagamento para uma parcela cancelada.");
        }

        if (receivable.getStatus() == ReceivableStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível registrar pagamento para uma parcela já paga.");
        }
    }


    public ReceivableResponse cancelReceivable(Long companyId, Long receivableId) {
        log.info("cancelReceivable() - START - companyId={}, receivableId={}", companyId, receivableId);

        ReceivableEntity receivable = findReceivable(companyId, receivableId);

        if (receivable.getStatus() == ReceivableStatus.PAID || receivable.getStatus() == ReceivableStatus.PARTIALLY_PAID) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível cancelar uma parcela que já possui pagamentos.");
        }

        List<PaymentTransactionEntity> existingTransactions = paymentTransactionRepository.findByReceivableId(receivableId);
        if (!existingTransactions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível cancelar uma parcela que possui transações de pagamento.");
        }

        receivable.setStatus(ReceivableStatus.CANCELED);
        receivable.setPendingAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        ReceivableEntity saved = receivableRepository.save(receivable);

        ReceivableResponse response = ReceivableMapper.fromEntity(saved, Collections.emptyList());
        log.info("cancelReceivable() - END - companyId={}, receivableId={}, status={}", companyId, receivableId, response != null ? response.getStatus() : null);
        return response;
    }

    public ReceivableResponse uploadPaymentFile(Long companyId, Long receivableId, MultipartFile file, String bankSlipCode) {
        log.info("uploadPaymentFile() - START - companyId={}, receivableId={}, originalFilename={}", companyId, receivableId, file != null ? file.getOriginalFilename() : null);

        ReceivableEntity receivable = findReceivable(companyId, receivableId);

        validateUploadPaymentFile(file, receivable);

        if (isPaymentFileAlreadyLinked(receivable)) {
            deleteOldPaymentFile(receivable);
        }

        String folder = BucketFolders.PAYMENT_FILE + companyId + "/" + receivableId;
        String fileKey = r2Service.upload(file, folder);
        String fileUrl = r2Service.getFileUrl(fileKey);

        receivable.setPaymentFileUrl(fileUrl);

        if (bankSlipCode != null && !bankSlipCode.isBlank()) {
            receivable.setBankSlipCode(bankSlipCode.trim());
        }

        ReceivableEntity saved = receivableRepository.save(receivable);
        List<PaymentTransactionEntity> transactions = paymentTransactionRepository.findByReceivableId(receivableId);
        ReceivableResponse response = ReceivableMapper.fromEntity(saved, transactions);

        log.info("uploadPaymentFile() - END - companyId={}, receivableId={}, paymentFileUrl={}", companyId, receivableId, fileUrl);
        return response;
    }

    private boolean isPaymentFileAlreadyLinked(ReceivableEntity receivable) {
        return receivable.getPaymentFileUrl() != null && !receivable.getPaymentFileUrl().isBlank();
    }

    private void validateUploadPaymentFile(MultipartFile file, ReceivableEntity receivable) {

        if (receivable.getStatus() == ReceivableStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível anexar arquivo de pagamento a uma parcela cancelada.");
        }

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo de pagamento é obrigatório.");
        }
    }

    private void deleteOldPaymentFile(ReceivableEntity receivable) {
        try {
            String oldKey = r2Service.extractKeyFromUrl(receivable.getPaymentFileUrl());
            r2Service.delete(oldKey);
        } catch (Exception ex) {
            log.warn("uploadPaymentFile() - WARN - Falha ao remover arquivo anterior no bucket: {}", ex.getMessage());
        }
    }

    public String getPaymentFileUrl(Long companyId, Long receivableId) {
        log.info("getPaymentFileUrl() - START - companyId={}, receivableId={}", companyId, receivableId);

        ReceivableEntity receivable = findReceivable(companyId, receivableId);

        if (!Objects.equals(receivable.getCompanyId(), companyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A parcela não pertence a empresa.");
        }

        String paymentFileUrl = receivable.getPaymentFileUrl();
        if (paymentFileUrl == null || paymentFileUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo de pagamento não encontrado para esta parcela.");
        }

        String temporaryUrl = r2Service.generateTemporaryUrl(paymentFileUrl, Duration.ofMinutes(5));
        log.info("getPaymentFileUrl() - END - companyId={}, receivableId={}, temporaryUrl={}", companyId, receivableId, temporaryUrl);
        return temporaryUrl;
    }

    public void deletePaymentFile(Long companyId, Long receivableId) {
        log.info("deletePaymentFile() - START - companyId={}, receivableId={}", companyId, receivableId);

        ReceivableEntity receivable = findReceivable(companyId, receivableId);

        validateDeletePaymentFile(receivable);

        deleteOldPaymentFile(receivable);

        receivable.setPaymentFileUrl(null);
        receivable.setBankSlipCode(null);

        receivableRepository.save(receivable);

        log.info("deletePaymentFile() - END - companyId={}, receivableId={}", companyId, receivableId);
    }

    private void validateDeletePaymentFile(ReceivableEntity receivable) {


        if (receivable.getStatus() == ReceivableStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível remover arquivo de pagamento de uma parcela cancelada.");
        }

        if (!isPaymentFileAlreadyLinked(receivable)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo de pagamento não encontrado para esta parcela.");
        }
    }

    @Transactional(readOnly = true)
    public Page<ReceivableSummaryResponse> listReceivables(
            String search,
            LocalDate startDate,
            LocalDate endDate,
            List<ReceivableStatus> statuses,
            List<PaymentMethod> paymentMethods,
            Long companyId,
            Pageable pageable
    ) {
        log.info("listReceivables() - START - companyId={}, search={}, startDate={}, endDate={}, statuses={}",
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
        List<ReceivableStatus> effectiveStatuses = hasStatuses ? statuses : List.of(ReceivableStatus.PENDING);

        Page<ReceivableSummaryResponse> page = receivableRepository.searchSummarized(
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

        log.info("listReceivables() - END - companyId={}, totalElements={}, totalPages={}",
                companyId, page.getTotalElements(), page.getTotalPages());
        return page;
    }

    @Transactional(readOnly = true)
    public ReceivableResponse findById(Long companyId, Long receivableId) {
        log.info("findById() - START - companyId={}, receivableId={}", companyId, receivableId);
        ReceivableEntity receivable = findReceivable(companyId, receivableId);
        List<PaymentTransactionEntity> transactions = paymentTransactionRepository.findByReceivableId(receivableId);
        ReceivableResponse response = ReceivableMapper.fromEntity(receivable, transactions);
        log.info("findById() - END - companyId={}, receivableId={}, status={}", companyId, receivableId, response != null ? response.getStatus() : null);
        return response;
    }



    public ReceivableResponse processSingleReceivable(Long companyId, ReceivableRequest receivableRequest) {
        log.info("processSingleReceivable() - START - companyId: {}, amount: {}, partnerId: {}", companyId, receivableRequest.getAmount(), receivableRequest.getPartnerId());
        ReceivableEntity savedReceivable = saveReceivable(companyId, receivableRequest);

        if (receivableRequest.getPaid()) {
            var savedTx = savePaymentTransaction(companyId, receivableRequest, savedReceivable);
            return ReceivableMapper.fromEntity(savedReceivable, List.of(savedTx));

        }

        return ReceivableMapper.fromEntity(savedReceivable, Collections.emptyList());
    }

    private ReceivableEntity saveReceivable(Long companyId, ReceivableRequest receivableRequest) {
        boolean isPaid = receivableRequest.getPaid();
        BigDecimal amount = receivableRequest.getAmount() != null ? receivableRequest.getAmount() : BigDecimal.ZERO;
        BigDecimal pendingAmount = isPaid ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amount.setScale(2, RoundingMode.HALF_UP);

        ReceivableEntity receivable = ReceivableEntity.builder()
                .orderId(receivableRequest.getOrderId())
                .serviceOrderId(receivableRequest.getServiceOrderId())
                .originType(resolveOriginType(receivableRequest))
                .companyId(companyId)
                .partnerId(receivableRequest.getPartnerId())
                .installmentNumber(receivableRequest.getInstallmentNumber())
                .dueDate(receivableRequest.getDueDate())
                .amount(amount)
                .pendingAmount(pendingAmount)
                .paymentMethod(receivableRequest.getPaymentMethod())
                .status(isPaid ? ReceivableStatus.PAID : ReceivableStatus.PENDING)
                .bankSlipCode(receivableRequest.getBankSlipCode())
                .paymentFileUrl(receivableRequest.getPaymentFileUrl())
                .paymentUrl(receivableRequest.getPaymentUrl())
                .notes(receivableRequest.getNotes())
                .build();

        return receivableRepository.save(receivable);
    }

    private ReceivableOriginType resolveOriginType(ReceivableRequest receivableRequest) {
        if (receivableRequest.getOriginType() != null) {
            return receivableRequest.getOriginType();
        }
        if (receivableRequest.getServiceOrderId() != null) {
            return ReceivableOriginType.SERVICE_ORDER;
        }
        if (receivableRequest.getOrderId() != null) {
            return ReceivableOriginType.ORDER;
        }
        return ReceivableOriginType.MANUAL;
    }

    private PaymentTransactionEntity savePaymentTransaction(Long companyId, PaymentTransactionRequest request, ReceivableEntity receivable) {

        LocalDateTime paidAt = request.getPaidAt() != null ? request.getPaidAt() : LocalDateTime.now();
        PaymentTransactionType transactionType = request.getTransactionType() != null
                ? request.getTransactionType()
                : PaymentTransactionType.PAYMENT;

        PaymentTransactionEntity transaction = PaymentTransactionEntity.builder()
                .receivableId(receivable.getId())
                .refundedTransactionId(request.getRefundedTransactionId())
                .companyId(companyId)
                .transactionType(transactionType)
                .paidAt(paidAt)
                .amountPaid(request.getAmountPaid())
                .paymentMethod(request.getPaymentMethod())
                .cardBrand(request.getCardBrand())
                .nsuAuthorization(request.getNsuAuthorization())
                .notes(request.getNotes())
                .build();

        return paymentTransactionRepository.save(transaction);
    }

    private PaymentTransactionEntity savePaymentTransaction(Long companyId, ReceivableRequest request, ReceivableEntity receivable) {

        PaymentTransactionEntity transaction = PaymentTransactionEntity.builder()
                .receivableId(receivable.getId())
                .companyId(companyId)
                .transactionType(PaymentTransactionType.PAYMENT)
                .paidAt(LocalDateTime.now())
                .amountPaid(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .cardBrand(request.getCardBrand())
                .nsuAuthorization(request.getNsuAuthorization())
                .notes(request.getNotes())
                .build();

        return paymentTransactionRepository.save(transaction);
    }

    private PaymentTransactionEntity savePaymentTransactionFromNewReceivable(Long companyId, ReceivableRequest request, ReceivableEntity saved) {
        PaymentTransactionEntity transaction = PaymentTransactionEntity.builder()
                .receivableId(saved.getId())
                .companyId(companyId)
                .transactionType(PaymentTransactionType.PAYMENT)
                .paidAt(LocalDateTime.now())
                .amountPaid(saved.getAmount())
                .paymentMethod(saved.getPaymentMethod())
                .cardBrand(request.getCardBrand())
                .nsuAuthorization(request.getNsuAuthorization())
                .notes(request.getNotes())
                .build();
        return paymentTransactionRepository.save(transaction);
    }

}
