package company.aura.erp.mapper;

import company.aura.erp.dto.catalog.CatalogPhysicalItemCreateRequest;
import company.aura.erp.modules.invoice.domain.InvoiceItemData;
import company.aura.erp.modules.invoice.domain.InvoiceItemTaxData;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public class CatalogItemMapper {

    private static final String NO_GTIN = "SEM GTIN";
    private static final String DEFAULT_CSOSN = "102";
    private static final BigDecimal MIN_UNIT_PRICE = new BigDecimal("0.01");
    private static final BigDecimal MAX_TAX_RATE = new BigDecimal("100.00");

    public static boolean isValidGtinEan(String gtinEan) {
        if (StringUtils.isBlank(gtinEan) || NO_GTIN.equalsIgnoreCase(gtinEan.trim())) {
            return false;
        }
        String digits = digitsOnly(gtinEan);
        return digits != null && digits.length() >= 8 && digits.length() <= 14 && !digits.chars().allMatch(c -> c == '0');
    }

    public static String normalizeGtinEan(String gtinEan) {
        if (!isValidGtinEan(gtinEan)) {
            return NO_GTIN;
        }
        return digitsOnly(gtinEan);
    }

    public static boolean hasIdentifyingCode(InvoiceItemData item) {
        if (item == null) {
            return false;
        }
        return isValidGtinEan(item.getGtinEan()) || StringUtils.isNotBlank(item.getProductCode());
    }

    public static Optional<String> validateCatalogCreatable(InvoiceItemData item) {
        if (item == null) {
            return Optional.of("Dados do item ausentes no XML.");
        }
        if (StringUtils.isBlank(item.getDescription())) {
            return Optional.of("Descrição do item (xProd) ausente no XML.");
        }
        if (StringUtils.isBlank(item.getUnitOfMeasure())) {
            return Optional.of("Unidade de medida (uCom) ausente no XML.");
        }
        String ncm = digitsOnly(item.getNcm());
        if (ncm == null || ncm.length() != 8) {
            return Optional.of("NCM inválido ou ausente no XML (deve conter 8 dígitos).");
        }
        String cfop = digitsOnly(item.getCfop());
        if (cfop == null || cfop.length() != 4) {
            return Optional.of("CFOP inválido ou ausente no XML (deve conter 4 dígitos).");
        }
        BigDecimal resolvedUnitPrice = resolveUnitPrice(item);
        if (resolvedUnitPrice == null || resolvedUnitPrice.compareTo(MIN_UNIT_PRICE) < 0) {
            return Optional.of("Valor unitário inválido ou zerado no XML.");
        }
        return Optional.empty();
    }

    public static CatalogPhysicalItemCreateRequest toPhysicalCreateRequest(InvoiceItemData item, Long categoryId) {
        if (item == null) {
            return null;
        }

        InvoiceItemTaxData taxes = item.getTaxes();
        String internalCode = truncate(trimToNull(item.getProductCode()), 50);
        String description = truncate(trimToNull(item.getDescription()), 255);
        BigDecimal unitPrice = resolveUnitPrice(item).setScale(2, RoundingMode.HALF_UP);
        String unitOfMeasure = truncate(trimToNull(item.getUnitOfMeasure()), 10);
        String ncm = digitsOnly(item.getNcm());
        String cfop = digitsOnly(item.getCfop());
        String csosn = resolveCsosn(taxes);
        String gtinEan = normalizeGtinEan(item.getGtinEan());
        String cest = resolveCest(item.getCest());
        Integer origin = resolveOrigin(taxes);

        BigDecimal defaultIcmsCreditRate = taxes != null ? normalizeRate(taxes.getIcmsCreditRate(), BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal icmsRate = taxes != null ? normalizeRate(taxes.getIcmsRate(), null) : null;
        BigDecimal pisRate = taxes != null ? normalizeRate(taxes.getPisRate(), null) : null;
        BigDecimal cofinsRate = taxes != null ? normalizeRate(taxes.getCofinsRate(), null) : null;

        return new CatalogPhysicalItemCreateRequest(
                internalCode,
                description,
                unitPrice,
                unitOfMeasure,
                ncm,
                cfop,
                csosn,
                gtinEan,
                cest,
                origin,
                defaultIcmsCreditRate,
                icmsRate,
                pisRate,
                cofinsRate,
                categoryId
        );
    }

    private static BigDecimal resolveUnitPrice(InvoiceItemData item) {
        if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(MIN_UNIT_PRICE) >= 0) {
            return item.getUnitPrice();
        }
        if (item.getTotalPrice() != null
                && item.getTotalPrice().compareTo(BigDecimal.ZERO) > 0
                && item.getQuantity() != null
                && item.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            return item.getTotalPrice().divide(item.getQuantity(), 2, RoundingMode.HALF_UP);
        }
        return null;
    }

    private static String resolveCsosn(InvoiceItemTaxData taxes) {
        if (taxes == null || StringUtils.isBlank(taxes.getIcmsCstOrCsosn())) {
            return DEFAULT_CSOSN;
        }
        return truncate(taxes.getIcmsCstOrCsosn().trim(), 4);
    }

    private static String resolveCest(String rawCest) {
        String digits = digitsOnly(rawCest);
        return (digits != null && digits.length() == 7) ? digits : null;
    }

    private static Integer resolveOrigin(InvoiceItemTaxData taxes) {
        if (taxes == null || taxes.getOrigin() == null) {
            return 0;
        }
        int origin = taxes.getOrigin();
        return (origin >= 0 && origin <= 8) ? origin : 0;
    }

    private static BigDecimal normalizeRate(BigDecimal rate, BigDecimal fallback) {
        if (rate == null) {
            return fallback;
        }
        BigDecimal clamped = rate.compareTo(BigDecimal.ZERO) < 0
                ? BigDecimal.ZERO
                : (rate.compareTo(MAX_TAX_RATE) > 0 ? MAX_TAX_RATE : rate);
        return clamped.setScale(2, RoundingMode.HALF_UP);
    }

    private static String digitsOnly(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    private static String trimToNull(String value) {
        return StringUtils.isNotBlank(value) ? value.trim() : null;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
