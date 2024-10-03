package no.idporten.wallet.verifier_demo.web;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import no.idporten.wallet.verifier_demo.config.ConfigProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.UUID;

@RequiredArgsConstructor
@Controller
public class IndexController {

    private final ConfigProvider configProvider;

    @GetMapping("/")
    public String demo(Model model, HttpSession session) {
        String state = UUID.randomUUID().toString();
        model.addAttribute("state", state);
        model.addAttribute("authzRequest", getQrcodeText(state));
        model.addAttribute(("responseStatusUri"), configProvider.getExternalBaseUrl() + "/response-status");
        model.addAttribute(("responseResultUri"), configProvider.getExternalBaseUrl() + "/response-result");
        session.setAttribute("state", state);
        return "index";
    }

    private String getQrcodeText(String state) {
        return "eudi-openid4vp://" +configProvider.getSiop2ClientId()
                +"?client_id="+configProvider.getSiop2ClientId()
                +"&request_uri="+configProvider.getExternalBaseUrl()+"/req/" + state;
    }

}
