package io.github.scrumagent.shared;

/**
 * Small, null-safe helpers over Jira issue keys shared by the capability modules — so the
 * "project prefix" semantics live in exactly one tested place rather than drifting between
 * {@code risk} and {@code dependencies}.
 */
public final class JiraKeys {

    private JiraKeys() {
    }

    /**
     * Project prefix of a Jira key, e.g. {@code PROJ} for {@code PROJ-123}. Null/empty/dash-less
     * safe: {@code null}/blank yields {@code ""}; a key with no {@code '-'} is returned whole.
     */
    public static String projectOf(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        int dash = key.lastIndexOf('-');
        return dash < 0 ? key : key.substring(0, dash);
    }
}
