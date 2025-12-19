# Testing Strategy

## Goals
- **Correctness:** Ensure business logic, mapping, and validation behave as expected.
- **Fast feedback:** Keep most tests fast and runnable without starting the full Spring context.
- **Safe refactoring:** Changes to DTOs/entities/mappers should not introduce silent regressions.
- **Good coverage where it matters:** Focus on critical paths (mapping, services, controllers, repositories).

---

## Test Pyramid (Priority)
1. **Unit Tests (many, fast)**
    - Mappers, utilities, pure service logic (no DB/HTTP/Spring context).
2. **Slice Tests (targeted, medium)**
    - `@WebMvcTest` for controllers (mock service layer)
    - `@DataJpaTest` for repositories/queries (H2 or Testcontainers if needed)
3. **Integration / End-to-End (few, slow)**
    - `@SpringBootTest` (optionally with Testcontainers DB)
    - Optional API tests (e.g., RestAssured)

---

## Unit Tests

### Mapper Tests
**Purpose:** Verify Entity ↔ DTO mapping is correct and stable.

**What to test:**
- `null` handling (`toDTO(null) -> null`, `toDTOList(null) -> null`)
- Full field mapping (all relevant fields are copied correctly)
- Special cases:
    - JSON parsing/serialization (e.g., `alternateSpecies`)
    - Localization & fallbacks (e.g., WeatherAlert DE/EN behavior)
- List mapping: size, order, element-wise mapping

**Guidelines:**
- No Spring annotations required
- No mocks needed (mappers have no external dependencies)
- For `float`/`double` values: use correct literals (`0.12f`) and/or delta assertions

---

## Service Tests (Unit)

### Purpose
Validate business logic independently from infrastructure.

**Technique:**
- JUnit 5 + Mockito
- Mock repositories/clients
- Test behavior and decisions, not implementation details

**What to test:**
- Happy path + edge cases
- Error handling (e.g., external API down, invalid response)
- Transformations and calculations
- Decisions (when to save/update/skip)

---

## Controller Tests (Slice)

### Purpose
Verify API behavior: status codes, request validation, response structure, and error cases.

**Technique:**
- `@WebMvcTest(MyController.class)`
- `MockMvc`
- Services as `@MockBean`

**What to test:**
- Status codes: 200/201/400/404/500
- Validation (`@Valid`) and bad requests
- Response JSON structure (required fields, types, values)
- Error responses (message/code where applicable)

**Test isolation**
- Reset shared state between tests if needed:
    - Mockito reset
    - static caches/singletons
    - custom global state
- If your controller tests require it, use `@AfterEach` to reset mocks/state.

---

## Repository Tests (Slice)

### Purpose
Verify database queries, constraints, and JPA mappings.

**Technique:**
- `@DataJpaTest`
- H2 by default (or Testcontainers Postgres/MySQL if needed)
- Use repository methods directly (optionally `TestEntityManager`)

**What to test:**
- Derived queries / custom queries
- Unique constraints (e.g., `forecastDateTime + city + country`)
- Nullability/column constraints (when meaningful)
- Index-related query correctness (performance testing is optional and usually integration-level)

---

## Integration Tests

### Purpose
Test real component collaboration (service + repository + mapping), close to production behavior.

**Technique:**
- `@SpringBootTest`
- `@AutoConfigureMockMvc` (or RestAssured)
- External APIs:
    - Prefer mocking via WireMock/MockWebServer
    - Or use a dedicated test profile with safe configuration

**What to test:**
- End-to-end flows (e.g., forecast import, alert update, detection processing)
- Serialization/deserialization with real Spring configuration
- Security behavior (if applicable)

---

## Test Data & Stability

### Time-dependent logic
Some entities/methods use `LocalDateTime.now()` (e.g., `WeatherAlert.isActive()`).
To avoid flaky tests:
- Prefer setting explicit timestamps and asserting deterministic values
- Avoid asserting “active right now” unless you control time
- If future refactoring is possible: inject a `Clock` for deterministic tests

### Avoid flaky tests
- No external network calls
- No dependence on local timezone unless explicitly tested
- No inter-test ordering dependencies

---

## Tooling & Standards

### Frameworks
- JUnit 5
- Mockito
- Spring Boot Test (`spring-boot-starter-test`)

### Naming conventions
- Test classes: `ClassUnderTestTest`
- Test methods:
    - `method_shouldExpectedBehavior_whenCondition()`

### Coverage
- Coverage is a tool, not the goal:
    - Prefer fewer meaningful tests over many shallow ones
    - Prioritize critical logic and mapping stability

---

## When to use which test type?
- **New DTO/entity/mapper fields** → extend mapper unit tests
- **New business rule** → service unit test + (optional) integration test
- **New endpoint** → controller slice test (happy + validation/error cases)
- **New query/constraint** → repository slice test

---

## Definition of Done (Testing)
For each new feature/change:
- Unit tests updated/added for core logic and mappings
- Controller tests for new endpoints (happy path + failure/validation)
- All tests pass locally and in CI
- No new flaky tests introduced






