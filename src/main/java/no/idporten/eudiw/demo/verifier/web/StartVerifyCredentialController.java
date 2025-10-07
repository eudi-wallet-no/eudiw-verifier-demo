package no.idporten.eudiw.demo.verifier.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import no.idporten.eudiw.demo.verifier.config.CredentialConfig;
import no.idporten.eudiw.demo.verifier.service.CacheService;
import no.idporten.eudiw.demo.verifier.service.OID4VPRequestService;
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

    private final ConfigProvider configProvider;
    private final OID4VPRequestService oid4VPRequestService;
    private final CacheService cacheService;

    @GetMapping(value = "/verify/{type}")
    public String startVerify(Model model, HttpSession session, @PathVariable("type") String type, @RequestHeader Map<String, String> headers, HttpServletRequest request) throws Exception {
        log.info("Index headers: {}", headers);
        log.info("Server name: {}", request.getServerName());
        log.info("Request URL: {}", request.getRequestURL());
        CredentialConfig credentialConfig = configProvider.getCredentialConfig(type);
        String state = UUID.randomUUID().toString();
        session.setAttribute("state", state);
        model.addAttribute("state", state);
        model.addAttribute("authzRequest", oid4VPRequestService.getAuthorizationRequest(type, state));
        Map<String, Object> authRequestClaimsSet = oid4VPRequestService.makeRequestJwt(type, state).getJWTClaimsSet().toJSONObject();
        List<ProtocolTrace> protocolTraceList = List.of(
                new UriTrace("authzRequest", "Authorization request", URI.create(oid4VPRequestService.getAuthorizationRequest(type, state))),
                new JsonTrace("authzRequestJwt", " JWT-Secured Authorization Request Body", authRequestClaimsSet),
                new JsonTrace("dcqlQuery", "DCQL query", (JSONObject) authRequestClaimsSet.get("dcql_query"))
        );
        model.addAttribute("traces", protocolTraceList);
        model.addAttribute(("responseStatusUri"), builPolldUri(request.getRequestURL().toString(), "response-status", type, state).toString());
        String responseResultUri = builPolldUri(request.getRequestURL().toString(), "response-result", type, state).toString();
        model.addAttribute(("responseResultUri"), responseResultUri);
        model.addAttribute("credentialConfig", credentialConfig);
        cacheService.addRUri(state, responseResultUri);
        return "start-verify";
    }

    URI builPolldUri(String requestURL, String... paths) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(requestURL)
                .replacePath("");
        builder.pathSegment(paths);
        return builder.build().toUri();
    }

}
