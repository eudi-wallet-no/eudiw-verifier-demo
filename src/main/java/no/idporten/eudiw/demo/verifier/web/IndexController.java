package no.idporten.eudiw.demo.verifier.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import no.idporten.eudiw.demo.verifier.config.LanguageSupportConfig;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

@Controller
public class IndexController {

    private final ConfigProvider configProvider;
    private final LocaleResolver localeResolver;

    public IndexController(ConfigProvider configProvider, LocaleResolver localeResolver) {
        this.configProvider = configProvider;
        this.localeResolver = localeResolver;
    }

    @GetMapping(value = "/")
    public String index(@RequestParam(name = "lang", required = false) String lang, Model model, HttpServletRequest request, HttpServletResponse response) {
        if (lang != null) {
            Locale locale = Locale.of(lang);
            localeResolver.setLocale(request, response, LanguageSupportConfig.SUPPORTED_LOCALES.contains(locale) ? locale : LanguageSupportConfig.DEFAULT_LOCALE);
        }
        model.addAttribute("credentialTypes", configProvider.getCredentialConfigurations());
        return "index";
    }

}
