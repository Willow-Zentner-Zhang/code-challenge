package interview.willow.codechallenge.controller;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "RepositoryPageResponse", description = "A page of repositories and its pagination metadata")
public record RepositoryPageResponse(
        List<RepositoryResponse> content,
        @Schema(description = "Current page number (1-based)", example = "1")
        int page,
        @Schema(description = "Requested number of repositories per page", example = "30")
        int size,
        @Schema(description = "Number of repositories in this page", example = "30")
        int numberOfElements,
        @Schema(description = "Total GitHub repositories matching the search", example = "250")
        long totalElements,
        @Schema(description = "Number of accessible pages", example = "9")
        int totalPages,
        @Schema(description = "Whether another page is available", example = "true")
        boolean hasNext,
        @Schema(description = "Whether GitHub reported incomplete search results", example = "false")
        boolean incompleteResults
) {
}
