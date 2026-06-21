package interview.willow.codechallenge.exception;

public class PageOutOfRangeException extends RuntimeException {

    public PageOutOfRangeException(int requestedPage, int lastPage) {
        super("Page %d is outside the allowed range. The last available page is %d."
                .formatted(requestedPage, lastPage));
    }
}
