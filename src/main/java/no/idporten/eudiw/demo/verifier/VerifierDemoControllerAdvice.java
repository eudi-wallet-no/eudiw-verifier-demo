package no.idporten.eudiw.demo.verifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class VerifierDemoControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(VerifierDemoControllerAdvice.class);

    @ExceptionHandler(VerificationException.class)
    public ResponseEntity<ErrorResponse> handleException(VerificationException e) {
        log.warn("Verification transaction failed", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getError(), e.getErrorDescription()));
    }

}
