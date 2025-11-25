package no.idporten.eudiw.demo.verifier.openid4vp;

import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Service
public class VerificationService {

    private final ConfigProvider verifierProxyProperties;
    private final OpenID4VPRequestService openID4VPRequestService;
    private final VerificationTransactionService verificationTransactionService;

    public VerificationService(ConfigProvider verifierProxyProperties, OpenID4VPRequestService openID4VPRequestService, VerificationTransactionService verificationTransactionService) {
        this.verifierProxyProperties = verifierProxyProperties;
        this.openID4VPRequestService = openID4VPRequestService;
        this.verificationTransactionService = verificationTransactionService;
    }

//    public StartVerificationResponse startVerification(@RequestBody StartVerificationRequest startVerificationRequest) throws Exception {
//        if (! verifierProxyProperties.getCredentialIssuers().contains(startVerificationRequest.credentialIssuer())) {
//            throw new VerificationException("invalid_request", "Unsupported credential issuer");
//        }
//        CredentialConfiguration credentialConfiguration = findCredentialConfiguration(startVerificationRequest);
//        if (credentialConfiguration == null) {
//            throw new VerificationException("invalid_request", "Unknown credential configuration");
//        }
//        String verifierTransactionId = UUID.randomUUID().toString();
//        URI requestUri = openID4VPRequestService.createAuthorizationRequest(credentialConfiguration, verifierTransactionId);
//        URI haipRequestUri = UriComponentsBuilder.fromUri(requestUri).scheme("haip-vp").build().toUri();
//        verificationTransactionService.initTransaction(verifierTransactionId, credentialConfiguration);
//        return new StartVerificationResponse(requestUri, haipRequestUri, verifierTransactionId);
//    }

//    public VerificationStatusResponse verifierStatus(String verifierTransactionId) {
//        return new VerificationStatusResponse(
//                verificationTransactionService.retrieveStatus(verifierTransactionId),
//                verifierTransactionId);
//    }
//
//    public VerificationResultResponse retrieveVerificationData(String verifierTransactionId) {
//        VerifiedCredentials verifiedCredentials = verificationTransactionService.retrieveVerifiedCredentials(verifierTransactionId);
//        return new VerificationResultResponse(verifierTransactionId, verifiedCredentials.vpToken(), verifiedCredentials.credentials());
//    }

}
