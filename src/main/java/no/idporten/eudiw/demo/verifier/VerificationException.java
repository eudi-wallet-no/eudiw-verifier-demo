package no.idporten.eudiw.demo.verifier;

public class VerificationException extends RuntimeException {

    private String error;
    private String errorDescription;

    public VerificationException(String errorCode, String errorMessage) {
        this(errorCode, errorMessage, null);
    }

    public VerificationException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.error = errorCode;
        this.errorDescription = errorMessage;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}
