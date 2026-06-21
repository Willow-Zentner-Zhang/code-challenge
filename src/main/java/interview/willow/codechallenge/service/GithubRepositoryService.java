package interview.willow.codechallenge.service;

import interview.willow.codechallenge.client.GithubApiClient;
import interview.willow.codechallenge.controller.RepositoryResponse;
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
    public List<RepositoryResponse> getRepositories(final String language, final LocalDate createdAfter) {

        return githubApiClient.searchRepositories(language, createdAfter)
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
    }

    private double calculateScore(int stars, int forks, Instant updatedAt) {

        return stars + forks - Duration.between(updatedAt, Instant.now()).toDays();
    }
}
