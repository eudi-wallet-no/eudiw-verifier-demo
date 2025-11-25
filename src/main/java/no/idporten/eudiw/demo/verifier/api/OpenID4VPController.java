package no.idporten.eudiw.demo.verifier.api;

import jakarta.servlet.http.HttpServletRequest;
import no.idporten.eudiw.demo.verifier.VerificationException;
import no.idporten.eudiw.demo.verifier.openid4vp.OpenID4VPRequestService;
import no.idporten.eudiw.demo.verifier.openid4vp.OpenID4VPResponseService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API used by wallet to retrieve authz request and deliver response.
 */
@RestController
public class OpenID4VPController {

    private final OpenID4VPRequestService openID4VPRequestService;
    private final OpenID4VPResponseService openID4VPResponseService;

    public OpenID4VPController(OpenID4VPRequestService openID4VPRequestService, OpenID4VPResponseService openID4VPResponseService) {
        this.openID4VPRequestService = openID4VPRequestService;
        this.openID4VPResponseService = openID4VPResponseService;
    }

    /**
     * Retrieve authorization request by request_id.
     */
    @GetMapping(value = "/openid4vp/authz-request/{flow}/{request_id}", produces = "application/oauth-authz-req+jwt")
    public ResponseEntity<String> retrieveRequest(@PathVariable("flow") String flow, @PathVariable("request_id") String requestId) throws Exception {
        return ResponseEntity.ok(openID4VPRequestService.retrieveAuthorizationRequest(requestId, flow));
    }

    /**
     * Receive wallet response.
     */
    @PostMapping(value = "/openid4vp/authz-response/{verifier_transaction_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> receiveResponse(@PathVariable("verifier_transaction_id") String verifierTransactionId,
                                                  EncryptedAuthorizationResponse encryptedAuthorizationResponse,
                                                  HttpServletRequest request
                                                  ) throws Exception {
        if (encryptedAuthorizationResponse.getResponse() == null) {
            throw new VerificationException("invalid_request", "Empty authorization response");
        }
        return ResponseEntity.ok(openID4VPResponseService.receiveResponse(verifierTransactionId, encryptedAuthorizationResponse));
    }

}
