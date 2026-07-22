package io.github.scrumagent.jira.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Low-level {@link JsonNode} primitives (unit conversions, list/text extraction, CSV membership)
 * moved verbatim from the former JiraClient god class. Pure functions, so behaviour — and every
 * derived metric — is byte-identical to before the split.
 */
final class JiraJsonUtil {

    private static final Logger log = LoggerFactory.getLogger(JiraJsonUtil.class);

    static final JsonNode EMPTY = JsonNodeFactory.instance.objectNode();

    private JiraJsonUtil() {
    }

    /** First node that holds a number, else the empty node (so {@link #hours} yields null). */
    static JsonNode firstNumber(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && node.isNumber()) {
                return node;
            }
        }
        return EMPTY;
    }

    static boolean csvContains(String csv, String value) {
        for (String part : csv.split("[,\\s]+")) {
            if (part.equals(value)) {
                return true;
            }
        }
        return false;
    }

    static Double hours(JsonNode seconds) {
        return seconds.isNumber() ? seconds.asDouble() / 3600.0 : null;
    }

    static List<String> names(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.path("name").asText()));
        return values;
    }

    static List<String> textList(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asText()));
        return values;
    }

    static String firstNonBlank(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && node.isTextual() && !node.asText().isBlank()) {
                return node.asText();
            }
        }
        return null;
    }

    /**
     * Logged work can never be negative; a negative value is bad data (a botched worklog edit, a
     * corrupt sync). Clamp it to 0 and warn — a negative would otherwise poison every downstream sum,
     * ratio and derived day figure. {@code null} and non-negative values pass through untouched.
     */
    static Double clampLogged(String key, Double value) {
        if (value != null && value < 0) {
            log.warn("Negative logged hours ({}) for {} — clamping to 0", value, key);
            return 0.0;
        }
        return value;
    }
}
