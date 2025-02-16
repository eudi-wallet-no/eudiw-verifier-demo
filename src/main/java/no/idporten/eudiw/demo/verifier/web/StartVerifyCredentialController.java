package no.idporten.eudiw.demo.verifier.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.demo.verifier.service.CacheService;
import no.idporten.wallet.verifier_demo.service.*;
import no.idporten.eudiw.demo.verifier.trace.JsonTrace;
import no.idporten.eudiw.demo.verifier.trace.ProtocolTrace;
import no.idporten.eudiw.demo.verifier.trace.UriTrace;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Controller
public class StartVerifyCredentialController {

    private final no.idporten.eudiw.demo.verifier.service.OID4VPRequestService OID4VPRequestService;
    private final CacheService cacheService;

    @GetMapping(value = "/verify/{type}")
    public String startVerify(Model model, HttpSession session, @PathVariable("type") String type, @RequestHeader Map<String, String> headers, HttpServletRequest request) throws Exception {
        log.info("Index headers: {}", headers);
        log.info("Server name: {}", request.getServerName());
        log.info("Request URL: {}", request.getRequestURL());
        String state = UUID.randomUUID().toString();
        session.setAttribute("state", state);
        model.addAttribute("state", state);
        model.addAttribute("authzRequest", OID4VPRequestService.getAuthorizationRequest(type, state));
        List<ProtocolTrace> protocolTraceList = List.of(
                new UriTrace("authzRequest", "Authorization request", URI.create(OID4VPRequestService.getAuthorizationRequest(type, state))),
                new JsonTrace("presentationRequest", "Presentation request", OID4VPRequestService.makeRequestJwt(type, state).getJWTClaimsSet().toJSONObject())
        );
        model.addAttribute("traces", protocolTraceList);

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
