package company.aura.erp.service;

import company.aura.erp.dto.invoice.InvoiceCreateRequest;
import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.enums.DocumentType;
import company.aura.erp.mapper.InvoiceMapper;
import company.aura.erp.modules.bucket.service.CloudflareR2Service;
import company.aura.erp.modules.invoice.domain.InvoiceIdentificationData;
import company.aura.erp.modules.invoice.domain.InvoiceTotalsData;
import company.aura.erp.modules.invoice.domain.ParsedInvoiceXml;
import company.aura.erp.modules.invoice.service.InvoiceXmlReaderService;
import company.aura.erp.repository.InvoiceRepository;
import company.aura.erp.repository.PartnerRepository;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.specification.InvoiceSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static company.aura.erp.mapper.InvoiceMapper.buildInvoiceEntity;
import static company.aura.erp.util.InvoiceUtil.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PartnerRepository partnerRepository;
    private final CloudflareR2Service r2Service;
    private final InvoiceXmlReaderService invoiceXmlReaderService;

    @Transactional
    public InvoiceResponse createExternalInvoice(InvoiceCreateRequest request, MultipartFile xmlFile, MultipartFile pdfFile, UserPrincipal currentUser) {
        Long partnerId = request != null ? request.getPartnerId() : null;
        log.info("createExternalInvoice() - START - partnerId={}, companyId={}", partnerId, currentUser.getCompanyId());

        validateExternalInvoiceInputs(request, xmlFile, pdfFile, currentUser);

        ParsedInvoiceXml parsedXml = parseInvoiceXml(xmlFile);
        String accessKey = extractAccessKey(parsedXml);

        validateUniqueAccessKey(accessKey, currentUser);

        UploadedInvoice uploadedInvoice = uploadInvoiceOnBucket(accessKey, xmlFile, pdfFile, currentUser);

        InvoiceEntity saved = saveInvoiceInDatabase(request, parsedXml, uploadedInvoice, currentUser);

        InvoiceResponse response = InvoiceMapper.fromEntity(saved);

        log.info("createExternalInvoice() - END - invoiceId={}, accessKey={}, totalAmount={}",
                response.getId(), response.getAccessKey(), response.getTotalAmount());
        return response;
    }

    @Transactional(readOnly = true)
    public String getPdfUrl(Long invoiceId, UserPrincipal currentUser) {
        log.info("getPdfUrl() - START - invoiceId={}, companyId={}", invoiceId, currentUser.getCompanyId());

        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota fiscal não encontrada com ID: " + invoiceId));

        if (!invoice.getCompanyId().equals(currentUser.getCompanyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A nota fiscal informada não pertence à empresa do usuário logado.");
        }

        String pdfUrl = invoice.getPdfUrl();
        if (pdfUrl == null || pdfUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo PDF não encontrado para esta nota fiscal.");
        }

        String temporaryUrl = r2Service.generateTemporaryUrl(pdfUrl, Duration.ofMinutes(5));
        log.info("getPdfUrl() - END - invoiceId={}, temporaryUrl={}", invoiceId, temporaryUrl);
        return temporaryUrl;
    }

    @Transactional(readOnly = true)
    public String getXmlUrl(Long invoiceId, UserPrincipal currentUser) {
        log.info("getXmlUrl() - START - invoiceId={}, companyId={}", invoiceId, currentUser.getCompanyId());

        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota fiscal não encontrada com ID: " + invoiceId));

        if (!invoice.getCompanyId().equals(currentUser.getCompanyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A nota fiscal informada não pertence à empresa do usuário logado.");
        }

        String xmlUrl = invoice.getXmlUrl();
        if (xmlUrl == null || xmlUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo XML não encontrado para esta nota fiscal.");
        }

        String temporaryUrl = r2Service.generateTemporaryUrl(xmlUrl, Duration.ofMinutes(5));
        log.info("getXmlUrl() - END - invoiceId={}, temporaryUrl={}", invoiceId, temporaryUrl);
        return temporaryUrl;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(Long id, Long companyId) {
        log.info("findById() - START - id={}, companyId={}", id, companyId);

        InvoiceEntity invoice = invoiceRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota fiscal não encontrada com ID: " + id));

        InvoiceResponse response = InvoiceMapper.fromEntity(invoice);

        log.info("findById() - END - id={}, companyId={}", id, companyId);
        return response;
    }

    public Page<InvoiceResponse> list(
            Long companyId,
            List<Long> partnerIds,
            String externalReference,
            Integer operationType,
            DocumentType documentType,
            String status,
            Pageable pageable
    ) {
        log.info("list() - START - companyId={}, partnerIds={}, externalReference={}, operationType={}, documentType={}, status={}, pageable={}",
                companyId, partnerIds, externalReference, operationType, documentType, status, pageable);

        Specification<InvoiceEntity> spec = InvoiceSpecification.filter(
                companyId, partnerIds, externalReference, operationType, documentType, status
        );

        Page<InvoiceResponse> responses = invoiceRepository.findAll(spec, pageable)
                .map(InvoiceMapper::fromEntity);

        log.info("list() - END - companyId={}, totalInvoices={}, totalPages={}", companyId, responses.getTotalElements(), responses.getTotalPages());
        return responses;
    }

    private void validateExternalInvoiceInputs(InvoiceCreateRequest request, MultipartFile xmlFile, MultipartFile danfePdf, UserPrincipal currentUser) {
        log.info("validateExternalInvoiceInputs() - START - partnerId={}, companyId={}",
                request != null ? request.getPartnerId() : null, currentUser.getCompanyId());

        if (request == null || request.getPartnerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ID do parceiro (cliente/destinatário) é obrigatório.");
        }
        if (xmlFile == null || xmlFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo XML da nota fiscal é obrigatório.");
        }
        if (danfePdf == null || danfePdf.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo PDF da DANFE é obrigatório.");
        }
        if (!partnerRepository.existsByIdAndCompanyId(request.getPartnerId(), currentUser.getCompanyId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parceiro não foi encontrado para esta empresa.");
        }

        log.info("validateExternalInvoiceInputs() - END - partnerId={}", request.getPartnerId());
    }

    private ParsedInvoiceXml parseInvoiceXml(MultipartFile xmlFile) {
        log.info("parseInvoiceXml() - START - fileName={}", xmlFile.getOriginalFilename());
        ParsedInvoiceXml parsedXml = invoiceXmlReaderService.read(xmlFile);

        InvoiceIdentificationData id = parsedXml.getIdentification();
        InvoiceTotalsData totals = parsedXml.getTotals();

        log.info("parseInvoiceXml() - END - accessKey={}, documentType={}, invoiceNumber={}, totalAmount={}",
                id != null ? id.getAccessKey() : null,
                id != null ? id.getDocumentType() : null,
                id != null ? id.getInvoiceNumber() : null,
                totals != null ? totals.getTotalAmount() : null);
        return parsedXml;
    }

    private void validateUniqueAccessKey(String accessKey, UserPrincipal currentUser) {
        if (StringUtils.isBlank(accessKey)) {
            log.warn("validateUniqueAccessKey() - WARN - Access key absent in XML for companyId={}", currentUser.getCompanyId());
            return;
        }
        String trimmedKey = accessKey.trim();
        if (invoiceRepository.existsByCompanyIdAndAccessKey(currentUser.getCompanyId(), trimmedKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma nota fiscal cadastrada com a chave de acesso: " + trimmedKey);
        }
    }

    private String extractAccessKey(ParsedInvoiceXml parsedXml) {
        if (parsedXml == null || parsedXml.getIdentification() == null) {
            return null;
        }
        String accessKey = parsedXml.getIdentification().getAccessKey();
        return StringUtils.isNotBlank(accessKey) ? accessKey.trim() : null;
    }

    private UploadedInvoice uploadInvoiceOnBucket(String accessKey, MultipartFile xmlFile, MultipartFile pdfFile, UserPrincipal currentUser) {
        log.info("uploadInvoiceOnBucket() - START - accessKey={}, companyId={}", accessKey, currentUser.getCompanyId());
        String baseFolder = buildCompanyFolderName(currentUser);
        String invoiceIdentifier = buildInvoiceIdentifier(accessKey);

        String xmlOriginalName = buildXmlOriginalFileName(xmlFile);
        String xmlKey = buildXmlBucketKey(baseFolder, invoiceIdentifier, xmlOriginalName);

        String pdfOriginalName = buildPdfOriginalName(pdfFile);
        String pdfKey = buildPdfBucketKey(baseFolder, invoiceIdentifier, pdfOriginalName);

        String xmlContentType = buildXmlContentType(xmlFile);
        String pdfContentType = buildPdfContentType(pdfFile);

        try {
            String xmlUrl = uploadFileInBucket(xmlFile, xmlKey, xmlContentType);
            String pdfUrl = uploadFileInBucket(pdfFile, pdfKey, pdfContentType);
            log.info("uploadInvoiceOnBucket() - END - xmlKey={}, pdfKey={}", xmlKey, pdfKey);
            return new UploadedInvoice(xmlUrl, pdfUrl);
        } catch (Exception e) {
            log.error("uploadInvoiceOnBucket() - ERROR - Error when upload invoice in bucket - error: {}", e.getMessage());
            deleteFileOnBucket(xmlKey);
            deleteFileOnBucket(pdfKey);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível fazer o upload do arquivo", e);
        }
    }

    private void deleteFileOnBucket(String xmlKey) {
        try {
            r2Service.delete(xmlKey);
        } catch (Exception ex) {
            log.warn("deleteFileOnBucket() - ERROR - Error when delete file in bucket - key: {}, error: {}", xmlKey, ex.getMessage());
        }
    }

    private record UploadedInvoice(String xmlUrl, String pdfUrl) {
    }

    private InvoiceEntity saveInvoiceInDatabase(
            InvoiceCreateRequest request,
            ParsedInvoiceXml parsedXml,
            UploadedInvoice uploadedInvoice,
            UserPrincipal currentUser
    ) {
        log.info("saveInvoiceInDatabase() - START - partnerId={}, companyId={}", request.getPartnerId(), currentUser.getCompanyId());
        InvoiceEntity entity = buildInvoiceEntity(currentUser, request, parsedXml, uploadedInvoice.xmlUrl(), uploadedInvoice.pdfUrl());
        InvoiceEntity saved = invoiceRepository.save(entity);
        log.info("saveInvoiceInDatabase() - END - invoiceId={}", saved.getId());
        return saved;
    }

    private String uploadFileInBucket(MultipartFile file, String fileKey, String contentType) throws IOException {
        r2Service.upload(fileKey, file.getInputStream(), file.getSize(), contentType);
        return r2Service.getFileUrl(fileKey);
    }

}
