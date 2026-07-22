package io.github.scrumagent.acceptance.config;

import io.cucumber.spring.CucumberContextConfiguration;
import io.github.scrumagent.acceptance.MockServerSupport;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Cucumber's single Spring bootstrap. Boots the full application in the default MOCK web
 * environment and auto-configures {@link org.springframework.test.web.servlet.MockMvc} so the
 * shared {@code ScrumApiDriver} can drive the read API without a real servlet port. Wires
 * {@code scrum.jira.base-url} to the already-running MockServer so the real {@code JiraClient}
 * talks to MockServer instead of a live Jira.
 */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
public class CucumberSpringBootConfig {

    @DynamicPropertySource
    static void jira(DynamicPropertyRegistry r) {
        r.add("scrum.jira.base-url", MockServerSupport::baseUrl);
    }
}
