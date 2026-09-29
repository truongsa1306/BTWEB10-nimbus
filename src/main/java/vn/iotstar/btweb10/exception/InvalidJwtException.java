package vn.iotstar.btweb10.exception;

/**
 * Raised by JwtService for any token that cannot be trusted. The reason is used only to pick a
 * generic client message (GlobalExceptionHandler); the token itself is never put in the message.
 */
public class InvalidJwtException extends RuntimeException {

    public enum Reason { MALFORMED, UNSUPPORTED_ALGORITHM, INVALID_SIGNATURE, EXPIRED, INVALID_CLAIMS }

    private final Reason reason;

    public InvalidJwtException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public InvalidJwtException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
