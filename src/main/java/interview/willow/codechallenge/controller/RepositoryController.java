package interview.willow.codechallenge.controller;

import interview.willow.codechallenge.service.GithubRepositoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/repositories")
@Validated
@Tag(name = "GitHub Repositories", description = "Repository search and popularity scoring APIs")
public class RepositoryController {

    private final GithubRepositoryService service;

    public RepositoryController(GithubRepositoryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Search repositories", description = """
            Search GitHub repositories by language and creation date,
            calculate popularity scores, and return them sorted by score.
            """)
    public List<RepositoryResponse> repositories(

            @Parameter(description = "Programming language", example = "java")
            @RequestParam
            @NotBlank
            String language,

            @Parameter(description = "Repositories created after this date", example = "2025-01-01")
            @RequestParam
            @NotNull
            @PastOrPresent
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate createdAfter) {
        return service.getRepositories(language, createdAfter);
    }
}