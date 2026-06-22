package interview.willow.codechallenge.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GithubApiClientTest {

    private final AtomicReference<RecordedRequest> recordedRequest = new AtomicReference<>();

    private HttpServer server;
    private GithubApiClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/search/repositories", this::respond);
        server.start();

        final var properties = new GithubApiProperties(
                URI.create("http://localhost:" + server.getAddress().getPort()),
                "2026-03-10",
                "stars",
                "desc");
        client = new GithubApiClient(RestClient.builder(), properties);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void searchesRepositoriesWithExpectedRequestAndMapsResponse() {
        final var response = client.searchRepositories(
                "java", LocalDate.of(2025, 1, 1), 2, 25);

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.incompleteResults()).isFalse();
        assertThat(response.items()).singleElement().satisfies(repository -> {
            assertThat(repository.name()).isEqualTo("code-challenge");
            assertThat(repository.stars()).isEqualTo(42);
            assertThat(repository.forks()).isEqualTo(7);
            assertThat(repository.updatedAt()).isEqualTo("2026-06-01T12:00:00Z");
            assertThat(repository.owner().login()).isEqualTo("willow");
        });

        assertThat(recordedRequest.get()).satisfies(request -> {
            assertThat(request.method()).isEqualTo("GET");
            assertThat(request.accept()).isEqualTo("application/vnd.github+json");
            assertThat(request.apiVersion()).isEqualTo("2026-03-10");
            assertThat(request.rawQuery())
                    .contains("q=language:java%20created:%3E2025-01-01")
                    .contains("sort=stars")
                    .contains("order=desc")
                    .contains("page=2")
                    .contains("per_page=25");
        });
    }

    @Test
    void usesConfiguredSortAndOrder() {
        final var properties = new GithubApiProperties(
                URI.create("http://localhost:" + server.getAddress().getPort()),
                "2026-03-10",
                "updated",
                "asc");
        final var configuredClient = new GithubApiClient(RestClient.builder(), properties);

        configuredClient.searchRepositories("java", LocalDate.of(2025, 1, 1), 1, 10);

        assertThat(recordedRequest.get().rawQuery())
                .contains("sort=updated")
                .contains("order=asc");
    }

    private void respond(final HttpExchange exchange) throws IOException {
        recordedRequest.set(new RecordedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestHeaders().getFirst("Accept"),
                exchange.getRequestHeaders().getFirst("X-GitHub-Api-Version"),
                exchange.getRequestURI().getRawQuery()));

        final var body = """
                {
                  "total_count": 1,
                  "incomplete_results": false,
                  "items": [{
                    "name": "code-challenge",
                    "stargazers_count": 42,
                    "forks": 7,
                    "updated_at": "2026-06-01T12:00:00Z",
                    "owner": {"login": "willow"}
                  }]
                }
                """;
        final var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private record RecordedRequest(String method, String accept, String apiVersion, String rawQuery) {
    }
}
