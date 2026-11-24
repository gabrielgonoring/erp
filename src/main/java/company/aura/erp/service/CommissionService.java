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
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceOrderItemRepository serviceOrderItemRepository;
    private final PayableRepository payableRepository;
    private final PayableService payableService;

    @Transactional
    public List<CommissionEntryResponse> generateEntriesForOrder(OrderEntity order, List<OrderItemEntity> items) {
        if (order == null || order.getStatus() != OrderStatus.COMPLETED || items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        if (serviceOrderRepository.existsByOrderIdAndCompanyId(order.getId(), order.getCompanyId())) {
            return Collections.emptyList();
        }

        List<CommissionRuleEntity> activeRules = commissionRuleRepository
                .findByCompanyIdAndStatus(order.getCompanyId(), CommissionRuleStatus.ACTIVE);
        LocalDateTime occurredAt = order.getCompletedAt() != null ? order.getCompletedAt() : LocalDateTime.now();

        List<CommissionEntryEntity> created = new ArrayList<>();
        for (OrderItemEntity item : items) {
            if (item.getProfessionalId() == null) {
                continue;
            }
            if (commissionEntryRepository.existsByCompanyIdAndSourceTypeAndSourceItemId(
                    order.getCompanyId(), CommissionSourceType.ORDER_ITEM, item.getId())) {
                continue;
            }
            CommissionEntryEntity entry = buildEntry(
                    order.getCompanyId(),
                    item.getProfessionalId(),
                    CommissionSourceType.ORDER_ITEM,
                    order.getId(),
                    item.getId(),
                    item.getCatalogItemId(),
                    item.getDescription(),
                    item.getTotalPrice(),
                    occurredAt,
                    activeRules
            );
            if (entry != null) {
                created.add(commissionEntryRepository.save(entry));
            }
        }
        return created.stream().map(CommissionEntryMapper::fromEntity).toList();
    }

    @Transactional
    public List<CommissionEntryResponse> generateEntriesForServiceOrder(ServiceOrderEntity serviceOrder, List<ServiceOrderItemEntity> items) {
        if (serviceOrder == null || serviceOrder.getStatus() != ServiceOrderStatus.COMPLETED || items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<CommissionRuleEntity> activeRules = commissionRuleRepository
                .findByCompanyIdAndStatus(serviceOrder.getCompanyId(), CommissionRuleStatus.ACTIVE);
        LocalDateTime occurredAt = serviceOrder.getCompletedAt() != null ? serviceOrder.getCompletedAt() : LocalDateTime.now();

        List<CommissionEntryEntity> created = new ArrayList<>();
        for (ServiceOrderItemEntity item : items) {
            if (item.getStatus() == ServiceOrderItemStatus.CANCELED || item.getProfessionalId() == null) {
                continue;
            }
            if (commissionEntryRepository.existsByCompanyIdAndSourceTypeAndSourceItemId(
                    serviceOrder.getCompanyId(), CommissionSourceType.SERVICE_ORDER_ITEM, item.getId())) {
                continue;
            }
            CommissionEntryEntity entry = buildEntry(
                    serviceOrder.getCompanyId(),
                    item.getProfessionalId(),
                    CommissionSourceType.SERVICE_ORDER_ITEM,
                    serviceOrder.getId(),
                    item.getId(),
                    item.getCatalogItemId(),
                    item.getDescription(),
                    item.getTotalPrice(),
                    occurredAt,
                    activeRules
            );
            if (entry != null) {
                created.add(commissionEntryRepository.save(entry));
            }
        }
        return created.stream().map(CommissionEntryMapper::fromEntity).toList();
    }

    @Transactional
    public List<CommissionEntryResponse> generateEntries(CommissionGenerateEntriesRequest request, Long companyId) {
        log.info("generateEntries() - START - companyId={}, request={}", companyId, request);

        LocalDate startDate = request != null ? request.getStartDate() : null;
        LocalDate endDate = request != null ? request.getEndDate() : null;
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser posterior à data final.");
        }

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        Long filterCollaboratorId = request != null ? request.getCollaboratorId() : null;

        List<CommissionEntryResponse> generated = new ArrayList<>();

        if (request != null && request.getOrderId() != null) {
            OrderEntity order = orderRepository.findByIdAndCompanyId(request.getOrderId(), companyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado com ID: " + request.getOrderId()));
            if (order.getStatus() != OrderStatus.COMPLETED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O pedido precisa estar concluído para gerar comissões.");
            }
            List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId()).stream()
                    .filter(i -> filterCollaboratorId == null || filterCollaboratorId.equals(i.getProfessionalId()))
                    .toList();
            generated.addAll(generateEntriesForOrder(order, items));
            return generated;
        }

        if (request != null && request.getServiceOrderId() != null) {
            ServiceOrderEntity so = serviceOrderRepository.findByIdAndCompanyId(request.getServiceOrderId(), companyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordem de serviço não encontrada com ID: " + request.getServiceOrderId()));
            if (so.getStatus() != ServiceOrderStatus.COMPLETED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A ordem de serviço precisa estar concluída para gerar comissões.");
            }
            List<ServiceOrderItemEntity> items = serviceOrderItemRepository
                    .findByServiceOrderIdAndStatusNotIn(so.getId(), List.of(ServiceOrderItemStatus.CANCELED)).stream()
                    .filter(i -> filterCollaboratorId == null || filterCollaboratorId.equals(i.getProfessionalId()))
                    .toList();
            generated.addAll(generateEntriesForServiceOrder(so, items));
            return generated;
        }

        List<OrderEntity> orders = orderRepository.findCompletedStandaloneOrdersForCommission(
                companyId, OrderStatus.COMPLETED, startDateTime, endDateTime);
        for (OrderEntity order : orders) {
            List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId()).stream()
                    .filter(i -> filterCollaboratorId == null || filterCollaboratorId.equals(i.getProfessionalId()))
                    .toList();
            generated.addAll(generateEntriesForOrder(order, items));
        }

        List<ServiceOrderEntity> serviceOrders = serviceOrderRepository.findCompletedServiceOrdersForCommission(
                companyId, ServiceOrderStatus.COMPLETED, startDateTime, endDateTime);
        for (ServiceOrderEntity so : serviceOrders) {
            List<ServiceOrderItemEntity> items = serviceOrderItemRepository
                    .findByServiceOrderIdAndStatusNotIn(so.getId(), List.of(ServiceOrderItemStatus.CANCELED)).stream()
                    .filter(i -> filterCollaboratorId == null || filterCollaboratorId.equals(i.getProfessionalId()))
                    .toList();
            generated.addAll(generateEntriesForServiceOrder(so, items));
        }

        log.info("generateEntries() - END - companyId={}, generatedCount={}", companyId, generated.size());
        return generated;
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

    private CommissionEntryEntity buildEntry(
            Long companyId,
            Long collaboratorId,
            CommissionSourceType sourceType,
            Long sourceDocumentId,
            Long sourceItemId,
            Long catalogItemId,
            String itemDescription,
            BigDecimal baseAmount,
            LocalDateTime occurredAt,
            List<CommissionRuleEntity> activeRules
    ) {
        CollaboratorEntity collaborator = collaboratorRepository.findByIdAndCompanyId(collaboratorId, companyId)
                .orElse(null);
        if (collaborator == null) {
            return null;
        }

        Long categoryId = null;
        if (catalogItemId != null) {
            categoryId = catalogItemRepository.findByIdAndCompanyId(catalogItemId, companyId)
                    .map(CatalogItemEntity::getCategoryId)
                    .orElse(null);
        }

        LocalDate occurredDate = occurredAt.toLocalDate();
        ResolvedCommissionRule resolved = resolveRule(collaborator, catalogItemId, categoryId, occurredDate, activeRules);
        if (resolved == null) {
            return null;
        }

        BigDecimal safeBase = baseAmount != null ? baseAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal percentage = resolved.percentage() != null ? resolved.percentage() : BigDecimal.ZERO;
        BigDecimal commissionAmount = safeBase.multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return CommissionEntryEntity.builder()
                .companyId(companyId)
                .collaboratorId(collaborator.getId())
                .collaboratorName(collaborator.getName())
                .sourceType(sourceType)
                .sourceDocumentId(sourceDocumentId)
                .sourceItemId(sourceItemId)
                .catalogItemId(catalogItemId)
                .categoryId(categoryId)
                .itemDescription(itemDescription)
                .baseAmount(safeBase)
                .appliedPercentage(percentage)
                .commissionAmount(commissionAmount)
                .ruleId(resolved.ruleId())
                .ruleSource(resolved.source())
                .status(CommissionEntryStatus.PENDING)
                .occurredAt(occurredAt)
                .build();
    }

    private ResolvedCommissionRule resolveRule(
            CollaboratorEntity collaborator,
            Long catalogItemId,
            Long categoryId,
            LocalDate date,
            List<CommissionRuleEntity> activeRules
    ) {
        List<CommissionRuleEntity> validRules = activeRules.stream()
                .filter(r -> isWithinValidity(r, date))
                .sorted(Comparator.comparing(CommissionRuleEntity::getId, Comparator.reverseOrder()))
                .toList();

        // 1. Collaborator + Item
        if (catalogItemId != null) {
            Optional<CommissionRuleEntity> match = validRules.stream()
                    .filter(r -> Objects.equals(r.getCollaboratorId(), collaborator.getId())
                            && Objects.equals(r.getCatalogItemId(), catalogItemId))
                    .findFirst();
            if (match.isPresent()) {
                return new ResolvedCommissionRule(match.get().getId(), match.get().getCommissionPercentage(), CommissionRuleSource.COLLABORATOR_ITEM);
            }
        }

        // 2. Collaborator + Category
        if (categoryId != null) {
            Optional<CommissionRuleEntity> match = validRules.stream()
                    .filter(r -> Objects.equals(r.getCollaboratorId(), collaborator.getId())
                            && Objects.equals(r.getCategoryId(), categoryId))
                    .findFirst();
            if (match.isPresent()) {
                return new ResolvedCommissionRule(match.get().getId(), match.get().getCommissionPercentage(), CommissionRuleSource.COLLABORATOR_CATEGORY);
            }
        }

        // 3. Collaborator general rule
        Optional<CommissionRuleEntity> collabDefault = validRules.stream()
                .filter(r -> Objects.equals(r.getCollaboratorId(), collaborator.getId())
                        && r.getCatalogItemId() == null && r.getCategoryId() == null)
                .findFirst();
        if (collabDefault.isPresent()) {
            return new ResolvedCommissionRule(collabDefault.get().getId(), collabDefault.get().getCommissionPercentage(), CommissionRuleSource.COLLABORATOR_DEFAULT);
        }

        // 4. Global + Item
        if (catalogItemId != null) {
            Optional<CommissionRuleEntity> match = validRules.stream()
                    .filter(r -> r.getCollaboratorId() == null && Objects.equals(r.getCatalogItemId(), catalogItemId))
                    .findFirst();
            if (match.isPresent()) {
                return new ResolvedCommissionRule(match.get().getId(), match.get().getCommissionPercentage(), CommissionRuleSource.ITEM);
            }
        }

        // 5. Global + Category
        if (categoryId != null) {
            Optional<CommissionRuleEntity> match = validRules.stream()
                    .filter(r -> r.getCollaboratorId() == null && Objects.equals(r.getCategoryId(), categoryId))
                    .findFirst();
            if (match.isPresent()) {
                return new ResolvedCommissionRule(match.get().getId(), match.get().getCommissionPercentage(), CommissionRuleSource.CATEGORY);
            }
        }

        // 6. Company default rule
        Optional<CommissionRuleEntity> companyDefault = validRules.stream()
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

    private record ResolvedCommissionRule(Long ruleId, BigDecimal percentage, CommissionRuleSource source) {
    }
}
