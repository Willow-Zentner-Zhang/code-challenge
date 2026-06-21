package interview.willow.codechallenge.service;

import interview.willow.codechallenge.client.GithubApiClient;
import interview.willow.codechallenge.controller.RepositoryPageResponse;
import interview.willow.codechallenge.controller.RepositoryResponse;
import interview.willow.codechallenge.exception.PageOutOfRangeException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class GithubRepositoryService {

    private final GithubApiClient githubApiClient;

    public GithubRepositoryService(GithubApiClient githubApiClient) {

        this.githubApiClient = githubApiClient;
    }

    @Cacheable("repositories")
    public RepositoryPageResponse getRepositories(final String language, final LocalDate createdAfter,
                                                   final int page, final int size) {

        final int maximumGithubPage = (int) Math.ceil(1_000.0 / size);
        if (page > maximumGithubPage) {
            throw new PageOutOfRangeException(page, maximumGithubPage);
        }

        final var searchResponse = githubApiClient.searchRepositories(language, createdAfter, page, size);
        final List<RepositoryResponse> repositories = searchResponse
                .items()
                .stream()
                .map(repo -> new RepositoryResponse(repo.name(),
                        repo.owner().login(),
                        repo.stars(),
                        repo.forks(),
                        repo.updatedAt(),
                        calculateScore(repo.stars(),
                                repo.forks(),
                                Instant.parse(repo.updatedAt()))))
                .sorted(Comparator.comparing(RepositoryResponse::popularityScore)
                        .reversed())
                .toList();

        // GitHub Search exposes at most the first 1,000 results for any query.
        final long accessibleElements = Math.min(searchResponse.totalCount(), 1_000);
        final int totalPages = (int) Math.ceil((double) accessibleElements / size);
        final int lastAvailablePage = Math.max(totalPages, 1);
        if (page > lastAvailablePage) {
            throw new PageOutOfRangeException(page, lastAvailablePage);
        }

        return new RepositoryPageResponse(
                repositories,
                page,
                size,
                repositories.size(),
                searchResponse.totalCount(),
                totalPages,
                page < totalPages,
                searchResponse.incompleteResults()
        );
    }

    private double calculateScore(int stars, int forks, Instant updatedAt) {

        return stars + forks - Duration.between(updatedAt, Instant.now()).toDays();
    }
}
