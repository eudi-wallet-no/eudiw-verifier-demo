package no.idporten.eudiw.demo.verifier.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.net.MalformedURLException;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Controller
public class IndexController {

    private final ConfigProvider configProvider;

    @GetMapping(value = "/")
    public String index(@RequestHeader Map<String, String> headers, HttpServletRequest request) {
        log.info("Index headers: {}", headers);
        log.info("Server name: {}", request.getServerName());
        // TODO loope gjennom config på en eller annen måte?
        if (request.getServerName().contains("demo-aldersverifisering")) {
            return "redirect:/verify/alder";
        }
        return "index";
    }

    @GetMapping(value = "/crl")
    public  String crl(@RequestHeader Map<String, String> headers, HttpServletRequest request) throws MalformedURLException {
        return "redirect:/files/root.crl.pem";
    }

}
