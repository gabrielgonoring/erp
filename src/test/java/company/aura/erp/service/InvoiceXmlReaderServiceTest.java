package company.aura.erp.service;

import company.aura.erp.dto.invoice.InvoiceCreateRequest;
import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.EnvironmentType;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.PersonType;
import company.aura.erp.mapper.InvoiceMapper;
import company.aura.erp.modules.invoice.domain.InvoiceAdditionalInfoData;
import company.aura.erp.modules.invoice.domain.InvoiceBillingData;
import company.aura.erp.modules.invoice.domain.InvoiceFreightData;
import company.aura.erp.modules.invoice.domain.InvoiceIdentificationData;
import company.aura.erp.modules.invoice.domain.InvoiceItemData;
import company.aura.erp.modules.invoice.domain.InvoicePartnerData;
import company.aura.erp.modules.invoice.domain.InvoiceProtocolData;
import company.aura.erp.modules.invoice.domain.InvoiceTotalsData;
import company.aura.erp.modules.invoice.domain.ParsedInvoiceXml;
import company.aura.erp.modules.invoice.enumerate.FreightMode;
import company.aura.erp.modules.invoice.enumerate.SefazPaymentCode;
import company.aura.erp.modules.invoice.exception.InvoiceXmlParseException;
import company.aura.erp.modules.invoice.parser.SecureXmlDocumentFactory;
import company.aura.erp.modules.invoice.parser.XmlElementReader;
import company.aura.erp.modules.invoice.parser.extractor.AdditionalInfoXmlExtractor;
import company.aura.erp.modules.invoice.parser.extractor.BillingXmlExtractor;
import company.aura.erp.modules.invoice.parser.extractor.FreightXmlExtractor;
import company.aura.erp.modules.invoice.parser.extractor.IdentificationXmlExtractor;
import company.aura.erp.modules.invoice.parser.extractor.ItemXmlExtractor;
import company.aura.erp.modules.invoice.parser.extractor.PartnerXmlExtractor;
import company.aura.erp.modules.invoice.parser.extractor.ProtocolXmlExtractor;
import company.aura.erp.modules.invoice.parser.extractor.TotalsXmlExtractor;
import company.aura.erp.modules.invoice.service.InvoiceXmlReaderService;
import company.aura.erp.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceXmlReaderServiceTest {

    private InvoiceXmlReaderService readerService;

    private static final String FULL_NFE_PROC_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <nfeProc versao="4.00" xmlns="http://www.portalfiscal.inf.br/nfe">
              <NFe xmlns="http://www.portalfiscal.inf.br/nfe">
                <infNFe Id="NFe35261112345678000195550010000012341000012345" versao="4.00">
                  <ide>
                    <cUF>35</cUF>
                    <cNF>00001234</cNF>
                    <natOp>VENDA DE MERCADORIA</natOp>
                    <mod>55</mod>
                    <serie>1</serie>
                    <nNF>1234</nNF>
                    <dhEmi>2026-11-27T10:15:30-03:00</dhEmi>
                    <dhSaiEnt>2026-11-27T14:00:00-03:00</dhSaiEnt>
                    <tpNF>1</tpNF>
                    <idDest>1</idDest>
                    <cMunFG>3550308</cMunFG>
                    <tpImp>1</tpImp>
                    <tpEmis>1</tpEmis>
                    <tpAmb>1</tpAmb>
                    <finNFe>1</finNFe>
                    <indFinal>0</indFinal>
                    <indPres>1</indPres>
                    <NFref>
                      <refNFe>35261012345678000195550010000009991000009999</refNFe>
                    </NFref>
                  </ide>
                  <emit>
                    <CNPJ>12345678000195</CNPJ>
                    <xNome>AURA SISTEMAS LTDA</xNome>
                    <xFant>AURA ERP</xFant>
                    <enderEmit>
                      <xLgr>Av Paulista</xLgr>
                      <nro>1000</nro>
                      <xCpl>Conj 101</xCpl>
                      <xBairro>Bela Vista</xBairro>
                      <cMun>3550308</cMun>
                      <xMun>Sao Paulo</xMun>
                      <UF>SP</UF>
                      <CEP>01310-100</CEP>
                      <cPais>1058</cPais>
                      <xPais>BRASIL</xPais>
                      <fone>(11) 3333-4444</fone>
                    </enderEmit>
                    <IE>111222333444</IE>
                    <IM>987654</IM>
                    <CNAE>6201501</CNAE>
                    <CRT>1</CRT>
                  </emit>
                  <dest>
                    <CNPJ>98765432000188</CNPJ>
                    <xNome>CLIENTE PARCEIRO COMERCIO SA</xNome>
                    <enderDest>
                      <xLgr>Rua das Flores</xLgr>
                      <nro>250</nro>
                      <xBairro>Centro</xBairro>
                      <cMun>3550308</cMun>
                      <xMun>Sao Paulo</xMun>
                      <UF>SP</UF>
                      <CEP>01001000</CEP>
                      <cPais>1058</cPais>
                      <xPais>Brasil</xPais>
                      <fone>11999998888</fone>
                    </enderDest>
                    <indIEDest>1</indIEDest>
                    <IE>555666777888</IE>
                    <email>fiscal@clienteparceiro.com.br</email>
                  </dest>
                  <det nItem="1">
                    <prod>
                      <cProd>PROD-001</cProd>
                      <cEAN>7891234567890</cEAN>
                      <xProd>Notebook Corporativo Pro</xProd>
                      <NCM>84713012</NCM>
                      <CEST>2103000</CEST>
                      <CFOP>5102</CFOP>
                      <uCom>UN</uCom>
                      <qCom>2.0000</qCom>
                      <vUnCom>3500.00</vUnCom>
                      <vProd>7000.00</vProd>
                      <cEANTrib>7891234567890</cEANTrib>
                      <uTrib>UN</uTrib>
                      <qTrib>2.0000</qTrib>
                      <vUnTrib>3500.00</vUnTrib>
                      <vFrete>100.00</vFrete>
                      <vDesc>200.00</vDesc>
                      <vOutro>50.00</vOutro>
                      <indTot>1</indTot>
                      <xPed>PED-2026-88</xPed>
                      <nItemPed>1</nItemPed>
                    </prod>
                    <imposto>
                      <vTotTrib>1120.00</vTotTrib>
                      <ICMS>
                        <ICMSSN101>
                          <orig>0</orig>
                          <CSOSN>101</CSOSN>
                          <pCredSN>2.82</pCredSN>
                          <vCredICMSSN>197.40</vCredICMSSN>
                        </ICMSSN101>
                      </ICMS>
                      <IPI>
                        <IPITrib>
                          <CST>50</CST>
                          <vBC>7000.00</vBC>
                          <pIPI>5.00</pIPI>
                          <vIPI>350.00</vIPI>
                        </IPITrib>
                      </IPI>
                      <PIS>
                        <PISAliq>
                          <CST>01</CST>
                          <vBC>7000.00</vBC>
                          <pPIS>0.65</pPIS>
                          <vPIS>45.50</vPIS>
                        </PISAliq>
                      </PIS>
                      <COFINS>
                        <COFINSAliq>
                          <CST>01</CST>
                          <vBC>7000.00</vBC>
                          <pCOFINS>3.00</pCOFINS>
                          <vCOFINS>210.00</vCOFINS>
                        </COFINSAliq>
                      </COFINS>
                    </imposto>
                    <infAdProd>Garantia de 12 meses</infAdProd>
                  </det>
                  <total>
                    <ICMSTot>
                      <vBC>0.00</vBC>
                      <vICMS>0.00</vICMS>
                      <vICMSDeson>0.00</vICMSDeson>
                      <vFCP>0.00</vFCP>
                      <vBCST>0.00</vBCST>
                      <vST>0.00</vST>
                      <vProd>7000.00</vProd>
                      <vFrete>100.00</vFrete>
                      <vSeg>0.00</vSeg>
                      <vDesc>200.00</vDesc>
                      <vII>0.00</vII>
                      <vIPI>350.00</vIPI>
                      <vIPIDevol>0.00</vIPIDevol>
                      <vPIS>45.50</vPIS>
                      <vCOFINS>210.00</vCOFINS>
                      <vOutro>50.00</vOutro>
                      <vNF>7300.00</vNF>
                      <vTotTrib>1120.00</vTotTrib>
                    </ICMSTot>
                  </total>
                  <transp>
                    <modFrete>0</modFrete>
                    <transporta>
                      <CNPJ>44555666000177</CNPJ>
                      <xNome>TRANSPORTADORA RAPIDA LTDA</xNome>
                      <IE>99887766</IE>
                      <xEnder>Rodovia Anhanguera Km 20</xEnder>
                      <xMun>Campinas</xMun>
                      <UF>SP</UF>
                    </transporta>
                    <veicTransp>
                      <placa>ABC1D23</placa>
                      <UF>SP</UF>
                      <RNTC>12345678</RNTC>
                    </veicTransp>
                    <vol>
                      <qVol>2</qVol>
                      <esp>CAIXA</esp>
                      <marca>AURA</marca>
                      <nVol>001-002</nVol>
                      <pesoL>4.500</pesoL>
                      <pesoB>5.200</pesoB>
                    </vol>
                  </transp>
                  <cobr>
                    <fat>
                      <nFat>FAT-1234</nFat>
                      <vOrig>7500.00</vOrig>
                      <vDesc>200.00</vDesc>
                      <vLiq>7300.00</vLiq>
                    </fat>
                    <dup>
                      <nDup>001</nDup>
                      <dVenc>2026-12-27</dVenc>
                      <vDup>3650.00</vDup>
                    </dup>
                    <dup>
                      <nDup>002</nDup>
                      <dVenc>2027-01-27</dVenc>
                      <vDup>3650.00</vDup>
                    </dup>
                  </cobr>
                  <pag>
                    <detPag>
                      <indPag>1</indPag>
                      <tPag>15</tPag>
                      <xPag>Boleto Bancario</xPag>
                      <vPag>7300.00</vPag>
                    </detPag>
                    <vTroco>0.00</vTroco>
                  </pag>
                  <infAdic>
                    <infAdFisco>Documento emitido por ME/EPP optante pelo Simples Nacional</infAdFisco>
                    <infCpl>Pedido referente ao contrato #88</infCpl>
                    <obsCont xCampo="VENDEDOR">
                      <xTexto>Gabriel</xTexto>
                    </obsCont>
                    <obsFisco xCampo="REGIME">
                      <xTexto>SN</xTexto>
                    </obsFisco>
                  </infAdic>
                </infNFe>
              </NFe>
              <protNFe versao="4.00">
                <infProt>
                  <tpAmb>1</tpAmb>
                  <verAplic>SP_NFE_PL009_V4</verAplic>
                  <chNFe>35261112345678000195550010000012341000012345</chNFe>
                  <dhRecbto>2026-11-27T10:16:00-03:00</dhRecbto>
                  <nProt>135260000999999</nProt>
                  <digVal>abc123digest==</digVal>
                  <cStat>100</cStat>
                  <xMotivo>Autorizado o uso da NF-e</xMotivo>
                </infProt>
              </protNFe>
            </nfeProc>
            """;

    @BeforeEach
    void setUp() {
        XmlElementReader elementReader = new XmlElementReader();
        SecureXmlDocumentFactory documentFactory = new SecureXmlDocumentFactory(elementReader);
        readerService = new InvoiceXmlReaderService(
                documentFactory,
                new IdentificationXmlExtractor(),
                new ProtocolXmlExtractor(),
                new PartnerXmlExtractor(),
                new ItemXmlExtractor(),
                new TotalsXmlExtractor(),
                new FreightXmlExtractor(),
                new BillingXmlExtractor(),
                new AdditionalInfoXmlExtractor()
        );
    }

    @Test
    @DisplayName("Deve ler XML completo de NF-e e retornar todos os domínios populados")
    void shouldReadFullNfeProcXmlSuccessfully() {
        ParsedInvoiceXml parsed = readerService.read(FULL_NFE_PROC_XML);

        assertNotNull(parsed);

        InvoiceIdentificationData id = parsed.getIdentification();
        assertNotNull(id);
        assertEquals("35261112345678000195550010000012341000012345", id.getAccessKey());
        assertEquals(DocumentType.NFE, id.getDocumentType());
        assertEquals(EnvironmentType.PRD, id.getEnvironment());
        assertEquals(1234, id.getInvoiceNumber());
        assertEquals(1, id.getSeries());
        assertEquals(1, id.getOperationType());
        assertEquals("VENDA DE MERCADORIA", id.getOperationNature());
        assertEquals(LocalDateTime.of(2026, 11, 27, 10, 15, 30), id.getIssuedAt());
        assertEquals(List.of("35261012345678000195550010000009991000009999"), id.getReferencedAccessKeys());

        InvoiceProtocolData protocol = parsed.getProtocol();
        assertNotNull(protocol);
        assertTrue(protocol.isAuthorized());
        assertEquals("100", protocol.getStatusCode());
        assertEquals("135260000999999", protocol.getAuthorizationProtocol());

        InvoicePartnerData emitter = parsed.getEmitter();
        assertNotNull(emitter);
        assertEquals(PersonType.PJ, emitter.getPersonType());
        assertEquals("12345678000195", emitter.getDocument());
        assertEquals("AURA SISTEMAS LTDA", emitter.getName());
        assertEquals("01310100", emitter.getAddress().getZipCode());
        assertEquals("1133334444", emitter.getPhone());

        InvoicePartnerData recipient = parsed.getRecipient();
        assertNotNull(recipient);
        assertEquals(PersonType.PJ, recipient.getPersonType());
        assertEquals("98765432000188", recipient.getDocument());
        assertEquals("fiscal@clienteparceiro.com.br", recipient.getEmail());
        assertEquals(recipient, parsed.getPartner());

        assertEquals(1, parsed.getItems().size());
        InvoiceItemData item = parsed.getItems().get(0);
        assertEquals(1, item.getItemNumber());
        assertEquals("PROD-001", item.getProductCode());
        assertEquals("84713012", item.getNcm());
        assertEquals("5102", item.getCfop());
        assertEquals(new BigDecimal("2.0000"), item.getQuantity());
        assertEquals(new BigDecimal("7000.00"), item.getTotalPrice());
        assertEquals(new BigDecimal("6950.00"), item.getNetTotalPrice());
        assertEquals("101", item.getTaxes().getIcmsCstOrCsosn());
        assertEquals(new BigDecimal("197.40"), item.getTaxes().getIcmsCreditAmount());
        assertEquals(new BigDecimal("350.00"), item.getTaxes().getIpiAmount());
        assertEquals(new BigDecimal("45.50"), item.getTaxes().getPisAmount());
        assertEquals(new BigDecimal("210.00"), item.getTaxes().getCofinsAmount());

        InvoiceTotalsData totals = parsed.getTotals();
        assertEquals(new BigDecimal("7000.00"), totals.getProductsAmount());
        assertEquals(new BigDecimal("100.00"), totals.getFreightAmount());
        assertEquals(new BigDecimal("200.00"), totals.getDiscountAmount());
        assertEquals(new BigDecimal("7300.00"), totals.getTotalAmount());

        InvoiceFreightData freight = parsed.getFreight();
        assertEquals(FreightMode.SENDER, freight.getFreightMode());
        assertEquals(0, freight.getFreightModeCode());
        assertNotNull(freight.getCarrier());
        assertEquals("44555666000177", freight.getCarrier().getDocument());
        assertEquals("ABC1D23", freight.getVehiclePlate());
        assertEquals(1, freight.getVolumes().size());
        assertEquals(2, freight.getVolumes().get(0).getQuantity());
        assertEquals(new BigDecimal("5.200"), freight.getVolumes().get(0).getGrossWeightKg());

        InvoiceBillingData billing = parsed.getBilling();
        assertNotNull(billing.getInvoiceSummary());
        assertEquals("FAT-1234", billing.getInvoiceSummary().getInvoiceNumber());
        assertEquals(new BigDecimal("7300.00"), billing.getInvoiceSummary().getNetAmount());
        assertEquals(2, billing.getInstallments().size());
        assertEquals(LocalDate.of(2026, 12, 27), billing.getInstallments().get(0).getDueDate());
        assertEquals(1, billing.getPayments().size());
        assertEquals(SefazPaymentCode.BOLETO_BANCARIO, billing.getPayments().get(0).getSefazPaymentCode());
        assertEquals(PaymentMethod.BOLETO, billing.getPayments().get(0).getPaymentMethod());

        InvoiceAdditionalInfoData additionalInfo = parsed.getAdditionalInfo();
        assertEquals("Pedido referente ao contrato #88", additionalInfo.getTaxpayerInfo());
        assertEquals(1, additionalInfo.getTaxpayerObservations().size());
        assertEquals("VENDEDOR", additionalInfo.getTaxpayerObservations().get(0).getField());
        assertEquals("Gabriel", additionalInfo.getTaxpayerObservations().get(0).getText());
    }

    @Test
    @DisplayName("Deve ler XML por MultipartFile, byte[] e InputStream incluindo BOM UTF-8")
    void shouldReadXmlFromMultipleInputSources() {
        String xmlWithBom = "\uFEFF" + FULL_NFE_PROC_XML;
        byte[] bytes = xmlWithBom.getBytes(StandardCharsets.UTF_8);

        MockMultipartFile multipartFile = new MockMultipartFile("xmlFile", "nfe.xml", "application/xml", bytes);

        ParsedInvoiceXml fromMultipart = readerService.read(multipartFile);
        ParsedInvoiceXml fromBytes = readerService.read(bytes);
        ParsedInvoiceXml fromStream = readerService.read(new ByteArrayInputStream(bytes));

        assertEquals("35261112345678000195550010000012341000012345", fromMultipart.getIdentification().getAccessKey());
        assertEquals("35261112345678000195550010000012341000012345", fromBytes.getIdentification().getAccessKey());
        assertEquals("35261112345678000195550010000012341000012345", fromStream.getIdentification().getAccessKey());
    }

    @Test
    @DisplayName("Deve executar métodos individuais por domínio da nota fiscal")
    void shouldExecuteDomainSpecificReaderMethods() {
        assertEquals(1234, readerService.readIdentification(FULL_NFE_PROC_XML).getInvoiceNumber());
        assertEquals("12345678000195", readerService.readEmitter(FULL_NFE_PROC_XML).getDocument());
        assertEquals("98765432000188", readerService.readRecipient(FULL_NFE_PROC_XML).getDocument());
        assertEquals("98765432000188", readerService.readPartner(FULL_NFE_PROC_XML).getDocument());
        assertEquals(1, readerService.readItems(FULL_NFE_PROC_XML).size());
        assertEquals(new BigDecimal("7300.00"), readerService.readTotals(FULL_NFE_PROC_XML).getTotalAmount());
        assertEquals("ABC1D23", readerService.readFreight(FULL_NFE_PROC_XML).getVehiclePlate());
        assertEquals(2, readerService.readBilling(FULL_NFE_PROC_XML).getInstallments().size());
        assertEquals("100", readerService.readProtocol(FULL_NFE_PROC_XML).getStatusCode());
        assertNotNull(readerService.readAdditionalInfo(FULL_NFE_PROC_XML).getFiscoInfo());
    }

    @Test
    @DisplayName("Deve resolver parceiro contraparte com base no CNPJ da própria empresa")
    void shouldResolveCounterpartyPartnerByCompanyCnpj() {
        ParsedInvoiceXml parsed = readerService.read(FULL_NFE_PROC_XML);

        InvoicePartnerData whenCompanyIsEmitter = readerService.resolveCounterpartyPartner(parsed, "12.345.678/0001-95");
        InvoicePartnerData whenCompanyIsRecipient = readerService.resolveCounterpartyPartner(parsed, "98.765.432/0001-88");

        assertEquals("98765432000188", whenCompanyIsEmitter.getDocument());
        assertEquals("12345678000195", whenCompanyIsRecipient.getDocument());
    }

    @Test
    @DisplayName("Deve ler NFC-e sem nfeProc e destinatário Pessoa Física (CPF)")
    void shouldReadNfceWithoutNfeProcAndNaturalPersonRecipient() {
        String nfceXml = """
                <NFe>
                  <infNFe Id="NFe35261112345678000195650010000000101000000101" versao="4.00">
                    <ide>
                      <natOp>VENDA CONSUMIDOR</natOp>
                      <mod>65</mod>
                      <serie>1</serie>
                      <nNF>10</nNF>
                      <dhEmi>2026-11-27T11:00:00-03:00</dhEmi>
                      <tpNF>1</tpNF>
                      <tpAmb>2</tpAmb>
                    </ide>
                    <emit>
                      <CNPJ>12345678000195</CNPJ>
                      <xNome>LOJA TESTE</xNome>
                    </emit>
                    <dest>
                      <CPF>12345678909</CPF>
                      <xNome>JOAO DA SILVA</xNome>
                      <indIEDest>9</indIEDest>
                    </dest>
                    <det nItem="1">
                      <prod>
                        <cProd>10</cProd>
                        <xProd>Cafe Expresso</xProd>
                        <NCM>09012100</NCM>
                        <CFOP>5102</CFOP>
                        <uCom>UN</uCom>
                        <qCom>1</qCom>
                        <vUnCom>10.00</vUnCom>
                        <vProd>10.00</vProd>
                      </prod>
                      <imposto>
                        <ICMS>
                          <ICMS00>
                            <orig>0</orig>
                            <CST>00</CST>
                            <vBC>10.00</vBC>
                            <pICMS>18.00</pICMS>
                            <vICMS>1.80</vICMS>
                          </ICMS00>
                        </ICMS>
                      </imposto>
                    </det>
                    <total>
                      <ICMSTot>
                        <vProd>10.00</vProd>
                        <vNF>10.00</vNF>
                      </ICMSTot>
                    </total>
                    <pag>
                      <detPag>
                        <tPag>17</tPag>
                        <vPag>10.00</vPag>
                      </detPag>
                    </pag>
                  </infNFe>
                </NFe>
                """;

        ParsedInvoiceXml parsed = readerService.read(nfceXml);

        assertEquals(DocumentType.NFCE, parsed.getIdentification().getDocumentType());
        assertEquals(EnvironmentType.STG, parsed.getIdentification().getEnvironment());
        assertNull(parsed.getProtocol());
        assertEquals(PersonType.PF, parsed.getRecipient().getPersonType());
        assertEquals("12345678909", parsed.getRecipient().getDocument());
        assertEquals("00", parsed.getItems().get(0).getTaxes().getIcmsCstOrCsosn());
        assertEquals(PaymentMethod.PIX, parsed.getBilling().getPayments().get(0).getPaymentMethod());
        assertEquals(FreightMode.NO_FREIGHT, parsed.getFreight().getFreightMode());
    }

    @Test
    @DisplayName("Deve converter ParsedInvoiceXml para InvoiceEntity via InvoiceMapper")
    void shouldMapParsedXmlToInvoiceEntityViaInvoiceMapper() {
        ParsedInvoiceXml parsed = readerService.read(FULL_NFE_PROC_XML);
        UserPrincipal user = UserPrincipal.builder()
                .id(1L)
                .companyId(10L)
                .email("user@company.com")
                .authorities(Collections.emptyList())
                .build();

        InvoiceEntity invoiceEntity = InvoiceMapper.buildInvoiceEntity(
                user, new InvoiceCreateRequest(5L), parsed, "https://r2/nfe.xml", "https://r2/danfe.pdf"
        );

        assertNotNull(invoiceEntity);
        assertEquals(10L, invoiceEntity.getCompanyId());
        assertEquals(5L, invoiceEntity.getPartnerId());
        assertEquals("35261112345678000195550010000012341000012345", invoiceEntity.getAccessKey());
        assertEquals("135260000999999", invoiceEntity.getAuthorizationProtocol());
        assertEquals("BOLETO", invoiceEntity.getPaymentMethod());
        assertEquals(LocalDate.of(2026, 12, 27), invoiceEntity.getDueDate());
        assertEquals(new BigDecimal("7300.00"), invoiceEntity.getTotalAmount());
    }

    @Test
    @DisplayName("Deve lançar InvoiceXmlParseException quando XML for vazio, inválido ou conter DOCTYPE")
    void shouldThrowInvoiceXmlParseExceptionOnInvalidInputs() {
        assertThrows(InvoiceXmlParseException.class, () -> readerService.read("   "));
        assertThrows(InvoiceXmlParseException.class, () -> readerService.read("<NFe><outro/></NFe>"));
        assertThrows(InvoiceXmlParseException.class, () -> readerService.read("<NFe><unclosed></NFe>"));

        String xxeXml = """
                <?xml version="1.0"?>
                <!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
                <NFe><infNFe Id="NFe123"/></NFe>
                """;
        assertThrows(InvoiceXmlParseException.class, () -> readerService.read(xxeXml));
    }
}
