package company.aura.erp.modules.invoice.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class XmlElementReader {

    public Optional<Element> findChild(Element parent, String tagName) {
        if (parent == null || StringUtils.isBlank(tagName)) {
            return Optional.empty();
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (isElementWithName(node, tagName)) {
                return Optional.of((Element) node);
            }
        }
        return Optional.empty();
    }

    public List<Element> findChildren(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        if (parent == null || StringUtils.isBlank(tagName)) {
            return result;
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (isElementWithName(node, tagName)) {
                result.add((Element) node);
            }
        }
        return result;
    }

    public Optional<Element> findFirstChildElement(Element parent) {
        if (parent == null) {
            return Optional.empty();
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                return Optional.of((Element) node);
            }
        }
        return Optional.empty();
    }

    public String getText(Element parent, String tagName) {
        return findChild(parent, tagName)
                .map(Element::getTextContent)
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .orElse(null);
    }

    public String getAttribute(Element element, String attributeName) {
        if (element == null || StringUtils.isBlank(attributeName) || !element.hasAttribute(attributeName)) {
            return null;
        }
        String value = element.getAttribute(attributeName);
        return StringUtils.isNotBlank(value) ? value.trim() : null;
    }

    public Integer getInteger(Element parent, String tagName) {
        String text = getText(parent, tagName);
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException ex) {
            log.warn("getInteger() - WARN - Invalid integer value for tag={}, value={}", tagName, text);
            return null;
        }
    }

    public BigDecimal getBigDecimal(Element parent, String tagName) {
        return getBigDecimal(parent, tagName, null);
    }

    public BigDecimal getBigDecimalOrZero(Element parent, String tagName) {
        return getBigDecimal(parent, tagName, BigDecimal.ZERO);
    }

    public BigDecimal getBigDecimal(Element parent, String tagName, BigDecimal defaultValue) {
        String text = getText(parent, tagName);
        if (text == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            log.warn("getBigDecimal() - WARN - Invalid decimal value for tag={}, value={}", tagName, text);
            return defaultValue;
        }
    }

    public LocalDate getLocalDate(Element parent, String tagName) {
        String text = getText(parent, tagName);
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text.length() >= 10 ? text.substring(0, 10) : text);
        } catch (DateTimeParseException ex) {
            log.warn("getLocalDate() - WARN - Invalid date value for tag={}, value={}", tagName, text);
            return null;
        }
    }

    public LocalDateTime getLocalDateTime(Element parent, String tagName) {
        String text = getText(parent, tagName);
        if (text == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // fallback to LocalDateTime or LocalDate without timezone offset
        }
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            // fallback to date-only format (legacy NF-e <dEmi>)
        }
        try {
            return LocalDate.parse(text).atStartOfDay();
        } catch (DateTimeParseException ex) {
            log.warn("getLocalDateTime() - WARN - Invalid datetime value for tag={}, value={}", tagName, text);
            return null;
        }
    }

    public String digitsOnly(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return StringUtils.isNotBlank(digits) ? digits : null;
    }

    private boolean isElementWithName(Node node, String targetName) {
        if (node == null || node.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        }
        String localName = node.getLocalName();
        String nodeName = localName != null ? localName : node.getNodeName();
        return targetName.equals(nodeName);
    }
}
