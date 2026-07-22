package localdev;

import io.github.scrumagent.ScrumAgentApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Boot class for the local dev harness. Equivalent to {@code ScrumAgentApplication}
 * (auto-configuration + {@code @ConfigurationPropertiesScan} over {@code io.github.scrumagent}),
 * but with ONE difference: the {@code acceptance} test beans are excluded from the component scan.
 *
 * <p>The harness runs on the TEST classpath (to reuse MockServer), so a plain
 * {@code SpringApplication.run(ScrumAgentApplication.class)} would otherwise scan Cucumber test
 * components such as {@code ScrumApiDriver} — which require a {@code MockMvc} bean that only exists
 * inside a {@code @SpringBootTest} slice — and fail to start. {@code @Modulith} is intentionally
 * omitted: it drives boundary verification, not runtime behaviour.
 *
 * <p>{@code ScrumAgentApplication} is also excluded: our scan of {@code io.github.scrumagent} would
 * otherwise re-register it, and ITS {@code @ComponentScan} (which has no excludes) would drag the
 * acceptance beans straight back in.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ConfigurationPropertiesScan("io.github.scrumagent")
@ComponentScan(
        basePackages = "io.github.scrumagent",
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = ScrumAgentApplication.class),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "io\\.github\\.scrumagent\\.acceptance\\..*")
        })
public class LocalDevApp {
}
