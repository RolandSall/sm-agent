package io.github.scrumagent.acceptance;

import org.mockserver.integration.ClientAndServer;

/**
 * Owns the single MockServer instance for the acceptance suite. It is started once, in a static
 * initializer, so it is already listening on its dynamic port <em>before</em> the Spring context
 * is created and {@code CucumberSpringBootConfig}'s {@code @DynamicPropertySource} reads
 * {@link #baseUrl()} to point {@code scrum.jira.base-url} at it.
 *
 * <p>Step definitions register expectations via {@link #server()}; the {@code @Before} hook resets
 * them between scenarios so each scenario starts from a clean slate.
 */
public final class MockServerSupport {

    private static final ClientAndServer SERVER = ClientAndServer.startClientAndServer();

    private MockServerSupport() {
    }

    public static ClientAndServer server() {
        return SERVER;
    }

    public static int port() {
        return SERVER.getLocalPort();
    }

    public static String baseUrl() {
        return "http://localhost:" + port();
    }
}
