---
name: cucumber-acceptance-test
description: >
  Write end-to-end Cucumber acceptance tests following the codebase conventions.
  Use when: write cucumber acceptance test, add acceptance test, create BDD test,
  write a feature file, cucumber e2e test, acceptance test for a feature,
  add cucumber scenario, implement BDD scenario, write gherkin test,
  add integration test using cucumber, cucumber step definitions, test a feature end to end.
argument-hint: "Feature name and brief description of the happy path to test"
---

# Cucumber Acceptance Test Skill

Write end-to-end Cucumber acceptance tests following the exact layering and naming conventions used across the codebase. The skill covers BDD feature authoring, the step-driver-repository stack, state management, API drivers, mock server stubbing, and RabbitMQ message assertions.

---

## Phase 0 — Mandatory Interview (BEFORE writing any code)

Before creating any file, ask the user **all three questions at once**:

1. **What feature are you testing?** (one-sentence description of what the system does)
2. **What is the happy-path scenario?** (the main success flow, end-to-end)
3. **Are there edge cases or negative scenarios to include?** (e.g., validation errors, disabled flags, missing data)

From the answers, **generate the `.feature` file first** and present it for approval. Only after the user confirms the feature file do you proceed to write any Java.

---

## Phase 0.5 — Cucumber Project Setup

**Check first:** if the module already has a `LaunchAcceptanceTest.java` and `CucumberSpringBootConfig.java` the setup is already done — skip this phase entirely.

### Gradle Dependencies (`build.gradle.kts`)

```kotlin
dependencies {
    // Cucumber core
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-spring")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
    testImplementation("org.junit.platform:junit-platform-suite")

    // Spring Boot test support (MockMvc + JPA)
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")

    // Mock server for stubbing external HTTP services
    testImplementation("org.mock-server:mockserver-netty")

    // Instancio — only add if the module already uses it
    testImplementation("org.instancio:instancio-junit")
}

// Cucumber generates test names at runtime — Gradle would otherwise
// run acceptance tests on every test task. Exclude the launcher from
// all tasks, then re-include it only for the dedicated acceptance task.
tasks.withType<Test> {
    exclude("**/*LaunchAcceptanceTest.class")
}

tasks.named<Test>("runAcceptanceTests") {
    setExcludes(emptyList<String>().asIterable())
}
```

---

### Test Runner

One class at the root of the acceptance package. No logic — just wires Cucumber to JUnit Platform and points it at the feature files and glue code.

```java
// acceptance/LaunchAcceptanceTest.java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")          // scans src/test/resources/features/
@ConfigurationParameter(
        key = GLUE_PROPERTY_NAME,
        value = "com.your.module.acceptance")  // base package for all step/config classes
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "com.your.module.acceptance.config.ConsoleReportHooks") // optional reporter
public class LaunchAcceptanceTest {}
```

---

### Spring Boot Configuration

One class in `acceptance/config/`. Binds Cucumber's lifecycle to the Spring context.

```java
// acceptance/config/CucumberSpringBootConfig.java
@CucumberContextConfiguration
@SpringBootTest(
        properties = {
            "spring.profiles.active=acceptance"
            // add any property overrides needed for tests
        },
        classes = {YourApplication.class})
@AutoConfigureMockMvc
public class CucumberSpringBootConfig {
    // @MockitoBean any scheduled jobs or async components that should not run during tests
}
```

---

### Third-Party Lifecycle Manager

One class in `acceptance/config/`. Starts containers and mock server **once per test suite** (`@BeforeAll`), resets state before each scenario (`@Before`), and cleans up after the suite (`@AfterAll`).

All `CrudRepository` beans and `State` beans are injected automatically by Spring — they are used in `@Before` to wipe the database and reset scenario state before each test.

```java
// acceptance/config/CucumberThirdPartyManager.java
public class CucumberThirdPartyManager {

    private static ClientAndServer mockServer;

    @Getter
    private static MockServerClient mockServerClient;

    private final List<CrudRepository<?>> repositories;  // all repos injected by Spring
    private final List<State> states;                    // all @ScenarioScope states injected

    public CucumberThirdPartyManager(List<CrudRepository<?>> repositories, List<State> states) {
        this.repositories = repositories;
        this.states = states;
    }

    @BeforeAll
    public static void setup() {
        // Start infrastructure containers (Postgres, RabbitMQ, Temporal, etc.)
        // This project uses an internal library to abstract Testcontainers —
        // replace with your own container startup mechanism (e.g. Testcontainers directly).
        startInfrastructureContainers();

        // Start mock server for stubbing external HTTP calls
        mockServer = ClientAndServer.startClientAndServer(
                Configuration.configuration().logLevel(Level.ERROR),
                PortFactory.findFreePort());
        mockServerClient = new MockServerClient("localhost", mockServer.getPort());

        // Point service-under-test at the mock server for external service URLs
        System.setProperty("servers.externalServiceA.url", "http://localhost:" + mockServer.getPort());
        System.setProperty("servers.externalServiceB.url", "http://localhost:" + mockServer.getPort());
    }

    @AfterAll
    public static void teardown() {
        // Clean up system properties set during setup
        System.clearProperty("servers.externalServiceA.url");
        System.clearProperty("servers.externalServiceB.url");
    }

    @Before
    public void cleanup() {
        // Runs before every scenario — wipes database rows and resets in-memory state
        repositories.forEach(CrudRepository::deleteAll);
        states.forEach(State::reset);
        mockServerClient.reset();   // clears all mock server stubs and recorded requests
    }
}
```

---

### Supporting Interfaces

Two thin interfaces that every repository and state class implements, enabling the lifecycle manager to reset everything generically.

```java
// acceptance/CrudRepository.java
public interface CrudRepository<T> {
    void save(T recordItem);
    void deleteAll();
}

// acceptance/State.java
public interface State {
    void reset();
}
```

Every `*Repository` class implements `CrudRepository<T>`.
Every `*State` class implements `State` and adds a `reset()` that nulls out stored `ResultActions` and received messages.

---

## Phase 1 — Feature File

### Location
```
src/test/resources/features/{domain}/{kebab-case-feature-name}.feature
```

### Structure Rules
- **Feature header**: 3-line BDD description — `In order to`, `As a`, `I want`
- **Background**: contains ALL data preparation steps (entity setup, mocks, external stubs) — nothing in the scenario setup
- **Scenarios**: plain English, PM-readable; assert steps start with `Then the system…`
- **Assertion boundary**: the first `Then` or `And` after a `When` marks where assertions begin — phrase it as an observable system behavior

### Generic Example
```gherkin
Feature: Order shipment confirmation
  In order to keep customers informed about their deliveries
  As a Warehouse Operator
  I want the platform to confirm shipment when an order is dispatched

  Background:
    Given the following orders exist
      | orderId  | customerId | status   |
      | order-1  | cust-42    | PENDING  |
      | order-2  | cust-99    | PENDING  |
    And the notification service is available for customer "cust-42"

  Scenario: Shipment confirmation is sent when an order is dispatched (happy path)
    When the warehouse dispatches order "order-1"
    Then the system publishes a shipment confirmation message for order "order-1"
    And the order status is updated to "SHIPPED"

  Scenario: No confirmation is sent when the notification service is disabled for a customer
    Given the notification service is disabled for customer "cust-99"
    When the warehouse dispatches order "order-2"
    Then no shipment confirmation message is published
```

---

## Phase 2 — Package & Directory Layout

All acceptance test code lives under:
```
src/test/java/{your/base/package}/{module}/acceptance/
```

```
acceptance/
├── models/
│   └── {entity}/
│       ├── {Entity}PersistenceDriver.java   ← orchestrates test data setup
│       ├── {Entity}Repository.java          ← implements CrudRepository<{Entity}>
│       └── {Entity}Record.java              ← plain DTO passed through steps
└── steps/
    ├── common/
    │   └── {entity}/
    │       └── {Entity}Step.java            ← reusable step definitions across features
    └── {feature}/
        ├── {Feature}ApiDriver.java          ← MockMvc HTTP calls
        ├── {Feature}MessageListener.java    ← RabbitMQ consumer
        ├── {Feature}State.java              ← @ScenarioScope assertion state
        └── {Feature}Steps.java             ← Cucumber @Given/@When/@Then
```

**Rule — common vs feature-specific:**
- Steps reused by multiple features → `steps/common/{entity}/{Entity}Step.java`
- Steps specific to one feature → `steps/{feature}/{Feature}Steps.java`

---

## Phase 3 — Repository Layer

### Decision Rule

| Situation | Implementation |
|---|---|
| The entity has a JPA `@Entity` class in the **same module's domain** | Implement `CrudRepository<DomainEntity>` backed by a Spring Data JPA interface (`TestJpaStore`) |
| The entity belongs to **another domain** or has no JPA entity in this module | Implement `CrudRepository<T>` using `JdbcTemplate` with raw SQL — obtain `JdbcTemplate` from a test datasource config (e.g. a static helper or `@Autowired`) |

### JPA-backed Repository (entity is in this module's domain)
```java
// models/order/OrderRepository.java
@Service
@AllArgsConstructor
public class OrderRepository implements CrudRepository<OrderEntity> {

    private final OrderTestJpaStore orderTestJpaStore;  // Spring Data JPA interface

    @Override
    public void save(OrderEntity entity) {
        orderTestJpaStore.save(entity);
    }

    @Override
    public void deleteAll() {
        orderTestJpaStore.deleteAll();
    }
}
```

```java
// models/order/OrderTestJpaStore.java
public interface OrderTestJpaStore extends JpaRepository<OrderEntity, String> {}
```

### JDBC Repository (entity is from another domain or no JPA entity exists)
```java
// models/customer/CustomerPreferenceTestRepository.java
@Service
public class CustomerPreferenceTestRepository implements CrudRepository<CustomerPreference> {

    private final JdbcTemplate jdbcTemplate;

    public CustomerPreferenceTestRepository() {
        // Obtain a JdbcTemplate connected to the test database.
        // Adapt to your project: use a static datasource config helper,
        // @Autowired DataSource, or an @Bean-provided JdbcTemplate.
        jdbcTemplate = TestDataSourceConfig.getJdbcTemplate();
    }

    @Override
    public void save(CustomerPreference item) {
        String sql = "INSERT INTO customer_preferences (id, customer_id, notification_enabled) "
                   + "VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, item.getId(), item.getCustomerId(), item.isNotificationEnabled());
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("TRUNCATE TABLE customer_preferences CASCADE");
    }
}
```

---

## Phase 4 — PersistenceDriver Pattern

The driver is the **only entry point for step definitions to write test data**. It receives a plain `*Record` DTO and orchestrates all the related entity creation before saving the main one.

```java
// models/order/OrderPersistenceDriver.java
@Service
@AllArgsConstructor
public class OrderPersistenceDriver {

    private final OrderRepository orderRepository;
    private final CustomerPersistenceDriver customerPersistenceDriver; // if order depends on customer

    public void save(OrderRecord record) {
        // 1. Ensure dependencies exist first
        customerPersistenceDriver.ensureExists(record.getCustomerId());

        // 2. Build the domain entity
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(record.getOrderId());
        entity.setCustomerId(record.getCustomerId());
        entity.setStatus(record.getStatus());

        // 3. Save
        orderRepository.save(entity);
    }
}
```

```java
// models/order/OrderRecord.java  — plain DTO, no JPA annotations
@Builder
@Value
public class OrderRecord {
    String orderId;
    String customerId;
    String status;
}
```

---

## Phase 5 — Step Definitions

Step classes use `@RequiredArgsConstructor` (not `@Service` — Cucumber's Spring integration manages the lifecycle). Steps delegate **immediately** to drivers, listeners, or the state object. **Zero assertion logic in steps.**

### Common step (reused across features)
```java
// steps/common/order/OrderStep.java
@RequiredArgsConstructor
public class OrderStep {

    private final OrderPersistenceDriver orderPersistenceDriver;

    @Given("the following orders exist")
    public void theFollowingOrdersExist(DataTable dataTable) {
        for (Map<String, String> row : dataTable.asMaps()) {
            orderPersistenceDriver.save(OrderRecord.builder()
                    .orderId(row.get("orderId"))
                    .customerId(row.get("customerId"))
                    .status(row.get("status"))
                    .build());
        }
    }
}
```

### Feature-specific steps
```java
// steps/shipment/ShipmentConfirmationSteps.java
@RequiredArgsConstructor
public class ShipmentConfirmationSteps {

    private final ShipmentConfirmationApiDriver apiDriver;
    private final ShipmentMessageListener messageListener;
    private final ShipmentConfirmationState state;

    @When("the warehouse dispatches order {string}")
    public void theWarehouseDispatchesOrder(String orderId) throws Exception {
        state.storeDispatchResult(apiDriver.dispatchOrder(orderId));
    }

    @Then("the system publishes a shipment confirmation message for order {string}")
    public void theSystemPublishesShipmentConfirmation(String orderId) throws Exception {
        messageListener.waitTillMessageConsumed();
        state.assertShipmentMessageSentForOrder(orderId);
    }

    @Then("the order status is updated to {string}")
    public void theOrderStatusIsUpdatedTo(String expectedStatus) throws Exception {
        state.assertOrderStatus(expectedStatus);
    }
}
```

---

## Phase 6 — State Class (@ScenarioScope)

The state class is the **single place for all assertions**. It is scoped to one Gherkin scenario so state is never shared between scenarios.

```java
// steps/shipment/ShipmentConfirmationState.java
@Component
@ScenarioScope
@RequiredArgsConstructor
public class ShipmentConfirmationState {

    private final ObjectMapper objectMapper;
    private ResultActions dispatchResult;
    private ShipmentConfirmationMessage receivedMessage;

    // --- Store methods (called from steps) ---

    public void storeDispatchResult(ResultActions result) {
        this.dispatchResult = result;
    }

    public void storeReceivedMessage(ShipmentConfirmationMessage message) {
        this.receivedMessage = message;
    }

    // --- Assert methods (all assertions live here) ---

    public void assertOrderStatus(String expectedStatus) throws Exception {
        dispatchResult.andExpect(status().isOk());
        ShipmentResponseApi response = objectMapper.readValue(
                dispatchResult.andReturn().getResponse().getContentAsString(),
                ShipmentResponseApi.class);
        assertThat(response.getStatus()).isEqualTo(expectedStatus);
    }

    public void assertShipmentMessageSentForOrder(String orderId) {
        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage.getOrderId()).isEqualTo(orderId);
    }

    public void assertNoMessagePublished() {
        assertThat(receivedMessage).isNull();
    }
}
```

---

## Phase 7 — API Driver (MockMvc)

Used to make HTTP calls to the service under test. Returns `ResultActions` so the State can assert on the response body.

```java
// steps/shipment/ShipmentConfirmationApiDriver.java
@Service
@RequiredArgsConstructor
public class ShipmentConfirmationApiDriver {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public ResultActions dispatchOrder(String orderId) throws Exception {
        return mockMvc.perform(
                post("/orders/{orderId}/dispatch", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    public ResultActions updateShipmentStatus(String orderId, String cinId) throws Exception {
        ShipmentUpdateRequest request = ShipmentUpdateRequest.builder()
                .confirmationId(cinId)
                .build();
        return mockMvc.perform(
                patch("/orders/{orderId}/shipment", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}
```

---

## Phase 8 — Mock Server (Stubbing External HTTP Services)

Use `CucumberThirdPartyManager.getMockServerClient()` to stub HTTP calls the service-under-test makes to external services. Define stubs in step definitions (usually in the `Background` steps or `Given` steps of the scenario).

```java
// steps/shipment/ShipmentConfirmationSteps.java  (in the step definitions)
@And("the notification service is available for customer {string}")
public void theNotificationServiceIsAvailableForCustomer(String customerId) {
    CucumberThirdPartyManager.getMockServerClient()
            .when(request()
                    .withMethod("GET")
                    .withPath("/notification-service/customers/.*/preferences"))
            .respond(response()
                    .withStatusCode(200)
                    .withBody(json(CustomerPreferenceApiResponse.builder()
                            .notificationEnabled(true)
                            .build())));
}

@And("the notification service is disabled for customer {string}")
public void theNotificationServiceIsDisabledForCustomer(String customerId) {
    CucumberThirdPartyManager.getMockServerClient()
            .when(request()
                    .withMethod("GET")
                    .withPath("/notification-service/customers/" + customerId + "/preferences"))
            .respond(response()
                    .withStatusCode(200)
                    .withBody(json(CustomerPreferenceApiResponse.builder()
                            .notificationEnabled(false)
                            .build())));
}
```

**Key imports:**
```java
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;
```

---

## Phase 9 — RabbitMQ Message Listener

Used to assert on messages the service publishes to RabbitMQ. Creates its own `RabbitTemplate` connection directly (does not use the application's connection).

```java
// steps/shipment/ShipmentMessageListener.java
@Service
public class ShipmentMessageListener {

    private final RabbitTemplate rabbitTemplate;
    private final ShipmentConfirmationState state;
    private final ObjectMapper objectMapper;

    @Autowired
    public ShipmentMessageListener(
            // Inject whichever Spring bean holds your RabbitMQ connection properties
            @Value("${spring.rabbitmq.host}") String host,
            @Value("${spring.rabbitmq.port}") int port,
            @Value("${spring.rabbitmq.username}") String username,
            @Value("${spring.rabbitmq.password}") String password,
            ShipmentConfirmationState state) {
        this.state = state;
        this.objectMapper = new ObjectMapper();

        // Build a dedicated connection factory for the test listener —
        // do NOT reuse the application's connection factory.
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        this.rabbitTemplate = new RabbitTemplate(connectionFactory);
    }

    public void waitTillMessageConsumed() throws Exception {
        Message message = rabbitTemplate.receive("shipmentConfirmationQueue");
        assertNotNull(message, "Expected a shipment confirmation message but none was received");
        ShipmentConfirmationMessage parsed = objectMapper.readValue(
                new String(message.getBody()), ShipmentConfirmationMessage.class);
        state.storeReceivedMessage(parsed);
    }
}
```

---

## Phase 10 — Instancio

Use **only if the module already uses Instancio** for test data generation (check existing test classes in the module first).

```java
// Simple random object — useful for IDs and strings you don't care about
String randomId = Instancio.create(String.class);

// Fully random domain object
MyDomainObject obj = Instancio.create(MyDomainObject.class);

// Customized: set specific fields, leave the rest random
MyDomainObject obj = Instancio.of(MyDomainObject.class)
        .set(field(MyDomainObject::getId), "fixed-id")
        .set(field(MyDomainObject::getStatus), "ACTIVE")
        .create();
```

**Do NOT use Instancio for entities that are being saved to the database** — use explicit builder patterns so every column value is predictable and assertable.

---

## Quick Reference — Naming Conventions

| Class Role | Naming Pattern | Annotations | Location |
|---|---|---|---|
| Step definitions (feature-specific) | `{Feature}Steps` | `@RequiredArgsConstructor` | `steps/{feature}/` |
| Step definitions (shared) | `{Entity}Step` | `@RequiredArgsConstructor` | `steps/common/{entity}/` |
| Assertion state | `{Feature}State` | `@Component @ScenarioScope @RequiredArgsConstructor` | `steps/{feature}/` |
| API driver (MockMvc) | `{Feature}ApiDriver` | `@Service @RequiredArgsConstructor` | `steps/{feature}/` |
| RabbitMQ listener | `{Feature}MessageListener` | `@Service` + `@Autowired` constructor | `steps/{feature}/` |
| Persistence driver | `{Entity}PersistenceDriver` | `@Service @AllArgsConstructor` | `models/{entity}/` |
| Repository (JPA) | `{Entity}Repository` | `@Service @AllArgsConstructor` | `models/{entity}/` |
| Repository (JDBC) | `{Entity}TestRepository` | `@Service` | `models/{entity}/` |
| Test DTO | `{Entity}Record` | `@Builder @Value` | `models/{entity}/` |
| JPA store interface | `{Entity}TestJpaStore` | extends `JpaRepository<E, ID>` | `models/{entity}/` |

---

## Checklist Before Handing Off Code

- [ ] `.feature` file shown and approved by user before any Java was written
- [ ] `Background` contains all data prep; scenarios contain only the action + assertions
- [ ] Each `assert*()` method lives in `{Feature}State`, not in step definitions
- [ ] State class has `@ScenarioScope` — no state leaks between scenarios
- [ ] Repository decision rule applied (JPA vs JDBC) based on entity ownership
- [ ] Step classes use `@RequiredArgsConstructor`, not `@Service`
- [ ] Mock server stubs are defined in `Given` / Background steps, not in `Then` steps
- [ ] `waitTillMessageConsumed()` asserts the message is not null before deserializing
