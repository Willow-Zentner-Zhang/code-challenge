package interview.willow.codechallenge;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class RepositoryApiIntegrationTests {

    private static final HttpServer GITHUB = startGithubStub();
    private static final AtomicReference<RecordedRequest> LAST_GITHUB_REQUEST = new AtomicReference<>();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @DynamicPropertySource
    static void githubProperties(final DynamicPropertyRegistry registry) {
        registry.add("github.api.base-url",
                () -> "http://localhost:" + GITHUB.getAddress().getPort());
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterAll
    static void stopGithubStub() {
        GITHUB.stop(0);
    }

    @Test
    void searchesGithubAndReturnsAProjectedSortedPage() throws Exception {
        mockMvc.perform(get("/api/v1/repositories")
                        .param("language", "java")
                        .param("createdAfter", "2025-01-01")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("popular"))
                .andExpect(jsonPath("$.content[0].owner").value("octocat"))
                .andExpect(jsonPath("$.content[1].name").value("quiet"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void translatesInvalidParametersAndServiceExceptionsToBadRequests() throws Exception {
        mockMvc.perform(get("/api/v1/repositories")
                        .param("language", "java")
                        .param("createdAfter", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Type mismatch"))
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/v1/repositories")
                        .param("language", "java")
                        .param("createdAfter", "2025-01-01")
                        .param("page", "11")
                        .param("size", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Page out of range"))
                .andExpect(jsonPath("$.message").value(
                        "Page 11 is outside the allowed range. The last available page is 10."));
    }

    @Test
    void sendsTheExpectedGithubRequestHeadersAndQuery() throws Exception {
        final var result = mockMvc.perform(get("/api/v1/repositories")
                        .param("language", "java")
                        .param("createdAfter", "2025-01-01")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("popular");
        final var request = LAST_GITHUB_REQUEST.get();
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.accept()).isEqualTo("application/vnd.github+json");
        assertThat(request.apiVersion()).isEqualTo("2026-03-10");
        assertThat(request.query())
                .contains("q=language:java%20created:%3E2025-01-01")
                .contains("sort=stars")
                .contains("order=desc")
                .contains("page=1")
                .contains("per_page=2");
    }

    private static HttpServer startGithubStub() {
        try {
            final var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/search/repositories", RepositoryApiIntegrationTests::respond);
            server.start();
            return server;
        } catch (final IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static void respond(final HttpExchange exchange) throws IOException {
        LAST_GITHUB_REQUEST.set(new RecordedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestHeaders().getFirst("Accept"),
                exchange.getRequestHeaders().getFirst("X-GitHub-Api-Version"),
                exchange.getRequestURI().getRawQuery()));

        final var body = """
                {
                  "total_count": 3,
                  "incomplete_results": false,
                  "items": [
                    {"name":"quiet","stargazers_count":10,"forks":1,
                     "updated_at":"2025-01-01T00:00:00Z","owner":{"login":"octocat"}},
                    {"name":"popular","stargazers_count":100,"forks":20,
                     "updated_at":"2026-01-01T00:00:00Z","owner":{"login":"octocat"}}
                  ]
                }
                """;
        final var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private record RecordedRequest(String method, String accept, String apiVersion, String query) {
    }
}
