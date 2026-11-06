package company.aura.erp.service;

import company.aura.erp.dto.invoice.InvoiceCreateRequest;
import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.repository.InvoiceRepository;
import company.aura.erp.repository.PartnerRepository;
import company.aura.erp.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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

    @Transactional
    public InvoiceResponse createExternalInvoice(InvoiceCreateRequest request, MultipartFile xmlFile, MultipartFile pdfFile, UserPrincipal currentUser) {
        log.info("createExternalInvoice() - START - accessKey={}, partnerId={}, companyId={}",
                request != null ? request.accessKey() : null,
                request != null ? request.partnerId() : null,
                currentUser != null ? currentUser.getCompanyId() : null);

        validateInvoiceRequest(request, xmlFile, pdfFile, currentUser);

        UploadedInvoice uploadedInvoice = uploadInvoiceOnBucket(request.accessKey(), xmlFile, pdfFile, currentUser);

        InvoiceEntity saved = saveInvoiceInDatabase(request, uploadedInvoice.xmlUrl(), uploadedInvoice.pdfUrl(), currentUser);

        InvoiceResponse response = InvoiceResponse.fromEntity(saved);

        log.info("createExternalInvoice() - END - invoiceId={}, accessKey={}", response.id(), response.accessKey());
        return response;
    }


    private void validateInvoiceRequest(InvoiceCreateRequest request, MultipartFile xmlFile, MultipartFile danfePdf, UserPrincipal currentUser) {

        if (xmlFile == null || xmlFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo XML da nota fiscal é obrigatório.");
        }
        if (danfePdf == null || danfePdf.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo PDF da DANFE é obrigatório.");
        }

        if(!partnerRepository.existsByIdAndCompanyId(request.partnerId(), currentUser.getCompanyId())){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parceiro não foi encontrado para esta empresa.");
        }

        if (isInvoiceAlreadyExist(request, currentUser)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma nota fiscal cadastrada com a chave de acesso: " + request.accessKey());
        }
    }

    private boolean isInvoiceAlreadyExist(InvoiceCreateRequest request, UserPrincipal currentUser){
        if (StringUtils.isBlank(request.accessKey())) {
            return false;
        }

        String trimmedKey = request.accessKey().trim();

        return invoiceRepository.existsByCompanyIdAndAccessKey(currentUser.getCompanyId(), trimmedKey);
    }

    private UploadedInvoice uploadInvoiceOnBucket(String accessKey, MultipartFile xmlFile, MultipartFile pdfFile, UserPrincipal currentUser) {
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
            return new UploadedInvoice(xmlUrl, pdfUrl);
        }
        catch (Exception e) {
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

    private InvoiceEntity saveInvoiceInDatabase(InvoiceCreateRequest request, String xmlUrl, String pdfUrl, UserPrincipal currentUser) {
        return invoiceRepository.save(buildInvoiceEntity(currentUser, request, xmlUrl, pdfUrl));
    }

    private String uploadFileInBucket(MultipartFile file, String fileKey, String contentType) throws IOException {
        r2Service.upload(fileKey, file.getInputStream(), file.getSize(), contentType);
        return r2Service.getFileUrl(fileKey);
    }

    public InvoiceResponse findById(Long id, Long companyId) {
        log.info("findById() - START - id={}, companyId={}", id, companyId);

        InvoiceEntity invoice = invoiceRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota fiscal não encontrada com ID: " + id));

        InvoiceResponse response = InvoiceResponse.fromEntity(invoice);

        log.info("findById() - END - id={}, companyId={}, invoice: {}", id, companyId, response);
        return response;
    }


    public List<InvoiceResponse> list(Long companyId) {
        log.info("list() - START - companyId={}", companyId);

        List<InvoiceResponse> responses = invoiceRepository.findByCompanyId(companyId)
                .stream()
                .map(InvoiceResponse::fromEntity)
                .toList();

        log.info("list() - END - companyId={}, totalInvoices={}", companyId, responses.size());
        return responses;
    }
}
