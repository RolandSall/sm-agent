plugins {
    java
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.scrumagent"
version = "0.0.1-SNAPSHOT"

java {
    // Pin the JDK so the build (and IntelliJ's Gradle import) compiles as Java 21 regardless of
    // the machine's default JDK — Gradle provisions/uses a matching toolchain (JDK 21 here).
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

// Same Spring AI line as ai-template so the MCP wiring is identical to order-mcp-server.
val springAiVersion = "1.1.8"
val springModulithVersion = "1.3.5"

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
        mavenBom("org.springframework.modulith:spring-modulith-bom:$springModulithVersion")
    }
}

dependencies {
    // MCP SERVER over Streamable HTTP (WebMvc): turns @Tool beans into an MCP endpoint at /mcp.
    // No model-provider starter — an MCP server exposes tools, it does not call an LLM,
    // so it needs no API key. Also brings spring-web (RestClient for Jira/Confluence).
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")

    // Modular monolith: enforced module boundaries (jira / hygiene / capacity / ...).
    implementation("org.springframework.modulith:spring-modulith-starter-core")

    // CQRS mediator — @Tool methods and REST controllers dispatch the same queries.
    implementation("io.github.springmediator:spring-mediator-starter:1.0.3")

    // The mediator's behavior auto-config introspects jakarta.validation at startup.
    implementation("org.springframework.boot:spring-boot-starter-validation")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")

    // Cucumber acceptance tests against a MockServer-stubbed Jira.
    testImplementation(platform("io.cucumber:cucumber-bom:7.20.1"))
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-spring")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
    testImplementation("org.junit.platform:junit-platform-suite")
    // Shaded artifact: MockServer relocates its own transitive deps (e.g. json-schema-validator 1.x)
    // so they don't clash with the MCP SDK's json-schema-validator 2.0.0 needed at server startup.
    testImplementation("org.mock-server:mockserver-netty:5.15.0:shaded")
}

// The shaded MockServer jar bundles a JUL SLF4J provider that competes with Logback and makes
// Spring Boot's logging system fail. For the TEST classpath only, drop Logback (Spring Boot falls
// back to java.util.logging) and the jul→slf4j bridge (which would otherwise loop). Production
// logging is untouched.
configurations.testRuntimeClasspath {
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "org.slf4j", module = "jul-to-slf4j")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// --- Local manual-dev harness -------------------------------------------------------------------
// Runs the real MCP+REST server on :8097 against an in-JVM MockServer that mimics Jira with rich,
// prod-like fixtures (profile 'local'). Reuses the TEST classpath, so MockServer + the logback/JUL
// isolation above apply and NOTHING here leaks into the production artifact.
//
//   ./gradlew :scrum-mcp-server:runLocal
//     MCP ON  → point a harness at http://localhost:8097/mcp (Claude Code: .mcp.json "scrum-local")
//     MCP OFF → ./scrum <cmd>   (curl wrapper → http://localhost:8097/api)
tasks.register<JavaExec>("runLocal") {
    group = "application"
    description = "Run the MCP+REST server against an in-JVM MockServer Jira (profile 'local', rich fixtures)."
    dependsOn("testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("localdev.LocalDevRunner")
}

// Generate development/mockserver/initializer.json from the SAME fixtures, for the containerized
// MockServer variant (development/docker-compose.yml). Dates are stamped now — regenerate if stale.
tasks.register<JavaExec>("generateMockInitializer") {
    group = "application"
    description = "Write development/mockserver/initializer.json from the local fixtures (for the Docker MockServer)."
    dependsOn("testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("localdev.GenerateInitializer")
    args("${rootDir}/development/mockserver/initializer.json")
}
