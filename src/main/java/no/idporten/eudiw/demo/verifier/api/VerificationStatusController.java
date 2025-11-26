package no.idporten.eudiw.demo.verifier.api;

import no.idporten.eudiw.demo.verifier.openid4vp.VerificationTransaction;
import no.idporten.eudiw.demo.verifier.openid4vp.VerificationTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * API for polling verification status.
 */
@RestController
public class VerificationStatusController {

    private static final Logger log = LoggerFactory.getLogger(VerificationStatusController.class);

    private final VerificationTransactionService verificationTransactionService;

    public VerificationStatusController(VerificationTransactionService verificationTransactionService) {
        this.verificationTransactionService = verificationTransactionService;
    }

    /**
     * Check status.
     * 204 - WAIT - keep checking
     * 200 - OK - stop checking, fetch result (cross device)
     * 200 - CLOSE - stop checking, fetch result (same device)
     */
    @RequestMapping(method = RequestMethod.GET, path = "/verification/status/{verifierTransactionId}")
    public ResponseEntity<String> verificationStatus(@PathVariable("verifierTransactionId") String verifierTransactionId) {
        VerificationTransaction verificationTransaction = verificationTransactionService.getVerificationTransaction(verifierTransactionId);
        if (verificationTransaction == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("WAIT");
        }
        if ("AVAILABLE".equals(verificationTransaction.getStatus())) {
            if ("same-device".equals(verificationTransaction.getFlow())) {
                return ResponseEntity.status(HttpStatus.OK).body("CLOSE");
            }
            return ResponseEntity.status(HttpStatus.OK).body("OK");
        } else {
            log.info("Continue polling for state {}", verifierTransactionId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("WAIT");
        }
    }

}
