package interview.willow.codechallenge.client;

import interview.willow.codechallenge.client.dto.GithubSearchResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

@Component
public class GithubApiClient {

    private static final String REPOSITORY_SEARCH_PATH = "/search/repositories";
    private static final String GITHUB_ACCEPT_HEADER = "application/vnd.github+json";

    private final RestClient restClient;

    private final GithubApiProperties properties;

    public GithubApiClient(RestClient.Builder builder, GithubApiProperties properties) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.baseUrl().toString())
                .defaultHeader("Accept", GITHUB_ACCEPT_HEADER)
                .defaultHeader("X-GitHub-Api-Version", properties.gitHubApiVersion())
                .build();
    }

    public GithubSearchResponse searchRepositories(String language, LocalDate createdAfter, int page, int size) {
        final var query = "language:%s created:>%s".formatted(language, createdAfter);

        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path(REPOSITORY_SEARCH_PATH)
                        .queryParam("q", query)
                        .queryParam("sort", properties.sort())
                        .queryParam("order", properties.order())
                        .queryParam("page", page)
                        .queryParam("per_page", size)
                        .build())
                .retrieve()
                .body(GithubSearchResponse.class);
    }
}
