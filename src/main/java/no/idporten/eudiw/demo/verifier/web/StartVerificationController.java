package no.idporten.eudiw.demo.verifier.web;

import jakarta.servlet.http.HttpServletRequest;
import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import no.idporten.eudiw.demo.verifier.config.CredentialConfig;
import no.idporten.eudiw.demo.verifier.openid4vp.OpenID4VPRequestService;
import no.idporten.eudiw.demo.verifier.openid4vp.VerificationTransactionService;
import no.idporten.eudiw.demo.verifier.trace.JsonTrace;
import no.idporten.eudiw.demo.verifier.trace.ProtocolTrace;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Start the verification process.
 */
@Controller
public class StartVerificationController {

    private final ConfigProvider configProvider;
    private final OpenID4VPRequestService openID4VPRequestService;
    private final VerificationTransactionService verificationTransactionService;

    public StartVerificationController(ConfigProvider configProvider, OpenID4VPRequestService openID4VPRequestService, VerificationTransactionService verificationTransactionService) {
        this.configProvider = configProvider;
        this.openID4VPRequestService = openID4VPRequestService;
        this.verificationTransactionService = verificationTransactionService;
    }

    @GetMapping(value = "/verify/{credentialConfigurationId}")
    public String startVerify(Model model, @PathVariable("credentialConfigurationId") String type, HttpServletRequest request) throws Exception {
        CredentialConfig credentialConfig = configProvider.getCredentialConfig(type);
        String verifierTransactionId = UUID.randomUUID().toString();
        List<ProtocolTrace> protocolTraceList = List.of(
                new JsonTrace("dcqlQuery", "DCQL Query", openID4VPRequestService.makeDCQLQuery(credentialConfig))
        );
        model.addAttribute("verifierTransactionId", verifierTransactionId);
        model.addAttribute("authorizationRequest", openID4VPRequestService.createAuthorizationRequest(credentialConfig, verifierTransactionId, "same-device"));
        model.addAttribute("traces", protocolTraceList);
        model.addAttribute(("responseStatusUri"), builPolldUri(request.getRequestURL().toString(), "verification", "status", verifierTransactionId).toString());
        String responseResultUri = builPolldUri(request.getRequestURL().toString(), "response-result", verifierTransactionId).toString();
        model.addAttribute(("responseResultUri"), responseResultUri);
        model.addAttribute("credentialConfig", credentialConfig);
        verificationTransactionService.initVerificationTransaction(verifierTransactionId, credentialConfig);
        return "start-verify";
    }

    private URI builPolldUri(String requestURL, String... paths) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(requestURL)
                .replacePath("");
        builder.pathSegment(paths);
        return builder.build().toUri();
    }

}
