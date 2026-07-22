package io.github.scrumagent;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Spring Modulith boundary test: verifies the application's module structure — no cyclic
 * dependencies, no reaches into another module's {@code internal} package, and that every
 * module's declared {@code allowedDependencies} are respected.
 */
class ModularityTests {

    private static final ApplicationModules MODULES = ApplicationModules.of(ScrumAgentApplication.class);

    @Test
    void verifiesModularStructure() {
        MODULES.verify();
    }
}
