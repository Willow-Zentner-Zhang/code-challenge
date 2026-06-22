package interview.willow.codechallenge.service;

import interview.willow.codechallenge.client.GithubApiClient;
import interview.willow.codechallenge.controller.RepositoryPageResponse;
import interview.willow.codechallenge.controller.RepositoryResponse;
import interview.willow.codechallenge.exception.PageOutOfRangeException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;

@Service
public class GithubRepositoryService {

    /** GitHub Search only makes the first 1,000 results of a query accessible. */
    private static final int MAXIMUM_ACCESSIBLE_RESULTS = 1_000;

    private final GithubApiClient githubApiClient;
    private final PopularityScoreCalculator scoreCalculator;

    public GithubRepositoryService(final GithubApiClient githubApiClient,
                                   final PopularityScoreCalculator scoreCalculator) {
        this.githubApiClient = githubApiClient;
        this.scoreCalculator = scoreCalculator;
    }

    @Cacheable(cacheNames = "repositories", sync = true)
    public RepositoryPageResponse getRepositories(final String language, final LocalDate createdAfter,
                                                   final int page, final int size) {

        final var maximumGithubPage = (int) Math.ceil((double) MAXIMUM_ACCESSIBLE_RESULTS / size);
        if (page > maximumGithubPage) {
            throw new PageOutOfRangeException(page, maximumGithubPage);
        }

        final var searchResponse = githubApiClient.searchRepositories(language, createdAfter, page, size);
        final var repositories = searchResponse
                .items()
                .stream()
                .map(repo -> new RepositoryResponse(repo.name(),
                        repo.owner().login(),
                        repo.stars(),
                        repo.forks(),
                        repo.updatedAt(),
                        scoreCalculator.calculate(repo.stars(),
                                repo.forks(),
                                Instant.parse(repo.updatedAt()))))
                .sorted(Comparator.comparing(RepositoryResponse::popularityScore)
                        .reversed())
                .toList();

        final var accessibleElements = Math.min(searchResponse.totalCount(), MAXIMUM_ACCESSIBLE_RESULTS);
        final var totalPages = (int) Math.ceil((double) accessibleElements / size);
        final var lastAvailablePage = Math.max(totalPages, 1);
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

}
