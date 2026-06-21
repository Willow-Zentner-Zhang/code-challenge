package interview.willow.codechallenge.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties("github.api")
public record GithubApiProperties(
        @NotNull URI baseUrl,
        @NotBlank String gitHubApiVersion,
        String sort,
        String order
) {
    public GithubApiProperties {
        sort = sort == null || sort.isBlank() ? "stars" : sort;
        order = order == null || order.isBlank() ? "desc" : order;
    }
}
