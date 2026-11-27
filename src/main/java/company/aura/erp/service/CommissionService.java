package company.aura.erp.service;

import company.aura.erp.dto.commission.*;
import company.aura.erp.dto.payable.PayableTransactionRequest;
import company.aura.erp.entity.*;
import company.aura.erp.enums.*;
import company.aura.erp.mapper.CommissionEntryMapper;
import company.aura.erp.mapper.CommissionStatementMapper;
import company.aura.erp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionRuleRepository commissionRuleRepository;
    private final CommissionStatementRepository commissionStatementRepository;
    private final CommissionEntryRepository commissionEntryRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final PayableRepository payableRepository;
    private final PayableService payableService;

    @Transactional
    public List<CommissionEntryResponse> generateEntriesForOrder(OrderEntity order, List<OrderItemEntity> items) {
        if (order == null || order.getStatus() != OrderStatus.COMPLETED || items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<CommissionSourceItem> sourceItems = items.stream()
                .filter(item -> item.getProfessionalId() != null)
                .map(item -> buildCommissionSourceItem(order, item))
                .toList();

        return generateEntries(order.getCompanyId(), sourceItems);
    }

    private static CommissionSourceItem buildCommissionSourceItem(OrderEntity orderEntity, OrderItemEntity item) {
        return new CommissionSourceItem(
                item.getId(),
                orderEntity.getCompanyId(),
                item.getProfessionalId(),
                item.getCatalogItemId(),
                item.getDescription(),
                item.getTotalPrice(),
                CommissionSourceType.ORDER_ITEM,
                orderEntity.getId(),
                orderEntity.getCompletedAt() != null ? orderEntity.getCompletedAt() : LocalDateTime.now()
        );
    }

    private List<CommissionEntryResponse> generateEntries(Long companyId, List<CommissionSourceItem> sourceItems) {
        if (sourceItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<CommissionRuleEntity> activeRules = commissionRuleRepository.findByCompanyIdAndStatus(companyId, CommissionRuleStatus.ACTIVE);

        List<CommissionEntryEntity> created = new ArrayList<>();

        for (CommissionSourceItem item : sourceItems) {
            if (commissionEntryRepository.existsByCompanyIdAndSourceTypeAndSourceItemId(companyId, item.sourceType(), item.itemId())) {
                continue;
            }

            CommissionEntryEntity entry = buildEntry(item, activeRules);

            if (entry != null) {
                created.add(commissionEntryRepository.save(entry));
            }
        }
        return created.stream().map(CommissionEntryMapper::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Page<CommissionEntryResponse> listEntries(
            String search,
            Long collaboratorId,
            Long statementId,
            List<CommissionEntryStatus> statuses,
            CommissionSourceType sourceType,
            LocalDate startDate,
            LocalDate endDate,
            Long companyId,
            Pageable pageable
    ) {
        log.info("listEntries() - START - companyId={}, collaboratorId={}, statementId={}, statuses={}",
                companyId, collaboratorId, statementId, statuses);

        boolean hasDateRange = (startDate != null && endDate != null);
        boolean hasStartDateOnly = (startDate != null && endDate == null);
        boolean hasEndDateOnly = (startDate == null && endDate != null);

        if (hasDateRange && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser posterior à data final.");
        }

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        boolean hasStatuses = (statuses != null && !statuses.isEmpty());

        return commissionEntryRepository.searchEntries(
                companyId,
                searchTerm,
                collaboratorId,
                statementId,
                hasStatuses,
                statuses,
                sourceType,
                hasDateRange,
                hasStartDateOnly,
                hasEndDateOnly,
                startDateTime,
                endDateTime,
                pageable
        ).map(CommissionEntryMapper::fromEntity);
    }

    @Transactional(readOnly = true)
    public CommissionEntryResponse findEntryById(Long entryId, Long companyId) {
        CommissionEntryEntity entry = commissionEntryRepository.findByIdAndCompanyId(entryId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lançamento de comissão não encontrado com ID: " + entryId));
        return CommissionEntryMapper.fromEntity(entry);
    }

    @Transactional
    public void cancelEntry(Long entryId, Long companyId) {
        log.info("cancelEntry() - START - entryId={}, companyId={}", entryId, companyId);

        CommissionEntryEntity entry = commissionEntryRepository.findByIdAndCompanyId(entryId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lançamento de comissão não encontrado com ID: " + entryId));

        if (entry.getStatus() != CommissionEntryStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Somente lançamentos de comissão com status PENDING podem ser cancelados.");
        }

        entry.setStatus(CommissionEntryStatus.CANCELED);
        commissionEntryRepository.save(entry);
        log.info("cancelEntry() - END - entryId={}, companyId={}", entryId, companyId);
    }

    @Transactional(readOnly = true)
    public boolean hasNonPendingEntriesForOrder(Long orderId, Long companyId) {
        return commissionEntryRepository.existsByCompanyIdAndSourceTypeAndSourceDocumentIdAndStatusNot(
                companyId,
                CommissionSourceType.ORDER_ITEM,
                orderId,
                CommissionEntryStatus.PENDING
        );
    }

    @Transactional
    public void cancelEntriesForOrder(Long orderId, Long companyId) {
        log.info("cancelEntriesForOrder() - START - orderId={}, companyId={}", orderId, companyId);

        List<CommissionEntryEntity> entries = commissionEntryRepository
                .findByCompanyIdAndSourceTypeAndSourceDocumentId(companyId, CommissionSourceType.ORDER_ITEM, orderId);

        List<CommissionEntryEntity> activeEntries = entries.stream()
                .filter(entry -> entry.getStatus() != CommissionEntryStatus.CANCELED)
                .toList();

        boolean hasNonPendingCommission = activeEntries.stream()
                .anyMatch(entry -> entry.getStatus() != CommissionEntryStatus.PENDING || entry.getStatementId() != null);

        if (hasNonPendingCommission) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não é possível cancelar o pedido pois existem lançamentos de comissão que não estão pendentes.");
        }

        if (!activeEntries.isEmpty()) {
            activeEntries.forEach(entry -> entry.setStatus(CommissionEntryStatus.CANCELED));
            commissionEntryRepository.saveAll(activeEntries);
        }

        log.info("cancelEntriesForOrder() - END - orderId={}, companyId={}, canceledCount={}",
                orderId, companyId, activeEntries.size());
    }

    @Transactional
    public CommissionStatementResponse createStatement(CommissionStatementCreateRequest request, Long companyId) {
        log.info("createStatement() - START - companyId={}, collaboratorId={}, period={}..{}",
                companyId, request.getCollaboratorId(), request.getPeriodStart(), request.getPeriodEnd());

        if (request.getPeriodStart().isAfter(request.getPeriodEnd())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial do período não pode ser posterior à data final.");
        }

        CollaboratorEntity collaborator = collaboratorRepository
                .findByIdAndCompanyIdAndStatusNot(request.getCollaboratorId(), companyId, CollaboratorStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Colaborador não encontrado com ID: " + request.getCollaboratorId()));

        List<CommissionEntryEntity> selectedEntries;
        if (request.getEntryIds() != null && !request.getEntryIds().isEmpty()) {
            selectedEntries = commissionEntryRepository.findAllByIdInAndCompanyId(request.getEntryIds(), companyId);
            if (selectedEntries.size() != request.getEntryIds().size()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Um ou mais lançamentos informados não foram encontrados.");
            }
            for (CommissionEntryEntity entry : selectedEntries) {
                if (!Objects.equals(entry.getCollaboratorId(), collaborator.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "O lançamento " + entry.getId() + " não pertence ao colaborador informado.");
                }
                if (entry.getStatus() != CommissionEntryStatus.PENDING || entry.getStatementId() != null) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "O lançamento " + entry.getId() + " não está disponível (PENDING) para fechamento.");
                }
            }
        } else {
            selectedEntries = commissionEntryRepository
                    .findByCompanyIdAndCollaboratorIdAndStatusAndStatementIdIsNullAndOccurredAtBetweenOrderByOccurredAtAscIdAsc(
                            companyId,
                            collaborator.getId(),
                            CommissionEntryStatus.PENDING,
                            request.getPeriodStart().atStartOfDay(),
                            request.getPeriodEnd().atTime(LocalTime.MAX)
                    );
        }

        if (selectedEntries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não há lançamentos de comissão pendentes para o colaborador e período informados.");
        }

        BigDecimal totalSales = selectedEntries.stream()
                .map(CommissionEntryEntity::getBaseAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalCommission = selectedEntries.stream()
                .map(CommissionEntryEntity::getCommissionAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        CommissionStatementEntity statement = CommissionStatementEntity.builder()
                .companyId(companyId)
                .collaboratorId(collaborator.getId())
                .collaboratorName(collaborator.getName())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .dueDate(request.getDueDate())
                .totalSalesAmount(totalSales)
                .totalCommissionAmount(totalCommission)
                .status(CommissionStatementStatus.OPEN)
                .notes(request.getNotes())
                .build();

        CommissionStatementEntity savedStatement = commissionStatementRepository.save(statement);

        for (CommissionEntryEntity entry : selectedEntries) {
            entry.setStatementId(savedStatement.getId());
            entry.setStatus(CommissionEntryStatus.IN_STATEMENT);
        }
        List<CommissionEntryEntity> updatedEntries = commissionEntryRepository.saveAll(selectedEntries);

        CommissionStatementResponse response = CommissionStatementMapper.fromEntity(savedStatement, updatedEntries);
        log.info("createStatement() - END - statementId={}, totalCommission={}, entriesCount={}",
                response.getId(), totalCommission, updatedEntries.size());
        return response;
    }

    @Transactional(readOnly = true)
    public Page<CommissionStatementSummaryResponse> listStatements(
            String search,
            Long collaboratorId,
            List<CommissionStatementStatus> statuses,
            LocalDate startDate,
            LocalDate endDate,
            Long companyId,
            Pageable pageable
    ) {
        log.info("listStatements() - START - companyId={}, collaboratorId={}, statuses={}", companyId, collaboratorId, statuses);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser posterior à data final.");
        }

        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        boolean hasStatuses = (statuses != null && !statuses.isEmpty());

        return commissionStatementRepository.searchStatements(
                companyId,
                searchTerm,
                collaboratorId,
                hasStatuses,
                statuses,
                startDate,
                endDate,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public CommissionStatementResponse findStatementById(Long statementId, Long companyId) {
        CommissionStatementEntity statement = findStatement(statementId, companyId);
        List<CommissionEntryEntity> entries = commissionEntryRepository
                .findByStatementIdAndCompanyIdOrderByOccurredAtDescIdDesc(statementId, companyId);
        return CommissionStatementMapper.fromEntity(statement, entries);
    }

    @Transactional
    public CommissionStatementResponse approveStatement(Long statementId, Long companyId) {
        log.info("approveStatement() - START - statementId={}, companyId={}", statementId, companyId);

        CommissionStatementEntity statement = findStatement(statementId, companyId);

        if (statement.getStatus() != CommissionStatementStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Somente fechamentos de comissão com status OPEN podem ser aprovados.");
        }

        if (statement.getTotalCommissionAmount() == null || statement.getTotalCommissionAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não é possível aprovar um fechamento com valor de comissão zerado.");
        }

        PayableEntity payable = PayableEntity.builder()
                .companyId(companyId)
                .collaboratorId(statement.getCollaboratorId())
                .originType(PayableOriginType.COMMISSION_STATEMENT)
                .originId(statement.getId())
                .description(String.format("Comissão %s (%s a %s)",
                        statement.getCollaboratorName(), statement.getPeriodStart(), statement.getPeriodEnd()))
                .installmentNumber(1)
                .dueDate(statement.getDueDate())
                .amount(statement.getTotalCommissionAmount())
                .pendingAmount(statement.getTotalCommissionAmount())
                .status(PayableStatus.PENDING)
                .notes(statement.getNotes())
                .build();

        PayableEntity savedPayable = payableRepository.save(payable);

        statement.setPayableId(savedPayable.getId());
        statement.setStatus(CommissionStatementStatus.APPROVED);
        CommissionStatementEntity savedStatement = commissionStatementRepository.save(statement);

        List<CommissionEntryEntity> entries = commissionEntryRepository
                .findByStatementIdAndCompanyIdOrderByOccurredAtDescIdDesc(statementId, companyId);
        entries.forEach(entry -> entry.setStatus(CommissionEntryStatus.APPROVED));
        List<CommissionEntryEntity> updatedEntries = commissionEntryRepository.saveAll(entries);

        CommissionStatementResponse response = CommissionStatementMapper.fromEntity(savedStatement, updatedEntries);
        log.info("approveStatement() - END - statementId={}, payableId={}", statementId, savedPayable.getId());
        return response;
    }

    @Transactional
    public CommissionStatementResponse payStatement(Long statementId, PayableTransactionRequest request, Long companyId) {
        log.info("payStatement() - START - statementId={}, companyId={}", statementId, companyId);

        CommissionStatementEntity statement = findStatement(statementId, companyId);

        if (statement.getStatus() == CommissionStatementStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível pagar um fechamento cancelado.");
        }
        if (statement.getStatus() == CommissionStatementStatus.OPEN || statement.getPayableId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O fechamento precisa ser aprovado antes do pagamento.");
        }

        payableService.payPayable(companyId, statement.getPayableId(), request);

        CommissionStatementEntity refreshed = findStatement(statementId, companyId);
        List<CommissionEntryEntity> entries = commissionEntryRepository
                .findByStatementIdAndCompanyIdOrderByOccurredAtDescIdDesc(statementId, companyId);

        CommissionStatementResponse response = CommissionStatementMapper.fromEntity(refreshed, entries);
        log.info("payStatement() - END - statementId={}, status={}", statementId, response.getStatus());
        return response;
    }

    @Transactional
    public void cancelStatement(Long statementId, Long companyId) {
        log.info("cancelStatement() - START - statementId={}, companyId={}", statementId, companyId);

        CommissionStatementEntity statement = findStatement(statementId, companyId);

        if (statement.getStatus() == CommissionStatementStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não é possível cancelar um fechamento de comissão já pago.");
        }
        if (statement.getStatus() == CommissionStatementStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O fechamento de comissão já está cancelado.");
        }

        if (statement.getPayableId() != null) {
            payableService.cancelPayable(companyId, statement.getPayableId());
        }

        List<CommissionEntryEntity> entries = commissionEntryRepository
                .findByStatementIdAndCompanyIdOrderByOccurredAtDescIdDesc(statementId, companyId);
        for (CommissionEntryEntity entry : entries) {
            entry.setStatementId(null);
            entry.setStatus(CommissionEntryStatus.PENDING);
        }
        commissionEntryRepository.saveAll(entries);

        statement.setStatus(CommissionStatementStatus.CANCELED);
        commissionStatementRepository.save(statement);

        log.info("cancelStatement() - END - statementId={}, companyId={}", statementId, companyId);
    }

    private CommissionStatementEntity findStatement(Long statementId, Long companyId) {
        return commissionStatementRepository.findByIdAndCompanyId(statementId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Fechamento de comissão não encontrado com ID: " + statementId));
    }

    private CommissionEntryEntity buildEntry(CommissionSourceItem item, List<CommissionRuleEntity> activeRules) {
        CollaboratorEntity collaborator = collaboratorRepository.findByIdAndCompanyId(item.collaboratorId(), item.companyId())
                .orElse(null);
        if (collaborator == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Colaborador inválido");
        }

        Long categoryId = null;
        if (item.catalogItemId() != null) {
            CatalogItemEntity catalogItem = catalogItemRepository
                    .findByIdAndCompanyId(item.catalogItemId(), item.companyId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Item de catálogo inválido"));
            categoryId = catalogItem.getCategoryId();
        }

        LocalDateTime occurredAt = item.occurredAt() != null ? item.occurredAt() : LocalDateTime.now();
        ResolvedCommissionRule commissionRule = resolveRule(item, categoryId, occurredAt.toLocalDate(), activeRules);

        if (commissionRule == null) {
            return null;
        }

        BigDecimal safeBase = item.totalPrice() != null ? item.totalPrice().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal percentage = commissionRule.percentage() != null ? commissionRule.percentage() : BigDecimal.ZERO;
        BigDecimal commissionAmount = safeBase.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return CommissionEntryEntity.builder()
                .companyId(item.companyId())
                .collaboratorId(collaborator.getId())
                .collaboratorName(collaborator.getName())
                .sourceType(item.sourceType())
                .sourceDocumentId(item.sourceDocumentId())
                .sourceItemId(item.itemId())
                .catalogItemId(item.catalogItemId())
                .categoryId(categoryId)
                .itemDescription(item.description())
                .baseAmount(safeBase)
                .appliedPercentage(percentage)
                .commissionAmount(commissionAmount)
                .ruleId(commissionRule.ruleId())
                .ruleSource(commissionRule.source())
                .status(CommissionEntryStatus.PENDING)
                .occurredAt(occurredAt)
                .build();
    }

    private ResolvedCommissionRule resolveRule(
            CommissionSourceItem item,
            Long categoryId,
            LocalDate occurredDate,
            List<CommissionRuleEntity> activeRules
    ) {
        List<CommissionRuleEntity> validRulesInOccurredDate = activeRules.stream()
                .filter(r -> isWithinValidity(r, occurredDate))
                .sorted(Comparator.comparing(CommissionRuleEntity::getId, Comparator.reverseOrder()))
                .toList();

        // 1. Collaborator + Item
        if (item.catalogItemId() != null) {
            Optional<CommissionRuleEntity> match = validRulesInOccurredDate.stream()
                    .filter(r -> Objects.equals(r.getCollaboratorId(), item.collaboratorId())
                            && Objects.equals(r.getCatalogItemId(), item.catalogItemId()))
                    .findFirst();
            if (match.isPresent()) {
                return new ResolvedCommissionRule(match.get().getId(), match.get().getCommissionPercentage(), CommissionRuleSource.COLLABORATOR_ITEM);
            }
        }

        // 2. Collaborator + Category
        if (categoryId != null) {
            Optional<CommissionRuleEntity> match = validRulesInOccurredDate.stream()
                    .filter(r -> Objects.equals(r.getCollaboratorId(), item.collaboratorId())
                            && Objects.equals(r.getCategoryId(), categoryId))
                    .findFirst();
            if (match.isPresent()) {
                return new ResolvedCommissionRule(match.get().getId(), match.get().getCommissionPercentage(), CommissionRuleSource.COLLABORATOR_CATEGORY);
            }
        }

        // 3. Collaborator general rule
        Optional<CommissionRuleEntity> collabDefault = validRulesInOccurredDate.stream()
                .filter(r -> Objects.equals(r.getCollaboratorId(), item.collaboratorId())
                        && r.getCatalogItemId() == null && r.getCategoryId() == null)
                .findFirst();

        if (collabDefault.isPresent()) {
            return new ResolvedCommissionRule(collabDefault.get().getId(), collabDefault.get().getCommissionPercentage(), CommissionRuleSource.COLLABORATOR_DEFAULT);
        }

        // 4. Global + Item
        if (item.catalogItemId() != null) {
            Optional<CommissionRuleEntity> match = validRulesInOccurredDate.stream()
                    .filter(r -> r.getCollaboratorId() == null && Objects.equals(r.getCatalogItemId(), item.catalogItemId()))
                    .findFirst();
            if (match.isPresent()) {
                return new ResolvedCommissionRule(match.get().getId(), match.get().getCommissionPercentage(), CommissionRuleSource.ITEM);
            }
        }

        // 5. Global + Category
        if (categoryId != null) {
            Optional<CommissionRuleEntity> match = validRulesInOccurredDate.stream()
                    .filter(r -> r.getCollaboratorId() == null && Objects.equals(r.getCategoryId(), categoryId))
                    .findFirst();
            if (match.isPresent()) {
                return new ResolvedCommissionRule(match.get().getId(), match.get().getCommissionPercentage(), CommissionRuleSource.CATEGORY);
            }
        }

        // 6. Company default rule
        Optional<CommissionRuleEntity> companyDefault = validRulesInOccurredDate.stream()
                .filter(r -> r.getCollaboratorId() == null && r.getCatalogItemId() == null && r.getCategoryId() == null)
                .findFirst();
        if (companyDefault.isPresent()) {
            return new ResolvedCommissionRule(companyDefault.get().getId(), companyDefault.get().getCommissionPercentage(), CommissionRuleSource.COMPANY_DEFAULT);
        }

        return null;
    }

    private boolean isWithinValidity(CommissionRuleEntity rule, LocalDate date) {
        if (rule.getValidFrom() != null && date.isBefore(rule.getValidFrom())) {
            return false;
        }
        if (rule.getValidUntil() != null && date.isAfter(rule.getValidUntil())) {
            return false;
        }
        return true;
    }

    private record CommissionSourceItem(
            Long itemId,
            Long companyId,
            Long collaboratorId,
            Long catalogItemId,
            String description,
            BigDecimal totalPrice,
            CommissionSourceType sourceType,
            Long sourceDocumentId,
            LocalDateTime occurredAt
    ) {
    }

    private record ResolvedCommissionRule(Long ruleId, BigDecimal percentage, CommissionRuleSource source) {
    }
}
