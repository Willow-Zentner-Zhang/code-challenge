package interview.willow.codechallenge.exception;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesPageOutOfRange() {
        final var response = handler.handlePageOutOfRange(new PageOutOfRangeException(11, 10));

        assertError(response, HttpStatus.BAD_REQUEST, "Page out of range",
                "Page 11 is outside the allowed range. The last available page is 10.");
    }

    @Test
    void handlesConstraintViolation() {
        final var exception = mock(ConstraintViolationException.class);
        when(exception.getMessage()).thenReturn("size: must be less than or equal to 100");

        final var response = handler.handleConstraintViolation(exception);

        assertError(response, HttpStatus.BAD_REQUEST, "Validation failed",
                "size: must be less than or equal to 100");
    }

    @Test
    void handlesTypeMismatchWithKnownRequiredType() {
        final var parameter = mock(MethodParameter.class);
        final var exception = new MethodArgumentTypeMismatchException(
                "tomorrow", java.time.LocalDate.class, "createdAfter", parameter, null);

        final var response = handler.handleTypeMismatch(exception);

        assertError(response, HttpStatus.BAD_REQUEST, "Type mismatch",
                "Invalid value 'tomorrow' for parameter 'createdAfter'. Expected type: LocalDate");
    }

    @Test
    void handlesTypeMismatchWhenRequiredTypeIsUnavailable() {
        final var parameter = mock(MethodParameter.class);
        final var exception = new MethodArgumentTypeMismatchException(
                "bad", null, "value", parameter, null);

        final var response = handler.handleTypeMismatch(exception);

        assertError(response, HttpStatus.BAD_REQUEST, "Type mismatch",
                "Invalid value 'bad' for parameter 'value'. Expected type: unknown");
    }

    @Test
    void handlesUnreadableMessageUsingTheRootCause() {
        final var exception = mock(HttpMessageNotReadableException.class);
        final var cause = new IllegalArgumentException("Unexpected character at column 4");
        when(exception.getMostSpecificCause()).thenReturn(cause);

        final var response = handler.handleUnreadable(exception);

        assertError(response, HttpStatus.BAD_REQUEST, "Malformed request", cause.getMessage());
    }

    @Test
    void handlesUnexpectedExceptions() {
        final var response = handler.handleGeneric(new IllegalStateException("GitHub is unavailable"));

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "GitHub is unavailable");
    }

    private void assertError(final org.springframework.http.ResponseEntity<?> response,
                             final HttpStatus status, final String error, final String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody())
                .asInstanceOf(MAP)
                .containsEntry("status", status.value())
                .containsEntry("error", error)
                .containsEntry("message", message)
                .containsKey("timestamp");
    }
}
