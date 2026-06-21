package interview.willow.codechallenge.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GithubSearchResponse(
        @JsonProperty("total_count") long totalCount,
        @JsonProperty("incomplete_results") boolean incompleteResults,
        List<GithubRepository> items
) {
}
