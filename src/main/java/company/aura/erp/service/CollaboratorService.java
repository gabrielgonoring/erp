package company.aura.erp.service;

import company.aura.erp.dto.collaborator.*;
import company.aura.erp.entity.CollaboratorEntity;
import company.aura.erp.entity.CommissionEntryEntity;
import company.aura.erp.entity.CommissionRuleEntity;
import company.aura.erp.entity.OrderItemEntity;
import company.aura.erp.enums.*;
import company.aura.erp.mapper.CollaboratorMapper;
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
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollaboratorService {

    private final CollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final CommissionRuleRepository commissionRuleRepository;
    private final CommissionEntryRepository commissionEntryRepository;

    @Transactional
    public CollaboratorResponse create(CollaboratorCreateRequest request, Long companyId) {
        log.info("create() - START - companyId={}, name={}, email={}, userId={}",
                companyId,
                request != null ? request.getName() : null,
                request != null ? request.getEmail() : null,
                request != null ? request.getUserId() : null);

        if (companyId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O usuário logado não possui uma empresa vinculada.");
        }

        validateUser(request.getUserId(), companyId);

        CollaboratorEntity collaborator = toEntity(request, companyId);
        CollaboratorEntity saved = collaboratorRepository.save(collaborator);
        CollaboratorResponse response = toResponse(saved);

        log.info("create() = END - collaboratorId={}, name={}, companyId={}",
                response.getId(), response.getName(), companyId);
        return response;
    }

    @Transactional
    public CollaboratorResponse update(Long id, CollaboratorUpdateRequest request, Long companyId) {
        log.info("update() - START - id={}, companyId={}, name={}",
                id, companyId, request != null ? request.getName() : null);

        CollaboratorEntity collaborator = collaboratorRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, CollaboratorStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado com ID: " + id));

        if (request.getUserId() != null) {
            validateUser(request.getUserId(), companyId);
        }

        collaborator.setName(request.getName());
        collaborator.setUserId(request.getUserId());
        collaborator.setCpf(request.getCpf());
        collaborator.setEmail(request.getEmail());
        collaborator.setPhone(request.getPhone());

        collaborator.setStreet(request.getStreet());
        collaborator.setNumber(request.getNumber());
        collaborator.setComplement(request.getComplement());
        collaborator.setNeighborhood(request.getNeighborhood());
        collaborator.setCity(request.getCity());
        collaborator.setState(request.getState());
        collaborator.setZipCode(request.getZipCode());
        collaborator.setIbgeCityCode(request.getIbgeCityCode());

        collaborator.setRg(request.getRg());
        collaborator.setBirthDate(request.getBirthDate());
        collaborator.setAdmissionDate(request.getAdmissionDate());
        collaborator.setResignationDate(request.getResignationDate());
        collaborator.setJobTitle(request.getJobTitle());
        collaborator.setPisPasep(request.getPisPasep());

        CollaboratorEntity updated = collaboratorRepository.save(collaborator);
        CollaboratorResponse response = toResponse(updated);

        log.info("update() = END - id={}, name={}, companyId={}",
                response.getId(), response.getName(), companyId);
        return response;
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        log.info("delete() - START - id={}, companyId={}", id, companyId);

        CollaboratorEntity collaborator = collaboratorRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, CollaboratorStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado com ID: " + id));

        collaborator.setStatus(CollaboratorStatus.DELETED.name());
        collaboratorRepository.save(collaborator);

        log.info("delete() = END - id={}, status={}", id, collaborator.getStatus());
    }

    public Page<CollaboratorSummaryResponse> listPaginated(String search, String status, Long companyId, Pageable pageable) {
        log.info("listPaginated() - START - companyId={}, search={}, status={}, pageable={}", companyId, search, status, pageable);

        Page<CollaboratorSummaryResponse> page = collaboratorRepository.searchByCompanyIdAndTerm(companyId, search, status, pageable)
                .map(this::toSummaryResponse);

        log.info("listPaginated() = END - companyId={}, totalElements={}, totalPages={}",
                companyId, page.getTotalElements(), page.getTotalPages());
        return page;
    }

    @Transactional(readOnly = true)
    public CollaboratorResponse findById(Long id, Long companyId) {
        log.info("findById() - START - id={}, companyId={}", id, companyId);

        CollaboratorEntity collaborator = collaboratorRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, CollaboratorStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado com ID: " + id));

        CollaboratorResponse response = toResponse(collaborator);

        log.info("findById() = END - id={}, collaborator: {}", id, response);
        return response;
    }

    @Transactional(readOnly = true)
    public CollaboratorCommissionReportResponse getSoldItemsReport(Long collaboratorId, LocalDate startDate, LocalDate endDate, Long companyId) {
        log.info("getSoldItemsReport() - START - collaboratorId={}, startDate={}, endDate={}, companyId={}", collaboratorId, startDate, endDate, companyId);

        validateReportSoldRequest(collaboratorId, startDate, endDate);

        CollaboratorEntity collaborator = collaboratorRepository.findByIdAndCompanyId(collaboratorId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado com ID: " + collaboratorId));

        List<OrderStatus> allowedOrderStatuses = List.of(OrderStatus.COMPLETED);

        List<OrderItemEntity> items = orderItemRepository.findByCollaboratorAndCompanyAndCreatedAtBetween(
                collaboratorId, companyId, allowedOrderStatuses, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        BigDecimal commissionRate = resolveDefaultCommissionRate(companyId, collaboratorId);
        Map<Long, CommissionEntryEntity> entriesByItemId = resolveEntriesByOrderItemId(companyId, items);

        List<CollaboratorSoldItemResponse> soldItems = items.stream()
                .map(item -> buildReportSoldItemResponse(item, commissionRate, entriesByItemId.get(item.getId())))
                .toList();

        BigDecimal totalSold = soldItems.stream()
                .map(CollaboratorSoldItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCommission = soldItems.stream()
                .map(CollaboratorSoldItemResponse::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CollaboratorCommissionReportHeader header = CollaboratorCommissionReportHeader.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalSold(totalSold)
                .totalCommission(totalCommission)
                .commissionPercentage(commissionRate)
                .build();

        CollaboratorCommissionReportResponse response = CollaboratorCommissionReportResponse.builder()
                .collaboratorId(collaborator.getId())
                .collaboratorName(collaborator.getName())
                .header(header)
                .items(soldItems)
                .build();

        log.info("getSoldItemsReport() = END - collaboratorId={}, totalSold={}, totalCommission={}, itemsCount={}", collaboratorId, totalSold, totalCommission, soldItems.size());
        return response;
    }

    private BigDecimal resolveDefaultCommissionRate(Long companyId, Long collaboratorId) {
        List<CommissionRuleEntity> activeRules = commissionRuleRepository.findByCompanyIdAndStatus(companyId, CommissionRuleStatus.ACTIVE);
        if (activeRules == null || activeRules.isEmpty()) {
            return BigDecimal.ZERO;
        }
        LocalDate today = LocalDate.now();
        List<CommissionRuleEntity> validRules = activeRules.stream()
                .filter(r -> (r.getValidFrom() == null || !today.isBefore(r.getValidFrom()))
                        && (r.getValidUntil() == null || !today.isAfter(r.getValidUntil())))
                .sorted(Comparator.comparing(CommissionRuleEntity::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return validRules.stream()
                .filter(r -> Objects.equals(r.getCollaboratorId(), collaboratorId)
                        && r.getCategoryId() == null && r.getCatalogItemId() == null)
                .findFirst()
                .or(() -> validRules.stream()
                        .filter(r -> r.getCollaboratorId() == null
                                && r.getCategoryId() == null && r.getCatalogItemId() == null)
                        .findFirst())
                .map(CommissionRuleEntity::getCommissionPercentage)
                .orElse(BigDecimal.ZERO);
    }

    private Map<Long, CommissionEntryEntity> resolveEntriesByOrderItemId(Long companyId, List<OrderItemEntity> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> itemIds = items.stream()
                .map(OrderItemEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CommissionEntryEntity> entries = commissionEntryRepository.findByCompanyIdAndSourceTypeAndSourceItemIdInAndStatusNot(
                companyId, CommissionSourceType.ORDER_ITEM, itemIds, CommissionEntryStatus.CANCELED
        );
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyMap();
        }
        return entries.stream().collect(Collectors.toMap(
                CommissionEntryEntity::getSourceItemId,
                Function.identity(),
                (a, b) -> a
        ));
    }

    private static CollaboratorSoldItemResponse buildReportSoldItemResponse(
            OrderItemEntity item,
            BigDecimal commissionRate,
            CommissionEntryEntity entry
    ) {
        BigDecimal itemTotal = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal itemCommission = (entry != null && entry.getCommissionAmount() != null)
                ? entry.getCommissionAmount()
                : itemTotal.multiply(commissionRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return CollaboratorSoldItemResponse.builder()
                .itemId(item.getId())
                .originalOrderId(item.getOrderId())
                .unitPrice(item.getUnitPrice())
                .commissionAmount(itemCommission)
                .date(item.getCreatedAt())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .totalPrice(itemTotal)
                .build();
    }

    private void validateReportSoldRequest(Long collaboratorId, LocalDate startDate, LocalDate endDate) {
        if (collaboratorId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ID do colaborador é obrigatório.");
        }
        if (startDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial é obrigatória.");
        }
        if (endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data final é obrigatória.");
        }
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser posterior à data final.");
        }
    }

    public CollaboratorEntity toEntity(CollaboratorCreateRequest request, Long companyId) {
        return CollaboratorMapper.toEntity(request, companyId);
    }

    public CollaboratorResponse toResponse(CollaboratorEntity entity) {
        return CollaboratorMapper.toResponse(entity);
    }

    public CollaboratorSummaryResponse toSummaryResponse(CollaboratorEntity entity) {
        return CollaboratorMapper.toSummaryResponse(entity);
    }

    private void validateUser(Long userId, Long companyId) {
        log.info("validateUser() - START - userId={}, companyId={}", userId, companyId);

        if (userId != null) {
            userRepository.findByIdAndCompanyId(userId, companyId)
                    .filter(u -> !UserStatus.DELETED.name().equalsIgnoreCase(u.getStatus()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado com ID: " + userId));
        }

        log.info("validateUser() = END - userId={}", userId);
    }
}
