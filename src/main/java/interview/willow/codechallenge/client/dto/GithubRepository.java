package interview.willow.codechallenge.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubRepository(

        String name,
        @JsonProperty("stargazers_count") Integer stars,
        Integer forks,
        @JsonProperty("updated_at") String updatedAt,
        Owner owner
) {
    public record Owner(String login) {
    }
}
