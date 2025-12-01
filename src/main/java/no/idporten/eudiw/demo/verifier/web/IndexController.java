package no.idporten.eudiw.demo.verifier.web;

import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    private final ConfigProvider configProvider;

    public IndexController(ConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @GetMapping(value = "/")
    public String index(Model model) {
        model.addAttribute("credentialTypes", configProvider.getCredentialConfigurations());
        return "index";
    }

}
