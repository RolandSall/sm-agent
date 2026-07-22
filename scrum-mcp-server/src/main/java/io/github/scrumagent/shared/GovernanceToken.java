package io.github.scrumagent.shared;

/**
 * Opaque handle threading a {@code prepare_*} preview to its {@code commit_*}. A human reviews the
 * preview, then the commit is dispatched with this token — nothing is written without one.
 *
 * @param value the opaque token string
 */
public record GovernanceToken(String value) {

    /**
     * Mint a fresh, opaque token for the content it guards. A random UUID (not a content hash) so the
     * token is unique per preview and unguessable — a content hash would collide for identical
     * previews and be trivially forgeable from the rendered text.
     */
    public static GovernanceToken forContent(String content) {
        return new GovernanceToken(java.util.UUID.randomUUID().toString());
    }
}
