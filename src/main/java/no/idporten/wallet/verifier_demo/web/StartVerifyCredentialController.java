package no.idporten.wallet.verifier_demo.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.wallet.verifier_demo.service.CacheService;
import no.idporten.wallet.verifier_demo.service.OID4VPRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Controller
public class StartVerifyCredentialController {

    private final OID4VPRequestService OID4VPRequestService;
    private final CacheService cacheService;

    @GetMapping(value = "/verify/{type}")
    public String startVerifyAge(Model model, HttpSession session, @PathVariable("type") String type, @RequestHeader Map<String, String> headers, HttpServletRequest request) {
        log.info("Index headers: {}", headers);
        log.info("Server name: {}", request.getServerName());
        log.info("Request URL: {}", request.getRequestURL());
        String state = UUID.randomUUID().toString();
        session.setAttribute("state", state);
        model.addAttribute("state", state);
        model.addAttribute("authzRequest", OID4VPRequestService.getAuthorizationRequest(type, state));
        // TODO enklest om kommer fra config!
        model.addAttribute(("responseStatusUri"), builPolldUri(request.getRequestURL().toString(), "response-status", type, state).toString());
        String responseResultUri = builPolldUri(request.getRequestURL().toString(), "response-result", type, state).toString();
        model.addAttribute(("responseResultUri"), responseResultUri);
        cacheService.addRUri(state, responseResultUri);
        return type + "/index";
    }

    URI builPolldUri(String requestURL, String... paths) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(requestURL)
                .replacePath("");
        builder.pathSegment(paths);
        return builder.build().toUri();
    }

}
