package company.aura.erp.mapper;

import company.aura.erp.dto.partner.PartnerCreateRequest;
import company.aura.erp.enums.PersonType;
import company.aura.erp.modules.invoice.domain.InvoiceAddressData;
import company.aura.erp.modules.invoice.domain.InvoiceCarrierData;
import company.aura.erp.modules.invoice.domain.InvoicePartnerData;
import org.apache.commons.lang3.StringUtils;

public class PartnerMapper {

    private static final int NON_TAXPAYER_IE_INDICATOR = 9;
    private static final int TAXPAYER_IE_INDICATOR = 1;
    private static final String DEFAULT_COUNTRY_CODE = "1058";
    private static final String DEFAULT_COUNTRY_NAME = "Brasil";

    public static String normalizeDocument(String rawDocument) {
        return digitsOnly(rawDocument);
    }

    public static boolean hasValidDocument(String rawDocument) {
        String digits = normalizeDocument(rawDocument);
        if (digits == null) {
            return false;
        }
        boolean validLength = digits.length() == 11 || digits.length() == 14;
        return validLength && !digits.chars().allMatch(c -> c == '0');
    }

    public static PartnerCreateRequest toCustomerCreateRequest(InvoicePartnerData partnerData) {
        if (partnerData == null) {
            return null;
        }

        String document = normalizeDocument(partnerData.getDocument());
        PersonType personType = resolvePersonType(partnerData.getPersonType(), document);
        InvoiceAddressData address = partnerData.getAddress();

        String countryCode = address != null && StringUtils.isNotBlank(address.getCountryCode())
                ? truncate(address.getCountryCode().trim(), 4)
                : DEFAULT_COUNTRY_CODE;
        String countryName = address != null && StringUtils.isNotBlank(address.getCountryName())
                ? truncate(address.getCountryName().trim(), 50)
                : DEFAULT_COUNTRY_NAME;

        return new PartnerCreateRequest(
                truncate(trimToNull(partnerData.getForeignId()), 100),
                true,
                false,
                false,
                personType,
                document,
                truncate(trimToNull(partnerData.getName()), 255),
                partnerData.getIndicatorIe() != null ? partnerData.getIndicatorIe() : NON_TAXPAYER_IE_INDICATOR,
                truncate(trimToNull(partnerData.getStateRegistration()), 20),
                sanitizeEmail(partnerData.getEmail()),
                truncate(trimToNull(partnerData.getPhone()), 20),
                address != null ? truncate(trimToNull(address.getStreet()), 255) : null,
                address != null ? truncate(trimToNull(address.getNumber()), 20) : null,
                address != null ? truncate(trimToNull(address.getComplement()), 100) : null,
                address != null ? truncate(trimToNull(address.getNeighborhood()), 100) : null,
                address != null ? resolveIbgeCode(address.getIbgeCityCode()) : null,
                address != null ? truncate(trimToNull(address.getCity()), 100) : null,
                address != null ? resolveState(address.getState()) : null,
                address != null ? resolveZipCode(address.getZipCode()) : null,
                countryCode,
                countryName
        );
    }

    public static PartnerCreateRequest toCarrierCreateRequest(InvoiceCarrierData carrierData) {
        if (carrierData == null) {
            return null;
        }

        String document = normalizeDocument(carrierData.getDocument());
        PersonType personType = resolvePersonType(carrierData.getPersonType(), document);
        String stateRegistration = truncate(trimToNull(carrierData.getStateRegistration()), 20);
        int indicatorIe = StringUtils.isNotBlank(stateRegistration) ? TAXPAYER_IE_INDICATOR : NON_TAXPAYER_IE_INDICATOR;

        return new PartnerCreateRequest(
                false,
                false,
                true,
                personType,
                document,
                truncate(trimToNull(carrierData.getName()), 255),
                indicatorIe,
                stateRegistration,
                null,
                null,
                truncate(trimToNull(carrierData.getAddress()), 255),
                null,
                null,
                null,
                null,
                truncate(trimToNull(carrierData.getCity()), 100),
                resolveState(carrierData.getState()),
                null,
                DEFAULT_COUNTRY_CODE,
                DEFAULT_COUNTRY_NAME
        );
    }

    private static PersonType resolvePersonType(PersonType personType, String normalizedDocument) {
        if (personType != null) {
            return personType;
        }
        if (normalizedDocument != null && normalizedDocument.length() == 11) {
            return PersonType.PF;
        }
        return PersonType.PJ;
    }

    private static String resolveState(String rawState) {
        String trimmed = trimToNull(rawState);
        if (trimmed == null || trimmed.length() != 2) {
            return null;
        }
        return trimmed.toUpperCase();
    }

    private static String resolveZipCode(String rawZipCode) {
        String digits = digitsOnly(rawZipCode);
        return (digits != null && digits.length() == 8) ? digits : null;
    }

    private static String resolveIbgeCode(String rawIbge) {
        String digits = digitsOnly(rawIbge);
        return digits != null ? truncate(digits, 7) : null;
    }

    private static String sanitizeEmail(String rawEmail) {
        String trimmed = trimToNull(rawEmail);
        if (trimmed == null || !trimmed.contains("@") || trimmed.length() > 255) {
            return null;
        }
        return trimmed;
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
