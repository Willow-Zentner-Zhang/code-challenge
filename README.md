# GitHub Repository Popularity API

A Spring Boot service that searches GitHub repositories by programming language and creation date, calculates a recency-aware popularity score, and returns a paginated response.

## Requirements

- Java 25
- Docker (optional)

The Maven Wrapper is included, so a separate Maven installation is not required.

## Run locally

```bash
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`.

## API

### Search repositories

```http
GET /api/v1/repositories?language=java&createdAfter=2025-01-01&page=1&size=30
```

Query parameters:

| Parameter | Required | Default | Constraints | Description |
| --- | --- | --- | --- | --- |
| `language` | yes | — | non-blank | Programming language to search for |
| `createdAfter` | yes | — | ISO date, not in the future | Only repositories created after this date |
| `page` | no | `1` | minimum `1` | One-based page number |
| `size` | no | `30` | `1`–`100` | Number of repositories per page |

Example:

```bash
curl "http://localhost:8080/api/v1/repositories?language=java&createdAfter=2025-01-01&page=1&size=2"
```

Example response:

```json
{
  "content": [
    {
      "name": "spring-ai",
      "owner": "spring-projects",
      "stars": 5200,
      "forks": 1400,
      "lastUpdated": "2026-06-10T14:15:22Z",
      "popularityScore": 336.42
    }
  ],
  "page": 1,
  "size": 2,
  "numberOfElements": 1,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false,
  "incompleteResults": false
}
```

Results on each returned page are ordered by `popularityScore` in descending order. GitHub supplies the underlying pages ordered by stars by default, so the API does not guarantee score ordering across different pages.

### Error responses

Invalid parameters and unavailable pages return `400 Bad Request`:

```json
{
  "timestamp": "2026-06-22T10:15:30.123",
  "status": 400,
  "error": "Validation failed",
  "message": "repositories.size: must be less than or equal to 100"
}
```

GitHub Search exposes at most the first 1,000 results for a query. Page requests beyond that accessible range return a `400` response even when GitHub reports a larger `totalElements` value.

### Interactive documentation

With the application running:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI specification: `http://localhost:8080/v3/api-docs`

## Popularity score

The score combines engagement and repository freshness:

```text
engagement    = stars + (2 × forks)
recencyFactor = 1 / (1 + ageInDays / 365)
score         = 100 × ln(1 + engagement) × recencyFactor
```

The result is rounded to two decimal places. Newer repositories retain more of their engagement score, while older repositories decay gradually.

## Configuration

Defaults are defined in `src/main/resources/application.properties`:

| Property | Default | Purpose |
| --- | --- | --- |
| `github.api.base-url` | `https://api.github.com` | GitHub API base URL |
| `github.api.git-hub-api-version` | `2026-03-10` | Value of the `X-GitHub-Api-Version` header |
| `github.api.sort` | `stars` | GitHub Search sort field |
| `github.api.order` | `desc` | GitHub Search sort direction |
| `spring.cache.caffeine.spec` | `maximumSize=500,expireAfterWrite=5m,recordStats` | Search response cache policy |

Spring Boot properties can be overridden with environment variables. For example:

```bash
GITHUB_API_BASE_URL=https://api.github.com ./mvnw spring-boot:run
```

Identical searches are cached for five minutes. The service currently uses unauthenticated GitHub API requests, so GitHub's unauthenticated rate limits apply.

## Tests

```bash
./mvnw test
```

The test suite covers score calculation, GitHub client behavior, validation and exception mapping, caching, and the full HTTP request flow using a local GitHub stub.

## Docker

Build and run the image:

```bash
docker build -t repository-popularity-api .
docker run --rm -p 8080:8080 repository-popularity-api
```

The container runs as a non-root user and exposes port `8080`.

## Health check

Spring Boot Actuator exposes:

```http
GET /actuator/health
```
