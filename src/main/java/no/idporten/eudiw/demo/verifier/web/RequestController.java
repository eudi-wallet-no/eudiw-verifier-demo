package no.idporten.eudiw.demo.verifier.web;

import com.nimbusds.jwt.JWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.demo.verifier.service.OID4VPRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequiredArgsConstructor
@Slf4j
public class RequestController {

    private final OID4VPRequestService oid4VPRequestService;


    @ResponseBody
    //@GetMapping(value = { "/req", "/request" }, produces = { "application/oauth-authz-req+jwt", "plain/text"})
    @GetMapping(value = {"/req/{type}/{state}", "/request/{state}"})
    public String request(HttpServletRequest request, HttpSession session, @PathVariable("type") String type, @PathVariable("state") String state) throws Exception {
        log.info("Authorization request was requested from {}", request.getRemoteHost());
        JWT authorizationRequest = oid4VPRequestService.makeRequestJwt(type, state);
        String serializedAuthorizationRequest = authorizationRequest.serialize();
        log.info("Authorization request; {}", serializedAuthorizationRequest);
        return serializedAuthorizationRequest;
    }


}
