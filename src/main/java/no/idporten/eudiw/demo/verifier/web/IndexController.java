package no.idporten.eudiw.demo.verifier.web;

import jakarta.servlet.http.HttpServletRequest;
import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@Controller
public class IndexController {

    private final ConfigProvider configProvider;

    public IndexController(ConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @GetMapping(value = "/")
    public String index(Model model, @RequestHeader Map<String, String> headers, HttpServletRequest request) {
        model.addAttribute("credentialTypes", configProvider.getCredentialConfigurations());
        return "index";
    }

}
