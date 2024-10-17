package no.idporten.wallet.verifier_demo.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.wallet.verifier_demo.config.ConfigProvider;

import org.springframework.stereotype.Controller;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        if ("demo-aldersverifisering.idporten.dev".equals(request.getServerName())) {
            return "redirect:/verify/alder";
        }
        if ("demo-fullmaktinnlogging.idporten.dev".equals(request.getServerName())) {
            return "redirect:/verify/fullmakt";
        }
        return "index";
    }

    @GetMapping(value = "/crl")
    public  String crl(@RequestHeader Map<String, String> headers, HttpServletRequest request) throws MalformedURLException {
        return "redirect:/files/root.crl.pem";
    }

}
