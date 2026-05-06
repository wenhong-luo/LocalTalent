package cn.localtalent.backend.auditcenter.service;

import cn.localtalent.backend.common.json.AuditJsonMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AuditCenterMaskingService {

    private static final String REDACTED = "[REDACTED]";
    private static final Pattern MOBILE_PATTERN = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(password|secret|token|authorization)([\\s\"':=]+)([^,\\s\"'}]+)");

    private final ObjectMapper objectMapper = AuditJsonMapper.create();

    public String sanitizeJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return rawJson;
        }
        try {
            JsonNode node = objectMapper.readTree(rawJson);
            return objectMapper.writeValueAsString(sanitizeNode(node, null));
        } catch (Exception ignored) {
            return sanitizeText(rawJson);
        }
    }

    public String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String masked = MOBILE_PATTERN.matcher(value).replaceAll("1**********");
        Matcher emailMatcher = EMAIL_PATTERN.matcher(masked);
        StringBuffer emailBuffer = new StringBuffer();
        while (emailMatcher.find()) {
            emailMatcher.appendReplacement(emailBuffer, "***@" + emailMatcher.group(2));
        }
        emailMatcher.appendTail(emailBuffer);
        return SECRET_PATTERN.matcher(emailBuffer.toString()).replaceAll("$1$2" + REDACTED);
    }

    public String sanitizeToken(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return REDACTED;
    }

    public String sanitizeUri(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        int queryStart = value.indexOf('?');
        if (queryStart < 0) {
            return sanitizeText(value);
        }
        String path = sanitizeText(value.substring(0, queryStart));
        String query = value.substring(queryStart + 1);
        StringBuilder sanitized = new StringBuilder(path).append('?');
        String[] parts = query.split("&", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sanitized.append('&');
            }
            String part = parts[i];
            int equals = part.indexOf('=');
            if (equals < 0) {
                sanitized.append(isSensitiveKey(part) ? REDACTED : sanitizeText(part));
                continue;
            }
            String key = part.substring(0, equals);
            String rawValue = part.substring(equals + 1);
            sanitized.append(key).append('=');
            sanitized.append(isSensitiveKey(key) ? REDACTED : sanitizeText(rawValue));
        }
        return sanitized.toString();
    }

    private JsonNode sanitizeNode(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (fieldName != null && isSensitiveKey(fieldName)) {
            if (isContactKey(fieldName) && node.isTextual()) {
                return TextNode.valueOf(sanitizeText(node.asText()));
            }
            return TextNode.valueOf(REDACTED);
        }
        if (node.isObject()) {
            ObjectNode copy = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                copy.set(field.getKey(), sanitizeNode(field.getValue(), field.getKey()));
            }
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = objectMapper.createArrayNode();
            for (JsonNode item : node) {
                copy.add(sanitizeNode(item, fieldName));
            }
            return copy;
        }
        if (node.isTextual()) {
            return TextNode.valueOf(sanitizeText(node.asText()));
        }
        return node;
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase();
        return isContactKey(normalized)
                || normalized.contains("resume")
                || normalized.contains("attachment")
                || normalized.contains("evidence")
                || normalized.contains("password")
                || normalized.contains("secret")
                || normalized.contains("token")
                || normalized.contains("authorization")
                || normalized.contains("signature")
                || normalized.contains("nonce")
                || normalized.contains("claim")
                || normalized.contains("material")
                || normalized.contains("license_attachment")
                || normalized.contains("audit_attachment")
                || normalized.contains("registration")
                || normalized.contains("signin")
                || normalized.contains("body")
                || normalized.contains("page_snapshot")
                || normalized.contains("sms_record")
                || normalized.contains("realname")
                || normalized.contains("id_card")
                || normalized.contains("identity_card");
    }

    private boolean isContactKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase();
        return normalized.contains("mobile")
                || normalized.contains("phone")
                || normalized.equals("tel")
                || normalized.contains("email");
    }
}
