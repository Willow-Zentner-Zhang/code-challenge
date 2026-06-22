package interview.willow.codechallenge.service;

import interview.willow.codechallenge.client.GithubApiClient;
import interview.willow.codechallenge.client.dto.GithubRepository;
import interview.willow.codechallenge.client.dto.GithubSearchResponse;
import interview.willow.codechallenge.exception.PageOutOfRangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GithubRepositoryServiceTest {

    private GithubApiClient client;
    private GithubRepositoryService service;

    @BeforeEach
    void setUp() {
        client = mock(GithubApiClient.class);
        final var clock = Clock.fixed(Instant.parse("2026-06-22T00:00:00Z"), ZoneOffset.UTC);
        service = new GithubRepositoryService(client, new PopularityScoreCalculator(clock));
    }

    @Test
    void returnsPaginationMetadataAndForwardsPageRequest() {
        final var createdAfter = LocalDate.of(2025, 1, 1);
        final var lowerScore = repository("second", 10, 2);
        final var higherScore = repository("first", 100, 20);

        when(client.searchRepositories("java", createdAfter, 2, 100))
                .thenReturn(new GithubSearchResponse(1_500, false, List.of(lowerScore, higherScore)));

        final var result = service.getRepositories("java", createdAfter, 2, 100);

        verify(client).searchRepositories("java", createdAfter, 2, 100);
        assertThat(result.content()).extracting("name").containsExactly("first", "second");
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(100);
        assertThat(result.numberOfElements()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(1_500);
        assertThat(result.totalPages()).isEqualTo(10);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.incompleteResults()).isFalse();
    }

    @Test
    void rejectsPageOutsideGithubSearchLimitBeforeCallingGithub() {
        final var createdAfter = LocalDate.of(2025, 1, 1);

        assertThatThrownBy(() -> service.getRepositories("java", createdAfter, 11, 100))
                .isInstanceOf(PageOutOfRangeException.class)
                .hasMessage("Page 11 is outside the allowed range. The last available page is 10.");
        verify(client, never()).searchRepositories("java", createdAfter, 11, 100);
    }

    @Test
    void rejectsPageAfterLastMatchingResultPage() {
        final var createdAfter = LocalDate.of(2025, 1, 1);

        when(client.searchRepositories("java", createdAfter, 3, 30))
                .thenReturn(new GithubSearchResponse(45, false, List.of()));

        assertThatThrownBy(() -> service.getRepositories("java", createdAfter, 3, 30))
                .isInstanceOf(PageOutOfRangeException.class)
                .hasMessage("Page 3 is outside the allowed range. The last available page is 2.");
    }

    @Test
    void exposesAnEmptyFirstPageWhenThereAreNoMatches() {
        final var createdAfter = LocalDate.of(2025, 1, 1);

        when(client.searchRepositories("java", createdAfter, 1, 30))
                .thenReturn(new GithubSearchResponse(0, true, List.of()));

        final var result = service.getRepositories("java", createdAfter, 1, 30);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalPages()).isZero();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.incompleteResults()).isTrue();
    }

    @Test
    void propagatesMalformedGithubDataForTheGlobalHandler() {
        final var createdAfter = LocalDate.of(2025, 1, 1);
        final var malformed = new GithubRepository("broken", 1, 1, "not-an-instant",
                new GithubRepository.Owner("owner"));

        when(client.searchRepositories("java", createdAfter, 1, 30))
                .thenReturn(new GithubSearchResponse(1, false, List.of(malformed)));

        assertThatThrownBy(() -> service.getRepositories("java", createdAfter, 1, 30))
                .isInstanceOf(DateTimeParseException.class);
    }

    private GithubRepository repository(final String name, final int stars, final int forks) {
        return new GithubRepository(
                name,
                stars,
                forks,
                "2026-06-20T00:00:00Z",
                new GithubRepository.Owner("owner")
        );
    }
}
