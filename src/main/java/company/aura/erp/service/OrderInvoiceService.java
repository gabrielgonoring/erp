package company.aura.erp.service;

import company.aura.erp.dto.invoice.InvoiceCreateRequest;
import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.dto.order.LinkOrderInvoice;
import company.aura.erp.dto.order.OrderExternalInvoiceRequest;
import company.aura.erp.dto.order.OrderInvoiceResponse;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderInvoiceEntity;
import company.aura.erp.enums.*;
import company.aura.erp.mapper.OrderInvoiceMapper;
import company.aura.erp.repository.OrderInvoiceRepository;
import company.aura.erp.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderInvoiceService {

    private final OrderInvoiceRepository orderInvoiceRepository;
    private final InvoiceService invoiceService;

    public OrderInvoiceResponse linkInvoice(Long invoiceId, LinkOrderInvoice request, UserPrincipal currentUser, OrderEntity order) {

        if (orderInvoiceRepository.existsByOrderIdAndInvoiceIdAndInvoicePurpose(order.getId(), invoiceId, request.getPurpose())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta nota fiscal já está vinculada a este pedido com a mesma finalidade.");
        }

        OrderInvoiceEntity orderInvoice = OrderInvoiceEntity.builder()
                .companyId(currentUser.getCompanyId())
                .orderId(order.getId())
                .invoiceId(invoiceId)
                .invoicePurpose(request.getPurpose())
                .notes(request.getNotes())
                .build();

        return OrderInvoiceMapper.fromEntity(orderInvoiceRepository.save(orderInvoice));
    }

    @Transactional
    public InvoiceResponse linkExternalInvoice(OrderEntity order, OrderExternalInvoiceRequest request, MultipartFile xmlFile, MultipartFile pdfFile, UserPrincipal currentUser) {

        validateExternalInvoiceRequest(request);

        InvoicePurpose purpose = request.getInvoicePurpose() != null
                ? request.getInvoicePurpose()
                : (request.getDocumentType() == DocumentType.NFSE ? InvoicePurpose.SERVICE : InvoicePurpose.PRODUCT);

        //todo: implementar a leitura do pdf ou xml para recuperar as informaçoes
        InvoiceCreateRequest invoiceRequest = InvoiceCreateRequest.builder()
                .partnerId(order.getPartnerId())
                .totalAmount(request.getTotalAmount() != null ? request.getTotalAmount() : order.getTotalAmount())
                .documentType(request.getDocumentType())
                .environment(EnvironmentType.PRD)
                .status(InvoiceStatus.ISSUED.name())
                .operationType(request.getOperationType())
                .externalReference(request.getExternalReference() != null ? request.getExternalReference() : "ORDER-" + order.getId())
                .build();

        InvoiceResponse invoiceResponse = invoiceService.createExternalInvoice(invoiceRequest, xmlFile, pdfFile, currentUser);

        OrderInvoiceEntity orderInvoice = OrderInvoiceEntity.builder()
                .companyId(currentUser.getCompanyId())
                .orderId(order.getId())
                .invoiceId(invoiceResponse.getId())
                .invoicePurpose(purpose)
                .notes(request.getNotes())
                .build();
        orderInvoiceRepository.save(orderInvoice);

        return invoiceResponse;
    }

    private void validateExternalInvoiceRequest(OrderExternalInvoiceRequest request) {
        if (request == null || request.getDocumentType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tipo de nota fiscal é obrigatório.");
        }

        if (request.getDocumentType() != DocumentType.NFE && request.getDocumentType() != DocumentType.NFSE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de nota fiscal não suportado para vinculação ao pedido. Utilize NFE ou NFSE.");
        }

        if (request.getOperationType()  == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tipo de operação é obrigatório");
        }

        if (request.getTotalAmount() != null && request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O valor total da nota fiscal deve ser maior que zero.");
        }
    }

    public List<OrderInvoiceResponse> findInvoicesByOrderId(Long orderId, Long companyId) {
        return orderInvoiceRepository.findByCompanyIdAndOrderId(companyId, orderId)
                .stream()
                .map(OrderInvoiceMapper::fromEntity)
                .toList();
    }



}
