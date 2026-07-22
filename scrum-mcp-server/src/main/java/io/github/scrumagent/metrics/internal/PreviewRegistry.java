package io.github.scrumagent.metrics.internal;

import io.github.scrumagent.shared.GovernanceToken;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backs the {@code prepare → commit} human-in-the-loop pattern. {@code prepare_*} registers the
 * rendered content it wants to publish and gets back a {@link GovernanceToken}; a human reviews the
 * preview; {@code commit_*} consumes the token (once) to retrieve the exact content that was approved.
 * In-memory with a TTL — a preview no one commits simply expires.
 */
@Component
public class PreviewRegistry {

    private static final Duration TTL = Duration.ofMinutes(15);

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    /** Register content for later commit; returns the token that guards it. */
    public GovernanceToken register(String content) {
        GovernanceToken token = GovernanceToken.forContent(content);
        entries.put(token.value(), new Entry(content, Instant.now().plus(TTL)));
        return token;
    }

    /** Consume the token exactly once, returning the approved content if still valid. */
    public Optional<String> consume(String token) {
        purgeExpired();
        Entry entry = entries.remove(token);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.content());
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        entries.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }

    private record Entry(String content, Instant expiresAt) {
    }
}
