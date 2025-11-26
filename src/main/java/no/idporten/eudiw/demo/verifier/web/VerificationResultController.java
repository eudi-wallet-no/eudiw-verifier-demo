package no.idporten.eudiw.demo.verifier.web;

import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import no.idporten.eudiw.demo.verifier.openid4vp.VerificationTransaction;
import no.idporten.eudiw.demo.verifier.openid4vp.VerificationTransactionService;
import no.idporten.eudiw.demo.verifier.openid4vp.VerifiedCredentials;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

@Controller
public class VerificationResultController {

    private final ConfigProvider configProvider;
    private final VerificationTransactionService verificationTransactionService;

    public VerificationResultController(ConfigProvider configProvider, VerificationTransactionService verificationTransactionService) {
        this.configProvider = configProvider;
        this.verificationTransactionService = verificationTransactionService;
    }

    @GetMapping("/response-result/{verifierTransactionId}")
    public String verificationResult(@PathVariable("verifierTransactionId") String verifierTransactionId, Model model) {
        VerificationTransaction verificationTransaction = verificationTransactionService.getVerificationTransaction(verifierTransactionId);
        VerifiedCredentials verifiedCredentials = verificationTransactionService.retrieveVerifiedCredentials(verifierTransactionId);
        MultiValueMap<String, Object> claims = new LinkedMultiValueMap<>();
        verifiedCredentials.credentials().forEach(claims::add);
        model.addAllAttributes(claims);
        model.addAttribute("traces", verificationTransaction.getProtocolTraces());
        String credentialConfigurationId = verificationTransaction.getCredentialConfiguration().getId();
        if ("alder".equals(credentialConfigurationId)) {
            return handleAlder(claims);
        }
        if ("forerkort".equals(credentialConfigurationId)) {
            return "forerkort/verify-result";
        }
        if ("inntekt".equals(credentialConfigurationId)) {
            return handleInntekt(claims, model);
        }
        model.addAttribute("claims", claims);
        model.addAttribute("credentialConfig", configProvider.getCredentialConfig(credentialConfigurationId));
        return "verify-result";
    }

    private static String handleAlder(MultiValueMap<String, Object> claims) {
        if (claims.containsKey("age_over_18") && (Boolean) claims.getFirst("age_over_18")) {
            return "alder/over18";
        } else {
            return "alder/under18";
        }
    }

    private String handleInntekt(MultiValueMap<String, Object> claims, Model model) {
        if (claims.containsKey("fastlonn")) {
            Map<String, Object> fastlonn = new TreeMap<>(Comparator.reverseOrder());
            fastlonn.putAll((Map) claims.get("fastlonn").getFirst());
            model.addAttribute("fastlonn", fastlonn);
            return "inntekt/verify-result";
        } else {
            return "/"; // ingenting delt, vis forside
        }
    }

}
