package no.idporten.eudiw.demo.verifier;

import lombok.Getter;

@Getter
public class VerificationException extends RuntimeException {

    private String error;
    private String errorDescription;

    public VerificationException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.error = errorCode;
        this.errorDescription = errorMessage;
    }

}
