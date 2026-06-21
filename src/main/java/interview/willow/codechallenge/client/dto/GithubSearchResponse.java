package interview.willow.codechallenge.client.dto;

import java.util.List;

public record GithubSearchResponse(

        List<GithubRepository> items
) {
}