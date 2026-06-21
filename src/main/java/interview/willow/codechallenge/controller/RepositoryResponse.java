package interview.willow.codechallenge.controller;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RepositoryResponse", description = "Repository details with calculated popularity score")
public record RepositoryResponse(

        @Schema(description = "Repository name", example = "spring-ai")
        String name,

        @Schema(description = "Repository owner", example = "spring-projects")
        String owner,

        @Schema(description = "Number of GitHub stars", example = "5200")
        int stars,

        @Schema(description = "Number of GitHub forks", example = "1400")
        int forks,

        @Schema(description = "Last update timestamp", example = "2026-06-10T14:15:22Z")
        String lastUpdated,

        @Schema(description = "Calculated popularity score", example = "119.83")
        Double popularityScore

) {
}
