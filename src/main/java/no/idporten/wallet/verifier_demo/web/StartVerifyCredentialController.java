package no.idporten.wallet.verifier_demo.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.wallet.verifier_demo.config.ConfigProvider;
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

    private final ConfigProvider configProvider;

    @GetMapping(value = "/verify/{type}")
    public String startVerifyAge(Model model, HttpSession session, @PathVariable("type") String type, @RequestHeader Map<String, String> headers, HttpServletRequest request) {
        log.info("Index headers: {}", headers);
        log.info("Server name: {}", request.getServerName());
        log.info("Request URL: {}", request.getRequestURL());
        String state = UUID.randomUUID().toString();
        session.setAttribute("state", state);
        model.addAttribute("state", state);
        model.addAttribute("authzRequest", getQrcodeText(type, state));
        // TODO enklest om kommer fra config!
        model.addAttribute(("responseStatusUri"), builPolldUri(request.getRequestURL().toString(), "response-status", type).toString());
        model.addAttribute(("responseResultUri"), builPolldUri(request.getRequestURL().toString(), "response-result", type).toString());
        return type + "/index";
    }

    URI builPolldUri(String requestURL, String... paths) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(requestURL)
                .replacePath("");
        builder.pathSegment(paths);
        return builder.build().toUri();
    }


    private String getQrcodeText(String type, String state) {
        return "eudi-openid4vp://" +configProvider.getSiop2ClientId()
                +"?client_id="+configProvider.getSiop2ClientId()
                +"&request_uri="+configProvider.getExternalBaseUrl()+"/req/" + type + "/" + state;
    }

}
