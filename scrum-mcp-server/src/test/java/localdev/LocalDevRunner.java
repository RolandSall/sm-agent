package localdev;

import org.mockserver.integration.ClientAndServer;
import org.springframework.boot.SpringApplication;

/**
 * Local manual-development entry point (run via {@code ./gradlew :scrum-mcp-server:runLocal}).
 *
 * <p>Boots an in-JVM MockServer on :1080 with rich, prod-like Jira fixtures ({@link LocalJiraFixtures}),
 * then starts the REAL Spring Boot application (MCP over Streamable HTTP at {@code /mcp} + the REST
 * API at {@code /api}) on :8097 under the {@code local} profile, which points {@code scrum.jira.base-url}
 * at that MockServer. One process, one command — no Docker, no real Jira.
 *
 * <p>Deliberately in the root {@code localdev} package (NOT under {@code io.github.scrumagent}) so
 * Spring Modulith's {@code ApplicationModules.of(ScrumAgentApplication.class)} never treats it as a
 * module. It lives in the test source set so it inherits MockServer and the logback/JUL isolation
 * from {@code build.gradle.kts} and never ships in the production artifact.
 */
public final class LocalDevRunner {

    private LocalDevRunner() {
    }

    public static void main(String[] args) {
        ClientAndServer mockJira = ClientAndServer.startClientAndServer(1080);
        Runtime.getRuntime().addShutdownHook(new Thread(mockJira::stop));
        LocalJiraFixtures.register(mockJira);

        System.setProperty("spring.profiles.active", "local");
        System.out.println("""

                ┌──────────────────────────────────────────────────────────────────────┐
                │  scrum-agent LOCAL dev harness                                         │
                │  Mock Jira  : http://localhost:1080  (in-JVM MockServer, rich data)    │
                │  MCP  (on)  : http://localhost:8097/mcp                                 │
                │  REST (off) : http://localhost:8097/api   (or ./scrum <cmd>)           │
                │  Stop       : Ctrl-C                                                    │
                └──────────────────────────────────────────────────────────────────────┘
                """);
        SpringApplication.run(LocalDevApp.class, args);
    }
}
