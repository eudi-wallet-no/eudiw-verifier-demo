package no.idporten.eudiw.demo.verifier;

public class StatusCommunicationException extends RuntimeException {

    private String error;
    private String errorDescription;


    public StatusCommunicationException(String errorCode, String errorMessage) {
        super(errorMessage);
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
