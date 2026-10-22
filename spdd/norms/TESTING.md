# Norm: Testing

What to test at which layer, and how tests are named and run.

---

## Two kinds of test

| Kind | Suffix | Scope | Runs in |
| --- | --- | --- | --- |
| **Unit test** | `*Test` | One class or one use case slice, with collaborators replaced by fakes or mocks. **No running application.** | the unit-test phase |
| **Integration test** | `*IT` | The **whole application running** (container started, real wiring), driven through its public API with only external systems replaced. | the integration-test phase |

The suffix is a contract: a test that starts the application is an `*IT`, whatever it asserts.

## By layer

- **Use cases:** unit tests with fake ports. They cover every business rule and outcome, including the failures handed to the presenter, and need no framework.
- **Adapters, clients, mappers, validators:** unit tests at their seam, where the logic is non-trivial.
- **Endpoints:** at least one integration test per endpoint (e.g. `*UseCaseControllerIT`), covering the happy path and the main error contracts.
- **CRUD services:** unit-test complex `validate` / `reconcile` / specification logic; cover persistence behaviour in integration tests.

## Traceability

- Each scenario of a spec (Gherkin in `spdd/prompt/`) maps to **one named test**, whose documentation quotes the scenario.
- A test class references the requirement it was generated from.

## Isolation

- Tests never depend on **live external systems**, **developer-local configuration**, or **execution order**.
- External systems are replaced in **shared test configuration**, not test by test. All integration tests use the same configuration, so they share one running application instead of starting a new one per test class. Test doubles are reset between tests, not redefined.
- Test configuration lives with the tests; local overrides are scoped so they never apply to test runs.
- Each test sets up the state it needs and leaves no shared state behind.

---

**Quarkus:** unit tests are plain JUnit with Mockito. Integration tests are `@QuarkusTest` classes named `*IT`: the test starts the application in the JVM with its real CDI wiring. The template `pom.xml` runs them with Failsafe in the integration-test phase, on the compiled classes; Surefire runs `*Test`. External systems are replaced with `@InjectMock` (installed per test, without a restart) or a shared `@Alternative` test bean. Do not use `@QuarkusIntegrationTest`: it targets the packaged artifact and cannot replace beans. Avoid per-class `@TestProfile`: each distinct profile restarts the application.
