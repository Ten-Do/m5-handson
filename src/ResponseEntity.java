import java.net.URI;

// Minimal stand-in for org.springframework.http.ResponseEntity.
public final class ResponseEntity<T> {
    private final int status;
    private final T body;
    private final URI location;

    private ResponseEntity(int status, T body, URI location) {
        this.status = status;
        this.body = body;
        this.location = location;
    }

    public static <T> ResponseEntity<T> ok(T body) { return new ResponseEntity<>(200, body, null); }
    public static <T> ResponseEntity<T> created(URI location, T body) { return new ResponseEntity<>(201, body, location); }
    public static <T> ResponseEntity<T> noContent() { return new ResponseEntity<>(204, null, null); }
    public static <T> ResponseEntity<T> badRequest() { return new ResponseEntity<>(400, null, null); }
    public static <T> ResponseEntity<T> notFound() { return new ResponseEntity<>(404, null, null); }
    public static <T> ResponseEntity<T> status(int status) { return new ResponseEntity<>(status, null, null); }

    public int getStatusCode() { return status; }
    public T getBody() { return body; }
    public URI getLocation() { return location; }
}
