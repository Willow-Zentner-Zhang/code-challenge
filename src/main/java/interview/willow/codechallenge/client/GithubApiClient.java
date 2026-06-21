package interview.willow.codechallenge.client;

import interview.willow.codechallenge.client.dto.GithubSearchResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

@Component
public class GithubApiClient {

    private final RestClient restClient;

    public GithubApiClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json").defaultHeader("X-GitHub-Api-Version", "2026-03-10").build();
    }

    public GithubSearchResponse searchRepositories(String language, LocalDate createdAfter) {
        final var query = "language:%s created:>%s".formatted(language, createdAfter);

        return restClient.get().uri(uriBuilder -> uriBuilder.path("/search/repositories").queryParam("q", query).queryParam("sort", "stars").queryParam("order", "desc").queryParam("per_page", 100).build()).retrieve().body(GithubSearchResponse.class);
    }
}
