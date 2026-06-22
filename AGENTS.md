# AGENTS.md

This file is the working guide for coding agents and contributors in this repository. Follow it for every change unless the task explicitly requires a different approach.

## Project overview

This repository contains a small Spring Boot REST service that:

1. accepts a programming language, creation date, and pagination parameters;
2. queries GitHub's repository search API;
3. maps GitHub's response into an internal API response;
4. calculates a recency-adjusted popularity score;
5. sorts the repositories in the current page by that score; and
6. caches identical searches for five minutes.

The project uses Java 25, Spring Boot 4, Maven, JUnit 5, AssertJ, Mockito, Caffeine, and springdoc-openapi.

## Important commands

Run all commands from the repository root.

```bash
# Run the full test suite
./mvnw test

# Run one test class
./mvnw -Dtest=PopularityScoreCalculatorTest test

# Run one test method
./mvnw -Dtest=PopularityScoreCalculatorTest#decaysGraduallyWithoutBecomingNegative test

# Build the application, including tests
./mvnw clean package

# Run the service locally
./mvnw spring-boot:run

# Build and run the container
docker build -t repository-popularity-api .
docker run --rm -p 8080:8080 repository-popularity-api
```

Do not skip tests in the final verification unless the change is documentation-only or the environment cannot run them. If tests cannot be run, state why.

## Repository layout

```text
src/main/java/interview/willow/codechallenge/
├── CodeChallengeApplication.java        Spring Boot entry point
├── client/                              GitHub HTTP integration
│   ├── GithubApiClient.java
│   ├── GithubApiProperties.java
│   └── dto/                             GitHub wire-format records
├── config/                              Framework and infrastructure beans
├── controller/                          Public HTTP endpoint and response records
├── exception/                           Domain exceptions and HTTP error mapping
└── service/                             Application behavior and score calculation

src/main/resources/application.properties
                                          Runtime defaults
src/test/java/interview/willow/codechallenge/
                                          Unit, client, and integration tests
```

Keep new code in the narrowest existing package. Create a new package only when it represents a clear architectural responsibility that does not fit the current structure.

## Architecture and dependency rules

The main request path is:

```text
HTTP request
  -> RepositoryController
  -> GithubRepositoryService
  -> GithubApiClient
  -> GitHub Search API

GithubRepositoryService
  -> PopularityScoreCalculator
  -> RepositoryPageResponse

Exceptions
  -> GlobalExceptionHandler
  -> JSON error response
```

Respect these boundaries:

- **Controllers** own HTTP concerns: routes, query parameters, validation annotations, OpenAPI annotations, and delegation. Controllers should not contain scoring, pagination, or GitHub integration logic.
- **Services** own application rules and orchestration. `GithubRepositoryService` validates accessible pages, invokes the client, maps external DTOs, calculates scores, orders the current page, and builds pagination metadata.
- **Clients** own external protocol details: paths, query construction, request headers, and deserialization. Do not leak `RestClient` or GitHub-specific DTOs into controllers.
- **DTOs and response records** are data-only types. Use Java records for immutable wire models when custom behavior is unnecessary.
- **Configuration classes** define infrastructure beans and cross-cutting framework features. Prefer constructor injection over field injection.
- **Exception handling** is centralized in `GlobalExceptionHandler`. Domain code should throw a meaningful exception rather than constructing HTTP responses.
- **Time-dependent behavior** must use the injected `Clock`; do not call `Instant.now()` or `LocalDateTime.now()` in business logic that needs deterministic tests.

Avoid circular dependencies and shortcuts between layers. In particular, the controller must not call the GitHub client directly, and the client must not construct public API response types.

## Behavioral invariants

Preserve these rules unless the requested feature intentionally changes them:

- `page` is one-based and must be at least 1.
- `size` must be between 1 and 100.
- `createdAfter` is an ISO-8601 date and cannot be in the future.
- GitHub Search exposes at most 1,000 results for a query. Requests outside that accessible range fail with `PageOutOfRangeException`.
- `totalElements` reports GitHub's complete reported count, while `totalPages` is capped to the accessible 1,000 results.
- A query with no matches permits page 1 and returns `totalPages = 0`.
- Popularity sorting applies to repositories in the current GitHub page only. Do not describe it as a global ordering across pages.
- The popularity score uses `stars + 2 * forks`, logarithmic scaling, and a 365-day recency factor, then rounds to two decimal places.
- Future update timestamps are treated as zero days old; negative star or fork counts are rejected.
- Identical service calls are cached in the `repositories` cache. Changes to method arguments or cache behavior require cache-focused tests.

When modifying pagination or scoring, consider both GitHub's upstream semantics and the public response metadata. Those two views are intentionally not identical.

## Java code style

Follow the style already present in the surrounding file.

### General conventions

- Use four spaces for indentation. Do not introduce tabs into edited code.
- Use one top-level type per file and match the filename to the type name.
- Keep package declarations first, followed by imports grouped as project imports, third-party imports, Java imports, and static imports.
- Remove unused imports. Do not use wildcard imports.
- Keep classes and methods focused; extract a collaborator when a class begins to own a separate responsibility.
- Prefer immutable data and `final` local variables/parameters in service and domain code, matching the established style.
- Use `var` only when the inferred type is immediately obvious from the right-hand side. Use an explicit type when it improves readability or communicates an interface boundary.
- Prefer records for immutable request, response, and external DTO shapes.
- Prefer descriptive names over abbreviations. Existing GitHub capitalization follows `Github...`; preserve it for consistency.
- Use `private static final` constants for meaningful fixed values. Include units in names when ambiguity is possible, such as `RECENCY_WINDOW_DAYS`.
- Use text blocks for multiline JSON and documentation strings.
- Use `String.formatted(...)` for readable message construction where appropriate.
- Do not add Lombok; Java records and explicit constructors are sufficient here.

### Spring conventions

- Use constructor injection. Do not add `@Autowired` to production fields or constructors when Spring can infer injection.
- Bind related configuration with validated `@ConfigurationProperties` records rather than scattered `@Value` fields.
- Keep runtime defaults in `application.properties` or in the relevant configuration type when a programmatic fallback is intentional.
- Validate requests at the HTTP boundary with Jakarta validation annotations.
- Use the configured `RestClient.Builder` for outbound calls.
- Keep cache names and policies centralized and documented when changed.
- Never commit credentials, access tokens, or environment-specific URLs. Add secret-bearing configuration through environment variables and document the variable names without example secrets.

### Error handling

- Throw specific exceptions for expected domain failures.
- Map exceptions to a stable status, error label, and useful message in `GlobalExceptionHandler`.
- Do not silently swallow malformed upstream data or substitute fabricated values unless the API contract explicitly defines a fallback.
- Avoid exposing credentials, full upstream payloads, stack traces, or other sensitive details in API error messages.
- Add handler tests whenever an exception mapping or error response shape changes.

### Public API changes

Treat controller routes, parameter names/defaults, validation constraints, JSON field names/types, status codes, and error bodies as public contracts.

For any contract change:

- update the controller and response schema annotations;
- add or update an HTTP-level test;
- update the README examples and parameter tables;
- consider backward compatibility and call out intentional breaking changes.

## Testing standards

Every behavior change should have a test that would fail before the change and pass afterward. Prefer the smallest test scope that proves the behavior, then add integration coverage when multiple layers interact.

### Test layers

- **Pure unit tests:** use for `PopularityScoreCalculator` and other deterministic business logic. Inject `Clock.fixed(...)` for time-dependent cases.
- **Service tests:** mock `GithubApiClient` and verify orchestration, mapping, ordering, page validation, metadata, and whether upstream calls occur.
- **Client tests:** use a local `HttpServer` on an ephemeral port to verify paths, encoded queries, headers, configuration, and JSON mapping. Do not call the real GitHub API.
- **Exception-handler tests:** invoke the handler directly for precise status and body assertions.
- **Integration tests:** use `@SpringBootTest`, `MockMvc`, and a local GitHub stub to cover request validation, serialization, caching, and the complete request path.

### Test style

- Use JUnit 5 `@Test` methods with behavior-oriented names such as `rejectsPageOutsideGithubSearchLimitBeforeCallingGithub`.
- Follow arrange, act, assert structure, separated by whitespace when it helps readability; comments are usually unnecessary.
- Use AssertJ for assertions and Mockito for mocks and interaction verification.
- Assert externally meaningful behavior, not private implementation details.
- Prefer exact assertions for stable values and focused assertions for dynamic payloads.
- Verify important negative interactions, for example that invalid pages do not call GitHub.
- Keep tests deterministic: use fixed clocks, fixed payloads, local stubs, and ephemeral ports.
- Clean shared state between tests. Integration tests that touch the cache must clear it before each test.
- Stop local HTTP servers in `@AfterEach` or `@AfterAll` to avoid leaking resources.

### Minimum coverage by change type

| Change | Expected verification |
| --- | --- |
| Score formula or time behavior | calculator unit tests, including boundaries |
| Pagination or mapping | service tests and an integration assertion |
| GitHub query/header/DTO | client test with recorded request and fixture response |
| Request validation | integration test expecting the exact status and error category |
| Error mapping | handler test and HTTP-level test when user-visible |
| Cache behavior | repeated-request integration test and cache reset between tests |
| Configuration default | properties binding/client test and documentation update |
| Documentation only | inspect rendered Markdown and run `git diff --check` |

Before completing a code change, run `./mvnw test`. For build, dependency, container, or packaging changes, also run `./mvnw clean package` and the relevant Docker command when available.

## Documentation standards

Documentation is part of the change, not a follow-up task.

### README

Update `README.md` when a change affects:

- prerequisites or local setup;
- Maven or Docker commands;
- endpoints, parameters, response fields, errors, or examples;
- scoring or pagination semantics;
- configuration properties or environment variables;
- caching, rate limits, health endpoints, or operational behavior.

Keep examples executable and synchronized with application defaults. Use fenced code blocks with a language identifier. Clearly distinguish GitHub's upstream limitations from behavior implemented by this service.

### OpenAPI documentation

Public endpoints must have accurate springdoc annotations:

- use `@Operation` for endpoint purpose and important behavior;
- use `@Parameter` for query semantics and realistic examples;
- use `@Schema` for public response fields and models;
- keep descriptions aligned with validation constraints and actual output.

After changing the API, check both Swagger UI at `/swagger-ui/index.html` and the OpenAPI document at `/v3/api-docs` when practical.

### Java documentation and comments

- Add Javadoc to public types or methods when the contract, algorithm, limitation, or units are not obvious from the code.
- Document architectural reasons and non-obvious constraints, not a line-by-line translation of the implementation.
- Keep comments current. Remove them when the code changes enough to make them misleading.
- Explain formulas, upstream limits, and surprising edge-case decisions near the code that enforces them.
- Do not add boilerplate Javadoc that merely repeats a method or field name.

### This file

Update `AGENTS.md` when architecture, tooling, test strategy, code style, or contributor workflow changes. Rules here must describe the repository as it exists; do not leave aspirational instructions that the build cannot support.

## Working safely in the repository

- Inspect `git status` before editing. The worktree may contain changes that belong to someone else.
- Preserve unrelated modifications and untracked files. Do not reset, revert, reformat, or delete them as part of another task.
- Keep the patch scoped to the requested behavior.
- Do not perform broad formatting passes unless formatting is the task.
- Do not edit generated build output under `target/`.
- Use the Maven Wrapper rather than relying on a globally installed Maven version.
- Do not weaken tests simply to make a change pass. Fix the implementation or update assertions only when the contract intentionally changed.
- Do not make tests depend on internet access or a real GitHub account.
- Review the final diff for accidental secrets, debug logging, unrelated edits, and stale documentation.

## Completion checklist

Before handing off a change, confirm the applicable items:

- [ ] The change respects controller, service, client, DTO, configuration, and exception boundaries.
- [ ] Public API behavior and GitHub's 1,000-result limitation remain correct.
- [ ] New or changed behavior has focused tests, including edge and failure cases.
- [ ] Time-based tests use a fixed `Clock`; HTTP integration tests use local stubs.
- [ ] `./mvnw test` passes, or the reason it could not run is reported.
- [ ] Packaging or container changes received additional build verification.
- [ ] README, OpenAPI annotations, Javadocs, and examples were updated where needed.
- [ ] Configuration changes contain no credentials and have documented defaults/overrides.
- [ ] `git diff --check` passes.
- [ ] The final diff contains only intentional changes and preserves pre-existing work.
