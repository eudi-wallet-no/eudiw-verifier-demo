package no.idporten.eudiw.demo.verifier.web;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import no.idporten.eudiw.demo.verifier.openid4vp.VerificationTransaction;
import no.idporten.eudiw.demo.verifier.openid4vp.VerificationTransactionService;
import no.idporten.eudiw.demo.verifier.openid4vp.VerifiedCredentials;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

@RequiredArgsConstructor
@Slf4j
@Controller
public class ResponseStatusPollController {

    private final ConfigProvider configProvider;
    private final VerificationTransactionService verificationTransactionService;

    @RequestMapping(method = RequestMethod.GET, path = "/response-status/{type}/{verifierTransactionId}")
    public ResponseEntity<String> pollStatus(@PathVariable("type") String type, @PathVariable("verifierTransactionId") String verifierTransactionId, HttpSession session) {
//        String state = (String) session.getAttribute("state");
        if (verifierTransactionId == null) {
            log.warn("No state in session {}", session.getId());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("WAIT");
        }
        VerificationTransaction verificationTransaction = verificationTransactionService.getVerificationTransaction(verifierTransactionId);
        if (verificationTransaction == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("WAIT");
        }
        if ("AVAILABLE".equals(verificationTransaction.getStatus())) {
            // same device = CLOSE???
            return ResponseEntity.status(HttpStatus.OK).body("OK");
        } else {
            log.info("Continue polling for state {} in session {}", verifierTransactionId, session.getId());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("WAIT");
        }
    }

    @GetMapping("/response-result/{type}/{verifierTransactionId}")
    public String pollComplete(@PathVariable("type") String type, @PathVariable("verifierTransactionId") String verifierTransactionId, Model model) {
        VerificationTransaction verificationTransaction = verificationTransactionService.getVerificationTransaction(verifierTransactionId);
        VerifiedCredentials verifiedCredentials = verificationTransactionService.retrieveVerifiedCredentials(verifierTransactionId);
        MultiValueMap<String, Object> claims = new LinkedMultiValueMap<>();
        verifiedCredentials.getCredentials().forEach(claims::add);
        model.addAllAttributes(claims);
        model.addAttribute("traces", verificationTransaction.getProtocolTraces());
        if ("alder".equals(type)) {
            return handleAlder(claims);
        }
        if ("forerkort".equals(type)) {
            return "forerkort/verify-result";
        }
        if ("inntekt".equals(type)) {
            return handleInntekt(claims, model);
        }
        model.addAttribute("claims", claims);
        model.addAttribute("credentialConfig", configProvider.getCredentialConfig(type));
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
