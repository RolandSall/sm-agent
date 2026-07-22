package io.github.scrumagent.jira.internal;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.function.Function;

import static io.github.scrumagent.jira.internal.JiraJsonUtil.EMPTY;

/**
 * Owns the Jira wire transport: the single {@link RestClient} (Bearer PAT auth, bounded timeouts,
 * the dev-only insecure-TLS escape hatch), the {@code GET} helper, and the response guards. This is
 * the ONE bean that constructs the RestClient. Extracted verbatim from the former JiraClient god
 * class so timeouts, headers and the insecure-TLS WARN are unchanged.
 */
@Component
class JiraHttp {

    private static final Logger log = LoggerFactory.getLogger(JiraHttp.class);

    /** Both bounded so a slow/hung Jira can never wedge the synchronous request thread. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final RestClient rest;

    JiraHttp(JiraProperties props, RestClient.Builder builder) {
        this.rest = builder
                .baseUrl(props.baseUrl())
                .requestFactory(requestFactory(props.insecureTls()))
                .defaultHeader("Authorization", "Bearer " + props.token())
                .defaultHeader("Accept", "application/json")
                .build();
        if (props.insecureTls()) {
            log.warn("scrum.jira.insecure-tls=true — TLS certificate validation is DISABLED for Jira "
                    + "calls. Use this ONLY for local piloting against a trusted corporate network; "
                    + "NEVER in production. The proper fix is importing the corporate CA into a "
                    + "Java truststore.");
        }
    }

    /** Issue a {@code GET} and deserialize the body as a {@link JsonNode} (null when Jira returns no body). */
    JsonNode get(Function<UriBuilder, URI> uriFunction) {
        return rest.get()
                .uri(uriFunction)
                .retrieve()
                .body(JsonNode.class);
    }

    /**
     * Normal path: the auto-detected factory with bounded timeouts. Insecure path (dev-only, gated by
     * {@code scrum.jira.insecure-tls}): a JDK HttpClient that trusts every server certificate, so a
     * corporate CA missing from the JVM truststore doesn't block piloting.
     */
    private static ClientHttpRequestFactory requestFactory(boolean insecureTls) {
        if (!insecureTls) {
            return ClientHttpRequestFactoryBuilder.detect().build(
                    ClientHttpRequestFactorySettings.defaults()
                            .withConnectTimeout(CONNECT_TIMEOUT)
                            .withReadTimeout(READ_TIMEOUT));
        }
        try {
            TrustManager[] trustAll = {new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new SecureRandom());
            HttpClient httpClient = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(CONNECT_TIMEOUT)
                    .build();
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
            factory.setReadTimeout(READ_TIMEOUT);
            return factory;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to build insecure-TLS Jira client", e);
        }
    }

    // --- response guards ---

    /** Fail loudly when a single-object endpoint returns an empty body rather than NPE later. */
    static JsonNode requireBody(JsonNode json, String endpoint) {
        if (json == null || json.isNull() || json.isMissingNode()) {
            throw new IllegalStateException("empty response from " + endpoint);
        }
        return json;
    }

    /** For list/search endpoints an absent body just means "no results" — coerce to an empty node. */
    static JsonNode orEmpty(JsonNode json) {
        return json == null ? EMPTY : json;
    }

    /**
     * Warn (do not silently drop) when a page came back full AND Jira reports more total than we
     * returned — the caller sees a capped view. No paging here by design; this only surfaces it.
     */
    static void warnIfTruncated(JsonNode json, String arrayField, int cap, String endpoint) {
        int returned = json.path(arrayField).size();
        int total = json.path("total").asInt(0);
        if (returned >= cap && total > returned) {
            log.warn("Truncated results from {}: returned {} of {} (cap {}); report reflects a partial view",
                    endpoint, returned, total, cap);
        }
    }

    /** Best-effort awareness only: note when a changelog page looks capped vs its reported total. */
    static void debugIfChangelogTruncated(JsonNode changelog, String key) {
        int returned = changelog.path("histories").size();
        int total = changelog.path("total").asInt(0);
        int max = changelog.path("maxResults").asInt(0);
        if (max > 0 && returned >= max && total > returned) {
            log.debug("Changelog for {} may be truncated: {} of {} histories returned", key, returned, total);
        }
    }
}
